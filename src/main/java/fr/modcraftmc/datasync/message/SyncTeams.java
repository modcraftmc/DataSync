package fr.modcraftmc.datasync.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.ftbsync.FTBSync;

public class SyncTeams extends BaseMessage {
    public static final String MESSAGE_NAME = "SyncTeams";
    public String teamUUID;
    public String teamType;
    public boolean remove;
    public JsonElement teamsData;

    public SyncTeams(String teamUUID, String teamType, JsonElement teamsData) {
        this(teamUUID, teamType, teamsData, false);
    }

    public SyncTeams(String teamUUID, String teamType, JsonElement teamsData, boolean remove) {
        super(MESSAGE_NAME);
        this.teamUUID = teamUUID;
        this.teamType = teamType;
        this.teamsData = teamsData;
        this.remove = remove;
    }

    @Override
    protected JsonObject serialize() {
        JsonObject json = super.serialize();
        json.addProperty("teamUUID", teamUUID);
        json.addProperty("teamType", teamType);
        json.addProperty("remove", remove);
        json.add("teamsData", teamsData);
        return json;
    }

    public static SyncTeams deserialize(JsonObject json) {
        String teamUUID = json.get("teamUUID").getAsString();
        String teamType = json.get("teamType").getAsString();
        boolean remove = json.get("remove").getAsBoolean();
        JsonElement teamsData = json.get("teamsData");
        return new SyncTeams(teamUUID, teamType, teamsData, remove);
    }

    @Override
    protected void handle() {
        FTBSync.handleTeamSync(this);
    }
}
