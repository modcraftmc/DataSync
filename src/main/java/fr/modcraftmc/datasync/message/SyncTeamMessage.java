package fr.modcraftmc.datasync.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.ftb.mods.ftbteams.data.Team;
import fr.modcraftmc.datasync.ftbsync.FTBSync;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class SyncTeamMessage extends BaseMessage{
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

    public static SyncTeamMessage deserialize(JsonObject json) {
        UUID teamUUID = UUID.fromString(json.get("teamUUID").getAsString());
        UUID playerUUID = UUID.fromString(json.get("playerUUID").getAsString());
        Component text = Component.Serializer.fromJson(json.get("text").getAsString());
        return new SyncTeamMessage(teamUUID, playerUUID, text);
    }

    @Override
    protected void handle() {
        FTBSync.handleTeamMessage(teamUUID, playerUUID, text);
    }
}
