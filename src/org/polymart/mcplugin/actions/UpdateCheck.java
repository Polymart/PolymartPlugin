package org.polymart.mcplugin.actions;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Consumer;
import org.polymart.mcplugin.Main;
import org.polymart.mcplugin.api.PolymartAPIHandler;
import org.polymart.mcplugin.text.TextFormatter;
import org.polymart.mcplugin.utils.JSONWrapper;
import org.polymart.mcplugin.utils.Utils;
import org.polymart.mcplugin.utils.XMaterial;
import org.polymart.mcplugin.utils.nbt.NBTUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.polymart.mcplugin.commands.MessageUtils.sendMessage;

public class UpdateCheck implements Listener{

    private static File namesToUpdateFile;
    private static YamlConfiguration namesToUpdateYML;
    private static List<String> pluginsWithUpdatedNames = new ArrayList<>();
    private static boolean didUpdateFileNames = false;

    public static void setup(){
        Main.that.getServer().getPluginManager().registerEvents(new UpdateCheck(), Main.that);

        namesToUpdateFile = new File(Main.that.getDataFolder(), "updates.yml");
        namesToUpdateYML = YamlConfiguration.loadConfiguration(namesToUpdateFile);

        deleteHelperJars();
    }

    private static void deleteHelperJars(){
        File helperFile = new File(Main.that.getDataFolder().getParentFile(), "PolymartHelper.jar");
        if(helperFile.exists()){helperFile.delete();}

        File helperFileTemp = new File(Main.that.getDataFolder().getParentFile(), "PolymartHelperTemp.jar");
        if(helperFileTemp.exists()){helperFileTemp.delete();}
    }

    private static Map<String, String> UPDATES_DONE = new HashMap<>();
    public static void disable(){
        if(UPDATES_DONE.size() == 0){return;}

        File dir = new File(Main.that.getDataFolder(), "history" + File.separator + "updates");
        dir.mkdirs();
        SimpleDateFormat formatter = new SimpleDateFormat("'Updates' yyyy-MM-dd 'at' HH.mm.ss'.txt'");
        formatter.setTimeZone(TimeZone.getDefault());

        Date date = new Date(System.currentTimeMillis());
        String name = formatter.format(date);
        File f = new File(dir, name);

        try{
            f.createNewFile();
            f.setWritable(true);
            BufferedWriter writer = new BufferedWriter(new FileWriter(f));

            StringBuilder s = new StringBuilder();
            for(Map.Entry<String, String> e : UPDATES_DONE.entrySet()){
                s.append(e.getKey() + ": " + e.getValue() + "\r\n");
            }
            UPDATES_DONE.clear();

            writer.write(s.toString());
            writer.close();
        }
        catch(IOException ex){
            ex.printStackTrace();
        }

        // Load PolymartHelper.jar to rename .jar files. This is kind of an involved process
        if(didUpdateFileNames){
            try{
                if(!namesToUpdateFile.exists()){namesToUpdateFile.createNewFile();}
                namesToUpdateYML.save(namesToUpdateFile);

                String jn = new File(Main.class.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .getPath()).getName();
                jn = jn.replaceAll("%20", " ");
                JarFile jf = new JarFile(new File(Main.that.getDataFolder().getParentFile(), jn));
                ZipEntry je = jf.getEntry("resources/PolymartHelper.jar");

                InputStream is = jf.getInputStream(je);

                File helperFileTemp = new File(Main.that.getDataFolder().getParentFile(), "PolymartHelperTemp.jar");
                if(!helperFileTemp.exists()){helperFileTemp.createNewFile();}
                int readBytes;
                byte[] buffer = new byte[4096];
                FileOutputStream os = new FileOutputStream(helperFileTemp);

                while((readBytes = is.read(buffer)) > 0){
                    os.write(buffer, 0, readBytes);
                }

                File helperFile = new File(Main.that.getDataFolder().getParentFile(), "PolymartHelper.jar");
                ZipFile helperZip = new ZipFile(helperFileTemp);
                ZipEntry helperPluginYamlEntry = helperZip.getEntry("plugin.yml");

                pluginsWithUpdatedNames.add(Main.that.getName());
                YamlConfiguration helperPluginYaml = loadConfig(helperZip, helperPluginYamlEntry);
                helperPluginYaml.set("loadbefore", new ArrayList<>(new HashSet<>(pluginsWithUpdatedNames))); // remove duplicates
                String helperPluginYamlString = helperPluginYaml.saveToString();

                ZipOutputStream out = new ZipOutputStream(new FileOutputStream(helperFile));
                Enumeration<? extends ZipEntry> entries = helperZip.entries();
                ByteArrayOutputStream bao = new ByteArrayOutputStream();

                while(entries.hasMoreElements()){
                    ZipEntry e = entries.nextElement();
                    out.putNextEntry(e);

                    if(e.getName().equals("plugin.yml")){
                        out.write(helperPluginYamlString.getBytes(StandardCharsets.UTF_8));
                    }
                    else{
                        InputStream iso = helperZip.getInputStream(e);

                        int read;
                        bao.reset();
                        while((read = iso.read(BUFFER, 0, 1024)) >= 0){
                            bao.write(BUFFER, 0, read);
                        }

                        out.write(bao.toByteArray());
                    }
                    out.closeEntry();
                }

                out.close();
                helperFileTemp.delete();
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
        }
        else{
            if(namesToUpdateFile.exists()){namesToUpdateFile.delete();}

            deleteHelperJars();
        }
    }

    public static boolean DO_AUTO_UPDATES = true;
    public static long lastCheckedForUpdates = 0;
    public static JSONWrapper cached = null;

    private static void setCached(JSONWrapper w){
        cached = w;
        lastCheckedForUpdates = System.currentTimeMillis();
    }

    private static final StringBuilder LOAD_CONFIG_STRING = new StringBuilder();
    private static byte[] BUFFER = new byte[1024];
    public static String loadToString(ZipFile zf, ZipEntry ze) throws IOException{
        InputStream is = zf.getInputStream(ze);
        int read;

        LOAD_CONFIG_STRING.setLength(0);
        while((read = is.read(BUFFER, 0, 1024)) >= 0){
            LOAD_CONFIG_STRING.append(new String(BUFFER, 0, read));
        }

        return LOAD_CONFIG_STRING.toString();
    }

    public static YamlConfiguration loadConfig(ZipFile zf, ZipEntry ze) throws IOException, InvalidConfigurationException{
        YamlConfiguration info = new YamlConfiguration();
        info.loadFromString(loadToString(zf, ze));
        return info;
    }

    public static void run(CommandSender sender, String[] toUpdate){
        sender.sendMessage(ChatColor.DARK_AQUA + "Polymart" + ChatColor.AQUA + ChatColor.BOLD + ">" + ChatColor.WHITE + " Checking for updates...");

        List<Map<String, Object>> resources = new ArrayList<>();
        Map<String, String> oldVersions = new HashMap<>();

        for(Plugin pl : Bukkit.getPluginManager().getPlugins()){
            try{
                oldVersions.put(pl.getDescription().getName().toLowerCase(), pl.getDescription().getVersion());

                URL url = pl.getClass().getProtectionDomain().getCodeSource().getLocation();
                File f = new File(URLDecoder.decode(url.getFile(), "UTF8"));
                ZipFile zf = new ZipFile(f);
                ZipEntry ze = zf.getEntry("polymart.yml");

                Map<String, Object> i = new HashMap<>();
                i.put("main", pl.getDescription().getMain());
                i.put("name", pl.getDescription().getName());
                i.put("version", pl.getDescription().getVersion());
                i.put("source", "UNKNOWN");

                jarFileNames.put(pl.getDescription().getName().toLowerCase(), f.getName());

                if(ze == null){
                    resources.add(i);
                    continue;
                }

                try{
                    YamlConfiguration info = loadConfig(zf, ze);

                    if(info.get("polymart.resource.id") != null){
                        i.put("id", info.get("polymart.resource.id"));
                        i.put("upload", info.get("polymart.upload.id"));
                        i.put("type", info.get("polymart.upload.type"));
                        i.put("source", "POLYMART");
                    }
                }
                catch(Exception ignore){}
                finally{
                    resources.add(i);
                }
            }
            catch(Exception ignore){
                //ex.printStackTrace();
            }
        }

        Map<String, Object> send = new HashMap<>();

        send.put("resources", resources);
        send.put("allow_redirects", "true");

        // Cache results for 15 seconds
        if(cached != null && (toUpdate.length > 0 || System.currentTimeMillis() - lastCheckedForUpdates < 15000)){
            PolymartAPIHandler.runActions(cached.get("actions"), sender);
            UpdateCheck.checkForUpdatesResponse(cached, oldVersions, sender, toUpdate);
            return;
        }

        PolymartAPIHandler.post("checkForUpdates", send, (JSONWrapper w) -> {
            UpdateCheck.setCached(w);
            UpdateCheck.checkForUpdatesResponse(w, oldVersions, sender, toUpdate);
        }, sender);
    }

    public static Map<String, Integer> alreadyDownloaded = new HashMap<>();
    private static Map<String, String> jarFileNames = new HashMap<>();
    public static List<String> currentlyDownloading = new ArrayList<>();

    public static void download(String name, String url, int update, String fileName, Consumer<Boolean> finished){
        if(url == null){
            finished.accept(false);
            return;
        }

        if(alreadyDownloaded.getOrDefault(name, Integer.MIN_VALUE) >= update || currentlyDownloading.contains(name)){
            finished.accept(true);
            return;
        }
        currentlyDownloading.add(name);

        Main.that.getServer().getScheduler().runTaskAsynchronously(Main.that, () -> {
            boolean success = false;
            String jarFileName = "";
            try{
                File saveDir = Bukkit.getUpdateFolderFile();
                //File saveDir = new File(Main.that.getDataFolder(), "updater" + File.separator + "downloads");
                saveDir.mkdirs();

                jarFileName = jarFileNames.get(name.toLowerCase());

                URL website = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) website.openConnection();
                connection.setInstanceFollowRedirects(true);
                ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream());
                FileOutputStream fos = new FileOutputStream(new File(saveDir, jarFileName));
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

                success = true;
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
            finally{
                final boolean sv = success;
                final String fn = jarFileName;
                Main.that.getServer().getScheduler().runTask(Main.that, () -> {
                    alreadyDownloaded.put(name, update);
                    currentlyDownloading.remove(name);

                    if(sv){
                        if(fileName != null && !fn.equalsIgnoreCase(fileName) && fileName.length() > 0 && fn.length() > 0 && fileName.endsWith(".jar")){
                            namesToUpdateYML.set("plugins." + name.toLowerCase() + ".originalFileName", fn);
                            namesToUpdateYML.set("plugins." + name.toLowerCase() + ".updatedFileName", fileName);
                            namesToUpdateYML.set("plugins." + name.toLowerCase() + ".time", System.currentTimeMillis() / 1000);
                            pluginsWithUpdatedNames.add(name);
                            didUpdateFileNames = true;
                        }
                    }

                    finished.accept(sv);
                });
            }
        });
    }

    public static Map<String, UpdateInfo> updateable = new HashMap<>();
    public static Map<String, String> oldVersions = new HashMap<>();

    public static List<UpdateInfo> upToDate, canDownloadFromPolymart, untracked, updateAvailable;

    public static void checkForUpdatesResponse(JSONWrapper wrapper, Map<String, String> oldVersions, CommandSender sender, String[] forced){
        UpdateCheck.oldVersions = oldVersions;

        if(!wrapper.get("success").asBoolean(false)){
            if(wrapper.get("actions").get("sendMessage").isNull()){
                sendMessage(sender, "It looks like something went wrong while trying to check for updates! Please check your internet connection, and make sure that your host isn't blocking your server from accessing " + ChatColor.YELLOW + "api.polymart.org:443" + ChatColor.WHITE + ".");
            }
            return;
        }

        List<JSONWrapper> polymartResources = wrapper.get("resources").asJSONWrapperList();
        String pm = wrapper.get("preMessage").asString();
        PRE_MESSAGE = pm == null || pm.length() < 1 ? null : ChatColor.translateAlternateColorCodes('&', pm);

        upToDate = new ArrayList<>();
        canDownloadFromPolymart = new ArrayList<>();
        untracked = new ArrayList<>();
        updateAvailable = new ArrayList<>();

        for(JSONWrapper r : polymartResources){
            String status = r.get("status").asString();

            List<UpdateInfo> addTo;// = new ArrayList<>();
            if(status == null){continue;}
            addTo = status.equalsIgnoreCase("UP_TO_DATE") || status.equalsIgnoreCase("ON_POLYMART_UP_TO_DATE") ? upToDate : (
                    status.equalsIgnoreCase("UPDATE_AVAILABLE") ? updateAvailable : (
                            status.equalsIgnoreCase("ON_POLYMART") || status.equalsIgnoreCase("ON_POLYMART_UPDATE_AVAILABLE") ? canDownloadFromPolymart : untracked
                    )
            );

            UpdateInfo ui = new UpdateInfo(
                    r.get("name").asString("ERR_NO_NAME_" + r.get("resource").get("id").asString("?")),
                    r.get("uploads").get("latest").get("version").asString(),
                    r.get("resource").get("url").asString(),
                    r.get("resource").get("transferURL").asString(),
                    r.get("uploads").get("latest").get("fileName").asString(),
                    r.get("uploads").get("latest").get("download").get("url").asString(),
                    r.get("uploads").get("latest").get("description").asString(),
                    r.get("uploads").get("latest").get("id").asInteger(-1),
                    r.get("behind").asInteger(0)
            );

            if(status.equalsIgnoreCase("UPDATE_AVAILABLE") && r.get("uploads").get("latest").get("download").get("url").asString() != null){
                updateable.put(ui.name.toLowerCase(), ui);
            }

            addTo.add(ui);
        }

        upToDate.sort(Comparator.comparing((UpdateInfo a) -> a.name));
        canDownloadFromPolymart.sort(Comparator.comparing((UpdateInfo a) -> a.name));
        untracked.sort(Comparator.comparing((UpdateInfo a) -> a.name));
        updateAvailable.sort(Comparator.comparing((UpdateInfo a) -> a.name));

        if(sender instanceof Player && forced.length == 0 && updateable.size() > 0){
            stagedForUpdate.clear();
            openInventory((Player) sender);
        }
        else{
            stagedForUpdate.clear();
            if(forced.length == 1 && forced[0].equalsIgnoreCase("all")){
                stagedForUpdate.addAll(updateable.keySet());
            }
            else{
                int notFoundCount = 0;
                StringBuilder notFound = new StringBuilder();
                for(String s : forced){
                    if(updateable.containsKey(s.toLowerCase())){
                        stagedForUpdate.add(s.toLowerCase());
                    }
                    else{
                        notFoundCount++;
                        notFound.append(ChatColor.RED + s + ChatColor.WHITE + ", ");
                    }
                }

                if(notFound.length() > 2){
                    notFound.setLength(notFound.length() - 2);
                    sender.sendMessage(ChatColor.RED + "Polymart" + ChatColor.DARK_RED + ChatColor.BOLD + ">" + ChatColor.WHITE + " Couldn't find the following plugin" + (notFoundCount == 1 ? "" : "s") + ": " + notFound);
                }
            }
            doUpdates(sender);
        }
    }

    public static boolean IS_STARTUP_UPDATE_RUN = true;
    private static String PRE_MESSAGE = null;
    public static void doUpdates(CommandSender p){
        boolean isStartup = IS_STARTUP_UPDATE_RUN;
        IS_STARTUP_UPDATE_RUN = false;

        List<UpdateInfo> updateAvailableUnstaged = new ArrayList<>(updateAvailable);
        updateAvailableUnstaged.removeIf((UpdateInfo ui) -> stagedForUpdate.contains(ui.name.toLowerCase()));

        int total = untracked.size() + upToDate.size() + canDownloadFromPolymart.size() + updateAvailable.size();

        TextFormatter res = TextFormatter.make();
        res.append("\n\n");

        if(isStartup){
            res.append(ChatColor.DARK_AQUA + "--------====================================--------" + ChatColor.WHITE + "\n");
            res.append(ChatColor.DARK_AQUA + "--------========[" + ChatColor.AQUA + " Polymart Plugins " + ChatColor.DARK_AQUA + "]========--------" + ChatColor.WHITE + "\n");
            res.append(ChatColor.DARK_AQUA + "--------====================================--------" + ChatColor.WHITE + "\n\n");
        }

        if(PRE_MESSAGE != null && PRE_MESSAGE.length() > 0){
            res.append(ChatColor.GOLD + "Polymart" + ChatColor.YELLOW.toString() + ChatColor.BOLD + "> " + ChatColor.WHITE + PRE_MESSAGE);
            res.append("\n\n");
        }

        if(untracked.size() > 0){
            res.append(ChatColor.RED + "[" + untracked.size() + " / " + total + "] " + ChatColor.WHITE + "Polymart doesn't know about these plugins. Ask the developer to upload them to Polymart so you can use the auto-updater!\n");
            String comma = "";
            for(UpdateInfo i : untracked){
                Plugin pl = Bukkit.getPluginManager().getPlugin(i.name);
                String website = pl != null ? pl.getDescription().getWebsite() : null;
                res.append(ChatColor.WHITE + comma);
                res.appendClickableWithURL(ChatColor.RED + i.name, website);
                comma = ", ";
            }
            res.append(ChatColor.WHITE + "\n\n");
        }
        if(upToDate.size() > 0){
            res.append(ChatColor.AQUA + "[" + upToDate.size() + " / " + total + "] " + ChatColor.WHITE + "These plugins are up-to-date\n");
            String comma = "";
            for(UpdateInfo i : upToDate){
                res.append(ChatColor.WHITE + comma);
                res.appendClickableWithURL(ChatColor.AQUA + i.name, i.url);
                comma = ", ";
            }
            res.append(ChatColor.WHITE + "\n\n");
        }
        if(canDownloadFromPolymart.size() > 0){
            res.append(ChatColor.GREEN + "[" + canDownloadFromPolymart.size() + " / " + total + "] " + ChatColor.WHITE + "These plugins are available on Polymart! Download them from Polymart to take full advantage of the auto-updater" + (p instanceof Player ? ". Click a plugin to download it:" : "") + "\n");
            String comma = "";
            for(UpdateInfo i : canDownloadFromPolymart){
                res.append(ChatColor.WHITE + comma);
                res.appendClickableWithURL(ChatColor.GREEN + i.name, i.transferURL);
                comma = ", ";
            }
            res.append(ChatColor.WHITE + "\n\n");
        }
        if(updateAvailableUnstaged.size() > 0){
            res.append(ChatColor.YELLOW + "[" + updateAvailableUnstaged.size() + " / " + total + "] " + ChatColor.WHITE + "These plugins have updates available from Polymart\n");
            String comma = "";
            for(UpdateInfo i : updateAvailableUnstaged){
                res.append(ChatColor.WHITE + comma);
                res.appendClickableWithURL(ChatColor.YELLOW + i.name, i.url);
                res.append(ChatColor.WHITE + " (" + i.behind + " behind)");
                comma = ", ";
            }
            res.append(ChatColor.WHITE.toString() + "\n" + "To update these plugins, use " + ChatColor.YELLOW + "/polymart update <list of plugins>" + ChatColor.WHITE + " or " + ChatColor.YELLOW + "/polymart update all" + ChatColor.WHITE + "\n\n");
        }
        if(stagedForUpdate.size() > 0){
            res.append(ChatColor.DARK_AQUA + "[" + stagedForUpdate.size() + " / " + total + "] " + ChatColor.WHITE + "These plugins are being updated\n");
            String comma = "";
            for(String s : stagedForUpdate){
                UpdateInfo i = updateable.get(s);
                res.append(ChatColor.WHITE + comma);
                res.appendClickableWithURL(ChatColor.DARK_AQUA + i.name, i.url);
                res.append(ChatColor.WHITE + " (" + i.behind + " behind)");
                comma = ", ";
            }
            res.append(ChatColor.WHITE.toString() + "\n\n");
        }

        if(updateAvailableUnstaged.size() == 0 && stagedForUpdate.size() == 0){
            res.append(ChatColor.GOLD + "Polymart" + ChatColor.YELLOW.toString() + ChatColor.BOLD + ">" + ChatColor.WHITE + " All of your Polymart plugins are up-to-date. Awesome!");
        }

        if(isStartup){
            if(updateAvailableUnstaged.size() == 0 && stagedForUpdate.size() == 0){
                res.append("\n\n");
            }
            res.append(ChatColor.DARK_AQUA + "--------====================================--------" + ChatColor.WHITE + "\n");
        }

        res.send(p);

        int totalCount = 0;
        for(String s : stagedForUpdate){
            UpdateInfo r = updateable.get(s);
            if(r != null){totalCount++;}
        }

        int totalCountFinal = totalCount;
        Counter counter = new Counter();

        for(String s : stagedForUpdate){
            UpdateInfo r = updateable.get(s);
            if(r == null){continue;}

            download(
                    r.name,
                    r.download,
                    r.uploadID,
                    r.fileName,
                    (Boolean b) -> {
                        String oldVersion = oldVersions.get(r.name.toLowerCase());
                        int cv = counter.add();

                        if(b){
                            UPDATES_DONE.put(r.name, oldVersion + " -> " + r.version);
                        }

                        String m = b ?
                                ChatColor.WHITE + "Downloaded " + ChatColor.GREEN + r.name + ChatColor.WHITE + " [" + ChatColor.YELLOW + oldVersion + ChatColor.WHITE + " -> " + ChatColor.GREEN + r.version + ChatColor.WHITE + "]" :
                                ChatColor.RED + "Could not download " + r.name + ". Check the console for errors.";
                        p.sendMessage(ChatColor.GRAY.toString() + cv + " / " + totalCountFinal + ChatColor.GRAY.toString() + ChatColor.BOLD + " > " + m);

                        if(cv == totalCountFinal){
                            sendMessage(p, (cv == 1 ? "1 plugin was" : cv + " plugins were") + " successfully downloaded! The updates will be installed automatically next time you restart the server");
                        }
                    });
        }

        stagedForUpdate.clear();
    }

    private static class Counter{
        int value;

        public int add(){
            value++;
            return value;
        }
    }

    public static void openInventory(Player p){
        int size = updateable.values().size();
        size = size + (size % 9 == 0 ? 0 : (9 - (size % 9)));
        size = Math.min(size + 9, 54);
        Inventory inv = Bukkit.createInventory(null, size, "Polymart Plugin Updates");

        inv.setItem(0, makeItemStack(XMaterial.BARRIER, ChatColor.RED + "Close", "cancel_all_updates", ChatColor.DARK_RED + "Without updating any plugins"));
        //inv.setItem(1, makeItemStack(XMaterial.BLUE_STAINED_GLASS_PANE, ChatColor.BLUE + "Stage ", "set_all_staged"));
        inv.setItem(8, makeItemStack(XMaterial.COMMAND_BLOCK, ChatColor.GREEN + "Update Selected Plugins", "do_updates"));

        int slot = 9;
        for(UpdateInfo ui : updateable.values()){
            if(slot > 53){
                p.sendMessage(ChatColor.RED.toString() + ChatColor.BOLD + "Stopping at 45 updates in the inventory. You still have more updates!");
                break;
            }

            inv.setItem(slot, makeItemStack(ui, true));
            stagedForUpdate.add(ui.name.toLowerCase());
            slot++;
        }

        p.openInventory(inv);
    }

    public static ItemStack makeItemStack(XMaterial material, String name, String action){
        return makeItemStack(material, name, action, null);
    }

    public static ItemStack makeItemStack(XMaterial material, String name, String action, String lore){
        ItemStack stack = Utils.newStack(material, name, ChatColor.WHITE, lore);
        stack = NBTUtils.set(stack, "org.polymart.mcplugin.watch", true);
        stack = NBTUtils.set(stack, "org.polymart.mcplugin.action", action);
        return stack;
    }

    public static ItemStack makeItemStack(UpdateInfo ui, boolean willUpdate){
        String bs = ChatColor.WHITE + " (" + ui.behind + " behind)";
        String itemName = willUpdate ? ChatColor.AQUA + ui.name + bs : ChatColor.GOLD + ui.name + bs;

        String lore = ChatColor.YELLOW + oldVersions.get(ui.name.toLowerCase()) + ChatColor.WHITE + " -> " + ChatColor.GREEN + ui.version + "\n";
        String udle = ui.description != null && ui.description.length() > 200 ? "..." : "";
        lore+=(ui.description == null || ui.description.length() < 3 ? "" : ChatColor.GRAY + ui.description.substring(0, Math.min(ui.description.length(), 200)).replaceAll("\\s+", " ").replaceAll("\\s+$", "") + udle);
        lore+="\n\n" + ChatColor.GRAY.toString() + ChatColor.ITALIC + (!willUpdate ? ChatColor.WHITE + "(click to update)" : "(click to cancel update)");
        ItemStack stack = Utils.newStack(willUpdate ? XMaterial.CYAN_WOOL : XMaterial.ORANGE_WOOL, itemName, ChatColor.GRAY, lore);

        stack = NBTUtils.set(stack, "org.polymart.mcplugin.watch", true);
        stack = NBTUtils.set(stack, "org.polymart.mcplugin.action", "stage_for_update");
        return NBTUtils.set(stack, "org.polymart.mcplugin.resource_name", ui.name);
    }

    public static List<String> stagedForUpdate = new ArrayList<>();
    @EventHandler
    public void inventoryClick(InventoryClickEvent e){
        ItemStack is = e.getCurrentItem();
        if(is == null){return;}
        //SafeNBT nbt = SafeNBT.get(is);
        if(!NBTUtils.getBoolean(is, "org.polymart.mcplugin.watch")){return;}
        Player p = (Player) e.getWhoClicked();
        Inventory inv = e.getInventory();


        String action = NBTUtils.get(is, "org.polymart.mcplugin.action", String.class);
        if(action.equalsIgnoreCase("do_updates")){
            doUpdates(p);
            p.closeInventory();
        }
        else if(action.equalsIgnoreCase("cancel_all_updates")){
            stagedForUpdate.clear();
            doUpdates(p);
            p.closeInventory();
        }
        if(!action.equalsIgnoreCase("stage_for_update")){return;}

        String name = NBTUtils.get(is, "org.polymart.mcplugin.resource_name", String.class);

        boolean staged = !stagedForUpdate.contains(name.toLowerCase());
        if(staged){
            stagedForUpdate.add(name.toLowerCase());
        }
        else{
            stagedForUpdate.remove(name.toLowerCase());
        }

        e.setCancelled(true);
        ItemStack changeTo = makeItemStack(updateable.get(name.toLowerCase()), staged);
        inv.setItem(e.getSlot(), changeTo);
    }


    private static class UpdateInfo{
        private String name, version, url, transferURL, fileName, download, description;
        private int behind, uploadID;

        public UpdateInfo(String name, String version, String url, String transferURL, String fileName, String download, String description, int uploadID, int behind){
            this.name = name;
            this.version = version;
            this.url = url;
            this.download = download;
            this.transferURL = transferURL;
            this.fileName = fileName;
            this.description = description;
            this.uploadID = uploadID;
            this.behind = behind;
        }
    }
}
