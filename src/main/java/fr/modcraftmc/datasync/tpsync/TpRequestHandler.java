package fr.modcraftmc.datasync.tpsync;

import fr.modcraftmc.datasync.DataSync;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Mod.EventBusSubscriber(modid = "datasync", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TpRequestHandler {
    private static final List<TpRequest> tpRequests = new ArrayList<>();
    public static final int tpTimeout = 20; //time in second before tp request expire

    public static void handleTpRequest(TpRequest tpRequest){
        String playerSourceName = tpRequest.getPlayerSourceName();
        if(ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(playerSourceName) != null){
            teleportPlayer(ServerLifecycleHooks.getCurrentServer(), playerSourceName, tpRequest.getPlayerTargetName());
            return;
        }
        tpRequests.add(tpRequest);
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event){
        cleanTpRequest();
        for(TpRequest tpRequest : tpRequests){
            if(tpRequest.getPlayerTargetName().equals(event.getEntity().getName().getString())){
                MinecraftServer server = event.getEntity().getServer();
                teleportPlayer(server, tpRequest.getPlayerSourceName(), tpRequest.getPlayerTargetName());
                tpRequests.remove(tpRequest);
                return;
            }
        }
    }

    private static void teleportPlayer(MinecraftServer server, String playerSourceName, String playerTargetName){
        ServerPlayer target = Objects.requireNonNull(server.getPlayerList().getPlayerByName(playerTargetName), "Target player for teleport request not found");
        ServerPlayer source = Objects.requireNonNull(server.getPlayerList().getPlayerByName(playerSourceName), "source player for teleport request not found");
        Vec3 position = target.position();
        source.teleportTo(position.x, position.y, position.z);
        source.sendSystemMessage(Component.literal("You have been teleported to " + playerTargetName), false);
    }

    private static void cleanTpRequest(){
        tpRequests.removeIf(tpRequest -> tpRequest.getTime() + tpTimeout < (int) System.currentTimeMillis() / 1000);
    }
}
