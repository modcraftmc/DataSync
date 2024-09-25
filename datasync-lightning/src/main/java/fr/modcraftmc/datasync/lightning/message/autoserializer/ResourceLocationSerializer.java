package fr.modcraftmc.datasync.lightning.message.autoserializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import fr.modcraftmc.crossservercore.api.message.autoserializer.FieldSerializer;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Type;

public class ResourceLocationSerializer extends FieldSerializer<ResourceLocation> {
    @Override
    public JsonElement serialize(ResourceLocation value) {
        return new JsonPrimitive(value.toString());
    }

    @Override
    public ResourceLocation deserialize(JsonElement json, Type typeOfT) {
        return new ResourceLocation(json.getAsString());
    }

    @Override
    public Type getType() {
        return ResourceLocation.class;
    }
}
