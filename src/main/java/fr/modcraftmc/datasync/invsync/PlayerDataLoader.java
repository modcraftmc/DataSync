package fr.modcraftmc.datasync.invsync;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoCollection;
import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.serialization.PlayerSerializer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.bson.Document;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = DataSync.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerDataLoader {
    private static Map<String, JsonObject> playerData = new HashMap<>();
    public static MongoCollection<Document> databasePlayerData;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(event.getEntity().getUUID());
        String playerName = player.getName().getString();

        if (loadDataFromTransferBuffer(player, playerName)) return;
        DataSync.LOGGER.info(String.format("No transfer data found for player %s (normal if first connection on this group)", playerName));

        if(loadDataFromDatabase(player, playerName)) return;
        DataSync.LOGGER.error(String.format("No data found for player %s in database kicking the player", playerName));
        player.connection.disconnect(Component.literal("No data found for you in database, please contact an administrator"));
    }

    @SubscribeEvent
    public static void onPlayerSave(PlayerEvent.SaveToFile event){
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(event.getEntity().getUUID());
        saveDataToDatabase(player);
    }

    private static boolean loadDataFromDatabase(ServerPlayer player, String playerName) {
        Document document = databasePlayerData.find(new Document("name", playerName)).first();
        if (document == null)
            return false;
        Gson gson = new Gson();
        JsonObject playerData = gson.fromJson(document.getString("data"), JsonObject.class);
        PlayerSerializer.deserializePlayer(playerData, player);
        return true;

    }

    public static void saveDataToDatabase(ServerPlayer player) {
        JsonObject playerData = PlayerSerializer.serializePlayer(player);
        Date date = new Date();
        Document document = new Document("name", player.getName().getString())
                .append("saveDate", new Timestamp(date.getTime()).toString())
                .append("data", playerData.toString());
        databasePlayerData.deleteMany(new Document("name", player.getName().getString()));
        if(!databasePlayerData.insertOne(document).wasAcknowledged()){
            DataSync.LOGGER.error(String.format("Error while saving data for player %s", player.getName().getString()));
        }
    }

    private static boolean loadDataFromTransferBuffer(ServerPlayer player, String playerName) {
        if (playerData.containsKey(playerName)) {
            JsonObject data = playerData.get(playerName);
            PlayerSerializer.deserializePlayer(data, player);
            playerData.remove(playerName);
            return true;
        }
        return false;
    }

    public static void pushDataToTransferBuffer(String playerName, JsonObject data) {
        playerData.put(playerName, data);
    }
}
