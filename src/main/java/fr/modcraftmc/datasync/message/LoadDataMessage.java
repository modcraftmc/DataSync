package fr.modcraftmc.datasync.message;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.PlayerDataInvalidator;
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
        Gson gson = new Gson();
        JsonObject playerData = gson.fromJson(data, JsonObject.class);
        PlayerDataLoader.pushDataToTransferBuffer(playerName, playerData);
        PlayerDataInvalidator.validatePlayerData(playerName);
    }
}
