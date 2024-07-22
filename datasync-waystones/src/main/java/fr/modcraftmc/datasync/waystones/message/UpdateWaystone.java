package fr.modcraftmc.datasync.waystones.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.annotation.AutoRegister;
import fr.modcraftmc.crossservercore.api.annotation.AutoSerialize;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncServer;
import fr.modcraftmc.datasync.waystones.DatasyncWaystones;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.core.Waystone;
import net.minecraft.nbt.CompoundTag;

@AutoRegister("update_waystones")
public class UpdateWaystone extends BaseMessage {

    @AutoSerialize
    private IWaystone iwaystone;
    @AutoSerialize
    private ISyncServer server;

    private UpdateWaystone() {}

    public UpdateWaystone(IWaystone waystone) {
        this(waystone, CrossServerCoreAPI.getServer());
    }

    private UpdateWaystone(IWaystone waystone, ISyncServer server) {
        this.iwaystone = waystone;
        this.server = server;
    }

    @Override
    public void handle() {
        DatasyncWaystones.waystoneManager.enableWaystone(iwaystone, server);
    }
}
