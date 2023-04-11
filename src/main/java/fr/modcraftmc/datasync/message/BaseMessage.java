package fr.modcraftmc.datasync.message;

import com.google.gson.JsonObject;

public abstract class BaseMessage {
    String messageName;

    BaseMessage(String messageName) {
        this.messageName = messageName;
    }
    protected JsonObject Serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("messageName", messageName);
        return jsonObject;
    }

    public String SerializeToString() {
        return Serialize().toString();
    }

    protected abstract void Handle();
}
