package fr.modcraftmc.datasync.tp.tpsync;

import fr.modcraftmc.datasync.tp.DatasyncTp;
import fr.modcraftmc.datasync.tp.message.TpRequestMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.bukkit.Location;
import org.goldenforge.GoldenForgeLib;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TpRequestHandler {
    private static final List<TpRequest> tpRequestsBuffer = new ArrayList<>();
    public static final int tpTimeout = 20; //time in second before tp request expire

    public static void handle(TpRequest tpRequest){
        DatasyncTp.LOGGER.debug("Handling tp request from " + tpRequest.getPlayerSourceName() + " to " + tpRequest.getPlayerTargetName());
        String playerSourceName = tpRequest.getPlayerSourceName();
        if(ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(playerSourceName) != null){
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            server.execute(() -> teleportPlayer(server, playerSourceName, tpRequest.getPlayerTargetName())); //handleTpRequest is called from another thread
            return;
        }

        tpRequestsBuffer.add(tpRequest);
    }

    public static void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event){
        cleanTpRequest();
        for(TpRequest tpRequest : tpRequestsBuffer){
            if(tpRequest.getPlayerSourceName().equals(event.getEntity().getName().getString())){
                MinecraftServer server = event.getEntity().getServer();
                teleportPlayer(server, tpRequest.getPlayerSourceName(), tpRequest.getPlayerTargetName());
                tpRequestsBuffer.remove(tpRequest);
                return;
            }
        }
    }

    private static void teleportPlayer(MinecraftServer server, String playerSourceName, String playerTargetName){
        DatasyncTp.LOGGER.debug("Teleporting player " + playerSourceName + " to " + playerTargetName);
        ServerPlayer target = Objects.requireNonNull(server.getPlayerList().getPlayerByName(playerTargetName), "Target player for teleport request not found");
        ServerPlayer source = Objects.requireNonNull(server.getPlayerList().getPlayerByName(playerSourceName), "source player for teleport request not found");
        Vec3 position = target.position();

        source.teleportTo(position.x, position.y, position.z);

//        if (GoldenForgeLib.isGoldenForge())
//            GoldenForgeLib.teleportAsync(source, Location.toLocation(target.level, target.position())).thenAccept((isTeleported) -> {
//                if (isTeleported)  source.sendSystemMessage(Component.literal("You have been teleported to " + playerTargetName), false);
//                else source.sendSystemMessage(Component.literal("Error while teleporting to " + playerTargetName), false);
//            });
//        else source.teleportTo(position.x, position.y, position.z);
    }

    private static void cleanTpRequest(){
        tpRequestsBuffer.removeIf(tpRequest -> tpRequest.getTime() + tpTimeout < (int) System.currentTimeMillis() / 1000);
    }
}