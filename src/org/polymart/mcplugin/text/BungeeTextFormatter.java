package org.polymart.mcplugin.text;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BungeeTextFormatter extends TextFormatter{

    private TextComponent s = new TextComponent();

    public BungeeTextFormatter append(String in){
        s.addExtra(in);
        return this;
    }

    public BungeeTextFormatter appendClickableWithURL(String in, String url){
        if(url == null || url.length() == 0){return append(in);}
        TextComponent message = new TextComponent(in);
        message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        s.addExtra(message);
        return this;
    }

    public BungeeTextFormatter appendClickableWithCommand(String in, String command){
        if(command == null || command.length() == 0){return append(in);}
        TextComponent message = new TextComponent(in);
        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        s.addExtra(message);
        return this;
    }


    @Override
    public String toString(){
        return s.toString();
    }

    public String asPlaintext(){
        return s.toPlainText();
    }

    public void send(CommandSender sender){
        if(sender instanceof Player){
            ((Player) sender).spigot().sendMessage(s);
        }
        else{
            sender.sendMessage(this.asPlaintext());
        }
    }
}
