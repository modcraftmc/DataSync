package fr.modcraftmc.datasync.waystones;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.mongodb.client.MongoCollection;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.datasync.waystones.message.WaystonesData;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.bson.Document;

import java.util.*;

public class WaystoneManager {

    private final Map<String, PendingWaytoneTp> pendingWaytoneTpBuffer = new HashMap<>();
    private final Map<String, PendingWaystoneData> pendingWaystoneDataBuffer = new HashMap<>();
    public final Map<IWaystone, String> waystoneServerMap = new HashMap<>();
    public static final int pendingWaystoneTpTimeout = 60; //time in second before tp request expire
    public static final int pendingWaystoneDataTimeout = 60; //time in second before tp request expire
    public static MongoCollection<Document> databaseWaystonesData;
    public static MongoCollection<Document> databasePlayerWaystonesData;

    public void addPendingWaystoneTp(String playerName, UUID waystoneUUID) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        Optional<IWaystone> optional = net.blay09.mods.waystones.core.WaystoneManager.get(server).getWaystoneById(waystoneUUID);
        if (optional.isEmpty()) {
            server.getPlayerList().getPlayerByName(playerName).sendSystemMessage(Component.literal("Waystone not found!"));
            return;
        }
        synchronized (pendingWaytoneTpBuffer) {
            pendingWaytoneTpBuffer.put(playerName, new PendingWaytoneTp(optional.get(), (int) System.currentTimeMillis() / 1000));
        }
    }

    public void removePendingWaystoneTpTimedOut(){
        pendingWaytoneTpBuffer.entrySet().removeIf(pendingWaytoneTpEntry -> pendingWaytoneTpEntry.getValue().time() + pendingWaystoneTpTimeout < (int) System.currentTimeMillis() / 1000);
    }

    public void addPendingWaystoneData(String playerName, List<UUID> waystones) {
        synchronized (pendingWaystoneDataBuffer) {
            pendingWaystoneDataBuffer.put(playerName, new PendingWaystoneData(waystones, (int) System.currentTimeMillis() / 1000));
        }
    }

    public void removePendingWaystoneDataTimedOut(){
        pendingWaystoneDataBuffer.entrySet().removeIf(pendingWaystoneDataEntry -> pendingWaystoneDataEntry.getValue().time() + pendingWaystoneDataTimeout < (int) System.currentTimeMillis() / 1000);
    }

    public void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        String playerName = player.getName().getString();

        synchronized (pendingWaytoneTpBuffer) {
            removePendingWaystoneTpTimedOut();
            if (pendingWaytoneTpBuffer.containsKey(playerName)) {
                PendingWaytoneTp pendingWaystone = pendingWaytoneTpBuffer.remove(playerName);
                PlayerWaystoneManager.tryTeleportToWaystone(event.getEntity(), pendingWaystone.toWaystone(), WarpMode.CUSTOM, null);
            }
        }

        synchronized (pendingWaystoneDataBuffer) {
            if(!loadWaystonesDataFromBuffer(player)){
                loadPlayerWaystonesFromDatabase(player);
            };
        }
    }

    private boolean loadWaystonesDataFromBuffer(Player player) {
        String playerName = player.getName().getString();
        removePendingWaystoneDataTimedOut();
        if (pendingWaystoneDataBuffer.containsKey(playerName)) {
            PendingWaystoneData pendingWaystone = pendingWaystoneDataBuffer.remove(playerName);

            tryActivateWaystonesForPlayer(player, pendingWaystone.waystones());
            return true;
        }

        return false;
    }

    public void onPlayerLeaved(PlayerEvent.PlayerLoggedOutEvent event){
        Player player = event.getEntity();
        broadcastPlayerWaystones(player);
        savePlayerWaystonesToDatabase(player);
    }

    public void tryActivateWaystonesForPlayer(Player player, List<UUID> waystones){
        Level level = ServerLifecycleHooks.getCurrentServer().overworld();
        IPlayerWaystoneData playersWaystoneData = PlayerWaystoneManager.getPlayerWaystoneData(level);

        for (UUID waystoneUUID : waystones) {
            Optional<IWaystone> optional = net.blay09.mods.waystones.core.WaystoneManager.get(ServerLifecycleHooks.getCurrentServer()).getWaystoneById(waystoneUUID);
            if (optional.isPresent()) {
                if(!playersWaystoneData.isWaystoneActivated(player, optional.get()))
                    PlayerWaystoneManager.activateWaystone(player, optional.get());
            }else{
                DatasyncWaystones.LOGGER.error("Waystone not found: " + waystoneUUID + " this mean the waystone hasn't been sync properly");
            }
        }

        WaystoneSyncManager.sendActivatedWaystones(player);
    }

    public void onServerStop(ServerStoppingEvent event) {
        saveWaystonesDataToDatabase();
    }

    public void initialiseDatabaseConnection() {
        databaseWaystonesData = CrossServerCoreAPI.instance.getOrCreateMongoCollection(References.WAYSTONES_DATA_COLLECTION_NAME);
        databasePlayerWaystonesData = CrossServerCoreAPI.instance.getOrCreateMongoCollection(References.PLAYER_WAYSTONES_DATA_COLLECTION_NAME);
    }

    public void setWaystoneServer(IWaystone waystone, String serverName) {
        dropWaystoneServer(waystone);
        waystoneServerMap.put(waystone, serverName);
    }

    public void dropWaystoneServer(IWaystone waystone) {
        var entries = waystoneServerMap.entrySet();
        for (var entry : entries) {
            if (entry.getKey().getWaystoneUid().equals(waystone.getWaystoneUid())) {
                waystoneServerMap.remove(entry.getKey());
                return;
            }
        }
    }

    public boolean isWaystoneOnCurrentServer(IWaystone waystone) {
        DatasyncWaystones.LOGGER.info("WaystoneManager.isWaystoneOnCurrentServer (" + waystone.getWaystoneUid() + ") : " + getWaystoneServer(waystone.getWaystoneUid()) + " == " + CrossServerCoreAPI.instance.getServerName());
        if(getWaystoneServer(waystone) == null)
            return false;
        return getWaystoneServer(waystone.getWaystoneUid()).equals(CrossServerCoreAPI.instance.getServerName());
    }

    private String getWaystoneServer(UUID waystoneUUID){
        var entries = waystoneServerMap.entrySet();
        for (var entry : entries) {
            if (entry.getKey().getWaystoneUid().equals(waystoneUUID)) {
                return entry.getValue();
            }
        }

        return null;
    }

    private void saveWaystonesDataToDatabase(){
        waystoneServerMap.forEach((waystone, server) -> {
            String waystoneUUID = waystone.getWaystoneUid().toString();
            Document document = new Document("uuid", waystoneUUID);
            CompoundTag tag = new CompoundTag();
            Waystone.write(waystone, tag);
            document.append("waystone", CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, tag).result().get().toString());
            document.append("server", server);

            databaseWaystonesData.deleteOne(new Document("uuid", waystoneUUID));
            databaseWaystonesData.insertOne(document);
        });
    }

    public void loadWaystonesDataFromDatabase(){
        databaseWaystonesData.find().forEach(document -> {
            Gson gson = new Gson();
            JsonObject waystoneJson = gson.fromJson(document.getString("waystone"), JsonObject.class);
            CompoundTag waystoneTag = CompoundTag.CODEC.parse(JsonOps.INSTANCE, waystoneJson).result().get();
            IWaystone waystone = Waystone.read(waystoneTag);
            String serverName = document.getString("server");

            enableWaystone(waystone, serverName);
        });
    }

    private void broadcastPlayerWaystones(Player player){
        Level level = ServerLifecycleHooks.getCurrentServer().overworld();
        IPlayerWaystoneData playersWaystoneData = PlayerWaystoneManager.getPlayerWaystoneData(level);

        List<UUID> waystoneUUIDs = new ArrayList<>();
        playersWaystoneData.getWaystones(player).forEach(waystone -> waystoneUUIDs.add(waystone.getWaystoneUid()));
        CrossServerCoreAPI.instance.sendCrossMessageToAllOtherServer(new WaystonesData(player.getName().getString(), waystoneUUIDs));
    }

    private void savePlayerWaystonesToDatabase(Player player){
        Level level = ServerLifecycleHooks.getCurrentServer().overworld();
        IPlayerWaystoneData playersWaystoneData = PlayerWaystoneManager.getPlayerWaystoneData(level);

        List<UUID> waystoneUUIDs = new ArrayList<>();
        playersWaystoneData.getWaystones(player).forEach(waystone -> waystoneUUIDs.add(waystone.getWaystoneUid()));

        Document document = new Document("player", player.getName().getString());
        String waystonesData = String.join(",", waystoneUUIDs.stream().map(UUID::toString).toList());
        document.append("waystones", waystonesData);

        databasePlayerWaystonesData.deleteOne(new Document("player", player.getName().getString()));
        databasePlayerWaystonesData.insertOne(document);
    }

    private void loadPlayerWaystonesFromDatabase(Player player){
        Document document = databasePlayerWaystonesData.find(new Document("player", player.getName().getString())).first();
        if(document == null) return;

        List<UUID> waystonesUUID = Arrays.stream(document.getString("waystones").split(",")).map(UUID::fromString).toList();
        tryActivateWaystonesForPlayer(player, waystonesUUID);
    }

    public String getWaystoneServer(IWaystone waystone) {
        return getWaystoneServer(waystone.getWaystoneUid());
    }

    public void enableWaystone(IWaystone waystone, String serverName) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        net.blay09.mods.waystones.core.WaystoneManager.get(server).addWaystone(waystone);
        setWaystoneServer(waystone, serverName);

        if(waystone.isGlobal())
            PlayerWaystoneManager.activeWaystoneForEveryone(server, waystone);

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            WaystoneSyncManager.sendWaystoneUpdate(player, waystone);
            WaystoneSyncManager.sendActivatedWaystones(player);
        }
    }

    private record PendingWaytoneTp(IWaystone toWaystone, int time) {}
    private record PendingWaystoneData(List<UUID> waystones, int time) {}
}
