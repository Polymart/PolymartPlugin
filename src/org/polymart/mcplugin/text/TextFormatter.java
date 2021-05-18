package org.polymart.mcplugin.text;

import org.bukkit.command.CommandSender;

public class TextFormatter{

    public static TextFormatter make(){
        try{
            Class.forName("net.md_5.bungee.api.chat.TextComponent");
            return new BungeeTextFormatter();
        }
        catch(Exception ignore){}
        return new TextFormatter();
    }

    private StringBuilder s = new StringBuilder();

    TextFormatter(){}

    public TextFormatter append(String in){
        s.append(in);
        return this;
    }

    public TextFormatter appendClickableWithURL(String in, String url){
        return this.append(in);
    }

    public TextFormatter appendClickableWithCommand(String in, String command){
        return this.append(in);
    }

    public int length(){
        return s.length();
    }

    public TextFormatter setLength(int length){
        s.setLength(length);
        return this;
    }

    @Override
    public String toString(){
        return s.toString();
    }

    public String asPlaintext(){
        return toString();
    }

    public void send(CommandSender sender){
        sender.sendMessage(this.toString());
    }
}
