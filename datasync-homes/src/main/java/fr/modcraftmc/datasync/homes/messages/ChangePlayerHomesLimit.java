package fr.modcraftmc.datasync.homes.messages;

import com.google.gson.JsonObject;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.datasync.homes.DatasyncHomes;

import java.util.Optional;

public class ChangePlayerHomesLimit extends BaseMessage {
    public static final String MESSAGE_NAME = "change_player_homes_limit";

    private final String playerName;
    private final Optional<Integer> limit;

    public ChangePlayerHomesLimit(String playerName, Optional<Integer> limit) {
        super(MESSAGE_NAME);
        this.playerName = playerName;
        this.limit = limit;
    }

    @Override
    protected JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        jsonObject.addProperty("playerName", playerName);
        jsonObject.addProperty("limit", limit.orElse(-1));
        return jsonObject;
    }

    public static ChangePlayerHomesLimit deserialize(JsonObject jsonObject) {
        String playerName = jsonObject.get("playerName").getAsString();
        int limit = jsonObject.get("limit").getAsInt();
        return new ChangePlayerHomesLimit(playerName, limit == -1 ? Optional.empty() : Optional.of(limit));
    }

    @Override
    public String getMessageName() {
        return MESSAGE_NAME;
    }

    @Override
    public void handle() {
        if (limit.isPresent()) {
            DatasyncHomes.homeManager.setCachedPlayerHomesLimit(playerName, limit.get());
        } else {
            DatasyncHomes.homeManager.unsetCachedPlayerHomesLimit(playerName);
        }
    }
}
