package fr.modcraftmc.datasync.tpsync;

import fr.modcraftmc.datasync.DataSync;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

public class TpaRequestHandler {
    public static List<TpaRequest> tpaRequestBuffer = new ArrayList<>();
    public static final int tpaTimeout = 20; //time in second before tpa request expire

    //TODO: handle case where player receive multiple tpa request
    public static void handle(TpaRequest tpaRequest) {
        cleanTpaRequest();
        ServerPlayer playerTarget = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(tpaRequest.getPlayerTargetName());
        if(playerTarget == null){
            DataSync.LOGGER.warn("Player " + tpaRequest.getPlayerTargetName() + " not found for tpa request");
            return;
        }
        tpaRequestBuffer.add(tpaRequest);
        informPlayer(playerTarget);
    }

    private static void informPlayer(ServerPlayer player){
        //send message to player with interactive text to accept or deny tpa request
        Component message = Component.literal("You have received a tpa request from " + player.getName().getString() + ". Click on buttons below to accept or deny the request or type /tpaccept or /tpdeny in chat\n").withStyle(style -> style.withColor(ChatFormatting.GOLD));
        Component acceptButton = Component.literal("[Accept]   ").withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"))).withStyle(style -> style.withColor(ChatFormatting.GREEN));
        Component denyButton = Component.literal("[Deny]").withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny"))).withStyle(style -> style.withColor(ChatFormatting.RED));
        //merge message and buttons
        message.getSiblings().add(acceptButton);
        message.getSiblings().add(denyButton);
        player.sendSystemMessage(message);
    }

    private static void cleanTpaRequest(){
        tpaRequestBuffer.removeIf(tpaRequest -> tpaRequest.getTime() + tpaTimeout < (int) System.currentTimeMillis() / 1000);
    }

    public static void accept(ServerPlayer player){
        cleanTpaRequest();
        for(TpaRequest tpaRequest : tpaRequestBuffer){
            if(tpaRequest.getPlayerTargetName().equals(player.getName().getString())){
                player.sendSystemMessage(Component.literal("Request accepted"));
                new TpRequest(tpaRequest.getPlayerSourceName(), tpaRequest.getPlayerTargetName()).fire();
                tpaRequestBuffer.remove(tpaRequest);
                return;
            }
        }
        player.sendSystemMessage(Component.literal("You don't have any tpa request"));
    }

    public static void deny(ServerPlayer player){
        cleanTpaRequest();
        for(TpaRequest tpaRequest : tpaRequestBuffer){
            if(tpaRequest.getPlayerTargetName().equals(player.getName().getString())){
                player.sendSystemMessage(Component.literal("Request denied"));
                tpaRequestBuffer.remove(tpaRequest);
                return;
            }
        }
        player.sendSystemMessage(Component.literal("You don't have any tpa request"));
    }
}
