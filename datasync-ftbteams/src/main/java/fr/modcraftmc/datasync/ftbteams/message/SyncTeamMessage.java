package fr.modcraftmc.datasync.ftbteams.message;

import com.google.gson.JsonObject;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.datasync.ftbteams.DatasyncFtbTeam;
import fr.modcraftmc.datasync.ftbteams.TeamsSynchronizer;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class SyncTeamMessage extends BaseMessage {
    public static final String MESSAGE_NAME = "SyncTeamMessage";
    public UUID teamUUID;
    public UUID playerUUID;
    public Component text;

    public SyncTeamMessage(UUID teamUUID, UUID playerUUID, Component text) {
        super(MESSAGE_NAME);
        this.teamUUID = teamUUID;
        this.playerUUID = playerUUID;
        this.text = text;
    }

    @Override
    protected JsonObject serialize() {
        JsonObject json = super.serialize();
        json.addProperty("teamUUID", teamUUID.toString());
        json.addProperty("playerUUID", playerUUID.toString());
        json.addProperty("text", Component.Serializer.toJson(text));
        return json;
    }

    @Override
    public String getMessageName() {
        return MESSAGE_NAME;
    }

    public static SyncTeamMessage deserialize(JsonObject json) {
        UUID teamUUID = UUID.fromString(json.get("teamUUID").getAsString());
        UUID playerUUID = UUID.fromString(json.get("playerUUID").getAsString());
        Component text = Component.Serializer.fromJson(json.get("text").getAsString());
        return new SyncTeamMessage(teamUUID, playerUUID, text);
    }

    @Override
    public void handle() {
        DatasyncFtbTeam.teamsSynchronizer.handleTeamMessage(teamUUID, playerUUID, text);
    }
}
