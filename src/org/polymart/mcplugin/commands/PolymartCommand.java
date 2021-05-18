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
import org.polymart.mcplugin.api.PolymartAPIHandler;
import org.polymart.mcplugin.api.PolymartAccount;
import org.polymart.mcplugin.server.UploadServerInfo;
import org.polymart.mcplugin.text.TextFormatter;
import org.polymart.mcplugin.utils.JSONWrapper;

import java.util.*;

import static org.polymart.mcplugin.commands.MessageUtils.*;

public class PolymartCommand implements TabExecutor {

    public static final List<String> TABCOMPLETE_ARGUMENTS = Arrays.asList("help", "search", "account", "login", "logout", "version", "update", "server");
    private long confirmingLogout = 0;


    private static List<String> alreadyLinkedElswhere = new ArrayList<>();
    private boolean linkingServer = false;
    private static String confirmingLinkServer;
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
            else if(args[0].equalsIgnoreCase("server")){
                if(verifyPermission(sender, "polymart.admin") && verifyAccount(sender)){
                    if(!PolymartAccount.serverLinked()){
                        if(linkingServer && args.length >= 3 && args[1].equalsIgnoreCase("link") && args[2].length() > 0){
                            if(alreadyLinkedElswhere.contains(args[2]) && (args.length < 4 || !args[3].equalsIgnoreCase("overwrite"))){
                                sendErrorMessage(sender, "This listing is already linked with another server. Linking it here will remove it from your other server. Are you sure you want to do this? To confirm, type " + ChatColor.YELLOW + "/polymart server link " + args[2] + " overwrite");
                            }
                            else{
                                sendMessage(sender, "Linking this server with your Polymart listing (polymart.org/server/" + args[2] + ")...");
                                Map<String, Object> map = new HashMap<>();
                                map.put("serverID", args[2]);
                                PolymartAPIHandler.post("linkServerInGame", map, (JSONWrapper json) -> {
                                    if(json.get("success").asBoolean(false)){
                                        PolymartAccount.setServer(
                                            json.get("server").get("id").asString(),
                                            json.get("server").get("token").asString()
                                        );

                                        sendMessage(sender, "Success! This server has been linked with your Polymart listing " + ChatColor.GREEN + "polymart.org/server/" + args[2]);
                                        UploadServerInfo.upload();
                                    }
                                    else{
                                        sendErrorMessage(sender, "It looks like something went wrong trying to link the server!");
                                    }
                                });
                                linkingServer = false;
                            }
                            return true;
                        }
                        sendMessage(sender, "Let's link your server with Polymart! Loading the servers you have listed on Polymart...");
                        PolymartAPIHandler.post("getUserServers", new HashMap<>(), (JSONWrapper json) -> {
                            List<JSONWrapper> l = json.get("servers").asJSONWrapperList();
                            if(l == null || l.size() < 1){
                                sendMessage(sender,"It looks like you haven't listed any servers on Polymart yet. Let's start with that! To list add your server, go to " + ChatColor.BLUE + "https://polymart.org/server/new" + ChatColor.AQUA + ". When you've added your server, come back here and type " + ChatColor.BLUE + "/polymart server");
                            }
                            else{
                                boolean player = sender instanceof Player;
                                sendMessage(sender, "Link your server to improve your experience on Polymart, access new features on your server listing, and to get more players! Polymart will periodically be sent info like the online players, the plugins you use, and other info and stats about your server.");
                                sendMessage(sender, "You have the following servers listed on your Polymart account." + (player ? "Click the listing that you'd like to link this server with:" : ""));
                                TextFormatter f = TextFormatter.make();
                                linkingServer = true;
                                for(JSONWrapper w : l){
                                    f.append(ChatColor.GRAY + "  - ");
                                    boolean linked = w.get("linked").asBoolean(false);
                                    ChatColor color = linked ? ChatColor.DARK_RED : ChatColor.AQUA;
                                    String str = color + w.get("name").asString();
                                    if(linked){
                                        str = str + ChatColor.RED + " [Already Linked Elsewhere]";
                                    }
                                    if(player){
                                        f.appendClickableWithCommand(str, "polymart server link " + w.get("id").asString());
                                    }
                                    else{
                                        f.append(str + " " + ChatColor.GRAY + "- link with /polymart server link " + w.get("id").asString());
                                    }
                                    f.append(ChatColor.GRAY + "\n");
                                }
                                f.send(sender);
                            }
                        });
                    }
                    else{
                        sendMessage(sender, "This server is linked with Polymart at " + ChatColor.BLUE + "https://polymart.org/server/" + PolymartAccount.getServerID());
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