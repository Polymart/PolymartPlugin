package org.polymart.mcplugin.utils;

import com.google.gson.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JSONWrapper{

    //Initialization
    public JsonObject json;
    public JsonArray list;
    public JsonElement value = null;

    public JSONWrapper(){
        this.json = null;
    }

    public JSONWrapper(String value){
        if(value == null){this.json = null;}

        try{this.json = (JsonObject) new JsonParser().parse(value);}
        catch(Exception ex){this.json = null;}
    }

//    public JSONWrapper(List<Object> list){
//        this.list = list;
////        JsonArray arr = new JsonArray();
////        this.value = new JsonArray();
//    }

    public JSONWrapper(JsonElement unwrapped){
        this.value = unwrapped;
        if(this.value.isJsonObject()){
            this.json = this.value.getAsJsonObject();
        }
    }
    public JSONWrapper(JsonObject unwrapped){this.json = unwrapped;}
    public JSONWrapper(JsonArray unwrapped){this.list = unwrapped;}

    //NotNull
    public JSONWrapper get(String key){
        if(this.json != null){
            JSONWrapper clone = this.clone();
            clone.value = this.json.has(key) ? this.json.get(key) : null;
            clone.json = this.json.has(key) && clone.value.isJsonObject() ? clone.value.getAsJsonObject() : null;

            if(clone.value != null && clone.value.isJsonArray()){
                clone.list = clone.value.getAsJsonArray();
            }
            return clone;
        }

        return new JSONWrapper();
    }

    //NotNull
    public JSONWrapper get(int index){
        if(this.list != null){
            JSONWrapper clone = this.clone();
            Object obj = this.list.size() <= index ? null : this.list.get(index);

            if(obj instanceof JsonObject){clone.json = (JsonObject) obj;}
            if(obj instanceof JsonArray){clone.list = (JsonArray) obj;}
            clone.value = (JsonElement) obj;
            return clone;
        }
        List<String> array = this.asStringList();

        if(array != null){return new JSONWrapper(array.get(index));}
        else{return new JSONWrapper();}
    }

    public List<JSONWrapper> iterator(){
        List<JSONWrapper> list = new ArrayList<>();

        for(Object obj : this.list == null ? new ArrayList<>() : this.list){
            if(obj instanceof JsonObject){list.add(new JSONWrapper((JsonObject) obj));}
            else if(obj instanceof JsonArray){list.add(new JSONWrapper((JsonArray) obj));}
            //else{list.add(new JSONWrapper(obj));}
        }

        return list;
    }

    public boolean isNull(){
        return this.value == null || this.value.isJsonNull();
    }

    public String asString(){return this.asString(null);}
    public String asString(String defaultValue){
        if(!isNull() && this.value.getAsString() != null){return this.value.getAsString();}
        if(this.asInteger() != null){return Objects.toString(this.asInteger());}
        return defaultValue;
    }

    public Boolean asBoolean(){return this.asBoolean(null);}
    public Boolean asBoolean(Boolean defaultValue){
        if(!isNull()){return this.value.getAsBoolean();}
        return defaultValue;
    }

    public Integer asInteger(){return this.asInteger(null);}
    public Integer asInteger(Integer defaultValue){
        try{
            if(!isNull()){
                return this.value.getAsInt();
            }
        }
        catch(Exception ignore){}
        return defaultValue;
    }

    public Float asFloat(){return this.asFloat(null);}
    public Float asFloat(Float defaultValue){
        if(!isNull()){return (Float) this.value.getAsFloat();}
        return defaultValue;
    }

    public List<String> asStringList(){return this.asStringList(null);}

    public List<String> asStringList(List<String> defaultValue){
        if(this.value instanceof List){
            List<String> values = new ArrayList<>();
            for(Object obj : (List) this.value){
                if(obj instanceof String){values.add((String) obj);}
            }

            return values;
        }
        else if(this.value instanceof JsonArray){
            JsonArray a = (JsonArray) this.value;

            List<String> values = new ArrayList<>();
            for(JsonElement e : a){
                values.add(e.getAsString());
            }

            return values;
        }
        return defaultValue;
    }

    public JSONWrapper clone(){
        JSONWrapper json = new JSONWrapper();
        json.json = this.json;
        json.list = this.list;
        json.value = this.value;

        return json;
    }

    public List<JSONWrapper> asJSONWrapperList(){
        List<JSONWrapper> result = new ArrayList<>();
        if(this.value != null && this.value.isJsonArray()){
            JsonArray array = this.value.getAsJsonArray();
            array.forEach((JsonElement e) -> {
                result.add(new JSONWrapper(e));
            });
        }
        return result;
    }
}

