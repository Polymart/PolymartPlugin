package org.polymart.mcplugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.polymart.mcplugin.Main;
import org.polymart.mcplugin.actions.Login;
import org.polymart.mcplugin.actions.Logout;
import org.polymart.mcplugin.actions.Search;
import org.polymart.mcplugin.actions.UpdateCheck;
import org.polymart.mcplugin.api.PolymartAccount;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.polymart.mcplugin.commands.MessageUtils.*;

public class UpdateCommand implements TabExecutor {

    public static final List<String> TABCOMPLETE_ARGUMENTS = Arrays.asList("help", "all");
    //private long confirmingLogout = 0;

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if(cmd.getName().equalsIgnoreCase("update")){
            if(args.length == 1 && args[0].toLowerCase().matches("^(help|\\?)$")){
                sendMessage(sender, "Use " + ChatColor.AQUA + "/update" + ChatColor.WHITE + " to see a list of plugins with updates available");
                sendMessage(sender, "Use " + ChatColor.AQUA + "/update all" + ChatColor.WHITE + " to update all plugins");
                sendMessage(sender, "Use " + ChatColor.AQUA + "/update FirstPluginName AnotherPluginName ..." + ChatColor.WHITE + " to update the plugins in the list");
                sendMessage(sender, "Use " + ChatColor.AQUA + "/update help" + ChatColor.WHITE + " to see this message");
                return true;
            }
            update(sender, args);
            return true;
        }
        return false;
    }

    public static void update(CommandSender sender, String[] args){
        if(verifyPermission(sender, "polymart.update") && verifyAccount(sender)){
            UpdateCheck.run(sender, args);
        }
    }

    private List<String> matchTabComplete(String arg, List<String> options) {
        List<String> completions = new ArrayList<>();
        StringUtil.copyPartialMatches(arg, options, completions);
        Collections.sort(completions);
        return completions;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return matchTabComplete(args[0], TABCOMPLETE_ARGUMENTS);
        }
        return null;
    }
}