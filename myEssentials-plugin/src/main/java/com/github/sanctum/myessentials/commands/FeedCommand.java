package com.github.sanctum.myessentials.commands;

import com.github.sanctum.labyrinth.formatting.completion.SimpleTabCompletion;
import com.github.sanctum.labyrinth.formatting.completion.TabCompletionIndex;
import com.github.sanctum.myessentials.model.CommandBuilder;
import com.github.sanctum.myessentials.model.InternalCommandData;
import com.github.sanctum.myessentials.util.ConfiguredMessage;
import com.github.sanctum.myessentials.util.events.PlayerPendingFeedEvent;
import com.github.sanctum.myessentials.util.moderation.PlayerSearch;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeedCommand extends CommandBuilder {
	public FeedCommand() {
		super(InternalCommandData.FEED_COMMAND);
	}

	@Override
	public @Nullable List<String> tabComplete(@NotNull Player player, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
		return SimpleTabCompletion.of(args).then(TabCompletionIndex.ONE, Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()))
				.get();
	}

	@Override
	public boolean playerView(@NotNull Player player, @NotNull String commandLabel, @NotNull String[] args) {
		if (args.length == 0) {
			if (testPermission(player)) {
				Bukkit.getPluginManager().callEvent(new PlayerPendingFeedEvent(null, player, 20));
				return true;
			}
			return true;
		}

		if (args.length == 1) {
			PlayerSearch search = PlayerSearch.look(args[0]);
			if (search.isValid()) {
				if (search.isOnline()) {
					Player target = search.getPlayer();
					if (testPermission(player)) {
						assert target != null;
						search.feed(player, 20);
						sendMessage(player, ConfiguredMessage.HEAL_TARGET_MAXED.replace(target.getName()));
					}
				} else {
					if (testPermission(player)) {
						sendMessage(player, ConfiguredMessage.HEAL_TARGET_NOT_ONLINE.replace(search.getOfflinePlayer().getName()));
					}
				}
			} else {
				if (testPermission(player)) {
					sendMessage(player, ConfiguredMessage.TARGET_NOT_FOUND.replace(args[0]));
				}
			}
		}
		return true;
	}

	@Override
	public boolean consoleView(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
		return false;
	}
}