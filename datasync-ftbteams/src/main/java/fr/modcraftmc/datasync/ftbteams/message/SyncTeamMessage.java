package fr.modcraftmc.datasync.ftbteams.message;

import com.google.gson.JsonObject;
import fr.modcraftmc.crossservercore.api.annotation.AutoRegister;
import fr.modcraftmc.crossservercore.api.annotation.AutoSerialize;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.datasync.ftbteams.DatasyncFtbTeam;
import fr.modcraftmc.datasync.ftbteams.TeamsSynchronizer;
import net.minecraft.network.chat.Component;

import java.util.UUID;

@AutoRegister("sync_team_message")
public class SyncTeamMessage extends BaseMessage {
    @AutoSerialize
    public UUID teamUUID;
    @AutoSerialize
    public UUID playerUUID;
    @AutoSerialize
    public Component text;

    public SyncTeamMessage() {}

    public SyncTeamMessage(UUID teamUUID, UUID playerUUID, Component text) {
        this.teamUUID = teamUUID;
        this.playerUUID = playerUUID;
        this.text = text;
    }

    @Override
    public void handle() {
        DatasyncFtbTeam.teamsSynchronizer.handleTeamMessage(teamUUID, playerUUID, text);
    }
}
