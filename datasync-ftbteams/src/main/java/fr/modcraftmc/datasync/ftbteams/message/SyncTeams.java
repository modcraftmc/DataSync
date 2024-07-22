package fr.modcraftmc.datasync.ftbteams.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.modcraftmc.crossservercore.api.annotation.AutoRegister;
import fr.modcraftmc.crossservercore.api.annotation.AutoSerialize;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.datasync.ftbteams.DatasyncFtbTeam;
import fr.modcraftmc.datasync.ftbteams.TeamsSynchronizer;

import java.util.UUID;

@AutoRegister("sync_teams")
public class SyncTeams extends BaseMessage {
    @AutoSerialize
    public UUID teamUUID;
    @AutoSerialize
    public String teamType;
    @AutoSerialize
    public boolean remove;
    @AutoSerialize
    public JsonElement teamsData;

    public SyncTeams() {}

    public SyncTeams(UUID teamUUID, String teamType, JsonElement teamsData) {
        this(teamUUID, teamType, teamsData, false);
    }

    public SyncTeams(UUID teamUUID, String teamType, JsonElement teamsData, boolean remove) {
        this.teamUUID = teamUUID;
        this.teamType = teamType;
        this.teamsData = teamsData;
        this.remove = remove;
    }

    @Override
    public void handle() {
        DatasyncFtbTeam.teamsSynchronizer.handleTeamSync(this);
    }
}
