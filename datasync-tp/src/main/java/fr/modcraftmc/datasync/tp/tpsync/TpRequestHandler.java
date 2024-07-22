package fr.modcraftmc.datasync.tp.tpsync;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class TpRequestHandler {
    private static final List<TpRequest> tpRequestsBuffer = new ArrayList<>();
    public static final int tpTimeout = 20; //time in second before tp request expire

    public static void handle(TpRequest tpRequest){
        UUID playerSourceUUID = tpRequest.getPlayerSource().getUUID();
        if(ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerSourceUUID) != null){
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            server.execute(() -> teleportPlayer(server, playerSourceUUID, tpRequest.getPlayerTarget().getUUID())); //handleTpRequest is called from another thread
            return;
        }

        tpRequestsBuffer.add(tpRequest);
    }

    public static void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event){
        cleanTpRequest();
        for(TpRequest tpRequest : tpRequestsBuffer){
            if(tpRequest.getPlayerSource().equals(event.getEntity().getName().getString())){
                MinecraftServer server = event.getEntity().getServer();
                teleportPlayer(server, tpRequest.getPlayerSource().getUUID(), tpRequest.getPlayerTarget().getUUID());
                tpRequestsBuffer.remove(tpRequest);
                return;
            }
        }
    }

    private static void teleportPlayer(MinecraftServer server, UUID playerSourceUUID, UUID playerTargetUUID){
        ServerPlayer target = Objects.requireNonNull(server.getPlayerList().getPlayer(playerTargetUUID), "Target player for teleport request not found");
        ServerPlayer source = Objects.requireNonNull(server.getPlayerList().getPlayer(playerSourceUUID), "source player for teleport request not found");
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
