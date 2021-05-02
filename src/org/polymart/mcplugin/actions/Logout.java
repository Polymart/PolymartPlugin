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

public class Logout{

    public static void logout(CommandSender sender){
        sendMessage(sender, "Logging you out...");

        Map<String, Object> params = new HashMap<>();
        params.put("service", "polymart.org");
        params.put("token", PolymartAccount.getToken());
        PolymartAPIHandler.post("invalidateAuthToken", params, (JSONWrapper wrapper) -> {
            if(wrapper == null || wrapper.get("result").get("message").asString("").isEmpty()){
                sendMessage(sender, "It looks like we weren't able to communicate with Polymart to log you out. Are you sure your server is connected to the internet and can access https://api.polymart.org?");
            }
            else{
                PolymartAccount.config.set("account", null);
                PolymartAccount.config.set("unlinkReason", null);
                PolymartAccount.save();
                sendMessage(sender, "You have been logged out! To log back in, use /polymart login");
            }
        });
    }
}
