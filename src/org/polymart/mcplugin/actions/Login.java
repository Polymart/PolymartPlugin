package org.polymart.mcplugin.actions;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.polymart.mcplugin.Main;
import org.polymart.mcplugin.api.PolymartAPIHandler;
import org.polymart.mcplugin.api.PolymartAccount;
import org.polymart.mcplugin.utils.JSONWrapper;

import java.util.HashMap;
import java.util.Map;

import static org.polymart.mcplugin.Main.sendMessage;

/**
 * This likely won't need to be modified at all
 */

public class Login{

    public static void login(CommandSender sender, String[] args){
        if(PolymartAccount.hasToken()){
            sendMessage(sender, "It looks like you're already logged in as https://polymart.org/user/" + PolymartAccount.getUserID() + ". If you want to switch accounts, use /polymart logout");
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("service", "polymart.org");
        params.put("return_url", "https://polymart.org/linkMinecraftPlugin");
        params.put("return_token", "1");
        params.put("state", sender.getName());
        sendMessage(sender, "Getting everything ready for you...");
        PolymartAPIHandler.post("authorizeUser", params, (JSONWrapper json) -> {
            System.out.println(json.get("success").json);
            if(json.get("success").asBoolean(false)){
                String url = json.get("result").get("url").asString();
                String token = json.get("result").get("token").asString();

                if(sender instanceof ConsoleCommandSender){
                    sendMessage(sender, "To finish linking your Polymart account, visit " + url);
                }
                else{
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                            "/tellraw " + sender.getName() +
                                    " {text:\"Click Here to finish linking your Polymart Account\",clickEvent:{action:open_url,value:\"" +
                                    url + "\"}}"
                    );
                }

                PolymartAccount.checkForLink(token, (Object[] info) -> {
                    if(info != null){
                        sendMessage(sender, "Awesome! Your Polymart account has been successfully linked. Your Polymart account: https://polymart.org/user/" + info[0]);
                        Main.that.getConfig().set("account.token.value", token);
                        Main.that.getConfig().set("account.token.expires", info[1]);
                        Main.that.getConfig().set("account.user.id", info[0]);
                        Main.that.saveConfig();
                    }
                    else{
                        sendMessage(sender, "Uh-oh. It looks like something went wrong.");
                    }
                }, 0);
            }
            else{
                sendMessage(sender, "Uh oh! It looks like something went wrong. Are you sure your server is connected to the internet and can access https://api.polymart.org? Check the console for errors.");
            }
        });
    }

}
