package com.github.sanctum.myessentials.commands;

import com.github.sanctum.myessentials.model.CommandBuilder;
import com.github.sanctum.myessentials.model.CommandData;
import com.github.sanctum.myessentials.util.ConfiguredMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class FlyCommand extends CommandBuilder {
    public FlyCommand() {
        super(CommandData.FLY_COMMAND);
    }

    @Override
    public boolean consoleView(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] strings) {
        sendMessage(sender, ConfiguredMessage.MUST_BE_PLAYER);
        return true;
    }

    @Override
    public boolean playerView(Player p, @NotNull String commandLabel, @NotNull String[] args) {
        if (!testPermission(p)) { // automatically sends no-perm message
            return true;
        }
        final Player player = (Player) p;
        if (player.getGameMode() != GameMode.SURVIVAL) {
            sendMessage(p, ConfiguredMessage.TRY_IN_SURVIVAL);
            return true;
        }
        if (player.getAllowFlight()) {
            player.setFlying(false);
            player.setAllowFlight(false);
            sendMessage(player, ConfiguredMessage.FLIGHT_OFF);
            final Listener listener = new Listener() {
                @EventHandler
                public void onNextFallDamage(EntityDamageEvent e) {
                    if (e.getEntityType() != EntityType.PLAYER) {
                        return;
                    }
                    if (e.getCause() != EntityDamageEvent.DamageCause.FALL) {
                        return;
                    }
                    final Player checkPlayer = (Player) e.getEntity();
                    if (checkPlayer.equals(player)) {
                        e.setCancelled(true);
                    }
                    e.getHandlers().unregister(this);
                }
            };
            Bukkit.getServer().getPluginManager().registerEvents(listener, PLUGIN);
            new BukkitRunnable() { // If they haven't taken fall damage within 10 seconds cancel one-time immunity
                @Override
                public void run() {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            EntityDamageEvent.getHandlerList().unregister(listener);
                        }
                    }.runTask(PLUGIN);
                }
            }.runTaskLaterAsynchronously(PLUGIN, 200L);
        } else {
            player.setAllowFlight(true);
            player.setVelocity(player.getVelocity().add(new Vector(0d, 0.6, 0d)));
            sendMessage(player, ConfiguredMessage.FLIGHT_ON);
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.setFlying(true);
                }
            }.runTaskLater(PLUGIN, 1L);
        }
        return true;
    }
}
