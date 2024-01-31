package org.polymart.mcplugin.utils.nbt;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.tags.ItemTagType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.polymart.mcplugin.Main;
import org.polymart.mcplugin.utils.ServerVersion;

import java.lang.reflect.Field;
import java.util.Objects;

public class NBTUtils{

    public static ItemStack set(ItemStack is, String k, Object v){
        if(is == null){return null;}
        if(ServerVersion.greaterOrEqual(1, 18)){

            ItemMeta itemMeta = is.getItemMeta();
            if(itemMeta == null){
                return is;
            }
            set(itemMeta.getPersistentDataContainer(), k, v);
            is.setItemMeta(itemMeta);
        }
        else{
            SafeNBT nbt = SafeNBT.get(is);
            nbt.setObject(k, v);
            return nbt.apply(is);
        }
        return is;
    }

    public static <T> T get(ItemStack is, String k, Class<T> clazz){
        return get(is, k, clazz, null);
    }

    public static boolean getBoolean(ItemStack is, String k){
        return Boolean.TRUE.equals(get(is, k, Boolean.class, Boolean.FALSE));
    }

    public static <T> T get(ItemStack is, String k, Class<T> clazz, T def){
        try{
            if(is == null){
                return def;
            }
            if(ServerVersion.greaterOrEqual(1, 18)){

                ItemMeta itemMeta = is.getItemMeta();
                if(itemMeta == null){
                    return def;
                }

                if(clazz == Boolean.class || clazz == boolean.class){
                    Byte b = itemMeta.getPersistentDataContainer().get(makeKey(k), PersistentDataType.BYTE);
                    Boolean bd = def instanceof Boolean ? (Boolean) def : Boolean.FALSE;
                    Boolean bool = b == null ? bd : b > 0;
                    return clazz.cast(bool);
                }

                PersistentDataType use = getType(clazz, PersistentDataType.STRING);
                Object o = itemMeta.getPersistentDataContainer().get(makeKey(k), use);
                return clazz.isInstance(o) ? clazz.cast(o) : def;
            }
            else{
                SafeNBT nbt = SafeNBT.get(is);
                Object o = nbt.getObject(k);
                return clazz.isInstance(o) ? clazz.cast(o) : def;
            }
        }
        catch(Exception ex){ex.printStackTrace(); return def;}
    }

    private static PersistentDataContainer set(PersistentDataContainer c, String k, Object v){
        NamespacedKey key = makeKey(k);
        if(v == null){
            c.remove(key);
            return c;
        }

        if(v instanceof Boolean){
            c.set(key, PersistentDataType.BYTE, (byte) (((Boolean) v) ? 1 : 0));
            return c;
        }

        PersistentDataType use = getType(v.getClass(), PersistentDataType.STRING);
        c.set(key, use, v);
        return c;
    }

    private static PersistentDataType getType(Class<?> vc, PersistentDataType def){
        if(vc == Boolean.class || vc == boolean.class){
            return PersistentDataType.BYTE;
        }

        Class<PersistentDataType> clazz = PersistentDataType.class;
        PersistentDataType use = null;
        for(Field f : clazz.getFields()){
            try{
                f.setAccessible(true);
                if(f.getType() == clazz){
                    PersistentDataType t = clazz.cast(f.get(null));
                    if(t.getPrimitiveType() == vc || t.getComplexType() == vc){
                        use = t;
                        break;
                    }
                }
                f.setAccessible(false);
            }
            catch(Exception ignore){}
        }

        return use == null ? def : use;
    }

    private static NamespacedKey makeKey(String k){
        return new NamespacedKey(Main.that, k);
    }

}
