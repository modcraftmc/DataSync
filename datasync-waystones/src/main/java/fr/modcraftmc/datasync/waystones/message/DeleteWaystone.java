package fr.modcraftmc.datasync.waystones.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import fr.modcraftmc.crossservercore.api.annotation.AutoRegister;
import fr.modcraftmc.crossservercore.api.annotation.AutoSerialize;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.datasync.waystones.DatasyncWaystones;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.core.PlayerWaystoneManager;
import net.blay09.mods.waystones.core.Waystone;
import net.blay09.mods.waystones.core.WaystoneManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;

@AutoRegister("delete_waystone")
public class DeleteWaystone extends BaseMessage {

    @AutoSerialize
    private IWaystone iwaystone;

    private DeleteWaystone() {}

    public DeleteWaystone(IWaystone waystone) {
        this.iwaystone = waystone;
    }

    @Override
    public void handle() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        WaystoneManager.get(server).removeWaystone(iwaystone);
        DatasyncWaystones.waystoneManager.dropWaystoneServer(iwaystone);
        PlayerWaystoneManager.removeKnownWaystone(server, iwaystone);
    }
}
