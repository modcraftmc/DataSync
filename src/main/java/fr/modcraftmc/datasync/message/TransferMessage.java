package fr.modcraftmc.datasync.message;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.PlayerDataInvalidator;
import fr.modcraftmc.datasync.rabbitmq.RabbitmqDirectPublisher;
import fr.modcraftmc.datasync.serialization.PlayerSerializer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.IOException;

public class TransferMessage extends BaseMessage {
    public static final String MESSAGE_NAME = "TransferMessage";
    private final String playerName;
    private final String oldServerName;
    private final String newServerName;
    private final boolean areLinked;

    public TransferMessage(String playerName, String oldServerName, String newServerName, boolean areLinked) {
        super(MESSAGE_NAME);
        this.playerName = playerName;
        this.oldServerName = oldServerName;
        this.newServerName = newServerName;
        this.areLinked = areLinked;
    }

    public JsonObject Serialize() {
        JsonObject jsonObject = super.Serialize();
        jsonObject.addProperty("oldServerName", oldServerName);
        jsonObject.addProperty("newServerName", newServerName);
        jsonObject.addProperty("playerName", playerName);
        jsonObject.addProperty("areLinked", areLinked);
        return jsonObject;
    }

    @Override
    protected void Handle() {
        if(!areLinked) return;

        Player player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(playerName);
        JsonObject playerData = PlayerSerializer.serializePlayer(player);
        Gson gson = new Gson();
        JsonObject messageData = new LoadDataMessage(playerName, gson.toJson(playerData)).Serialize();
        String rawMessage = gson.toJson(messageData);

        try {
            RabbitmqDirectPublisher.instance.publish(newServerName, rawMessage);
        } catch (IOException e) {
            DataSync.LOGGER.error(String.format("Error while publishing message to rabbitmq cannot transfer player %s data from %s to %s : %s", playerName, oldServerName, newServerName, e.getMessage()));
        }

        //TODO: save player data to database

        PlayerDataInvalidator.invalidatePlayerData(playerName);
    }

    protected static TransferMessage Deserialize(JsonObject json) {
        String oldServerName = json.get("oldServerName").getAsString();
        String newServerName = json.get("newServerName").getAsString();
        String playerName = json.get("playerName").getAsString();
        boolean areLinked = json.get("areLinked").getAsBoolean();
        return new TransferMessage(playerName, oldServerName, newServerName, areLinked);
    }
}
