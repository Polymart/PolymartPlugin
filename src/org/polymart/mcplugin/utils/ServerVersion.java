package org.polymart.mcplugin.utils;

import org.bukkit.Bukkit;

public class ServerVersion{


    public static final int MAJOR_VERSION;
    public static final int MINOR_VERSION;
    public static final int UPDATE_VERSION;
    public static final String VERSION_STRING;
    public static final String VERSION_STRING_FULL;

    public static boolean greaterOrEqual(int major, int minor){
        return MAJOR_VERSION > major || (MAJOR_VERSION >= major && MINOR_VERSION >= minor);
    }

    public static boolean shouldUseMaterialData(){
        return !greaterOrEqual(1, 13);
    }

    static{
        String v = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        String[] vs = v.split("_");
        String vstr = vs[0] + "_" + vs[1];

        int major = 1;
        int minor = 16;
        int update = 0;
        try{
            major = Integer.parseInt(vs[0].replaceAll("[^0-9]", ""));
            minor = Integer.parseInt(vs[1].replaceAll("[^0-9]", ""));
            update = vs.length > 2 ? Integer.parseInt(vs[2].replaceAll("[^0-9]", "")) : 0;
        }
        catch(Exception ex){ex.printStackTrace();}
        finally{
            MAJOR_VERSION = major;
            MINOR_VERSION = minor;
            UPDATE_VERSION = update;
        }

        VERSION_STRING = MAJOR_VERSION + "." + MINOR_VERSION;
        VERSION_STRING_FULL = UPDATE_VERSION == 0 ? VERSION_STRING : MAJOR_VERSION + "." + MINOR_VERSION + "." + UPDATE_VERSION;
    }

}
