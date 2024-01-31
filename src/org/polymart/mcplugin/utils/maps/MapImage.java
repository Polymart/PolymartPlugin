package org.polymart.mcplugin.utils;

import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;

public class MapImage{

    private SoftReference<BufferedImage> cacheImage;
    private boolean hasRendered = false;

    public MapImage(String url){
        this.cacheImage = new SoftReference<>(this.getImage(url));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void render(MapView view, MapCanvas canvas, Player player){
        if(this.hasRendered){
            return;
        }

        if(this.cacheImage.get() != null){
            canvas.drawImage(0, 0, this.cacheImage.get());
            this.hasRendered = true;
        }else{
            player.sendMessage(ChatColor.RED + "Attempted to render the image, but the cached image was null!");
            ImgMap.logMessage(ChatColor.RED + "While rendering image map ID #" + view.getId() + ", cacheImage was garbage collected.");
            this.hasRendered = true;
        }
    }

    public BufferedImage getImage(String url) throws IOException{
        boolean useCache = ImageIO.getUseCache();

        // Temporarily disable cache, if it isn't already,
        // so we can get the latest image.
        ImageIO.setUseCache(false);

        BufferedImage image = ImageIO.read(new URL(url));
        RenderUtils.resizeImage(image);

        // Renable it with the old value.
        ImageIO.setUseCache(useCache);

        return image;
    }

}
