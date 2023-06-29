package fr.modcraftmc.datasync.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.ftbsync.FTBSync;

public class SyncQuests extends BaseMessage {
public static final String MESSAGE_NAME = "SyncQuests";
    public JsonElement questsData;

    public SyncQuests(JsonElement questsData) {
        super(MESSAGE_NAME);
        this.questsData = questsData;
    }

    @Override
    protected JsonObject serialize() {
        JsonObject json = super.serialize();
        json.add("questsData", questsData);
        return json;
    }

    public static SyncQuests deserialize(JsonObject json) {
        JsonElement questsData = json.get("questsData");
        return new SyncQuests(questsData);
    }

    @Override
    protected void handle() {
        FTBSync.handleSyncQuests(this);
    }
}
