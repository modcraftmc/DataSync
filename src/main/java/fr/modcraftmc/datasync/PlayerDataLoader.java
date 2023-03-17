package fr.modcraftmc.datasync;

import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.serialization.PlayerSerializer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = DataSync.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerDataLoader {
    public static Map<String, JsonObject> playerData = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(event.getEntity().getName().getString());
        String playerName = player.getName().getString();

        if (loadDataFromTransferBuffer(player, playerName)) return;
        DataSync.LOGGER.info(String.format("No transfer data found for player %s (normal if first connection on this group)", playerName));

        if(!PlayerDataInvalidator.isPlayerDataInvalidated(playerName)) return;
        DataSync.LOGGER.warn(String.format("data invalid for player %s, loading data from db", playerName));

        //TODO: load data from database then validate it
    }

    private static boolean loadDataFromTransferBuffer(ServerPlayer player, String playerName) {
        if (playerData.containsKey(playerName)) {
            JsonObject data = playerData.get(playerName);
            PlayerSerializer.deserializePlayer(data, player);
            return true;
        }
        return false;
    }

}
