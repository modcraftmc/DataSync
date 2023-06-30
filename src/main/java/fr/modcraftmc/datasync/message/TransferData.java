package fr.modcraftmc.datasync.message;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.invsync.PlayerDataLoader;
import fr.modcraftmc.datasync.rabbitmq.RabbitmqDirectPublisher;
import fr.modcraftmc.datasync.serialization.PlayerSerializer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.IOException;

public class TransferData extends BaseMessage {
    public static final String MESSAGE_NAME = "TransferDataMessage";
    private final String playerName;
    private final String oldServerName;
    private final String newServerName;
    private final boolean areLinked;

    public TransferData(String playerName, String oldServerName, String newServerName, boolean areLinked) {
        super(MESSAGE_NAME);
        this.playerName = playerName;
        this.oldServerName = oldServerName;
        this.newServerName = newServerName;
        this.areLinked = areLinked;
    }

    public JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        jsonObject.addProperty("oldServerName", oldServerName);
        jsonObject.addProperty("newServerName", newServerName);
        jsonObject.addProperty("playerName", playerName);
        jsonObject.addProperty("areLinked", areLinked);
        return jsonObject;
    }

    @Override
    protected void handle() {
        if(!areLinked) return;

        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(playerName);
        JsonObject playerData = PlayerSerializer.serializePlayer(player);
        Gson gson = new Gson();
        String messageData = new LoadData(playerName, gson.toJson(playerData)).serializeToString();

        DataSync.LOGGER.info("Sending data to " + newServerName + " for player " + playerName);
        DataSync.serverCluster.getServer(newServerName).ifPresent(server -> server.sendMessage(messageData));

        PlayerDataLoader.saveDataToDatabase(player);
    }

    protected static TransferData deserialize(JsonObject json) {
        String oldServerName = json.get("oldServerName").getAsString();
        String newServerName = json.get("newServerName").getAsString();
        String playerName = json.get("playerName").getAsString();
        boolean areLinked = json.get("areLinked").getAsBoolean();
        return new TransferData(playerName, oldServerName, newServerName, areLinked);
    }
}
