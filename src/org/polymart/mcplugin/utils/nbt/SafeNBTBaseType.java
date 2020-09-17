package org.polymart.mcplugin.utils.nbt;

import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;

public enum SafeNBTBaseType{

    STRING(String.class, "String"),
    DOUBLE(double.class, "Double"),
    FLOAT(float.class, "Float"),
    LONG(long.class, "Long"),
    INT(int.class, "Int"),
    SHORT(short.class, "Short"),
    BYTE(byte.class, "Byte"),
    LONG_ARRAY(long[].class, "LongArray"),
    INT_ARRAY(int[].class, "IntArray"),
    BYTE_ARRAY(byte[].class, "ByteArray"),
    ;

    private Class<?> innerClazz;
    private Class<?> nbtBaseClass;
    private String name;

    <T> SafeNBTBaseType(Class<T> innerClazz, String name){
        try{
            this.innerClazz = innerClazz;
            String version = "net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            this.nbtBaseClass = Class.forName(version + ".NBTTag" + name);
            this.name = name;
        }
        catch(Exception ex){ex.printStackTrace();}
    }

    public Class<?> getInnerClass(){
        return this.innerClazz;
    }

    public String getName(){
        return this.name;
    }

    public static SafeNBTBaseType get(Class<?> clazz){
        for(SafeNBTBaseType type : values()){
            if(type.innerClazz.equals(clazz)){
                return type;
            }
        }

        if(clazz == Float.class){return FLOAT;}
        else if(clazz == Integer.class){return INT;}
        return null;
    }

    public <T> Object make(T value){
        try{
            Constructor m = nbtBaseClass.getDeclaredConstructor(innerClazz);
            m.setAccessible(true);
            Object o = m.newInstance(value);
            m.setAccessible(false);

            return o;
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        return null;
    }


}

