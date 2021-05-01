package org.polymart.mcplugin.commands;

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

public class PolymartCommand implements TabExecutor {

    public static final List<String> TABCOMPLETE_ARGUMENTS = Arrays.asList("help", "search", "account", "login", "logout", "version");
    private final Main polymartPlugin;
    private long confirmingLogout = 0;

    public PolymartCommand(Main polymartPlugin) {
        this.polymartPlugin = polymartPlugin;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if(cmd.getName().equalsIgnoreCase("polymart")){
            if(args.length == 0 || args[0].toLowerCase().matches("^(web|website|info|version|ver)$")){
                polymartPlugin.sendMessage(sender, "Running Polymart v" + polymartPlugin.getDescription().getVersion() + " for https://polymart.org");
            }
            else if(args[0].toLowerCase().matches("^(help|\\?)$")){
                polymartPlugin.sendMessage(sender, "Usage: /polymart <help/version/login/account/logout/search>");
            }
            else if(args[0].toLowerCase().matches("search")){
                if(polymartPlugin.verifyPermission(sender, "polymart.search")){
                    if(sender instanceof Player){
                        Search.search((Player) sender);
                    }
                    else{
                        polymartPlugin.sendMessage(sender, "Only players can run this command. Sorry!");
                    }
                }
            }
            else if(args[0].equalsIgnoreCase("account")){
                if(polymartPlugin.verifyPermission(sender, "polymart.admin")){
                    if(!PolymartAccount.hasToken()){
                        polymartPlugin.sendMessage(sender, "You aren't logged in! Log in with /polymart login");
                    }
                    else{
                        polymartPlugin.sendMessage(sender, "You're currently logged in to Polymart as https://polymart.org/user/" + PolymartAccount.getUserID() + ". To log out, use /polymart logout");
                    }
                }
            }
            else if(args[0].equalsIgnoreCase("login")){
                if(polymartPlugin.verifyPermission(sender, "polymart.admin")){
                    Login.login(sender, Arrays.copyOfRange(args, 1, args.length));
                }
            }
            else if(args[0].equalsIgnoreCase("logout")){
                if(polymartPlugin.verifyPermission(sender, "polymart.admin")){
                    if(!PolymartAccount.hasToken()){
                        polymartPlugin.sendMessage(sender, "You're already logged out! To log in, use /polymart login");
                    }
                    else if(confirmingLogout > System.currentTimeMillis()){
                        Logout.logout(sender);
                        confirmingLogout = 0;
                    }
                    else{
                        confirmingLogout = System.currentTimeMillis() + 10_000;
                        polymartPlugin.sendMessage(sender, "Are you sure you want to log out? To confirm, type /polymart logout. This will expire in 10 seconds.");
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
