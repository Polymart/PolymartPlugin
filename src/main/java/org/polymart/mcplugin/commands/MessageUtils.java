package org.polymart.mcplugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.polymart.mcplugin.api.PolymartAccount;

public class MessageUtils {

  public static boolean verifyAccount(CommandSender sender) {
    if (!PolymartAccount.hasToken()) {
      if (PolymartAccount.config.getString("unlinkReason") != null
          && PolymartAccount.config.getString("unlinkReason").equalsIgnoreCase("IP_BAD")) {
        sendMessage(
            sender,
            "It looks like your IP address changed since the last time you used the Polymart plugin! To log back in, use "
                + ChatColor.YELLOW
                + "/polymart login");
      } else {
        sendMessage(
            sender, "You aren't logged in! Log in with " + ChatColor.YELLOW + "/polymart login");
      }
      return false;
    }
    return true;
  }

  public static boolean verifyPermission(CommandSender sender, String permission) {
    if (!sender.hasPermission(permission)) {
      sendMessage(sender, "You don't have permission to run this command!");
      return false;
    }
    return true;
  }

  public static void sendMessage(CommandSender sender, String message) {
    sender.sendMessage(
        ChatColor.DARK_AQUA
            + "Polymart"
            + ChatColor.AQUA
            + ChatColor.BOLD
            + "> "
            + ChatColor.WHITE
            + message);
  }

  public static void sendErrorMessage(CommandSender sender, String message) {
    sender.sendMessage(
        ChatColor.DARK_RED
            + "Polymart"
            + ChatColor.RED
            + ChatColor.BOLD
            + "> "
            + ChatColor.WHITE
            + message);
  }
}
