package fr.modcraftmc.datasync.homes.messages;

import com.google.gson.JsonObject;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.datasync.homes.DatasyncHomes;
import fr.modcraftmc.datasync.homes.HomeManager;

public class SetHome extends BaseMessage {
    public static final String MESSAGE_NAME = "set_home";

    public enum ActionType {
        SET,
        DELETE
    }

    public final ActionType actionType;
    public final HomeManager.Home home;
    public final String playerName;

    public SetHome(String playerName, ActionType actionType, HomeManager.Home home) {
        super(MESSAGE_NAME);
        this.playerName = playerName;
        this.actionType = actionType;
        this.home = home;
    }

    @Override
    public String getMessageName() {
        return MESSAGE_NAME;
    }

    @Override
    protected JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        jsonObject.addProperty("playerName", playerName);
        jsonObject.addProperty("actionType", actionType.name());
        jsonObject.add("home", home.serialize());
        return jsonObject;
    }

    public static SetHome deserialize(JsonObject jsonObject) {
        return new SetHome(
                jsonObject.get("playerName").getAsString(),
                ActionType.valueOf(jsonObject.get("actionType").getAsString()),
                HomeManager.Home.deserialize(jsonObject.get("home").getAsJsonObject())
        );
    }

    @Override
    public void handle() {
        switch (actionType) {
            case SET -> DatasyncHomes.homeManager.addCachedHome(playerName, home);
            case DELETE -> DatasyncHomes.homeManager.removeCachedHome(playerName, home.name());
        }
    }
}
