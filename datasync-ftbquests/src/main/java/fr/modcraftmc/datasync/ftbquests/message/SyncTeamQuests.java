package fr.modcraftmc.datasync.ftbquests.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.datasync.ftbquests.DatasyncFtbQuests;

public class SyncTeamQuests extends BaseMessage {
    public static final String MESSAGE_NAME = "sync_team_quests";
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

    @Override
    public String getMessageName() {
        return MESSAGE_NAME;
    }

    public static SyncTeamQuests deserialize(JsonObject json) {
        String teamUUID = json.get("teamUUID").getAsString();
        JsonElement questsData = json.get("questsData");
        return new SyncTeamQuests(teamUUID, questsData);
    }

    @Override
    public void handle() {
        DatasyncFtbQuests.questsSynchronizer.handleTeamQuestsSync(this);
    }

}
