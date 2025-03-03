package fr.modcraftmc.datasync.waystones.message;

import fr.modcraftmc.crossservercore.api.annotation.AutoRegister;
import fr.modcraftmc.crossservercore.api.annotation.AutoSerialize;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.datasync.waystones.DatasyncWaystones;
import net.blay09.mods.waystones.api.Waystone;
import net.blay09.mods.waystones.core.PlayerWaystoneManager;
import net.blay09.mods.waystones.core.WaystoneManagerImpl;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@AutoRegister("delete_waystone")
public class DeleteWaystone extends BaseMessage {

    @AutoSerialize
    private Waystone waystone;

    private DeleteWaystone() {}

    public DeleteWaystone(Waystone waystone) {
        this.waystone = waystone;
    }

    @Override
    public void handle() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        WaystoneManagerImpl.get(server).removeWaystone(waystone);
        DatasyncWaystones.waystoneManager.dropWaystoneServer(waystone);
        PlayerWaystoneManager.removeKnownWaystone(server, waystone);
    }
}
