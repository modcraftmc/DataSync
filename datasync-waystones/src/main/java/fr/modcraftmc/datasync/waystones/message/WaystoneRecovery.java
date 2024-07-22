package fr.modcraftmc.datasync.waystones.message;

import fr.modcraftmc.crossservercore.api.annotation.AutoRegister;
import fr.modcraftmc.crossservercore.api.annotation.AutoSerialize;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.datasync.waystones.DatasyncWaystones;

import java.util.UUID;

@AutoRegister("waystone_recovery")
public class WaystoneRecovery extends BaseMessage {

    @AutoSerialize
    private UUID waystoneUUID;

    private WaystoneRecovery() {}

    public WaystoneRecovery(UUID waystoneUUID) {
        this.waystoneUUID = waystoneUUID;
    }

    @Override
    public void handle() {
        DatasyncWaystones.waystoneManager.onRecoveryAsked(waystoneUUID);
    }
}
