package org.polymart.mcplugin.utils.nbt;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.polymart.mcplugin.Main;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class SafeNBT {

  private static final String version =
      "net.minecraft.server."
          + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
  private static final String cbVersion =
      "org.bukkit.craftbukkit."
          + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
  static Class<?> tagCompoundClass;
  static Class<?> nbtBaseClass;
  static Class<?> nmsItemstackClass;
  static Class<?> craftItemstackClass;
  static Class<?> mojangsonParserClass;

  static {
    try {
      tagCompoundClass = Class.forName(version + ".NBTTagCompound");
      nbtBaseClass = Class.forName(version + ".NBTBase");
      nmsItemstackClass = Class.forName(version + ".ItemStack");
      craftItemstackClass = Class.forName(cbVersion + ".inventory.CraftItemStack");
      mojangsonParserClass = Class.forName(version + ".MojangsonParser");
    } catch (Exception ex) {
      ex.printStackTrace();
      Bukkit.getPluginManager().disablePlugin(Main.that);
    }
  }

  private final Object tagCompund;

  public SafeNBT() {
    this(null);
  }

  public SafeNBT(Object tagCompound) {
    Object toSet = tagCompound;
    if (tagCompound == null) {
      try {
        toSet = tagCompoundClass.newInstance();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
    this.tagCompund = toSet;
  }

  public static SafeNBT getFromJSON(String json) {
    try {
      Method m = mojangsonParserClass.getMethod("parse", String.class);
      m.setAccessible(true);
      Object o = m.invoke(null, json);
      m.setAccessible(false);
      return new SafeNBT(o);
    } catch (Exception ignore) {
    }
    return null;
  }

  public static SafeNBT get(ItemStack item) {
    try {
      Method m = craftItemstackClass.getMethod("asNMSCopy", ItemStack.class);
      m.setAccessible(true);
      Object nmsStack = m.invoke(null, item);
      m.setAccessible(false);

      Method getCompound = nmsItemstackClass.getMethod("getTag");
      getCompound.setAccessible(true);
      Object nbtCompound = getCompound.invoke(nmsStack);
      getCompound.setAccessible(false);

      return new SafeNBT(nbtCompound);
    } catch (Exception ex) {
      ex.printStackTrace();
      return new SafeNBT();
    }
  }

  public Object getTagCompund() {
    return tagCompund;
  }

  public SafeNBT getCompound(String key) {
    try {
      Method m = tagCompoundClass.getMethod("getCompound", String.class);
      m.setAccessible(true);
      Object r = m.invoke(this.tagCompund, key);
      m.setAccessible(false);
      return r == null ? null : new SafeNBT(r);
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  public void remove(String key) {
    try {
      Method m = tagCompoundClass.getMethod("remove", String.class);
      m.setAccessible(true);
      m.invoke(this.tagCompund, key);
      m.setAccessible(false);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public void setObject(String key, Object o) {
    if (o instanceof String) {
      setString(key, (String) o);
    } else if (o instanceof Integer) {
      setInt(key, (Integer) o);
    } else if (o instanceof Double) {
      setDouble(key, (Double) o);
    } else if (o instanceof Long) {
      setLong(key, (Long) o);
    } else if (o instanceof List) {
      SafeNBTList list = new SafeNBTList();
      for (Object e : (List) o) {
        if (e instanceof Map) {
          SafeNBT mapNBT = new SafeNBT();
          for (Object k : ((Map) e).keySet()) {
            if (k instanceof String) {
              Object v = ((Map) e).get(k);
              mapNBT.setObject((String) k, v);
            }
          }
          list.add(mapNBT);
        } else {
          list.addGeneric(e);
        }
      }
      set(key, list);
    }
  }

  public String getString(String key) {
    try {
      Method m = tagCompoundClass.getMethod("getString", String.class);
      m.setAccessible(true);
      Object r = m.invoke(this.tagCompund, key);
      m.setAccessible(false);
      return r instanceof String ? (String) r : null;
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  public void setString(String key, String value) {
    try {
      Method m = tagCompoundClass.getMethod("setString", String.class, String.class);
      m.setAccessible(true);
      m.invoke(this.tagCompund, key, value);
      m.setAccessible(false);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public Object getObject(String key) {
    Object o;

    for (SafeNBTBaseType type : SafeNBTBaseType.values()) {
      o = getObject(key, type);
      if (o != null) {
        if (type == SafeNBTBaseType.STRING && o.toString().length() == 0) {
          continue;
        }
        if (o instanceof Number && ((Number) o).intValue() == 0) {
          continue;
        }

        return o;
      }
    }
    return null;
  }

  public Object getObject(String key, SafeNBTBaseType type) {
    try {
      Method m = tagCompoundClass.getMethod("get" + type.getName(), String.class);
      m.setAccessible(true);
      Object o = m.invoke(this.tagCompund, key);
      m.setAccessible(false);
      return o;
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }

  public Integer getInt(String key) {
    try {
      Method m = tagCompoundClass.getMethod("getInt", String.class);
      m.setAccessible(true);
      Object r = m.invoke(this.tagCompund, key);
      m.setAccessible(false);
      return r instanceof Integer ? (Integer) r : null;
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  public void setInt(String key, Integer value) {
    try {
      Method m = tagCompoundClass.getMethod("setInt", String.class, int.class);
      m.setAccessible(true);
      m.invoke(this.tagCompund, key, value);
      m.setAccessible(false);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public Double getDouble(String key) {
    try {
      Method m = tagCompoundClass.getMethod("getDouble", String.class);
      m.setAccessible(true);
      Object r = m.invoke(this.tagCompund, key);
      m.setAccessible(false);

      return r instanceof Double ? (Double) r : null;
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  public void setDouble(String key, Double value) {
    try {
      Method m = tagCompoundClass.getMethod("setDouble", String.class, double.class);
      m.setAccessible(true);
      m.invoke(this.tagCompund, key, value);
      m.setAccessible(false);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public Long getLong(String key) {
    try {
      Method m = tagCompoundClass.getMethod("getLong", String.class);
      m.setAccessible(true);
      Object r = m.invoke(this.tagCompund, key);
      m.setAccessible(false);
      return r instanceof Long ? (Long) r : null;
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  public void setIntArray(String key, int[] value) {
    try {
      Method m = tagCompoundClass.getMethod("setIntArray", String.class, int[].class);
      m.setAccessible(true);
      m.invoke(this.tagCompund, key, value);
      m.setAccessible(false);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public void setLong(String key, Long value) {
    try {
      Method m = tagCompoundClass.getMethod("setLong", String.class, long.class);
      m.setAccessible(true);
      m.invoke(this.tagCompund, key, value);
      m.setAccessible(false);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public SafeNBTList getList(String key) {
    try {
      Method m = tagCompoundClass.getMethod("get", String.class);
      m.setAccessible(true);
      Object r = m.invoke(this.tagCompund, key);
      m.setAccessible(false);
      return r == null ? null : new SafeNBTList(r);
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  public void setShort(String key, Short value) {
    try {
      Method m = tagCompoundClass.getMethod("setShort", String.class, short.class);
      m.setAccessible(true);
      m.invoke(this.tagCompund, key, value);
      m.setAccessible(false);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public void setBoolean(String key, Boolean value) {
    try {
      Method m = tagCompoundClass.getMethod("setBoolean", String.class, boolean.class);
      m.setAccessible(true);
      m.invoke(this.tagCompund, key, value);
      m.setAccessible(false);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public boolean getBoolean(String key) {
    try {
      Method m = tagCompoundClass.getMethod("getBoolean", String.class);
      m.setAccessible(true);
      Boolean b = (Boolean) m.invoke(this.tagCompund, key);
      m.setAccessible(false);
      return b;
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return false;
  }

  public void set(String key, SafeNBT value) {
    try {
      Method m = tagCompoundClass.getMethod("set", String.class, nbtBaseClass);
      m.setAccessible(true);
      m.invoke(this.tagCompund, key, value.tagCompund);
      m.setAccessible(false);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public void set(String key, SafeNBTList value) {
    try {
      Method m = tagCompoundClass.getMethod("set", String.class, nbtBaseClass);
      m.setAccessible(true);
      m.invoke(this.tagCompund, key, value.getTagList());
      m.setAccessible(false);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public void setStrings(Map<String, String> map) {
    try {
      Method m = tagCompoundClass.getMethod("setString", String.class, String.class);
      m.setAccessible(true);
      map.forEach(
          (String key, String value) -> {
            try {
              m.invoke(this.tagCompund, key, value);
            } catch (Exception ex) {
              ex.printStackTrace();
            }
          });

      m.setAccessible(false);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  //    public boolean listForKeyIsEmpty(String key){
  //
  //    }

  public boolean hasKey(String key) {
    try {
      Method m = tagCompoundClass.getMethod("hasKey", String.class);
      m.setAccessible(true);
      Object o = m.invoke(this.tagCompund, key);
      m.setAccessible(false);

      return o instanceof Boolean && (Boolean) o;
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return false;
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

  public ItemStack apply(ItemStack item) {
    try {
      Method nmsGet = craftItemstackClass.getMethod("asNMSCopy", ItemStack.class);
      nmsGet.setAccessible(true);
      Object nmsStack = nmsGet.invoke(null, item);
      nmsGet.setAccessible(false);

      Method nbtSet = nmsItemstackClass.getMethod("setTag", tagCompoundClass);
      nbtSet.setAccessible(true);
      nbtSet.invoke(nmsStack, this.tagCompund);
      nbtSet.setAccessible(false);

      Method m = craftItemstackClass.getMethod("asBukkitCopy", nmsItemstackClass);
      m.setAccessible(true);
      Object o = m.invoke(null, nmsStack);
      m.setAccessible(false);

      return o instanceof ItemStack ? (ItemStack) o : null;
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  public String toString() {
    return "SafeNBT(" + tagCompund + ")";
  }

  public String toJSON() {
    return Objects.toString(tagCompund);
  }

  public Set<String> getKeys() {
    try {
      Field f = tagCompoundClass.getDeclaredField("map");
      f.setAccessible(true);
      Map m = (Map) f.get(tagCompund);
      f.setAccessible(false);

      return (Set<String>) m.keySet();
    } catch (Exception ex) {
      ex.printStackTrace();
      return new HashSet<>();
    }
  }
}
