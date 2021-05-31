package org.polymart.mcplugin.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.polymart.mcplugin.utils.nbt.SafeNBT;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Utils {

  private static final String HMAC_SHA256 = "HmacSHA256";
  private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

  public static String makeURLFriendlyString(String s) {
    return s.toLowerCase()
        .replaceAll("[^a-z0-9]", "-")
        .replaceAll("-+", "-")
        .replaceAll("^-*(.+?)-*$", "$1");
  }

  public static String hmacSha256(String value, String key) {
    Mac sha256Hmac;

    try {
      final byte[] byteKey = key.getBytes(StandardCharsets.UTF_8);
      sha256Hmac = Mac.getInstance(HMAC_SHA256);
      SecretKeySpec keySpec = new SecretKeySpec(byteKey, HMAC_SHA256);
      sha256Hmac.init(keySpec);
      byte[] data = sha256Hmac.doFinal(value.getBytes(StandardCharsets.UTF_8));

      return bytesToHex(data);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX_ARRAY[v >>> 4];
      hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
    }
    return new String(hexChars);
  }

  public static ItemStack newStack(XMaterial m, String displayName) {
    return newStack(m, displayName, null, null);
  }

  public static ItemStack newStack(
      XMaterial m, String displayName, ChatColor loreColor, String lore) {
    ItemStack is = m.parseItem();
    ItemMeta im = is.getItemMeta();
    im.setDisplayName(displayName);
    if (lore != null) {
      im.setLore(splitPreservingWordsAndColorCodes(lore, 32, loreColor));
    }
    is.setItemMeta(im);
    return is;
  }

  public static ItemStack newActionStack(
      XMaterial m, String displayName, ChatColor loreColor, String lore, String action) {
    ItemStack is = newStack(m, displayName, loreColor, lore);
    SafeNBT nbt = SafeNBT.get(is);
    nbt.setBoolean("org.polymart.mcplugin.watch", true);
    nbt.setString("org.polymart.mcplugin.action", action);
    return nbt.apply(is);
  }

  public static String[] splitPreservingWords(String text, int length) {
    return text.replaceAll("(?:\\s*)(.{1," + length + "})(?:\\s+|\\s*$)", "$1\n").split("\n");
  }

  public static List<String> splitPreservingWordsAndColorCodes(
      String text, int length, final ChatColor def) {
    String[] split = splitPreservingWords(text, length);
    List<String> colored = new ArrayList<>();
    String color = def.toString();
    for (String str : split) {
      String c = ChatColor.getLastColors(str);
      if (!c.isEmpty()) {
        color = c;
      }
      if (color.equalsIgnoreCase(ChatColor.RESET.toString())) {
        color = def.toString();
      }
      colored.add(color + str);
    }
    return colored;
  }
}
