package fr.modcraftmc.datasync.inventory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoCollection;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.datasync.inventory.message.TransferData;
import fr.modcraftmc.datasync.inventory.serialization.PlayerSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.bson.Document;

import java.sql.Timestamp;
import java.util.*;

@Mod.EventBusSubscriber(modid = DatasyncInventory.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerDataSynchronizer {
    private static List<TemporalPlayerData> playerData = new ArrayList<>();
    private static int keepTime = 30; // seconds to hold data
    public static MongoCollection<Document> databasePlayerData;
    private static List<ServerPlayer> savablePlayers = new ArrayList<>();

    public static void checkSavablePlayers(){
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        savablePlayers.removeIf(player -> !server.getPlayerList().getPlayers().contains(player));
    }

    public static void checkTemporalPlayerData(){
        playerData.removeIf(temporalPlayerData -> temporalPlayerData.time + keepTime < System.currentTimeMillis() / 1000);
    }

    public static void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(event.getEntity().getUUID());
        String playerName = player.getName().getString();

        if (loadDataFromTransferBuffer(player, playerName)) return;
        DatasyncInventory.LOGGER.info(String.format("No transfer data found for player %s (normal if first connection on this group)", playerName));

        loadDataFromDatabase(player, playerName);
    }

    public static void onPlayerSave(PlayerEvent.SaveToFile event){
        checkSavablePlayers();
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(event.getEntity().getUUID());
        if(savablePlayers.contains(player))
            saveDataToDatabase(player);
    }

    public static void onPlayerLeaved(PlayerEvent.PlayerLoggedOutEvent event){
        checkSavablePlayers();
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(event.getEntity().getUUID());
        if(savablePlayers.contains(player)) {
            broadcastPlayerDataToTransferBuffer(player.getName().getString(), PlayerSerializer.serializePlayer(player));
        }
    }

    private static boolean loadDataFromDatabase(ServerPlayer player, String playerName) {
        Document document = databasePlayerData.find(new Document("name", playerName)).first();
        if (document == null) {
            DatasyncInventory.LOGGER.info(String.format("Creating new data for player %s", playerName));
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
        databasePlayerData.insertOne(document).wasAcknowledged();
    }

    private static boolean loadDataFromTransferBuffer(ServerPlayer player, String playerName) {
        checkTemporalPlayerData();
        for (TemporalPlayerData temporalPlayerData : playerData) {
            if (temporalPlayerData.name.equals(playerName)) {
                PlayerSerializer.deserializePlayer(temporalPlayerData.data, player);
                playerData.remove(temporalPlayerData);
                if(!savablePlayers.contains(player))
                    savablePlayers.add(player);
                return true;
            }
        }
        return false;
    }

    public static void broadcastPlayerDataToTransferBuffer(String playerName, JsonObject data) {
        CrossServerCoreAPI.instance.sendCrossMessageToAllOtherServer(new TransferData(playerName, data));
    }

    public static void pushDataToTransferBuffer(String playerName, JsonObject data) {
        playerData.removeIf(temporalPlayerData -> temporalPlayerData.name.equals(playerName));
        playerData.add(new TemporalPlayerData(playerName, data));
    }

    public static class TemporalPlayerData {
        public String name;
        public JsonObject data;
        public int time;

        public TemporalPlayerData(String name, JsonObject data, int time) {
            this.name = name;
            this.data = data;
            this.time = time;
        }

        public TemporalPlayerData(String name, JsonObject data) {
            this(name, data, (int) (System.currentTimeMillis() / 1000));
        }
    }
}