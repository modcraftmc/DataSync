package fr.modcraftmc.datasync.homes.messages;

import com.google.gson.JsonObject;
import fr.modcraftmc.crossservercore.message.BaseMessage;
import fr.modcraftmc.datasync.homes.DatasyncHomes;
import fr.modcraftmc.datasync.homes.HomeManager;

public class HomeTpRequest extends BaseMessage {
    public static final String MESSAGE_NAME = "home_tp_request";

    private final String playerName;
    private final HomeManager.Home home;



    public HomeTpRequest(String playerName, HomeManager.Home home) {
        super(MESSAGE_NAME);
        this.playerName = playerName;
        this.home = home;
    }


    @Override
    protected JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        jsonObject.addProperty("playerName", playerName);
        jsonObject.add("home", home.serialize());
        return jsonObject;
    }

    public static HomeTpRequest deserialize(JsonObject jsonObject) {
        return new HomeTpRequest(
                jsonObject.get("playerName").getAsString(),
                HomeManager.Home.deserialize(jsonObject.get("home").getAsJsonObject())
        );
    }

    @Override
    public String getMessageName() {
        return MESSAGE_NAME;
    }

    @Override
    protected void handle() {
        DatasyncHomes.homeManager.addPendingHomeTp(this);
    }

    public String getPlayerName() {
        return playerName;
    }

    public HomeManager.Home getHome() {
        return home;
    }
}
