package org.polymart.mcplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.polymart.mcplugin.actions.Search;
import org.polymart.mcplugin.actions.UpdateCheck;
import org.polymart.mcplugin.api.PolymartAccount;
import org.polymart.mcplugin.commands.PolymartCommand;
import org.polymart.mcplugin.commands.UpdateCommand;
import org.polymart.mcplugin.server.UploadServerInfo;

import static org.polymart.mcplugin.commands.MessageUtils.sendMessage;

public class Main extends JavaPlugin implements Listener {

  public static Main that;

  @Override
  public void onEnable() {
    that = this;

    this.getServer().getPluginManager().registerEvents(this, this);

    this.saveDefaultConfig();
    PolymartAccount.setup();

    Search.setup();
    UpdateCheck.setup();
    UploadServerInfo.setup();

    this.getCommand("polymart").setExecutor(new PolymartCommand());
    this.getCommand("update").setExecutor(new UpdateCommand());

    if (PolymartAccount.hasToken()) {
      UpdateCheck.run(Bukkit.getConsoleSender(), new String[0]);
    } else {
      Bukkit.getConsoleSender()
          .sendMessage(
              ChatColor.DARK_AQUA
                  + "--------====================================--------"
                  + ChatColor.WHITE);
      Bukkit.getConsoleSender()
          .sendMessage(
              ChatColor.DARK_AQUA
                  + "--------========["
                  + ChatColor.AQUA
                  + " Polymart Plugins "
                  + ChatColor.DARK_AQUA
                  + "]========--------"
                  + ChatColor.WHITE);
      Bukkit.getConsoleSender()
          .sendMessage(
              ChatColor.DARK_AQUA
                  + "--------====================================--------"
                  + ChatColor.WHITE);
      Bukkit.getConsoleSender().sendMessage("");
      Bukkit.getConsoleSender()
          .sendMessage(
              "Log in to your Polymart account with /polymart login to start using the Polymart plugin!");
      Bukkit.getConsoleSender().sendMessage("");
      Bukkit.getConsoleSender()
          .sendMessage(
              ChatColor.DARK_AQUA
                  + "--------====================================--------"
                  + ChatColor.WHITE
                  + "\n\n");
    }
  }

  @EventHandler
  public void playerJoin(PlayerJoinEvent e) {
    if (e.getPlayer().isOp() && !PolymartAccount.hasToken()) {
      if (PolymartAccount.getToken() == null) {
        sendMessage(
            e.getPlayer(),
            "Log in to your Polymart account with "
                + ChatColor.AQUA
                + "/polymart login"
                + ChatColor.WHITE
                + " to start using the Polymart plugin!");
      } else {
        sendMessage(
            e.getPlayer(),
            "It looks like your Polymart account token expired! Log back in to your Polymart account with "
                + ChatColor.AQUA
                + "/polymart login");
      }
    }
  }

  @Override
  public void onDisable() {
    UpdateCheck.disable();
    PolymartAccount.save();

    that = null;
  }
}
