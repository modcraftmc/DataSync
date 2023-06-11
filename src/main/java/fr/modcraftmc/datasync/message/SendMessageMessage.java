package fr.modcraftmc.datasync.message;

import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.DataSync;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

public class SendMessageMessage extends BaseMessage {
    public static String MESSAGE_NAME = "SendMessage";

    public Component message;
    public String playerName;

    public SendMessageMessage(Component message, String playerName) {
        super(MESSAGE_NAME);
        this.message = message;
        this.playerName = playerName;
    }

    public JsonObject serialize() {
        JsonObject json = super.serialize();
        json.addProperty("message", Component.Serializer.toJson(message));
        json.addProperty("playerName", playerName);
        return json;
    }

    public static SendMessageMessage deserialize(JsonObject json) {
        Component message = Component.Serializer.fromJson(json.get("message").getAsString());
        String playerName = json.get("playerName").getAsString();
        return new SendMessageMessage(message, playerName);
    }

    public void send(){
        DataSync.playersLocation.getPlayerLocation(playerName).ifPresent(playerLocation -> {
            DataSync.serverCluster.getServer(playerLocation.getName()).sendMessage(this.serializeToString());
        });
    }

    @Override
    protected void handle() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
        if (player != null) {
            player.sendSystemMessage(message, false);
        }
    }
}
