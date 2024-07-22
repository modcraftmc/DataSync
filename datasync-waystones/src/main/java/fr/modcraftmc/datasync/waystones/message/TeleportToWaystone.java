package fr.modcraftmc.datasync.waystones.message;

import com.google.gson.JsonObject;
import fr.modcraftmc.crossservercore.api.annotation.AutoRegister;
import fr.modcraftmc.crossservercore.api.annotation.AutoSerialize;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncPlayer;
import fr.modcraftmc.datasync.waystones.DatasyncWaystones;

import java.util.UUID;

@AutoRegister("teleport_to_waystone")
public class TeleportToWaystone extends BaseMessage {

    @AutoSerialize
    private ISyncPlayer player;
    @AutoSerialize
    private UUID waystoneUUID;

    private TeleportToWaystone() {}

    public TeleportToWaystone(ISyncPlayer player, UUID waystoneUUID) {
        this.player = player;
        this.waystoneUUID = waystoneUUID;
    }

    @Override
    public void handle() {
        DatasyncWaystones.waystoneManager.addPendingWaystoneTp(player, waystoneUUID);
    }
}
