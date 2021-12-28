package org.polymart.mcplugin.utils.nbt;

import org.bukkit.Bukkit;
import org.polymart.mcplugin.utils.ServerVersion;

import java.lang.reflect.Constructor;
import java.util.function.BiFunction;

public enum SafeNBTBaseType{

    STRING(String.class, SafeNBT::getString, "String"),
    DOUBLE(double.class, SafeNBT::getDouble,"Double"),
    FLOAT(float.class, SafeNBT::getFloat, "Float"),
    LONG(long.class, SafeNBT::getLong, "Long"),
    INT(int.class, SafeNBT::getInt, "Int"),
    SHORT(short.class, SafeNBT::getShort, "Short"),
    BYTE(byte.class, SafeNBT::getByte, "Byte"),
    //LONG_ARRAY(long[].class, SafeNBT::getLongArray, "LongArray"),
    INT_ARRAY(int[].class, SafeNBT::getIntArray, "IntArray"),
    BYTE_ARRAY(byte[].class, SafeNBT::getByteArray, "ByteArray"),
    ;

    private BiFunction<SafeNBT, String, ?> getFunction;
    private Class<?> innerClazz;
    private Class<?> nbtBaseClass;
    private String name;

    <T> SafeNBTBaseType(Class<T> innerClazz, BiFunction<SafeNBT, String, T> get, String name){
        try{
            this.getFunction = get;
            this.innerClazz = innerClazz;
            String version = "net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            if(ServerVersion.greaterOrEqual(1, 17)){
                this.nbtBaseClass = Class.forName("net.minecraft.nbt.NBTBase");
            }
            else{
                this.nbtBaseClass = Class.forName(version + ".NBTTag" + name);
            }
            this.name = name;
        }
        catch(Exception ex){ex.printStackTrace();}
    }

    public Object get(SafeNBT nbt, String key){
        return getFunction.apply(nbt, key);
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