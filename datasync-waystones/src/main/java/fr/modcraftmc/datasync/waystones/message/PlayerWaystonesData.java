package fr.modcraftmc.datasync.waystones.message;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.modcraftmc.crossservercore.api.annotation.AutoRegister;
import fr.modcraftmc.crossservercore.api.annotation.AutoSerialize;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncPlayer;
import fr.modcraftmc.datasync.waystones.DatasyncWaystones;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@AutoRegister("player_waystones_data")
public class PlayerWaystonesData extends BaseMessage {

    @AutoSerialize
    private ISyncPlayer player;
    @AutoSerialize
    private List<UUID> waystones;

    private PlayerWaystonesData() {}

    public PlayerWaystonesData(ISyncPlayer player, List<UUID> waystones) {
        this.player = player;
        this.waystones = waystones;
    }

    public ISyncPlayer getPlayer() {
        return player;
    }

    public List<UUID> getWaystones() {
        return waystones;
    }

    @Override
    public void handle() {
        DatasyncWaystones.waystoneManager.addPendingWaystoneData(player, waystones);
    }
}
