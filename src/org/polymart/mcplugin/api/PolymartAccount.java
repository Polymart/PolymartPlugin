package org.polymart.mcplugin.api;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.polymart.mcplugin.Main;
import org.polymart.mcplugin.utils.JSONWrapper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.polymart.mcplugin.commands.MessageUtils.sendMessage;

/**
This likely won't need to be modified much, at all
 */

public class PolymartAccount{

    public static YamlConfiguration config;
    private static File file;

    public static void setup(){
        try{
            file = new File(Main.that.getDataFolder(), "account.yml");
            if(!file.exists()){
                file.createNewFile();
            }

            config = YamlConfiguration.loadConfiguration(file);
        }
        catch(Exception ex){ex.printStackTrace();}
    }

    public static void save(){
        try{
            config.save(file);
        }
        catch(Exception ignore){}
    }

    public static void checkForLink(String token, Consumer<Object[]> confirmed, int tries){
        if(tries > 30){
            confirmed.accept(null);
            return;
        }
        long time = tries > 25 ? 400L : tries > 20 ? 200L : 100L;
        Main.that.getServer().getScheduler().runTaskLater(Main.that, () -> {
            Map<String, Object> params = new HashMap<>();
            params.put("token", token);
            PolymartAPIHandler.post("verifyAuthToken", params, (JSONWrapper json) -> {
                if(json.get("success").asBoolean(false)){
                    confirmed.accept(
                            new Object[]{
                                    json.get("result").get("user").get("id").asString(),
                                    json.get("result").get("expires").asInteger()
                            }
                    );
                }
                else{
                    checkForLink(token, confirmed, tries + 1);
                }
            });
        }, time);
    }

    public static boolean hasToken(){
        int expires = config.getInt("account.token.expires");
        return getToken() != null && expires > (System.currentTimeMillis() / 1000);
    }

    public static String getToken(){
        return config.getString("account.token.value");
    }

    public static String getUserID(){
        return config.getString("account.user.id");
    }
}
