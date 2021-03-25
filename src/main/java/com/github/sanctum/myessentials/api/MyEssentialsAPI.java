/*
 *  Copyright 2021 Sanctum <https://github.com/the-h-team>
 *
 *  This file is part of myEssentials.
 *
 *  This software is currently in development and its licensing has not
 *  yet been chosen.
 */
package com.github.sanctum.myessentials.api;

import com.github.sanctum.labyrinth.data.FileList;
import com.github.sanctum.labyrinth.data.FileManager;
import com.github.sanctum.myessentials.Essentials;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public interface MyEssentialsAPI {

    static MyEssentialsAPI getInstance() {
        MyEssentialsAPI api = Bukkit.getServicesManager().load(MyEssentialsAPI.class);
        return api != null ? api : Essentials.getInstance();
    }

    /**
     * Get data for all commands registered by MyEssentials.
     *
     * @return set of data for all registered commands
     */
    Set<CommandData> getRegisteredCommands();

    FileList getFileList();

    Location getPreviousLocation(UUID id);

    FileManager getAddonFile(String name, String directory);

}
