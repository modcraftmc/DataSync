package fr.modcraftmc.datasync.waystones;

import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.core.PlayerWaystoneManager;
import net.blay09.mods.waystones.core.WarpMode;
import net.blay09.mods.waystones.core.Waystone;
import net.blay09.mods.waystones.core.WaystoneManager;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WaystoneTpHandler {

    private static Map<String, UUID> waitingTp = new HashMap<>();

    public static void addTp(String playerName, UUID waystoneUUID) {
        waitingTp.put(playerName, waystoneUUID);
    }

    public static void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        if (waitingTp.containsKey(event.getEntity().getGameProfile().getName())) {
            IWaystone waystone = WaystoneManager.get(event.getEntity().getServer()).getWaystoneById(waitingTp.get(event.getEntity().getGameProfile().getName())).get();
            PlayerWaystoneManager.tryTeleportToWaystone(event.getEntity(), waystone, WarpMode.CUSTOM, null);

            waitingTp.remove(event.getEntity().getGameProfile().getName());
        }
    }
}
