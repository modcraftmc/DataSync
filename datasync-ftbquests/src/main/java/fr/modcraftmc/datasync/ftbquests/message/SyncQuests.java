package fr.modcraftmc.datasync.ftbquests.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.datasync.ftbquests.DatasyncFtbQuests;

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

    @Override
    public String getMessageName() {
        return MESSAGE_NAME;
    }

    public static SyncQuests deserialize(JsonObject json) {
        JsonElement questsData = json.get("questsData");
        return new SyncQuests(questsData);
    }

    @Override
    public void handle() {
        DatasyncFtbQuests.questsSynchronizer.handleSyncQuests(this);
    }
}
