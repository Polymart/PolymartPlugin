package org.polymart.mcplugin.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.polymart.mcplugin.Main;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PolymartPlaceholderAPIExpansion extends PlaceholderExpansion {

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "polymart";
    }

    @Override
    public String getAuthor() {
        return "Polymart";
    }

    @Override
    public String getVersion() {
        return Bukkit.getServer().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        String[] args = params.split("_");

        if (args.length == 0) {
            return null;
        }

        switch (args[0].toLowerCase()) {
            case "version":
                if (args.length != 2) {
                    return null;
                }
                Plugin otherPlugin = Bukkit.getServer().getPluginManager().getPlugin(args[1]);
                if (otherPlugin == null) {
                    return null;
                }
                return otherPlugin.getDescription().getVersion();
            case "description":
                if (args.length != 2) {
                    return null;
                }
                Plugin otherPlugin1 = Bukkit.getServer().getPluginManager().getPlugin(args[1]);
                if (otherPlugin1 == null) {
                    return null;
                }
                return otherPlugin1.getDescription().getDescription();
            case "author":
                if (args.length != 2) {
                    return null;
                }
                Plugin otherPlugin2 = Bukkit.getServer().getPluginManager().getPlugin(args[1]);
                if (otherPlugin2 == null) {
                    return null;
                }
                return String.join(", ", otherPlugin2.getDescription().getAuthors());
        }

        return null;
    }
}