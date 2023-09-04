package fr.modcraftmc.datasync.waystones;

import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.core.PlayerWaystoneManager;
import net.blay09.mods.waystones.core.WarpMode;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class WaystoneManager {

    private final Map<String, PendingWaytoneTp> pendingWaytoneTp = new HashMap<>();
    public static final int pendingWaystoneTimeout = 240; //time in second before tp request expire

    public void addPendingWaystoneTp(String playerName, UUID waystoneUUID) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        Optional<IWaystone> optional = net.blay09.mods.waystones.core.WaystoneManager.get(server).getWaystoneById(waystoneUUID);
        if (optional.isEmpty()) {
            server.getPlayerList().getPlayerByName(playerName).sendSystemMessage(Component.literal("Waystone not found!"));
            return;
        }
        synchronized (pendingWaytoneTp) {
            pendingWaytoneTp.put(playerName, new PendingWaytoneTp(optional.get(), (int) System.currentTimeMillis() / 1000));
        }
    }

    public void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        String playerName = event.getEntity().getGameProfile().getName();

        synchronized (pendingWaytoneTp) {
            pendingWaytoneTp.entrySet().removeIf(pendingWaytoneTpEntry -> pendingWaytoneTpEntry.getValue().time() + pendingWaystoneTimeout < (int) System.currentTimeMillis() / 1000);
            if (pendingWaytoneTp.containsKey(playerName)) {
                PendingWaytoneTp pendingWaystone = pendingWaytoneTp.remove(playerName);
                PlayerWaystoneManager.tryTeleportToWaystone(event.getEntity(), pendingWaystone.toWaystone(), WarpMode.CUSTOM, null);
            }
        }
    }

    private record PendingWaytoneTp(IWaystone toWaystone, int time) {}
}
