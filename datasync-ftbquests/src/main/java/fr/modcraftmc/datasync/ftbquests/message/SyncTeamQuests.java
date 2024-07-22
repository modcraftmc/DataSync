package fr.modcraftmc.datasync.ftbquests.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.modcraftmc.crossservercore.api.annotation.AutoRegister;
import fr.modcraftmc.crossservercore.api.annotation.AutoSerialize;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.datasync.ftbquests.DatasyncFtbQuests;

import java.util.UUID;

@AutoRegister("sync_team_quests")
public class SyncTeamQuests extends BaseMessage {
    @AutoSerialize
    public UUID teamUUID;
    @AutoSerialize
    public JsonElement questsData;

    public SyncTeamQuests(UUID teamUUID, JsonElement questsData) {
        this.teamUUID = teamUUID;
        this.questsData = questsData;
    }

    @Override
    public void handle() {
        DatasyncFtbQuests.questsSynchronizer.handleTeamQuestsSync(this);
    }

}
