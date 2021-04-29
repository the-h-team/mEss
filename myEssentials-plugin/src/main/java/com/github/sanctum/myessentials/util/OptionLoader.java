package com.github.sanctum.myessentials.util;

import com.github.sanctum.labyrinth.data.FileManager;
import com.github.sanctum.labyrinth.gui.InventoryRows;
import com.github.sanctum.myessentials.Essentials;
import com.github.sanctum.myessentials.api.MyEssentialsAPI;
import com.github.sanctum.myessentials.util.moderation.PlayerSearch;
import java.io.InputStream;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

public enum OptionLoader {
	SPECIFIED, SILENT_KICK, SILENT_BAN, GUI_SINGLE_SIZE, GUI_SCALED_SIZE, GROUP_COLOR, GROUP_PREFIX;

	private static final FileManager CONFIG = MyEssentialsAPI.getInstance().getFileList().find("config", "Configuration");
	private static final FileConfiguration SEARCH = CONFIG.getConfig();

	public boolean enabled() {
		boolean result = false;
		switch (this) {
			case SILENT_BAN:
				result = SEARCH.getBoolean("Procedures.moderation.silent-ban");
				break;
			case SILENT_KICK:
				result = SEARCH.getBoolean("Procedures.moderation.silent-kick");
				break;
		}
		return result;
	}

	public Object get() {
		return "";
	}

	public String getString() {
		return null;
	}

	public int getInt() {
		int result = 9;
		InventoryRows rows;
		switch (this) {
			case GUI_SCALED_SIZE:
				rows = InventoryRows.valueOf(SEARCH.getString("Procedure.gui.scaled-size"));
				result = rows.getSlotCount();
				break;
			case GUI_SINGLE_SIZE:
				rows = InventoryRows.valueOf(SEARCH.getString("Procedure.gui.single-size"));
				result = rows.getSlotCount();
				break;
		}
		return result;
	}

	public double getDouble() {
		double result = 9;
		InventoryRows rows;
		switch (this) {
			case GUI_SCALED_SIZE:
				rows = InventoryRows.valueOf(SEARCH.getString("Procedure.gui.scaled-size"));
				result = rows.getSlotCount();
				break;
			case GUI_SINGLE_SIZE:
				rows = InventoryRows.valueOf(SEARCH.getString("Procedure.gui.single-size"));
				result = rows.getSlotCount();
				break;
		}
		return result;
	}

	public Object get(String path) {
		Object result = null;
		switch (this) {

			case SPECIFIED:
				result = SEARCH.get(path);
				break;
			case SILENT_KICK:
			case SILENT_BAN:
				result = enabled();
				break;
			case GUI_SINGLE_SIZE:
			case GUI_SCALED_SIZE:
				result = getInt();
				break;
		}
		return result;
	}

	public String getString(String path) {
		String result = "";
		switch (this) {

			case SPECIFIED:
				result = SEARCH.getString(path);
				break;
			case SILENT_KICK:
				break;
			case SILENT_BAN:
				break;
			case GUI_SINGLE_SIZE:
				break;
			case GUI_SCALED_SIZE:
				break;
			case GROUP_COLOR:
				result = SEARCH.isString("Format.groups." + path + ".color") ? SEARCH.getString("Format.groups." + path + ".color") : "&f";
				break;
			case GROUP_PREFIX:
				result = SEARCH.isString("Format.groups." + path + ".prefix") ? SEARCH.getString("Format.groups." + path + ".prefix") : "&7[&fDefault&7]";
				break;
		}
		return result;
	}

	public int getInt(String path) {
		int result = 0;
		switch (this) {

			case SPECIFIED:
				result = SEARCH.getInt(path);
				break;
			case SILENT_KICK:
				break;
			case SILENT_BAN:
				break;
			case GUI_SINGLE_SIZE:
				break;
			case GUI_SCALED_SIZE:
				break;
		}
		return result;
	}

	public double getDouble(String path) {
		double result = 0.0;
		switch (this) {

			case SPECIFIED:
				result = SEARCH.getDouble(path);
				break;
			case SILENT_KICK:
				break;
			case SILENT_BAN:
				break;
			case GUI_SINGLE_SIZE:
				break;
			case GUI_SCALED_SIZE:
				break;
		}
		return result;
	}

	public static void recordRemainingBans() {
		for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
			PlayerSearch search = PlayerSearch.look(player);
			if (search.getBanTimer() != null) {
				// Update the timer to the current remaining time in hard storage
				search.getBanTimer().update();
			}
		}
	}

	public static void renewRemainingBans() {
		for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
			PlayerSearch search = PlayerSearch.look(player);
			// Simply making the call to the timer will check hard storage and create a new cached instance based off
			// the remaining time of the previous usage.
			search.getBanTimer();
		}
	}

	public static void checkConfig() {
		if (!CONFIG.exists()) {
			InputStream copy = Essentials.getInstance().getResource("config.yml");
			assert copy != null;
			FileManager.copy(copy, CONFIG.getFile());
			CONFIG.reload();
		}
	}

}
