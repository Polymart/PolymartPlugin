package org.polymart.mcplugin.utils.nbt;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.polymart.mcplugin.Main;
import org.polymart.mcplugin.utils.ServerVersion;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class SafeNBT{

    private static final String version = "net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    private static final String cbVersion = "org.bukkit.craftbukkit." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    static Class<?> tagCompoundClass;
    static Class<?> nbtBaseClass;
    static Class<?> nmsItemstackClass;
    static Class<?> craftItemstackClass;
    static Class<?> mojangsonParserClass;

    private final Object tagCompund;

    static{
        try{
            if(ServerVersion.greaterOrEqual(1, 17)){
                tagCompoundClass = Class.forName("net.minecraft.nbt.NBTTagCompound");
                nbtBaseClass = Class.forName("net.minecraft.nbt.NBTBase");
                nmsItemstackClass = Class.forName("net.minecraft.world.item.ItemStack");
                mojangsonParserClass = Class.forName("net.minecraft.nbt.MojangsonParser");
            }
            else{
                tagCompoundClass = Class.forName(version + ".NBTTagCompound");
                nbtBaseClass = Class.forName(version + ".NBTBase");
                nmsItemstackClass = Class.forName(version + ".ItemStack");
                mojangsonParserClass = Class.forName(version + ".MojangsonParser");
            }
            craftItemstackClass = Class.forName(cbVersion + ".inventory.CraftItemStack");
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public SafeNBT(){
        this(null);
    }

    public SafeNBT(Object tagCompound){
        Object toSet = tagCompound;
        if(tagCompound == null){
            try{
                toSet = tagCompoundClass.newInstance();
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
        }
        this.tagCompund = toSet;
    }

    public static SafeNBT getFromJSON(String json){
        try{
            Method m = mojangsonParserClass.getMethod("parse", String.class);
            m.setAccessible(true);
            Object o = m.invoke(null, json);
            m.setAccessible(false);
            return new SafeNBT(o);
        }
        catch(Exception ignore){}
        return null;
    }

    public Object getTagCompund(){
        return tagCompund;
    }

    public SafeNBT getCompound(String key){
        if(ServerVersion.greaterOrEqual(1, 18)){
            try{
                Object o = getObjectRaw(key);
                return o == null ? null : new SafeNBT(o);
            }
            catch(Exception ex){
                ex.printStackTrace();
                return null;
            }
        }

        try{
            Method m = tagCompoundClass.getMethod("getCompound", String.class);
            m.setAccessible(true);
            Object r = m.invoke(this.tagCompund, key);
            m.setAccessible(false);
            return r == null ? null : new SafeNBT(r);
        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    public void remove(String key){
        String name = ServerVersion.greaterOrEqual(1, 18) ? "r" : "remove";
        try{
            Method m = tagCompoundClass.getMethod(name, String.class);
            m.setAccessible(true);
            m.invoke(this.tagCompund, key);
            m.setAccessible(false);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void setObject(String key, Object o){
        if(ServerVersion.greaterOrEqual(1, 18)){
            NBTEditor.setTag(tagCompund, key, o);
            return;
        }

        if(o instanceof String){setString(key, (String) o);}
        else if(o instanceof Integer){setInt(key, (Integer) o);}
        else if(o instanceof Double){setDouble(key, (Double) o);}
        else if(o instanceof Long){setLong(key, (Long) o);}
        else if(o instanceof List){
            SafeNBTList list = new SafeNBTList();
            for(Object e : (List) o){
                if(e instanceof Map){
                    SafeNBT mapNBT = new SafeNBT();
                    for(Object k : ((Map) e).keySet()){
                        if(k instanceof String){
                            Object v = ((Map) e).get(k);
                            mapNBT.setObject((String) k, v);
                        }
                    }
                    list.add(mapNBT);
                }
                else{
                    list.addGeneric(e);
                }
            }
            set(key, list);
        }
    }

    public String getString(String key){
        if(ServerVersion.greaterOrEqual(1, 18)){
            return NBTEditor.getString(tagCompund, key);
        }

        try{
            Method m = tagCompoundClass.getMethod("getString", String.class);
            m.setAccessible(true);
            Object r = m.invoke(this.tagCompund, key);
            m.setAccessible(false);
            return r instanceof String ? (String) r : null;
        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    public void setString(String key, String value){
        if(ServerVersion.greaterOrEqual(1, 18)){
            NBTEditor.setTag(tagCompund, key, value);
            return;
        }

        try{
            Method m = tagCompoundClass.getMethod("setString", String.class, String.class);
            m.setAccessible(true);
            m.invoke(this.tagCompund, key, value);
            m.setAccessible(false);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public Object getObject(String key){
        Object o;

        for(SafeNBTBaseType type : SafeNBTBaseType.values()){
            o = type.get(this, key);//getObject(key, type);
            if(o != null){
                if(type == SafeNBTBaseType.STRING && o.toString().length() == 0){continue;}
                if(o instanceof Number && ((Number) o).intValue() == 0){
                    continue;
                }

                return o;
            }
        }
        return null;
    }

//    public Object getObject(String key, SafeNBTBaseType type){
//        try{
//            Method m = tagCompoundClass.getMethod("get" + type.getName(), String.class);
//            m.setAccessible(true);
//            Object o = m.invoke(this.tagCompund, key);
//            m.setAccessible(false);
//            return o;
//        }
//        catch(Exception ex){
//            ex.printStackTrace();
//        }
//        return null;
//    }

    public Integer getInt(String key){
        if(ServerVersion.greaterOrEqual(1, 18)){
            return NBTEditor.getInt(tagCompund, key);
        }

        try{
            Method m = tagCompoundClass.getMethod("getInt", String.class);
            m.setAccessible(true);
            Object r = m.invoke(this.tagCompund, key);
            m.setAccessible(false);
            return r instanceof Integer ? (Integer) r : null;
        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    public void setInt(String key, Integer value){
        if(ServerVersion.greaterOrEqual(1, 18)){
            NBTEditor.setTag(tagCompund, key, value);
            return;
        }

        try{
            Method m = tagCompoundClass.getMethod("setInt", String.class, int.class);
            m.setAccessible(true);
            m.invoke(this.tagCompund, key, value);
            m.setAccessible(false);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

//    public long[] getLongArray(String key){
//        if(ServerVersion.greaterOrEqual(1, 18)){
//            return NBTEditor.getLongArray(tagCompund, key);
//        }
//
//        try{
//            Method m = tagCompoundClass.getMethod("getLongArray", String.class);
//            m.setAccessible(true);
//            Object r = m.invoke(this.tagCompund, key);
//            m.setAccessible(false);
//
//            return r instanceof long[] ? (long[]) r : null;
//        }
//        catch(Exception ex){
//            ex.printStackTrace();
//            return null;
//        }
//    }

    public int[] getIntArray(String key){
        if(ServerVersion.greaterOrEqual(1, 18)){
            return NBTEditor.getIntArray(tagCompund, key);
        }

        try{
            Method m = tagCompoundClass.getMethod("getIntArray", String.class);
            m.setAccessible(true);
            Object r = m.invoke(this.tagCompund, key);
            m.setAccessible(false);

            return r instanceof int[] ? (int[]) r : null;
        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    public byte[] getByteArray(String key){
        if(ServerVersion.greaterOrEqual(1, 18)){
            return NBTEditor.getByteArray(tagCompund, key);
        }

        try{
            Method m = tagCompoundClass.getMethod("getByteArray", String.class);
            m.setAccessible(true);
            Object r = m.invoke(this.tagCompund, key);
            m.setAccessible(false);

            return r instanceof byte[] ? (byte[]) r : null;
        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    public Byte getByte(String key){
        if(ServerVersion.greaterOrEqual(1, 18)){
            return NBTEditor.getByte(tagCompund, key);
        }

        try{
            Method m = tagCompoundClass.getMethod("getShort", String.class);
            m.setAccessible(true);
            Object r = m.invoke(this.tagCompund, key);
            m.setAccessible(false);

            return r instanceof Byte ? (Byte) r : null;
        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    public Short getShort(String key){
        if(ServerVersion.greaterOrEqual(1, 18)){
            return NBTEditor.getShort(tagCompund, key);
        }

        try{
            Method m = tagCompoundClass.getMethod("getShort", String.class);
            m.setAccessible(true);
            Object r = m.invoke(this.tagCompund, key);
            m.setAccessible(false);

            return r instanceof Short ? (Short) r : null;
        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    public Float getFloat(String key){
        if(ServerVersion.greaterOrEqual(1, 18)){
            return NBTEditor.getFloat(tagCompund, key);
        }

        try{
            Method m = tagCompoundClass.getMethod("getFloat", String.class);
            m.setAccessible(true);
            Object r = m.invoke(this.tagCompund, key);
            m.setAccessible(false);

            return r instanceof Float ? (Float) r : null;
        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    public Double getDouble(String key){
        if(ServerVersion.greaterOrEqual(1, 18)){
            return NBTEditor.getDouble(tagCompund, key);
        }

        try{
            Method m = tagCompoundClass.getMethod("getDouble", String.class);
            m.setAccessible(true);
            Object r = m.invoke(this.tagCompund, key);
            m.setAccessible(false);

            return r instanceof Double ? (Double) r : null;
        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    public void setDouble(String key, Double value){
        if(ServerVersion.greaterOrEqual(1, 18)){
            NBTEditor.setTag(tagCompund, key, value);
            return;
        }


        try{
            Method m = tagCompoundClass.getMethod("setDouble", String.class, double.class);
            m.setAccessible(true);
            m.invoke(this.tagCompund, key, value);
            m.setAccessible(false);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public Long getLong(String key){
        if(ServerVersion.greaterOrEqual(1, 18)){
            return NBTEditor.getLong(tagCompund, key);
        }

        try{
            Method m = tagCompoundClass.getMethod("getLong", String.class);
            m.setAccessible(true);
            Object r = m.invoke(this.tagCompund, key);
            m.setAccessible(false);
            return r instanceof Long ? (Long) r : null;
        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    public void setIntArray(String key, int[] value){
        if(ServerVersion.greaterOrEqual(1, 18)){
            NBTEditor.setTag(tagCompund, key, value);
            return;
        }

        try{
            Method m = tagCompoundClass.getMethod("setIntArray", String.class, int[].class);
            m.setAccessible(true);
            m.invoke(this.tagCompund, key, value);
            m.setAccessible(false);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void setLong(String key, Long value){
        if(ServerVersion.greaterOrEqual(1, 18)){
            NBTEditor.setTag(tagCompund, key, value);
            return;
        }


        try{
            Method m = tagCompoundClass.getMethod("setLong", String.class, long.class);
            m.setAccessible(true);
            m.invoke(this.tagCompund, key, value);
            m.setAccessible(false);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public Object getObjectRaw(String key){
        String name = ServerVersion.greaterOrEqual(1, 18) ? "c" : "get";
        try{
            Method m = tagCompoundClass.getMethod(name, String.class);
            m.setAccessible(true);
            Object r = m.invoke(this.tagCompund, key);
            m.setAccessible(false);
            return r == null ? null : new SafeNBTList(r);
        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    public SafeNBTList getList(String key){
        try{
            Object r = getObjectRaw(key);
            return r == null ? null : new SafeNBTList(r);
        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    public void setShort(String key, Short value){
        if(ServerVersion.greaterOrEqual(1, 18)){
            NBTEditor.setTag(tagCompund, key, value);
            return;
        }


        try{
            Method m = tagCompoundClass.getMethod("setShort", String.class, short.class);
            m.setAccessible(true);
            m.invoke(this.tagCompund, key, value);
            m.setAccessible(false);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }


    public Boolean getBoolean(String key){
        if(ServerVersion.greaterOrEqual(1, 18)){
            return NBTEditor.getBoolean(tagCompund, key);
        }

        try{
            Method m = tagCompoundClass.getMethod("getBoolean", String.class);
            m.setAccessible(true);
            Object o = m.invoke(this.tagCompund, key);
            m.setAccessible(false);

            return o instanceof Boolean ? (Boolean) o : null;
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        return null;
    }

    public void setBoolean(String key, Boolean value){
        if(ServerVersion.greaterOrEqual(1, 18)){
            NBTEditor.setTag(tagCompund, key, value);
            return;
        }

        try{
            Method m = tagCompoundClass.getMethod("setBoolean", String.class, boolean.class);
            m.setAccessible(true);
            m.invoke(this.tagCompund, key, value);
            m.setAccessible(false);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void set(String key, SafeNBT value){
        String name = ServerVersion.greaterOrEqual(1, 18) ? "a" : "set";

        try{
            Method m = tagCompoundClass.getMethod(name, String.class, nbtBaseClass);
            m.setAccessible(true);
            m.invoke(this.tagCompund, key, value.tagCompund);
            m.setAccessible(false);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void set(String key, SafeNBTList value){
        String name = ServerVersion.greaterOrEqual(1, 18) ? "a" : "set";

        try{
            Method m = tagCompoundClass.getMethod(name, String.class, nbtBaseClass);
            m.setAccessible(true);
            m.invoke(this.tagCompund, key, value.getTagList());
            m.setAccessible(false);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void setStrings(Map<String, String> map){
        if(ServerVersion.greaterOrEqual(1, 18)){
            map.forEach((String k, String v) -> NBTEditor.setTag(tagCompund, k, v));
            return;
        }

        try{
            Method m = tagCompoundClass.getMethod("setString", String.class, String.class);
            m.setAccessible(true);
            map.forEach((String key, String value) -> {
                try{
                    m.invoke(this.tagCompund, key, value);
                }
                catch(Exception ex){ex.printStackTrace();}
            });

            m.setAccessible(false);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public boolean hasKey(String key){
        String name = ServerVersion.greaterOrEqual(1, 18) ? "e" : "hasKey";

        try{
            Method m = tagCompoundClass.getMethod(name, String.class);
            m.setAccessible(true);
            Object o = m.invoke(this.tagCompund, key);
            m.setAccessible(false);

            return o instanceof Boolean && (Boolean) o;
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        return false;
    }

//    public boolean listForKeyIsEmpty(String key){
//
//    }

    public ItemStack apply(ItemStack item){
        if(item == null || item.getType() == Material.AIR){return null;}

        try{
            Method nmsGet = craftItemstackClass.getMethod("asNMSCopy", ItemStack.class);
            nmsGet.setAccessible(true);
            Object nmsStack = nmsGet.invoke(null, item);
            nmsGet.setAccessible(false);

            if(nmsStack == null){return null;}

            String set = ServerVersion.greaterOrEqual(1, 18) ? "c" : "setTag";
            Method nbtSet = nmsItemstackClass.getMethod(set, tagCompoundClass);
            nbtSet.setAccessible(true);
            nbtSet.invoke(nmsStack, this.tagCompund);
            nbtSet.setAccessible(false);

            Method m = craftItemstackClass.getMethod("asBukkitCopy", nmsItemstackClass);
            m.setAccessible(true);
            Object o = m.invoke(null, nmsStack);
            m.setAccessible(false);

            return o instanceof ItemStack ? (ItemStack) o : null;
        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }
//
//    public ItemStack applyInPlace(ItemStack item){
//        try{
//            Method nmsGet = craftItemstackClass.getMethod("getHandle");
//            nmsGet.setAccessible(true);
//            Object nmsStack = nmsGet.invoke(item);
//            nmsGet.setAccessible(false);
//
//            Field nbtSet = craftItemstackClass.getField("tag");
//            nbtSet.setAccessible(true);
//            nbtSet.set(nmsStack, this.tagCompund);//.invoke(nmsStack, this.tagCompund);
//            nbtSet.setAccessible(false);
//
////            Method m = craftItemstackClass.getMethod("asBukkitCopy", nmsItemstackClass);
////            m.setAccessible(true);
////            Object o = m.invoke(null, nmsStack);
////            m.setAccessible(false);
//
//            return item;//return o instanceof ItemStack ? (ItemStack) o : null;
//        }
//        catch(Exception ex){
//            ex.printStackTrace();
//            return null;
//        }
//    }

    public String toString(){
        return "SafeNBT(" + tagCompund + ")";
    }

    public String toJSON(){
        return Objects.toString(tagCompund);
    }

    public Set<String> getKeys(){
        try{
            Map m = null;
            if(ServerVersion.greaterOrEqual(1, 17)){
                try{
                    Field f = tagCompoundClass.getDeclaredField("x");
                    f.setAccessible(true);
                    m = (Map) f.get(tagCompund);
                    f.setAccessible(false);
                }
                catch(Exception ignore){}
                for(Field f : tagCompoundClass.getDeclaredFields()){
                    if(f.getType() == Map.class){
                        f.setAccessible(true);
                        m = (Map) f.get(tagCompund);
                        f.setAccessible(false);
                        break;
                    }
                }
            }
            else{
                Field f = tagCompoundClass.getDeclaredField("map");
                f.setAccessible(true);
                m = (Map) f.get(tagCompund);
                f.setAccessible(false);
            }

            return (Set<String>) m.keySet();
        }
        catch(Exception ex){
            ex.printStackTrace();
            return new HashSet<>();
        }

    }

    public static SafeNBT get(ItemStack item){
        try{
            Method m = craftItemstackClass.getMethod("asNMSCopy", ItemStack.class);
            m.setAccessible(true);
            Object nmsStack = m.invoke(null, item);
            m.setAccessible(false);

            if(item == null || nmsStack == null){
                return new SafeNBT();
            }

            String name = ServerVersion.greaterOrEqual(1, 18) ? "s" : "getTag";
            Method getCompound = nmsItemstackClass.getMethod(name);
            getCompound.setAccessible(true);
            Object nbtCompound = getCompound.invoke(nmsStack);
            getCompound.setAccessible(false);

            return new SafeNBT(nbtCompound);
        }
        catch(Exception ex){
            //ex.printStackTrace();
            return new SafeNBT();
        }
    }

}
