package fr.modcraftmc.datasync.message;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.PlayerDataLoader;

public class LoadDataMessage extends BaseMessage{
    public static final String MESSAGE_NAME = "LoadDataMessage";

    private final String playerName;
    private final String data;

    LoadDataMessage(String playerName, String data) {
        super(MESSAGE_NAME);
        this.playerName = playerName;
        this.data = data;
    }

    @Override
    protected JsonObject Serialize() {
        JsonObject jsonObject = super.Serialize();
        jsonObject.addProperty("playerName", playerName);
        jsonObject.addProperty("data", data);
        return jsonObject;
    }

    public static LoadDataMessage Deserialize(JsonObject json) {
        String playerName = json.get("playerName").getAsString();
        String data = json.get("data").getAsString();
        return new LoadDataMessage(playerName, data);
    }

    @Override
    protected void Handle() {
        DataSync.LOGGER.info(String.format("Loading player %s data from message", playerName));

        Gson gson = new Gson();
        JsonObject playerData = gson.fromJson(data, JsonObject.class);
        PlayerDataLoader.playerData.put(playerName, playerData);
    }
}
