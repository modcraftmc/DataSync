package fr.modcraftmc.datasync.tp.tpsync;

import fr.modcraftmc.datasync.tp.DatasyncTp;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

public class TpaHereRequestHandler {
    public static List<TpaHereRequest> tpaRequestBuffer = new ArrayList<>();
    public static final int tpaTimeout = 20; //time in second before tpa request expire

    //TODO: handle case where player receive multiple tpa request
    public static void handle(TpaHereRequest tpaRequest) {
        cleanTpaHereRequest();
        ServerPlayer playerTarget = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(tpaRequest.getPlayerTargetName());
        if(playerTarget == null){
            DatasyncTp.LOGGER.warn("Player " + tpaRequest.getPlayerTargetName() + " not found for tpahere request");
            return;
        }
        tpaRequestBuffer.add(tpaRequest);
        informPlayer(playerTarget, tpaRequest.getPlayerSourceName());
    }

    private static void informPlayer(ServerPlayer player, String playerSourceName) {
        Component message = Component.literal("You have received a tpahere request from " + playerSourceName + ". Click on buttons below to accept or deny the request or type /tpahereaccept or /tpheredeny in chat\n").withStyle(style -> style.withColor(ChatFormatting.GOLD));
        Component acceptButton = Component.literal("[Accept]   ").withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpahereaccept"))).withStyle(style -> style.withColor(ChatFormatting.GREEN));
        Component denyButton = Component.literal("[Deny]").withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpheredeny"))).withStyle(style -> style.withColor(ChatFormatting.RED));

        message.getSiblings().add(acceptButton);
        message.getSiblings().add(denyButton);

        player.sendSystemMessage(message);
    }

    private static void cleanTpaHereRequest(){
        tpaRequestBuffer.removeIf(tpaRequest -> tpaRequest.getTime() + tpaTimeout < (int) System.currentTimeMillis() / 1000);
    }

    public static void accept(ServerPlayer player){
        cleanTpaHereRequest();
        for(TpaHereRequest tpaRequest : tpaRequestBuffer){
            if(tpaRequest.getPlayerTargetName().equals(player.getName().getString())){
                player.sendSystemMessage(Component.literal("Request accepted"));
                new TpRequest(tpaRequest.getPlayerTargetName(), tpaRequest.getPlayerSourceName()).fire();
                tpaRequestBuffer.remove(tpaRequest);
                return;
            }
        }
        player.sendSystemMessage(Component.literal("You don't have any tpa here request"));
    }

    public static void deny(ServerPlayer player){
        cleanTpaHereRequest();
        for(TpaHereRequest tpaRequest : tpaRequestBuffer){
            if(tpaRequest.getPlayerTargetName().equals(player.getName().getString())){
                player.sendSystemMessage(Component.literal("Request denied"));
                tpaRequestBuffer.remove(tpaRequest);
                return;
            }
        }
        player.sendSystemMessage(Component.literal("You don't have any tpa here request"));
    }
}
