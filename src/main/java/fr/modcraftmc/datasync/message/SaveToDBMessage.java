package fr.modcraftmc.datasync.message;

import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.PlayerDataLoader;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.ServerLifecycleHooks;

public class SaveToDBMessage extends BaseMessage {
    public static final String MESSAGE_NAME = "SaveToDBMessage";
    private final String playerName;
    private final String serverName;

    public SaveToDBMessage(String playerName, String serverName) {
        super(MESSAGE_NAME);
        this.playerName = playerName;
        this.serverName = serverName;
    }

    public JsonObject Serialize() {
        JsonObject jsonObject = super.Serialize();
        jsonObject.addProperty("serverName", serverName);
        jsonObject.addProperty("playerName", playerName);
        return jsonObject;
    }

    public static SaveToDBMessage Deserialize(JsonObject json) {
        String serverName = json.get("serverName").getAsString();
        String playerName = json.get("playerName").getAsString();
        return new SaveToDBMessage(playerName, serverName);
    }

    @Override
    protected void Handle() {
        DataSync.LOGGER.info("Saving player %s data to database: " + playerName);
        Player player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(playerName);

        PlayerDataLoader.saveDataToDatabase(player);
    }
}
