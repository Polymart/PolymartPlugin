package org.polymart.mcplugin.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.polymart.mcplugin.Main;
import org.polymart.mcplugin.utils.JSONWrapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Consumer;

/**
This likely won't need to be modified much, at all
 */

public class PolymartAPIHandler{

    public static void post(String action, Map<String, Object> parameters, Consumer<JSONWrapper> response){
        makeCall(action, parameters, response, "POST");
    }

    private static void makeCall(String action, Map<String, Object> parameters, Consumer<JSONWrapper> response, String method){
        final String url = "https://api.polymart.org/v1/" + action;

        if(!parameters.containsKey("token")){
            parameters.put("token", PolymartAccount.getToken());
        }

        Main.that.getServer().getScheduler().runTaskAsynchronously(Main.that, () -> {
            JsonObject json = null;

            try{
                HttpURLConnection http = (HttpURLConnection) new URL(url).openConnection();
                http.setRequestMethod(method); // PUT is another valid option
                http.setDoOutput(true);

                StringJoiner sj = new StringJoiner("&");
                for(Map.Entry<String, Object> entry : parameters.entrySet())
                    sj.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "="
                            + URLEncoder.encode(entry.getValue() == null ? "" : String.valueOf(entry.getValue()), "UTF-8"));
                byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
                int length = out.length;

                http.setFixedLengthStreamingMode(length);
                http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
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

            response.accept(new JSONWrapper(json).get("response"));
        });
    }

}
