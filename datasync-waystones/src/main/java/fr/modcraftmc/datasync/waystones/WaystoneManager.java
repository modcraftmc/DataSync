package fr.modcraftmc.datasync.waystones;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import com.mongodb.client.model.ReplaceOptions;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.events.CrossServerCoreReadyEvent;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncPlayer;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncServer;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncServerProxy;
import fr.modcraftmc.crossservercore.api.sharedpersistentdata.ISharedDataStore;
import fr.modcraftmc.crossservercore.api.sharedpersistentdata.SharedDataStore;
import fr.modcraftmc.crossservercore.api.sharedpersistentdata.SharedDataStoreNotReadyException;
import fr.modcraftmc.datasync.waystones.message.PlayerWaystonesData;
import fr.modcraftmc.datasync.waystones.message.UpdateWaystone;
import fr.modcraftmc.datasync.waystones.message.WaystoneRecovery;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class WaystoneManager {

    private final Map<ISyncPlayer, PendingWaytoneTp> pendingWaytoneTpBuffer = new HashMap<>();
    private final Map<ISyncPlayer, PendingWaystoneData> pendingWaystoneDataBuffer = new HashMap<>();
    public final Map<UUID, Pair<ISyncServer, IWaystone>> waystoneServerMap = new ConcurrentHashMap<>();
    public static final int pendingWaystoneTpTimeout = 60; //time in second before tp request expire
    public static final int pendingWaystoneDataTimeout = 60; //time in second before tp request expire
    public static ISharedDataStore databaseWaystonesData = new SharedDataStore(References.WAYSTONES_DATA_COLLECTION_NAME);
    public static ISharedDataStore databasePlayerWaystonesData = new SharedDataStore(References.PLAYER_WAYSTONES_DATA_COLLECTION_NAME);

    public WaystoneManager() {
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerJoined);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLeaved);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStop);
        MinecraftForge.EVENT_BUS.addListener(this::onCrossServerCoreReadyEvent);
    }

    public void onCrossServerCoreReadyEvent(CrossServerCoreReadyEvent event) {
        loadWaystonesDataFromDatabase();
    }

    public void addPendingWaystoneTp(ISyncPlayer player, UUID waystoneUUID) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        Optional<IWaystone> optional = net.blay09.mods.waystones.core.WaystoneManager.get(server).getWaystoneById(waystoneUUID);
        if (optional.isEmpty()) {
            server.getPlayerList().getPlayer(player.getUUID()).sendSystemMessage(Component.literal("Waystone not found!"));
            return;
        }
        synchronized (pendingWaytoneTpBuffer) {
            pendingWaytoneTpBuffer.put(player, new PendingWaytoneTp(optional.get(), (int) System.currentTimeMillis() / 1000));
        }
    }

    public void removePendingWaystoneTpTimedOut(){
        pendingWaytoneTpBuffer.entrySet().removeIf(pendingWaytoneTpEntry -> pendingWaytoneTpEntry.getValue().time() + pendingWaystoneTpTimeout < (int) System.currentTimeMillis() / 1000);
    }

    public void addPendingWaystoneData(ISyncPlayer player, List<UUID> waystones) {
        synchronized (pendingWaystoneDataBuffer) {
            pendingWaystoneDataBuffer.put(player, new PendingWaystoneData(waystones, (int) System.currentTimeMillis() / 1000));
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
        saveAllWaystonesDataToDatabase();
    }

    public void setWaystoneServer(IWaystone waystone, ISyncServer syncServer) {
        DatasyncWaystones.LOGGER.info("adding waystone : "+ waystone.getName() + " to server : " + syncServer);
        var previousValue = waystoneServerMap.put(waystone.getWaystoneUid(), new Pair<>(syncServer, waystone));
        if(previousValue == null ) //waystone was not in the map
            saveAllWaystonesDataToDatabase();
    }

    public void dropWaystoneServer(IWaystone waystone) {
        Optional.ofNullable(waystoneServerMap.remove(waystone.getWaystoneUid())).ifPresent(serverWaystonePair -> {
            ISyncServer previousServer = serverWaystonePair.getFirst();
            if(CrossServerCoreAPI.getServer().equals(previousServer))
                removeWaystoneDataFromDatabase(waystone);
            DatasyncWaystones.LOGGER.info("removed waystone : " + waystone.getName() + " from server : " + previousServer);
        });

    }

    public boolean isWaystoneOnCurrentServer(IWaystone waystone) {
        if(getWaystoneServer(waystone) == null)
            return false;
        return getWaystoneServer(waystone.getWaystoneUid()).equals(CrossServerCoreAPI.getServerName());
    }

    private ISyncServer getWaystoneServer(UUID waystoneUUID){
        return getWaystoneServer(waystoneUUID, true);
    }

    private ISyncServer getWaystoneServer(UUID waystoneUUID, boolean tryRecovery){
        if(waystoneServerMap.containsKey(waystoneUUID))
            return waystoneServerMap.get(waystoneUUID).getFirst();

        if(tryRecovery)
            performWaystoneRecovery(waystoneUUID, true);

        return null;
    }

    public void onRecoveryAsked(UUID waystoneUUID){
        if(CrossServerCoreAPI.getServerName().equals(getWaystoneServer(waystoneUUID, false))){ // if the waystone is on the current server
            IWaystone waystone = waystoneServerMap.get(waystoneUUID).getSecond();
            CrossServerCoreAPI.sendCrossMessageToAllOtherServer(new UpdateWaystone(waystone));
            return;
        }

        performWaystoneRecovery(waystoneUUID, false);
    }

    public void performWaystoneRecovery(UUID waystoneUUID, boolean askOtherServers){
        DatasyncWaystones.LOGGER.warn("Waystone not found: " + waystoneUUID + ", the waystone was not found in the database, trying to find it in the world.");

        net.blay09.mods.waystones.core.WaystoneManager waystoneManager = net.blay09.mods.waystones.core.WaystoneManager.get(ServerLifecycleHooks.getCurrentServer());
        AtomicBoolean recovered = new AtomicBoolean(false);
        waystoneManager.getWaystoneById(waystoneUUID).ifPresent(waystone -> {
            waystoneManager.getWaystoneAt(ServerLifecycleHooks.getCurrentServer().getLevel(waystone.getDimension()), waystone.getPos()).ifPresent(
                    waystone1 -> {
                        if(!waystone1.getWaystoneUid().equals(waystoneUUID)) return;

                        DatasyncWaystones.LOGGER.info("Waystone found in the world. Recovering it.");
                        setWaystoneServer(waystone1, CrossServerCoreAPI.getServer());
                        CrossServerCoreAPI.sendCrossMessageToAllOtherServer(new UpdateWaystone(waystone1));
                        recovered.set(true);
                    }
            );
        });

        if(recovered.get())
            return;

        DatasyncWaystones.LOGGER.error("Unable to recover waystone from the world.");
        if(askOtherServers) {
            DatasyncWaystones.LOGGER.warn("Asking other servers to recover the waystone.");
            CrossServerCoreAPI.sendCrossMessageToAllOtherServer(new WaystoneRecovery(waystoneUUID));
        }
    }

    private void saveWaystoneDataToDatabase(Pair<ISyncServer, IWaystone> serverWaystonePair){
        if(!CrossServerCoreAPI.getServer().equals(serverWaystonePair.getFirst())){ //only save waystone data for the current server
            return;
        }

        String waystoneUUID = serverWaystonePair.getSecond().getWaystoneUid().toString();
        Document document = new Document("uuid", waystoneUUID);
        CompoundTag tag = new CompoundTag();
        Waystone.write(serverWaystonePair.getSecond(), tag);
        document.append("waystone", CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, tag).result().get().toString());
        document.append("server", serverWaystonePair.getFirst());

        databaseWaystonesData.accessOrThrow().replaceOne(new Document("uuid", waystoneUUID), document, new ReplaceOptions().upsert(true));
    }

    private void removeWaystoneDataFromDatabase(IWaystone waystone) {
        databaseWaystonesData.accessOrThrow().deleteMany(new Document("uuid", waystone.getWaystoneUid().toString()));
    }

    private void saveAllWaystonesDataToDatabase(){
        waystoneServerMap.forEach((uuid, serverWaystonePair) -> {
            saveWaystoneDataToDatabase(serverWaystonePair);
        });
    }

    public void loadWaystonesDataFromDatabase(){
        try {
            databaseWaystonesData.access().find().forEach(document -> {
                Gson gson = new Gson();
                JsonObject waystoneJson = gson.fromJson(document.getString("waystone"), JsonObject.class);
                CompoundTag waystoneTag = CompoundTag.CODEC.parse(JsonOps.INSTANCE, waystoneJson).result().get();
                IWaystone waystone = Waystone.read(waystoneTag);

                String serverName = document.getString("server");
                ISyncServerProxy syncServer = CrossServerCoreAPI.getImmediateServer(serverName);

                enableWaystone(waystone, syncServer);
            });
        } catch (SharedDataStoreNotReadyException e) {
            DatasyncWaystones.LOGGER.error("Unable to load waystones data from database, trying to read before the database is ready");
        }
    }

    private void broadcastPlayerWaystones(Player player){
        Level level = ServerLifecycleHooks.getCurrentServer().overworld();
        IPlayerWaystoneData playersWaystoneData = PlayerWaystoneManager.getPlayerWaystoneData(level);

        List<UUID> waystoneUUIDs = new ArrayList<>();
        playersWaystoneData.getWaystones(player).forEach(waystone -> waystoneUUIDs.add(waystone.getWaystoneUid()));
        ISyncPlayer syncPlayer = CrossServerCoreAPI.getImmediatePlayer(player);
        CrossServerCoreAPI.sendCrossMessageToAllOtherServer(new PlayerWaystonesData(syncPlayer, waystoneUUIDs));
    }

    private void savePlayerWaystonesToDatabase(Player player){
        Level level = ServerLifecycleHooks.getCurrentServer().overworld();
        IPlayerWaystoneData playersWaystoneData = PlayerWaystoneManager.getPlayerWaystoneData(level);

        List<UUID> waystoneUUIDs = new ArrayList<>();
        playersWaystoneData.getWaystones(player).forEach(waystone -> waystoneUUIDs.add(waystone.getWaystoneUid()));

        Document document = new Document("player", player.getName().getString());
        String waystonesData = String.join(",", waystoneUUIDs.stream().map(UUID::toString).toList());
        document.append("waystones", waystonesData);

        databasePlayerWaystonesData.accessOrThrow().replaceOne(new Document("player", player.getName().getString()), document, new ReplaceOptions().upsert(true));
    }

    private void loadPlayerWaystonesFromDatabase(Player player){
        Document document = databasePlayerWaystonesData.accessOrThrow().find(new Document("player", player.getName().getString())).first();
        if(document == null) return;
        String waystones = document.get("waystones").toString();
        if (waystones.isEmpty()) return;

        List<UUID> waystonesUUID = Arrays.stream(document.getString("waystones").split(",")).map(UUID::fromString).toList();
        tryActivateWaystonesForPlayer(player, waystonesUUID);
    }

    public ISyncServer getWaystoneServer(IWaystone waystone) {
        return getWaystoneServer(waystone.getWaystoneUid());
    }

    public void enableWaystone(IWaystone waystone, ISyncServer syncServer) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        net.blay09.mods.waystones.core.WaystoneManager.get(server).addWaystone(waystone);
        setWaystoneServer(waystone, syncServer);

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
