package fr.modcraftmc.datasync.invsync;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoCollection;
import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.serialization.PlayerSerializer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.bson.Document;

import java.sql.Timestamp;
import java.util.*;

@Mod.EventBusSubscriber(modid = DataSync.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerDataLoader {
    private static Map<String, JsonObject> playerData = new HashMap<>();
    public static MongoCollection<Document> databasePlayerData;
    private static List<ServerPlayer> savablePlayers = new ArrayList<>();

    public static void checkSavablePlayers(){
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        savablePlayers.removeIf(player -> !server.getPlayerList().getPlayers().contains(player));
    }

    public static void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(event.getEntity().getUUID());
        String playerName = player.getName().getString();

        if (loadDataFromTransferBuffer(player, playerName)) return;
        DataSync.LOGGER.info(String.format("No transfer data found for player %s (normal if first connection on this group)", playerName));

        loadDataFromDatabase(player, playerName);
//        if(loadDataFromDatabase(player, playerName)) return;
//        DataSync.LOGGER.error(String.format("No data found for player %s in database kicking the player", playerName));
//        player.connection.disconnect(Component.literal("No data found for you in database, please contact an administrator"));
    }

    public static void onPlayerSave(PlayerEvent.SaveToFile event){
        checkSavablePlayers();
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(event.getEntity().getUUID());
        if(savablePlayers.contains(player))
            saveDataToDatabase(player);
    }

    private static boolean loadDataFromDatabase(ServerPlayer player, String playerName) {
        Document document = databasePlayerData.find(new Document("name", playerName)).first();
        if (document == null) {
            DataSync.LOGGER.info(String.format("Creating new data for player %s", playerName));
            document = new Document("name", playerName).append("data", "{}");
        }
        Gson gson = new Gson();
        JsonObject playerData = gson.fromJson(document.getString("data"), JsonObject.class);
        PlayerSerializer.deserializePlayer(playerData, player);
        if(!savablePlayers.contains(player))
            savablePlayers.add(player);
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
            if(!savablePlayers.contains(player))
                savablePlayers.add(player);
            return true;
        }
        return false;
    }

    public static void pushDataToTransferBuffer(String playerName, JsonObject data) {
        playerData.put(playerName, data);
    }
}
