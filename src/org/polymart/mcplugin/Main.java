package org.polymart.mcplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.polymart.mcplugin.actions.Login;
import org.polymart.mcplugin.actions.Logout;
import org.polymart.mcplugin.actions.Search;
import org.polymart.mcplugin.api.PolymartAccount;

import java.util.*;

public class Main extends JavaPlugin{

    public static Main that;

    @Override
    public void onEnable(){
        that = this;

        this.saveDefaultConfig();

        Search.setup();
    }

    @Override
    public void onDisable(){
        that = null;
    }

    private static long confirmingLogout = 0;
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if(cmd.getName().equalsIgnoreCase("polymart")){
            if(args.length == 0 || args[0].toLowerCase().matches("^(web|website|info|version|ver)$")){
                sendMessage(sender, "Running Polymart v" + this.getDescription().getVersion() + " for https://polymart.org");
            }
            else if(args[0].toLowerCase().matches("^(help|\\?)$")){
                sendMessage(sender, "Usage: /polymart <help/version/login/account/logout/search>");
            }
            else if(args[0].toLowerCase().matches("search")){
                if(verifyPermission(sender, "polymart.search")){
                    if(sender instanceof Player){
                        Search.search((Player) sender);
                    }
                    else{
                        sendMessage(sender, "Only players can run this command. Sorry!");
                    }
                }
            }
            else if(args[0].equalsIgnoreCase("account")){
                if(verifyPermission(sender, "polymart.admin")){
                    if(!PolymartAccount.hasToken()){
                        sendMessage(sender, "You aren't logged in! Log in with /polymart login");
                    }
                    else{
                        sendMessage(sender, "You're currently logged in to Polymart as https://polymart.org/user/" + PolymartAccount.getUserID() + ". To log out, use /polymart logout");
                    }
                }
            }
            else if(args[0].equalsIgnoreCase("login")){
                if(verifyPermission(sender, "polymart.admin")){
                    Login.login(sender, Arrays.copyOfRange(args, 1, args.length));
                }
            }
            else if(args[0].equalsIgnoreCase("logout")){
                if(verifyPermission(sender, "polymart.admin")){
                    if(!PolymartAccount.hasToken()){
                        sendMessage(sender, "You're already logged out! To log in, use /polymart login");
                    }
                    else if(confirmingLogout > System.currentTimeMillis()){
                        Logout.logout(sender);
                        confirmingLogout = 0;
                    }
                    else{
                        confirmingLogout = System.currentTimeMillis() + 10_000;
                        sendMessage(sender, "Are you sure you want to log out? To confirm, type /polymart logout. This will expire in 10 seconds.");
                    }
                }
            }

            return true;
        }
        return false;
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
