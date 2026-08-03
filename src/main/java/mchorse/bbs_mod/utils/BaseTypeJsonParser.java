package mchorse.bbs_mod.utils;

import mchorse.bbs_mod.data.types.ByteArrayType;
import mchorse.bbs_mod.data.types.ByteType;
import mchorse.bbs_mod.data.types.DoubleType;
import mchorse.bbs_mod.data.types.FloatType;
import mchorse.bbs_mod.data.types.IntArrayType;
import mchorse.bbs_mod.data.types.IntType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.LongType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.data.types.ShortArrayType;
import mchorse.bbs_mod.data.types.ShortType;
import mchorse.bbs_mod.data.types.StringType;
import mchorse.bbs_mod.data.types.BaseType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Map;

/**
 * Parses the classic Blockbuster DataStorage JSON representation into the
 * local {@link BaseType} tree. The original writer wraps every value in a
 * type-tagged object: {"map": {...}}, {"list": [...]}, {"string": "..."},
 * {"int": 5}, ... . This is the format found inside .mcpr / film exports
 * of older Blockbuster versions.
 */
public class BaseTypeJsonParser
{
    public static BaseType parse(String json)
    {
        if (json == null || json.isEmpty())
        {
            return null;
        }

        try
        {
            return fromJson(JsonParser.parseString(json));
        }
        catch (Exception e)
        {
            mchorse.bbs_mod.BBSMod.LOGGER.error("Failed to parse BaseType JSON", e);
        }

        return null;
    }

    public static BaseType fromJson(JsonElement el)
    {
        if (el == null || el.isJsonNull())
        {
            return null;
        }

        if (el.isJsonPrimitive())
        {
            var p = el.getAsJsonPrimitive();

            if (p.isString()) return new StringType(p.getAsString());
            if (p.isBoolean()) return new ByteType((byte) (p.getAsBoolean() ? 1 : 0));
            if (p.isNumber())
            {
                double d = p.getAsDouble();

                if (d == Math.floor(d) && Math.abs(d) < 2_147_483_647L)
                {
                    return new IntType((int) d);
                }

                return new DoubleType(d);
            }
        }

        if (el.isJsonArray())
        {
            ListType list = new ListType();

            for (JsonElement child : el.getAsJsonArray())
            {
                list.add(fromJson(child));
            }

            return list;
        }

        if (el.isJsonObject())
        {
            JsonObject obj = el.getAsJsonObject();

            /* Single-key type wrapper: {"map": {...}}. */
            if (obj.keySet().size() == 1)
            {
                Map.Entry<String, JsonElement> entry = obj.entrySet().iterator().next();
                String type = entry.getKey();
                JsonElement val = entry.getValue();

                switch (type)
                {
                    case "map":
                    {
                        MapType map = new MapType();

                        for (Map.Entry<String, JsonElement> e : obj.entrySet().iterator().next().getValue().getAsJsonObject().entrySet())
                        {
                            map.put(e.getKey(), fromJson(e.getValue()));
                        }

                        return map;
                    }
                    case "list":
                    {
                        ListType list = new ListType();

                        for (JsonElement child : val.getAsJsonArray())
                        {
                            list.add(fromJson(child));
                        }

                        return list;
                    }
                    case "string": return new StringType(val.getAsString());
                    case "byte": return new ByteType(val.getAsByte());
                    case "short": return new ShortType(val.getAsShort());
                    case "int": return new IntType(val.getAsInt());
                    case "float": return new FloatType(val.getAsFloat());
                    case "long": return new LongType(val.getAsLong());
                    case "double": return new DoubleType(val.getAsDouble());
                    case "boolean": return new ByteType((byte) (val.getAsBoolean() ? 1 : 0));
                    case "byte_array":
                    {
                        JsonArray arr = val.getAsJsonArray();
                        byte[] bytes = new byte[arr.size()];

                        for (int i = 0; i < arr.size(); i++) bytes[i] = arr.get(i).getAsByte();

                        return new ByteArrayType(bytes);
                    }
                    case "int_array":
                    {
                        JsonArray arr = val.getAsJsonArray();
                        int[] ints = new int[arr.size()];

                        for (int i = 0; i < arr.size(); i++) ints[i] = arr.get(i).getAsInt();

                        return new IntArrayType(ints);
                    }
                    case "short_array":
                    {
                        JsonArray arr = val.getAsJsonArray();
                        short[] shorts = new short[arr.size()];

                        for (int i = 0; i < arr.size(); i++) shorts[i] = arr.get(i).getAsShort();

                        return new ShortArrayType(shorts);
                    }
                    default:
                    {
                        /* Unknown wrapper: try the raw value as a map. */
                        return fromJson(val);
                    }
                }
            }

            /* No type wrapper: treat as a plain map. */
            MapType map = new MapType();

            for (Map.Entry<String, JsonElement> e : obj.entrySet())
            {
                map.put(e.getKey(), fromJson(e.getValue()));
            }

            return map;
        }

        return null;
    }
}
