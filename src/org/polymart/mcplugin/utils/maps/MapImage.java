package org.polymart.mcplugin.utils.maps;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;

public class MapImage{

    public static ItemStack getMapImage(Player p, String url, ItemStack fallback){
        try{
            MapImageRenderer mir = new MapImageRenderer(url);

            ItemStack i = new ItemStack(Material.MAP, 1);
            MapView view = Bukkit.createMap(p.getWorld());
            for(MapRenderer renderer : view.getRenderers())
                view.removeRenderer(renderer);
            view.addRenderer(mir);

            MapMeta mm = (MapMeta) i.getItemMeta();
            mm.setMapView(view);

            return i;
        }
        catch(Exception ex){
            return fallback;
        }
    }

}
