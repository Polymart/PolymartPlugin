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
import org.polymart.mcplugin.api.PolymartAccount;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.polymart.mcplugin.commands.MessageUtils.*;

public class PolymartCommand implements TabExecutor {

    public static final List<String> TABCOMPLETE_ARGUMENTS = Arrays.asList("help", "search", "account", "login", "logout", "version", "update");
    private long confirmingLogout = 0;

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if(cmd.getName().equalsIgnoreCase("polymart")){
            if(args.length == 0 || args[0].toLowerCase().matches("^(web|website|info|version|ver)$")){
                sendMessage(sender, "Running Polymart v" + Main.that.getDescription().getVersion() + " for https://polymart.org");
            }
            else if(args[0].toLowerCase().matches("^(help|\\?)$")){
                sendMessage(sender, "Usage: /polymart <update/search/login/logout/account/version/help>");
            }
            else if(args[0].toLowerCase().matches("search") || args[0].toLowerCase().matches("s")){
                if(verifyPermission(sender, "polymart.search")){
                    if(sender instanceof Player){
                        String query = null;
                        if(args.length > 1){
                            StringBuilder q = new StringBuilder();
                            for(int i = 1; i < args.length; i++){
                                q.append(args[i] + " ");
                            }
                            if(q.length() > 0){
                                q.setLength(q.length() - 1);
                                query = q.toString();
                            }
                        }
                        Search.search((Player) sender, query);
                    }
                    else{
                        sendMessage(sender, "Only players can run this command. Sorry!");
                    }
                }
            }
            else if(args[0].equalsIgnoreCase("account")){
                if(verifyPermission(sender, "polymart.admin")){
                    if(verifyAccount(sender)){
                        sendMessage(sender, "You're currently logged in to Polymart as " + ChatColor.YELLOW + "https://polymart.org/user/" + PolymartAccount.getUserID() + ChatColor.WHITE + ". To log out, use /polymart logout");
                    }
                }
            }
            else if(args[0].equalsIgnoreCase("update") || args[0].equalsIgnoreCase("u")){
                UpdateCommand.update(sender, Arrays.copyOfRange(args, 1, args.length));
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