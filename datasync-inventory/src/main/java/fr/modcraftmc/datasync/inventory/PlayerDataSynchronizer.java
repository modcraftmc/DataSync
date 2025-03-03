package fr.modcraftmc.datasync.inventory;

import com.mongodb.client.model.ReplaceOptions;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncPlayer;
import fr.modcraftmc.crossservercore.api.sharedpersistentdata.ISharedDataStore;
import fr.modcraftmc.crossservercore.api.sharedpersistentdata.SharedDataStore;
import fr.modcraftmc.datasync.inventory.message.TransferData;
import fr.modcraftmc.datasync.inventory.serialization.PlayerSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.bson.Document;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@EventBusSubscriber(modid = DatasyncInventory.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class PlayerDataSynchronizer {
    private static List<TemporalPlayerData> playerData = new ArrayList<>();
    private static int keepTime = 30; // seconds to hold data
    public static ISharedDataStore databasePlayerData = new SharedDataStore(References.PLAYER_DATA_COLLECTION_NAME);
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
        CrossServerCoreAPI.getPlayer(player.getUUID()).ifPresentOrElse(syncPlayer -> {
            if (loadDataFromTransferBuffer(player, syncPlayer)) return;
            DatasyncInventory.LOGGER.info(String.format("No transfer data found for player %s (normal if first connection on this group)", syncPlayer.getName()));

            loadDataFromDatabase(player, syncPlayer);
        }, () -> {
            DatasyncInventory.LOGGER.error("Player not found in cross server core for data transfer (receive)");
        });
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
            CrossServerCoreAPI.getPlayer(player.getUUID()).ifPresentOrElse(syncPlayer -> {
                broadcastPlayerDataToTransferBuffer(syncPlayer, PlayerSerializer.serializePlayer(player));
            }, () -> {
                DatasyncInventory.LOGGER.error("Player not found in cross server core for data transfer (send)");
            });
        }
    }

    private static boolean loadDataFromDatabase(ServerPlayer player, ISyncPlayer syncPlayer) {
        String playerName = syncPlayer.getName();
        Document document = databasePlayerData.accessOrThrow().find(new Document("name", playerName)).first();
        if (document == null) {
            DatasyncInventory.LOGGER.info(String.format("Creating new data for player %s", playerName));
            document = new Document("name", playerName).append("data", "{}");
        }
        PlayerSerializer.deserializePlayer(document.getString("data"), player);
        if(!savablePlayers.contains(player))
            savablePlayers.add(player);
        return true;
    }

    public static void saveDataToDatabase(ServerPlayer player) {
        String playerData = PlayerSerializer.serializePlayer(player);
        Date date = new Date();
        Document document = new Document("name", player.getName().getString())
                .append("saveDate", new Timestamp(date.getTime()).toString())
                .append("data", playerData);
        databasePlayerData.accessOrThrow().replaceOne(new Document("name", player.getName().getString()), document, new ReplaceOptions().upsert(true));
    }

    private static boolean loadDataFromTransferBuffer(ServerPlayer player, ISyncPlayer syncPlayer) {
        checkTemporalPlayerData();
        for (TemporalPlayerData temporalPlayerData : playerData) {
            if (temporalPlayerData.player.equals(syncPlayer)) {
                PlayerSerializer.deserializePlayer(temporalPlayerData.data, player);
                playerData.remove(temporalPlayerData);
                if(!savablePlayers.contains(player))
                    savablePlayers.add(player);
                return true;
            }
        }
        return false;
    }

    public static void broadcastPlayerDataToTransferBuffer(ISyncPlayer syncPlayer, String data) {
        CrossServerCoreAPI.sendCrossMessageToAllOtherServer(new TransferData(syncPlayer, data));
    }

    public static void pushDataToTransferBuffer(ISyncPlayer syncPlayer, String data) {
        playerData.removeIf(temporalPlayerData -> temporalPlayerData.player.equals(syncPlayer));
        playerData.add(new TemporalPlayerData(syncPlayer, data));
    }

    public static class TemporalPlayerData {
        public ISyncPlayer player;
        public String data;
        public int time;

        public TemporalPlayerData(ISyncPlayer player, String data, int time) {
            this.player = player;
            this.data = data;
            this.time = time;
        }

        public TemporalPlayerData(ISyncPlayer player, String data) {
            this(player, data, (int) (System.currentTimeMillis() / 1000));
        }
    }
}