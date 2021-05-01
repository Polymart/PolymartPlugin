package org.polymart.mcplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.polymart.mcplugin.actions.Search;
import org.polymart.mcplugin.commands.PolymartCommand;

public class Main extends JavaPlugin{

    public static Main that;

    @Override
    public void onEnable(){
        that = this;

        super.getCommand("polymart").setExecutor(new PolymartCommand(this));
        this.saveDefaultConfig();

        Search.setup();
    }

    @Override
    public void onDisable(){
        that = null;
    }


    public boolean verifyPermission(CommandSender sender, String permission){
        if(!sender.hasPermission(permission)){
            sendMessage(sender, "You don't have permission to run this command!");
            return false;
        }
        return true;
    }

    public static void sendMessage(CommandSender sender, String message){
        sender.sendMessage(ChatColor.GOLD + "[Polymart] " + ChatColor.YELLOW + message);
    }
}
