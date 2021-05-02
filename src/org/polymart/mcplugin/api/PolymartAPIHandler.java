package org.polymart.mcplugin.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import jdk.nashorn.internal.ir.debug.JSONWriter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.polymart.mcplugin.Main;
import org.polymart.mcplugin.utils.JSONWrapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Consumer;

import static org.polymart.mcplugin.commands.MessageUtils.sendMessage;

/**
This likely won't need to be modified much, at all
 */

public class PolymartAPIHandler{

    private static String IP = null;

    public static void post(String action, Map<String, Object> parameters, Consumer<JSONWrapper> response){
        post(action, parameters, response, Bukkit.getConsoleSender());
    }

    public static void post(String action, Map<String, Object> parameters, Consumer<JSONWrapper> response, CommandSender sender){
        makeCall(action, parameters, response, "POST", sender);
    }

    private static void makeCall(String action, Map<String, Object> parameters, Consumer<JSONWrapper> response, String method, CommandSender sender){
        if(IP == null){
            Main.that.getServer().getScheduler().runTaskAsynchronously(Main.that, () -> {
                try{
                    HttpURLConnection http = (HttpURLConnection) new URL("https://checkip.amazonaws.com").openConnection();
                    http.setRequestMethod("GET");
                    http.setDoOutput(true);

                    http.connect();
                    OutputStream os = http.getOutputStream();
                    os.flush();
                    os.close();

                    BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
                    String inputLine;
                    StringBuilder stringResp = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        stringResp.append(inputLine);
                    }
                    in.close();

                    String s = stringResp.toString();
                    String[] sp = s.split("[^0-9a-f.:]+");

                    IP = sp[sp.length - 1];
                }
                catch(Exception ex){
                    ex.printStackTrace();
                }

                doMakeCall(action, parameters, response, method, sender);
            });
        }
        else{
            doMakeCall(action, parameters, response, method, sender);
        }
    }

    private static void doMakeCall(String action, Map<String, Object> parameters, Consumer<JSONWrapper> response, String method, CommandSender sender){
        final String url = "https://api.polymart.org/v1/" + action;

        if(!parameters.containsKey("token")){
            parameters.put("token", PolymartAccount.getToken());
        }

        Map<String, Object> session = new HashMap<>();

        session.put("token", PolymartAccount.getToken());
        session.put("source", "ingame");
        session.put("ip", IP);

        Map<String, Object> server = new HashMap<>();
        server.put("ip", Bukkit.getServer().getIp());
        server.put("port", Bukkit.getServer().getPort());
        session.put("server", server);

        parameters.put("session", session);

        parameters.put("requestSource", "ingame");

        Main.that.getServer().getScheduler().runTaskAsynchronously(Main.that, () -> {
            JsonObject json = null;

            try{
                HttpURLConnection http = (HttpURLConnection) new URL(url).openConnection();
                http.setRequestMethod(method); // PUT is another valid option
                http.setDoOutput(true);

                Gson gson = new GsonBuilder().create();
                String sj = gson.toJson(parameters);

//                StringJoiner sj = new StringJoiner("&");
//                for(Map.Entry<String, Object> entry : parameters.entrySet())
//                    sj.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "="
//                            + URLEncoder.encode(entry.getValue() == null ? "" : String.valueOf(entry.getValue()), "UTF-8"));
                byte[] out = sj.getBytes(StandardCharsets.UTF_8);
                int length = out.length;

                http.setFixedLengthStreamingMode(length);
                http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                http.connect();
                OutputStream os = http.getOutputStream();
                os.write(out);
                os.flush();
                os.close();

                BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
                String inputLine;
                StringBuilder stringResp = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    stringResp.append(inputLine);
                }
                in.close();

                JsonParser parser = new JsonParser();
                json = (JsonObject) parser.parse(stringResp.toString());
            }
            catch(Exception ex){
                ex.printStackTrace();
            }

            JSONWrapper responseJSON = new JSONWrapper(json).get("response");

            runActions(responseJSON.get("actions"), sender);
            Main.that.getServer().getScheduler().runTask(Main.that, () -> response.accept(responseJSON));
        });
    }

    public static void runActions(JSONWrapper actions, CommandSender sender){
        if(actions == null){return;}

        if(actions.get("sendMessage").asString() != null){
            sendMessage(sender, ChatColor.translateAlternateColorCodes('&', actions.get("sendMessage").asString()));
        }
        if(actions.get("logout").asBoolean(false)){
            PolymartAccount.config.set("account", null);
            PolymartAccount.config.set("unlinkReason", "IP_BAD");
            PolymartAccount.save();
        }
    }

}
