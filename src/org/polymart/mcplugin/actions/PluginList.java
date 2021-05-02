package org.polymart.mcplugin.actions;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class PluginList implements Listener{

    public static void list(Player p){
        List<Plugin> sorted = Arrays.asList(Bukkit.getPluginManager().getPlugins());
        sorted.sort(Comparator.comparing(Plugin::getName));

        for(Plugin pl : sorted){
            String v = pl.getDescription().getVersion();
        }
    }

}
