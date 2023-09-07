package fr.modcraftmc.datasync.homes.messages;

import com.google.gson.JsonObject;
import fr.modcraftmc.crossservercore.message.BaseMessage;
import fr.modcraftmc.datasync.homes.DatasyncHomes;

public class ChangeGlobalHomesLimit extends BaseMessage {
    public static final String MESSAGE_NAME = "change_global_homes_limit";
    private final int limit;

    public ChangeGlobalHomesLimit(int limit) {
        super(MESSAGE_NAME);
        this.limit = limit;
    }

    @Override
    protected JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        jsonObject.addProperty("limit", limit);
        return jsonObject;
    }

    public static ChangeGlobalHomesLimit deserialize(JsonObject jsonObject) {
        return new ChangeGlobalHomesLimit(
                jsonObject.get("limit").getAsInt()
        );
    }

    @Override
    public String getMessageName() {
        return MESSAGE_NAME;
    }

    @Override
    protected void handle() {
        DatasyncHomes.homeManager.setGlobalHomesLimit(limit);
    }
}
