package org.polymart.mcplugin.actions;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.polymart.mcplugin.Main;
import org.polymart.mcplugin.Resource;
import org.polymart.mcplugin.api.PolymartAPIHandler;
import org.polymart.mcplugin.utils.XMaterial;
import org.polymart.mcplugin.utils.maps.MapImage;
import org.polymart.mcplugin.utils.nbt.NBTUtils;
import org.polymart.mcplugin.utils.JSONWrapper;
import org.polymart.mcplugin.utils.Utils;

import java.util.*;

import static org.polymart.mcplugin.commands.MessageUtils.sendMessage;

/**
Action for searching resources — open the inventory with Search.search(player)
 */

public class Search implements Listener{

    public static void setup(){
        Main.that.getServer().getPluginManager().registerEvents(new Search(), Main.that);
    }

    public static final int PAGE_LENGTH = 45;

    public static void search(Player p, String query){
        search(p, query, 0);
    }

    private static Map<String, Resource> resources = new HashMap<>();

    public static void search(Player p, String query, int page){
        Map<String, Object> params = new HashMap<>();
        params.put("limit", PAGE_LENGTH);
        params.put("start", page * PAGE_LENGTH);
        params.put("query", query);
        params.put("thumbnailSize", 256);
        PolymartAPIHandler.post("search", params, (JSONWrapper json) -> {
            List<JSONWrapper> resourceList = json.get("result").asJSONWrapperList();
            List<ItemStack> items = new ArrayList<>();
            for(JSONWrapper resource : resourceList){
                Resource r = new Resource(resource);
                resources.put(r.getId(), r);
                items.add(makeItemStack(p, r));
            }

            ItemStack[] contents = items.toArray(new ItemStack[]{});
            Main.that.getServer().getScheduler().runTask(Main.that, () -> {
                Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_RED + "[BETA] " + ChatColor.DARK_GRAY + "Polymart Search");
                inv.setContents(contents);
                p.openInventory(inv);
            });
        });
    }

    private static ItemStack NEXT_PAGE = Utils.newActionStack(XMaterial.BLUE_STAINED_GLASS_PANE, ChatColor.BLUE + "Next Page", null, null, "next_page");
    private static ItemStack LAST_PAGE = Utils.newActionStack(XMaterial.BLUE_STAINED_GLASS_PANE, ChatColor.BLUE + "Next Page", null, null, "last_page");

    public static ItemStack makeItemStack(Player p, Resource r){
        String itemName = (r.canDownload() ? ChatColor.GREEN : ChatColor.YELLOW) + r.getTitle();
        String priceInfo = !r.canDownload() && r.hasPrice() ? "\n\n" + ChatColor.DARK_GRAY + "(" + r.getPrice() + " " + r.getCurrency() + ")" : "";

        ItemStack base = MapImage.getMapImage(p, r.getThumbnailURL(), XMaterial.ACACIA_SIGN.parseItem());

        ItemStack stack = Utils.newStack(base, itemName, ChatColor.GRAY, r.getSubtitle() + priceInfo);

        stack = NBTUtils.set(stack, "org.polymart.mcplugin.watch", true);
        stack = NBTUtils.set(stack, "org.polymart.mcplugin.action", "open_resource");
        return NBTUtils.set(stack, "org.polymart.mcplugin.resource_id", r.getId());
    }

    @EventHandler
    public void inventoryClick(InventoryClickEvent e){
        ItemStack is = e.getCurrentItem();
        if(is == null){return;}
        if(!NBTUtils.getBoolean(is, "org.polymart.mcplugin.watch")){return;}
        Player p = (Player) e.getWhoClicked();


        String action = NBTUtils.get(is, "org.polymart.mcplugin.action", String.class);
        if(!action.equalsIgnoreCase("open_resource")){return;}

        String id = NBTUtils.get(is, "org.polymart.mcplugin.resource_id", String.class);
        Resource r = resources.get(id);
        e.setCancelled(true);

        sendMessage(p, "Visit " + r.getUrl());
    }
}
