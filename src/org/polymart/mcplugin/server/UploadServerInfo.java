package org.polymart.mcplugin.server;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.polymart.mcplugin.Main;
import org.polymart.mcplugin.api.PolymartAPIHandler;
import org.polymart.mcplugin.api.PolymartAccount;
import org.polymart.mcplugin.utils.JSONWrapper;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class UploadServerInfo{

    public static final long UPLOAD_INTERVAL = 5 * 60 * 20;
    private static int SEND_NUMBER = 0;
    public static void setup(){
        Main.that.getServer().getScheduler().runTaskTimer(Main.that, UploadServerInfo::upload, 20L, UPLOAD_INTERVAL);
    }

    public static void upload(){
        if(!PolymartAccount.hasToken() || !PolymartAccount.serverLinked()){
            return;
        }

        Map<String, Object> data = new HashMap<>();
        int onlineCount = Bukkit.getOnlinePlayers().size();
        ByteBuffer buff = ByteBuffer.allocate(onlineCount * 16);
        int index = 0;
        List<String> list = new ArrayList<>();
        for(Player p : Bukkit.getOnlinePlayers()){
//            buff.putLong(index, p.getUniqueId().getMostSignificantBits());
//            buff.putLong(index + 8, p.getUniqueId().getLeastSignificantBits());
//            index+=16;
            list.add(p.getUniqueId().toString());
        }
        Map<String, Object> players = new HashMap<>();
        players.put("list", list);
        players.put("max", Bukkit.getServer().getMaxPlayers());
        players.put("online", Bukkit.getServer().getOnlinePlayers().size());
        data.put("players", players);

        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("version", Bukkit.getServer().getVersion());
        serverInfo.put("bukkitVersion", Bukkit.getServer().getBukkitVersion());
        serverInfo.put("gamemode", Bukkit.getServer().getDefaultGameMode());
        serverInfo.put("motd", Bukkit.getServer().getMotd());
        serverInfo.put("onlineMode", Bukkit.getServer().getOnlineMode());
        data.put("server", serverInfo);

        Runtime r = Runtime.getRuntime();
        Map<String, Object> machine = new HashMap<>();

        Map<String, Object> memory = new HashMap<>();
        memory.put("max", r.maxMemory());
        memory.put("total", r.totalMemory());
        memory.put("free", r.freeMemory());
        machine.put("memory", memory);

        machine.put("coreCount", r.availableProcessors());
        machine.put("os", System.getProperty("os.name"));
        machine.put("java", System.getProperty("java.version"));
        machine.put("arch", System.getProperty("os.arch"));
        data.put("machine", machine);

        List<Map<String, Object>> plugins = new ArrayList<>();
        byte[] buffer = new byte[1024];
        StringBuilder s = new StringBuilder();
        for(Plugin pl : Bukkit.getPluginManager().getPlugins()){
            try{
                URL url = pl.getClass().getProtectionDomain().getCodeSource().getLocation();
                File f = new File(URLDecoder.decode(url.getFile(), "UTF8"));
                ZipFile zf = new ZipFile(f);
                ZipEntry ze = zf.getEntry("polymart.yml");

                Map<String, Object> i = new HashMap<>();
                i.put("main", pl.getDescription().getMain());
                i.put("name", pl.getDescription().getName());
                i.put("version", pl.getDescription().getVersion());
                i.put("source", "UNKNOWN");

                try{
                    InputStream is = zf.getInputStream(ze);
                    int read;

                    s.setLength(0);
                    while((read = is.read(buffer, 0, 1024)) >= 0){
                        s.append(new String(buffer, 0, read));
                    }

                    YamlConfiguration info = new YamlConfiguration();
                    info.loadFromString(s.toString());

                    if(info.get("polymart.resource.id") != null){
                        i.put("id", info.get("polymart.resource.id"));
                        i.put("upload", info.get("polymart.upload.id"));
                        i.put("type", info.get("polymart.upload.type"));
                        i.put("source", "POLYMART");
                    }
                }
                catch(Exception ignore){}
                finally{
                    plugins.add(i);
                }
            }
            catch(Exception ignore){}
        }
        data.put("plugins", plugins);

        Map<String, Object> params = new HashMap<>();
        params.put("data", data);
        params.put("interval", UPLOAD_INTERVAL);
        params.put("sendCount", SEND_NUMBER);
        PolymartAPIHandler.post("serverSendStatus", params, (JSONWrapper json) -> {
            SEND_NUMBER++;
        });
    }

}
