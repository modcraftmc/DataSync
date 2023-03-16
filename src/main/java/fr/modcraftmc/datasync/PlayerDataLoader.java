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
        if (!playerData.containsKey(playerName)) {
            DataSync.LOGGER.warn(String.format("No data found for player %s", playerName));
            return;
        }
        JsonObject data = playerData.get(playerName);
        PlayerSerializer.deserializePlayer(data, player);
    }
}
