package fr.modcraftmc.datasync.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.ftbsync.FTBSync;

public class SyncTeamQuests extends BaseMessage {
    public static final String MESSAGE_NAME = "SyncTeamQuests";
    public String teamUUID;
    public JsonElement questsData;

    public SyncTeamQuests(String teamUUID, JsonElement questsData) {
        super(MESSAGE_NAME);
        this.teamUUID = teamUUID;
        this.questsData = questsData;
    }

    @Override
    protected JsonObject serialize() {
        JsonObject json = super.serialize();
        json.addProperty("teamUUID", teamUUID);
        json.add("questsData", questsData);
        return json;
    }

    public static SyncTeamQuests deserialize(JsonObject json) {
        String teamUUID = json.get("teamUUID").getAsString();
        JsonElement questsData = json.get("questsData");
        return new SyncTeamQuests(teamUUID, questsData);
    }

    @Override
    protected void handle() {
        FTBSync.handleTeamQuestsSync(this);
    }

}
