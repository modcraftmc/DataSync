package fr.modcraftmc.datasync.message;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.rabbitmq.RabbitmqDirectPublisher;
import fr.modcraftmc.datasync.serialization.PlayerSerializer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.IOException;

public class TransferMessage extends BaseMessage {
    public static final String MESSAGE_NAME = "TransferMessage";
    private String playerName;
    private String oldServerName;
    private String newServerName;

    public TransferMessage(String playerName, String oldServerName, String newServerName) {
        super(MESSAGE_NAME);
        this.playerName = playerName;
        this.oldServerName = oldServerName;
        this.newServerName = newServerName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getOldServerName() {
        return oldServerName;
    }

    public String getNewServerName() {
        return newServerName;
    }

    public JsonObject Serialize() {
        JsonObject jsonObject = super.Serialize();
        jsonObject.addProperty("oldServerName", oldServerName);
        jsonObject.addProperty("newServerName", newServerName);
        jsonObject.addProperty("playerName", playerName);
        return jsonObject;
    }

    @Override
    protected void Handle() {
        DataSync.LOGGER.info(String.format("Transferring player %s data to server %s: ", playerName, newServerName));
        Player player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(playerName);
        JsonObject playerData = PlayerSerializer.serializePlayer(player);
        Gson gson = new Gson();
        JsonObject messageData = new LoadDataMessage(playerName, gson.toJson(playerData)).Serialize();
        try {
            RabbitmqDirectPublisher.instance.publish(newServerName, gson.toJson(messageData));
        } catch (IOException e) {
            DataSync.LOGGER.error(String.format("Error while publishing message to rabbitmq cannot transfer player %s data from %s to %s : %s", playerName, oldServerName, newServerName, e.getMessage()));
        }
    }

    protected static TransferMessage Deserialize(JsonObject json) {
        String oldServerName = json.get("oldServerName").getAsString();
        String newServerName = json.get("newServerName").getAsString();
        String playerName = json.get("playerName").getAsString();
        return new TransferMessage(playerName, oldServerName, newServerName);
    }
}
