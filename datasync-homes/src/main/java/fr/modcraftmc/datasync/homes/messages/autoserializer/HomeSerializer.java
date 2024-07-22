package fr.modcraftmc.datasync.homes.messages.autoserializer;

import com.google.gson.JsonElement;
import fr.modcraftmc.crossservercore.api.message.autoserializer.FieldSerializer;
import fr.modcraftmc.datasync.homes.HomeManager;

import java.lang.reflect.Type;

public class HomeSerializer extends FieldSerializer<HomeManager.Home> {

    @Override
    public JsonElement serialize(HomeManager.Home value) {
        return value.serialize();
    }

    @Override
    public HomeManager.Home deserialize(JsonElement json, Type typeOfT) {
        return HomeManager.Home.deserialize(json.getAsJsonObject());
    }

    @Override
    public Type getType() {
        return HomeManager.Home.class;
    }
}
