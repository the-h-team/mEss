/*
 *  Copyright 2021 Sanctum <https://github.com/the-h-team>
 *
 *  This file is part of myEssentials.
 *
 *  This software is currently in development and its licensing has not
 *  yet been chosen.
 */
package com.github.sanctum.myessentials.listeners;

import com.github.sanctum.labyrinth.LabyrinthProvider;
import com.github.sanctum.labyrinth.data.Region;
import com.github.sanctum.labyrinth.event.custom.DefaultEvent;
import com.github.sanctum.labyrinth.event.custom.Subscribe;
import com.github.sanctum.labyrinth.library.AFK;
import com.github.sanctum.labyrinth.library.Cooldown;
import com.github.sanctum.labyrinth.library.Message;
import com.github.sanctum.labyrinth.library.StringUtils;
import com.github.sanctum.labyrinth.library.TimeWatch;
import com.github.sanctum.labyrinth.task.Schedule;
import com.github.sanctum.myessentials.api.MyEssentialsAPI;
import com.github.sanctum.myessentials.util.ConfiguredMessage;
import com.github.sanctum.myessentials.util.events.PendingTeleportEvent;
import com.github.sanctum.myessentials.util.events.PlayerFeedEvent;
import com.github.sanctum.myessentials.util.events.PlayerHealEvent;
import com.github.sanctum.myessentials.util.events.PlayerPendingFeedEvent;
import com.github.sanctum.myessentials.util.events.PlayerPendingHealEvent;
import com.github.sanctum.myessentials.util.moderation.KickReason;
import com.github.sanctum.myessentials.util.moderation.PlayerSearch;
import com.github.sanctum.myessentials.util.teleportation.TeleportRequest;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.BanEntry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

// TODO: cleanup class a little, were gonna have to make due with consolidation like "PlayerEvent" & "EntityEvent" & others
public class PlayerEventListener implements Listener {
	private final Map<UUID, Boolean> taskScheduled = new HashMap<>();
	private final AtomicReference<Location> teleportLocation = new AtomicReference<>();
	private final Plugin plugin = JavaPlugin.getProvidingPlugin(getClass());

	private static final Map<UUID, Location> prevLocations = new HashMap<>();

	void sendMessage(Player p, String text) {
		Message.form(p).send(text);
	}

	int random(int bounds) {
		return (int) (Math.random() * bounds * (Math.random() > 0.5 ? 1 : -1));
	}

	/**
	 * Checks if a location is safe (solid ground with 2 breathable blocks)
	 *
	 * @param location Location to check
	 * @return True if location is safe
	 */
	boolean hasSurface(Location location) {
		Block feet = location.getBlock();
		Block head = feet.getRelative(BlockFace.UP);
		if (!feet.getType().isAir() && !feet.getLocation().add(0, 1, 0).getBlock().getType().isAir() && !head.getType().isAir()) {
			return false; // not transparent (will suffocate)
		}
		return feet.getRelative(BlockFace.DOWN).getType().isSolid(); // not solid
	}


	@EventHandler
	public void onMove(PlayerMoveEvent e) {

		if (e.getTo() != null) {
			if (e.getFrom().getX() != e.getTo().getX() && e.getFrom().getY() != e.getTo().getY() && e.getFrom().getZ() != e.getTo().getZ()) {
				TeleportRequest r = MyEssentialsAPI.getInstance().getTeleportRunner().getActiveRequests()
						.stream().filter(pr -> pr.getPlayerTeleporting().getUniqueId().equals(e.getPlayer().getUniqueId()))
						.findFirst()
						.orElse(null);

				if (r != null) {
					if (r.getStatus() == TeleportRequest.Status.ACCEPTED) {
						MyEssentialsAPI.getInstance().getTeleportRunner().cancelRequest(r);
						Message.form(e.getPlayer()).setPrefix(MyEssentialsAPI.getInstance().getPrefix()).send(ConfiguredMessage.TP_CANCELLED.get());
					}
				}

			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onTeleport(PendingTeleportEvent e) {
		if (!e.getDestination().getDestinationPlayer().isPresent()) return;
		Player p = e.getPlayerToTeleport();

		e.setDelay(0);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onLogin(PlayerLoginEvent e) {
		Player p = e.getPlayer();

		PlayerSearch search = PlayerSearch.look(p);
		Cooldown timer = search.getBanTimer("&r(D&r)&e{DAYS} &r(H&r)&e{HOURS} &r(M&r)&e{MINUTES} &r(S&r)&e{SECONDS}").orElse(null);
		if (timer != null) {
			if (!timer.isComplete()) {
				e.disallow(PlayerLoginEvent.Result.KICK_BANNED, KickReason.next()
						.input(1, MyEssentialsAPI.getInstance().getPrefix())
						.input(2, " ")
						.input(3, ConfiguredMessage.LOGIN_TEMP_BANNED.toString())
						.input(4, "")
						.input(5, ConfiguredMessage.LOGIN_BANNED_REASON.replace(search.getBanEntry().map(BanEntry::getReason).orElse("null")))
						.input(6, "")
						.input(7, ConfiguredMessage.LOGIN_BAN_EXPIRES.replace(timer.fullTimeLeft()))
						.toString());
			} else {
				PlayerSearch.look(p).unban();
				Cooldown.remove(timer);
				e.allow();
			}
		}

		if (e.getResult() == PlayerLoginEvent.Result.ALLOWED) {
			Schedule.sync(() -> {
				if (Region.spawn().isPresent()) {
					if (Region.spawn().get().contains(e.getPlayer())) {
						if (p.getLocation().getBlock().getType() == Material.ORANGE_CARPET) {
							if (!taskScheduled.containsKey(p.getUniqueId())) {
								taskScheduled.put(p.getUniqueId(), true);

								Schedule.sync(() -> {
									int x = random(10500);
									int z = random(3500);
									int y = 150;
									teleportLocation.set(new Location(p.getWorld(), x, y, z));
									y = Objects.requireNonNull(teleportLocation.get().getWorld()).getHighestBlockYAt(teleportLocation.get());
									teleportLocation.get().setY(y);
									Message.form(p).action(MyEssentialsAPI.getInstance().getPrefix() + " Searching for suitable location...");
									p.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 10, 1);

								}).cancelAfter(p).cancelAfter(task -> {
									if (taskScheduled.containsKey(p.getUniqueId()) && !taskScheduled.get(p.getUniqueId())) {
										sendMessage(p, ConfiguredMessage.SEARCH_INTERRUPTED.toString());
										task.cancel();
										return;
									}
									if (!taskScheduled.containsKey(p.getUniqueId())) {
										sendMessage(p, ConfiguredMessage.SEARCH_INTERRUPTED.toString());
										task.cancel();
										return;
									}
									if (teleportLocation.get() != null) {
										if (hasSurface(teleportLocation.get())) {
											p.teleport(teleportLocation.get());
											teleportLocation.set(null);
											sendMessage(p, ConfiguredMessage.TELEPORTED_SAFEST_LOCATION.replace(p.getWorld().getName()));
											taskScheduled.remove(p.getUniqueId());
											task.cancel();
										}
									}
								}).repeat(0, 3 * 20);
							}

						} else {
							if (taskScheduled.containsKey(p.getUniqueId()) && taskScheduled.get(p.getUniqueId())) {
								taskScheduled.remove(p.getUniqueId());
								sendMessage(p, ConfiguredMessage.STOPPING_SEARCH.toString());
							}
						}
					}
				}
			}).repeatReal(0, 2 * 20);
		}

	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onTeleport(PlayerTeleportEvent e) {
		if (!e.getFrom().equals(e.getTo())) {
			prevLocations.put(e.getPlayer().getUniqueId(), e.getPlayer().getLocation());
		}
	}

	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent e) {
		if (StringUtils.use(e.getMessage()).containsIgnoreCase("/plugins")) {
			e.getPlayer().performCommand("pl");
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
			if (e.getItem() != null) {
				ItemStack i = e.getItem();
				if (i.hasItemMeta()) {
					/*
					if (i.getItemMeta().getPersistentDataContainer().has(PowertoolCommand.KEY, PersistentDataType.STRING)) {
						e.setCancelled(true);
						String command = i.getItemMeta().getPersistentDataContainer().get(PowertoolCommand.KEY, PersistentDataType.STRING);
						Bukkit.dispatchCommand(e.getPlayer(), command);

					}

					 */
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onPendingHealEvent(PlayerPendingHealEvent e) {
		Bukkit.getPluginManager().callEvent(new PlayerHealEvent(e));
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onFinalHealEvent(PlayerHealEvent e) {
		final Player target = e.getTarget();
		final CommandSender healer = e.getHealer();
		double s = target.getHealth() + e.getAmount();
		e.getTarget().setHealth(s < 20 ? s : 20);
		if (healer != null) {
			if (healer instanceof Player) {
				Player heal = (Player) healer;
				Message.form(target).send(ConfiguredMessage.PLAYER_HEALED_YOU.replace(plugin, heal.getName()));
			} else {
				Message.form(target).send(ConfiguredMessage.CONSOLE_HEALED_YOU.replace(plugin));
			}
		} else {
			Message.form(target).send(ConfiguredMessage.HEALED.replace(plugin));
		}
	}


	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onPendingFeedEvent(PlayerPendingFeedEvent e) {
		Bukkit.getPluginManager().callEvent(new PlayerFeedEvent(e));
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onFinalHealEvent(PlayerFeedEvent e) {
		final Player target = e.getTarget();
		final CommandSender healer = e.getHealer();

		int food_level = target.getFoodLevel() + e.getAmountReal();

		food_level = Math.min(food_level, 20);
		e.getTarget().setFoodLevel(food_level);

		// Only set saturation if food is full
		if (food_level == 20) {
			e.getTarget().setSaturation(20);
		}

		if (healer != null) {
			if (healer instanceof Player) {
				Player heal = (Player) healer;
				Message.form(target).send(ConfiguredMessage.PLAYER_FED_YOU.replace(plugin, heal.getName()));
			} else {
				Message.form(target).send(ConfiguredMessage.CONSOLE_FED_YOU.replace(plugin));
			}
		} else {
			Message.form(target).send(ConfiguredMessage.FED.replace(plugin));
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onWorldChange(PlayerChangedWorldEvent e) {
		prevLocations.put(e.getPlayer().getUniqueId(), e.getPlayer().getLocation());
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onDeath(PlayerDeathEvent e) {
		prevLocations.put(e.getEntity().getUniqueId(), e.getEntity().getLocation());
	}

	public static Map<UUID, Location> getPrevLocations() {
		return Collections.unmodifiableMap(prevLocations);
	}


	public static AFK supply(Player player) {
		if (AFK.get(player) != null) {
			return AFK.get(player);
		}
		return AFK.Initializer.use(player)
				.next(LabyrinthProvider.getInstance().getPluginInstance())
				.next((e, subscription) -> {
					Player p = e.getAfk().getPlayer();
					switch (e.getStatus()) {
						case AWAY:
							TimeWatch.Recording recording = e.getAfk().getRecording();
							long minutes = recording.getMinutes();
							long seconds = recording.getSeconds();
							String format = "&cYou will be kicked in &4{0} &cseconds.";
							if (minutes == 14) {
								if (seconds == 50) {
									p.sendTitle(StringUtils.use("&eHey AFK person!").translate(), StringUtils.use(MessageFormat.format(format, 10)).translate(), 0, 12, 5);
								}
								if (seconds == 51) {
									p.sendTitle(StringUtils.use("&eHey AFK person!").translate(), StringUtils.use(MessageFormat.format(format, 9)).translate(), 0, 12, 5);
								}
								if (seconds == 52) {
									p.sendTitle(StringUtils.use("&eHey AFK person!").translate(), StringUtils.use(MessageFormat.format(format, 8)).translate(), 0, 12, 5);
								}
								if (seconds == 53) {
									p.sendTitle(StringUtils.use("&eHey AFK person!").translate(), StringUtils.use(MessageFormat.format(format, 7)).translate(), 0, 12, 5);
								}
								if (seconds == 54) {
									p.sendTitle(StringUtils.use("&eHey AFK person!").translate(), StringUtils.use(MessageFormat.format(format, 6)).translate(), 0, 12, 5);
								}
								if (seconds == 55) {
									p.sendTitle(StringUtils.use("&eHey AFK person!").translate(), StringUtils.use(MessageFormat.format(format, 5)).translate(), 0, 12, 5);
								}
								if (seconds == 56) {
									p.sendTitle(StringUtils.use("&eHey AFK person!").translate(), StringUtils.use(MessageFormat.format(format, 4)).translate(), 0, 12, 5);
								}
								if (seconds == 57) {
									p.sendTitle(StringUtils.use("&eHey AFK person!").translate(), StringUtils.use(MessageFormat.format(format, 3)).translate(), 0, 12, 5);
								}
								if (seconds == 58) {
									p.sendTitle(StringUtils.use("&eHey AFK person!").translate(), StringUtils.use(MessageFormat.format(format, 2)).translate(), 0, 12, 5);
								}
								if (seconds == 59) {
									p.sendTitle(StringUtils.use("&eHey AFK person!").translate(), StringUtils.use(MessageFormat.format(format, 1)).translate(), 0, 12, 5);
								}
							}
							break;
						case PENDING:
							Bukkit.broadcastMessage(StringUtils.use(MyEssentialsAPI.getInstance().getPrefix() + " &7Player &b" + p.getName() + " &7is now AFK").translate());
							p.setDisplayName(StringUtils.use("&7*AFK&r " + p.getDisplayName()).translate());
							e.getAfk().set(AFK.Status.AWAY);
							break;
						case RETURNING:
							p.setDisplayName(p.getName());
							Bukkit.broadcastMessage(StringUtils.use(MyEssentialsAPI.getInstance().getPrefix() + " &7Player &b" + p.getName() + " &7is no longer AFK").translate());
							e.getAfk().reset(AFK.Status.ACTIVE);
							break;
						case REMOVABLE:
							Bukkit.broadcastMessage(StringUtils.use(MyEssentialsAPI.getInstance().getPrefix() + " &c&oPlayer &b" + p.getName() + " &c&owas kicked for being AFK too long.").translate());
							p.kickPlayer(StringUtils.use(MyEssentialsAPI.getInstance().getPrefix() + "\n" + "&c&oAFK too long.\n&c&oKicking to ensure safety :)").translate());
							e.getAfk().remove();
							break;
						case CHATTING:
						case EXECUTING:
							e.getAfk().set(AFK.Status.RETURNING);
							break;
						case LEAVING:
							p.setDisplayName(p.getName());
							Bukkit.broadcastMessage(StringUtils.use(MyEssentialsAPI.getInstance().getPrefix() + " &7Player &b" + p.getName() + " &7is no longer AFK").translate());
							e.getAfk().remove();
							break;
					}
				})
				.finish();
	}


	@Subscribe
	public void afkInit(DefaultEvent.Join e) {
		Player p = e.getPlayer();
		if (supply(p) != null) {
			Message.form(p).setPrefix(MyEssentialsAPI.getInstance().getPrefix()).send("&eTry not to afk too long!");
		}

	}


}
