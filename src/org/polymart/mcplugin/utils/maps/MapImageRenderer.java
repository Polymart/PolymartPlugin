package org.polymart.mcplugin.utils.maps;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.URL;

class MapImageRenderer extends MapRenderer{

    private SoftReference<BufferedImage> cacheImage;
    private boolean hasRendered = false;

    public MapImageRenderer(String url) throws IOException{
        this.cacheImage = new SoftReference<>(this.getImage(url));
    }

    @Override
    public void render(MapView view, MapCanvas canvas, Player player){
        if(this.hasRendered){
            return;
        }

        if(this.cacheImage.get() != null){
            canvas.drawImage(0, 0, this.cacheImage.get());
            this.hasRendered = true;
        }
        else{
//            player.sendMessage(ChatColor.RED + "Attempted to render the image, but the cached image was null!");
//            ImgMap.logMessage(ChatColor.RED + "While rendering image map ID #" + view.getId() + ", cacheImage was garbage collected.");
            this.hasRendered = true;
        }
    }

    public BufferedImage getImage(String url) throws IOException{
        boolean useCache = ImageIO.getUseCache();

        // Temporarily disable cache, if it isn't already,
        // so we can get the latest image.
        ImageIO.setUseCache(false);

        BufferedImage image = ImageIO.read(new URL(url));
        resize(image, 256, 256);
        //RenderUtils.resizeImage(image);

        // Renable it with the old value.
        ImageIO.setUseCache(useCache);

        return image;
    }

    private BufferedImage resize(BufferedImage image, int w, int h) throws IOException{
        if(image.getWidth() == w && image.getHeight() == h){return image;}

        final BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = resized.createGraphics();
        g.drawImage(image, 0, 0, w, h, null);
        g.dispose();
        return resized;
    }

}
