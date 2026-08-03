/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonDeserializationContext
 *  com.google.gson.JsonDeserializer
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  com.mojang.math.Quadrant
 *  net.minecraft.core.Direction
 *  net.minecraft.util.GsonHelper
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.resources.model.cuboid;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.math.Quadrant;
import java.lang.reflect.Type;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;

public record CuboidFace(@Nullable Direction cullForDirection, int tintIndex, String texture, @Nullable UVs uvs, Quadrant rotation) {
    public static final int NO_TINT = -1;

    public static float getU(UVs uvs, Quadrant rotation, int vertex) {
        return uvs.getVertexU(rotation.rotateVertexIndex(vertex)) / 16.0f;
    }

    public static float getV(UVs uvs, Quadrant rotation, int index) {
        return uvs.getVertexV(rotation.rotateVertexIndex(index)) / 16.0f;
    }

    public record UVs(float minU, float minV, float maxU, float maxV) {
        public float getVertexU(int index) {
            return index == 0 || index == 1 ? this.minU : this.maxU;
        }

        public float getVertexV(int index) {
            return index == 0 || index == 3 ? this.minV : this.maxV;
        }
    }

    protected static class Deserializer
    implements JsonDeserializer<CuboidFace> {
        private static final int DEFAULT_TINT_INDEX = -1;
        private static final int DEFAULT_ROTATION = 0;

        protected Deserializer() {
        }

        public CuboidFace deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();
            Direction cullDirection = Deserializer.getCullFacing(object);
            int tintIndex = Deserializer.getTintIndex(object);
            String texture = Deserializer.getTexture(object);
            UVs uvs = Deserializer.getUVs(object);
            Quadrant rotation = Deserializer.getRotation(object);
            return new CuboidFace(cullDirection, tintIndex, texture, uvs, rotation);
        }

        private static int getTintIndex(JsonObject object) {
            return GsonHelper.getAsInt((JsonObject)object, (String)"tintindex", (int)-1);
        }

        private static String getTexture(JsonObject object) {
            return GsonHelper.getAsString((JsonObject)object, (String)"texture");
        }

        private static @Nullable Direction getCullFacing(JsonObject object) {
            String cullFace = GsonHelper.getAsString((JsonObject)object, (String)"cullface", (String)"");
            return Direction.byName((String)cullFace);
        }

        private static Quadrant getRotation(JsonObject object) {
            int rotation = GsonHelper.getAsInt((JsonObject)object, (String)"rotation", (int)0);
            return Quadrant.parseJson((int)rotation);
        }

        private static @Nullable UVs getUVs(JsonObject object) {
            if (!object.has("uv")) {
                return null;
            }
            JsonArray uvArray = GsonHelper.getAsJsonArray((JsonObject)object, (String)"uv");
            if (uvArray.size() != 4) {
                throw new JsonParseException("Expected 4 uv values, found: " + uvArray.size());
            }
            float minU = GsonHelper.convertToFloat((JsonElement)uvArray.get(0), (String)"minU");
            float minV = GsonHelper.convertToFloat((JsonElement)uvArray.get(1), (String)"minV");
            float maxU = GsonHelper.convertToFloat((JsonElement)uvArray.get(2), (String)"maxU");
            float maxV = GsonHelper.convertToFloat((JsonElement)uvArray.get(3), (String)"maxV");
            return new UVs(minU, minV, maxU, maxV);
        }
    }
}

