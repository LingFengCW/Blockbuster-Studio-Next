/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonDeserializationContext
 *  com.google.gson.JsonDeserializer
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  net.minecraft.resources.Identifier
 *  net.minecraft.util.GsonHelper
 *  net.minecraft.util.valueproviders.ConstantFloat
 *  net.minecraft.util.valueproviders.FloatProvider
 *  net.minecraft.util.valueproviders.SampledFloat
 *  org.apache.commons.lang3.Validate
 */
package net.minecraft.client.resources.sounds;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.SampledFloat;
import org.apache.commons.lang3.Validate;

public class SoundEventRegistrationSerializer
implements JsonDeserializer<SoundEventRegistration> {
    private static final FloatProvider DEFAULT_FLOAT = ConstantFloat.of((float)1.0f);

    public SoundEventRegistration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = GsonHelper.convertToJsonObject((JsonElement)json, (String)"entry");
        boolean replace = GsonHelper.getAsBoolean((JsonObject)object, (String)"replace", (boolean)false);
        String subtitle = GsonHelper.getAsString((JsonObject)object, (String)"subtitle", null);
        List<Sound> sounds = this.getSounds(object);
        return new SoundEventRegistration(sounds, replace, subtitle);
    }

    private List<Sound> getSounds(JsonObject object) {
        ArrayList result = Lists.newArrayList();
        if (object.has("sounds")) {
            JsonArray array = GsonHelper.getAsJsonArray((JsonObject)object, (String)"sounds");
            for (int i = 0; i < array.size(); ++i) {
                JsonElement element = array.get(i);
                if (GsonHelper.isStringValue((JsonElement)element)) {
                    Identifier name = Identifier.parse((String)GsonHelper.convertToString((JsonElement)element, (String)"sound"));
                    result.add(new Sound(name, (SampledFloat)DEFAULT_FLOAT, (SampledFloat)DEFAULT_FLOAT, 1, Sound.Type.FILE, false, false, 16));
                    continue;
                }
                result.add(this.getSound(GsonHelper.convertToJsonObject((JsonElement)element, (String)"sound")));
            }
        }
        return result;
    }

    private Sound getSound(JsonObject object) {
        Identifier name = Identifier.parse((String)GsonHelper.getAsString((JsonObject)object, (String)"name"));
        Sound.Type type = this.getType(object, Sound.Type.FILE);
        float volume = GsonHelper.getAsFloat((JsonObject)object, (String)"volume", (float)1.0f);
        Validate.isTrue((volume > 0.0f ? 1 : 0) != 0, (String)"Invalid volume", (Object[])new Object[0]);
        float pitch = GsonHelper.getAsFloat((JsonObject)object, (String)"pitch", (float)1.0f);
        Validate.isTrue((pitch > 0.0f ? 1 : 0) != 0, (String)"Invalid pitch", (Object[])new Object[0]);
        int weight = GsonHelper.getAsInt((JsonObject)object, (String)"weight", (int)1);
        Validate.isTrue((weight > 0 ? 1 : 0) != 0, (String)"Invalid weight", (Object[])new Object[0]);
        boolean preload = GsonHelper.getAsBoolean((JsonObject)object, (String)"preload", (boolean)false);
        boolean stream = GsonHelper.getAsBoolean((JsonObject)object, (String)"stream", (boolean)false);
        int attenuationDistance = GsonHelper.getAsInt((JsonObject)object, (String)"attenuation_distance", (int)16);
        return new Sound(name, (SampledFloat)ConstantFloat.of((float)volume), (SampledFloat)ConstantFloat.of((float)pitch), weight, type, stream, preload, attenuationDistance);
    }

    private Sound.Type getType(JsonObject sound, Sound.Type fallback) {
        Sound.Type type = fallback;
        if (sound.has("type")) {
            type = Sound.Type.getByName(GsonHelper.getAsString((JsonObject)sound, (String)"type"));
            Objects.requireNonNull(type, "Invalid type");
        }
        return type;
    }
}

