package fr.modcraftmc.datasync.inventory.message;

import com.google.gson.JsonObject;
import fr.modcraftmc.crossservercore.api.annotation.AutoRegister;
import fr.modcraftmc.crossservercore.api.annotation.AutoSerialize;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncPlayer;
import fr.modcraftmc.datasync.inventory.PlayerDataSynchronizer;

@AutoRegister("transfer_data")
public class TransferData extends BaseMessage {

    @AutoSerialize
    public ISyncPlayer player;
    public String data;

    private TransferData() {}

    public TransferData(ISyncPlayer player, String data) {
        this.player = player;
        this.data = data;
    }

    @Override
    public void handle() {
        PlayerDataSynchronizer.pushDataToTransferBuffer(player, data);
    }
}
