package fr.modcraftmc.datasync.waystones.message;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.datasync.waystones.DatasyncWaystones;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WaystonesData extends BaseMessage {
    public static final String MESSAGE_NAME = "waystones_data";

    private String playerName;
    private List<UUID> waystones;

    public WaystonesData(String playerName, List<UUID> waystones) {
        super(MESSAGE_NAME);
        this.playerName = playerName;
        this.waystones = waystones;
    }

    public String getPlayerName() {
        return playerName;
    }

    public List<UUID> getWaystones() {
        return waystones;
    }

    @Override
    protected JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        jsonObject.addProperty("player", playerName);
        JsonArray waystonesArray = new JsonArray();
        for (UUID waystone : waystones) {
            waystonesArray.add(waystone.toString());
        }
        jsonObject.add("waystones", waystonesArray);

        return jsonObject;
    }

    public static WaystonesData deserialize(JsonObject json) {
        String playerName = json.get("player").getAsString();
        List<UUID> waystones = new ArrayList<>();
        JsonArray waystonesArray = json.get("waystones").getAsJsonArray();
        for (JsonElement waystoneElement : waystonesArray) {
            waystones.add(UUID.fromString(waystoneElement.getAsString()));
        }

        return new WaystonesData(playerName, waystones);
    }


    @Override
    public String getMessageName() {
        return MESSAGE_NAME;
    }

    @Override
    public void handle() {
        DatasyncWaystones.waystoneManager.addPendingWaystoneData(playerName, waystones);
    }
}
