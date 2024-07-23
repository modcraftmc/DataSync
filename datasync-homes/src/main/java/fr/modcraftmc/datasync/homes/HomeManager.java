package fr.modcraftmc.datasync.homes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.client.model.ReplaceOptions;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.CrossServerCoreProxyExtensionAPI;
import fr.modcraftmc.crossservercore.api.events.CrossServerCoreReadyEvent;
import fr.modcraftmc.crossservercore.api.events.PlayerJoinClusterEvent;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncPlayer;
import fr.modcraftmc.crossservercore.api.sharedpersistentdata.ISharedDataStore;
import fr.modcraftmc.crossservercore.api.sharedpersistentdata.SharedDataStore;
import fr.modcraftmc.crossservercore.api.sharedpersistentdata.SharedDataStoreNotReadyException;
import fr.modcraftmc.datasync.homes.messages.ChangeGlobalHomesLimit;
import fr.modcraftmc.datasync.homes.messages.ChangePlayerHomesLimit;
import fr.modcraftmc.datasync.homes.messages.HomeTpRequest;
import fr.modcraftmc.datasync.homes.messages.SetHome;
import fr.modcraftmc.datasync.homes.serialization.SerializationUtil;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class HomeManager {

    private final HashMap<ISyncPlayer, HomesData> playerHomesDataMap = new HashMap<>();
    private final HashMap<ISyncPlayer, PendingHomeTp> pendingHomeTpList = new HashMap<>();

    private static final String homesCollectionName = "homes";
    private ISharedDataStore homesCollection = new SharedDataStore(homesCollectionName);

    private static int pendingHomeTpTimeout = 120;
    private int maxHomes = 5;

    public HomeManager() {
        MinecraftForge.EVENT_BUS.addListener(this::onCrossServerCoreReady);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerJoinCluster);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLeaveCluster);
    }

    private void onCrossServerCoreReady(CrossServerCoreReadyEvent event){
        loadGlobalHomesLimitFromDatabase();
    }

    private void onPlayerJoinCluster(PlayerJoinClusterEvent event){
        loadPlayerHomesData(event.getPlayer());
    }

    private void onPlayerLeaveCluster(PlayerJoinClusterEvent event){
        unloadPlayerHomesData(event.getPlayer());
    }

    public List<String> getHomeNames(ISyncPlayer player) {
        List<String> homesNames = new ArrayList<>();
        HomesData playerHomesData = playerHomesDataMap.get(player);
        if(playerHomesData == null)
            return homesNames;

        for (Home home : playerHomesData.homes()) {
            homesNames.add(home.name());
        }
        return homesNames;
    }

    public Home getHomeByName(ISyncPlayer player, String name) {
        return playerHomesDataMap.get(player).homes().stream().filter((home) -> home.name.equals(name)).findFirst().get(); //heh
    }

    public int getPlayerHomesLimit(ISyncPlayer player) {
        return playerHomesDataMap.get(player).homesLimit().orElse(maxHomes);
    }

    public Optional<Integer> getPlayerHomesLimitOptional(ISyncPlayer player){
        return playerHomesDataMap.get(player).homesLimit();
    }

    public int getRemainingHomes(ISyncPlayer player) {
        return  getPlayerHomesLimit(player) - playerHomesDataMap.get(player).homes().size();
    }

    public boolean canCreateHome(ISyncPlayer player) {
        return getRemainingHomes(player) > 0;
    }

    public void tryTeleportPlayerToHome(ISyncPlayer playerToTeleport, ISyncPlayer playerHomeOwner, String targetHome) {
        Home target = null;
        for (Home home : playerHomesDataMap.get(playerHomeOwner).homes()) {
            if (home.name().equals(targetHome)) {
                target = home;
                break;
            }
        }

        if (target == null) {
            throw new CommandRuntimeException(Component.literal("Home " + targetHome + " not found for player " + playerHomeOwner));
        }

        tryTeleportPlayerToHome(playerToTeleport, target);
    }

    private void tryTeleportPlayerToHome(ISyncPlayer playerToTeleport, Home target) {
        if(!target.server.equals(CrossServerCoreAPI.getServerName())){
            CrossServerCoreAPI.getServer(target.server).ifPresentOrElse(server -> {
                CrossServerCoreProxyExtensionAPI.transferPlayer(playerToTeleport, server);
                addPendingHomeTp(playerToTeleport, target);
            }, () -> {
                DatasyncHomes.LOGGER.error("Server {} not found", target.server);
            });
        } else {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(playerToTeleport.getUUID());
            if (serverPlayer != null) {
                ServerLevel serverLevel = server.getLevel(SerializationUtil.GetResourceKey(SerializationUtil.StringToJsonElement(target.dimension), Registry.DIMENSION_REGISTRY));
                if (serverLevel != null) {
                    server.execute(() -> serverPlayer.teleportTo(serverLevel, target.x, target.y, target.z, serverPlayer.getYRot(), serverPlayer.getXRot()));
                }
            }
        }
    }

    public void addPendingHomeTp(ISyncPlayer playerToTeleport, Home home){
        CrossServerCoreAPI.sendCrossMessageToServer(new HomeTpRequest(playerToTeleport, home), home.server());
    }

    public void addPendingHomeTp(HomeTpRequest homeTpRequest) {
        synchronized (pendingHomeTpList) {
            pendingHomeTpList.put(homeTpRequest.getPlayer(), new PendingHomeTp(homeTpRequest.getHome(), (int) System.currentTimeMillis() / 1000));
        }
    }

    public void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event){
        synchronized (pendingHomeTpList) {
            pendingHomeTpList.entrySet().removeIf(pendingHomeTp -> pendingHomeTp.getValue().time() + pendingHomeTpTimeout < (int) System.currentTimeMillis() / 1000);

            ISyncPlayer player = CrossServerCoreAPI.getPlayer(event.getEntity().getUUID()).orElseThrow();
            if (pendingHomeTpList.containsKey(player)) {
                PendingHomeTp pendingHomeTp = pendingHomeTpList.remove(player);
                tryTeleportPlayerToHome(player, pendingHomeTp.home());
            }
        }
    }

    public void loadPlayerHomesData(ISyncPlayer player){
        playerHomesDataMap.put(player, getHomesDataFromDatabase(player));
    }

    public void unloadPlayerHomesData(ISyncPlayer player){
        String playerName = player.getName();
        if(ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(playerName) != null)
            savePlayerHomesData(player);
        playerHomesDataMap.remove(playerName);
    }

    public void savePlayerHomesData(ISyncPlayer player) {
        String playerName = player.getName();
        if(!playerHomesDataMap.containsKey(player)){
            DatasyncHomes.LOGGER.error("Trying to save player homes data for player {} but player isn't loaded", playerName);
            return;
        }

        Document document = new Document("player", playerName).append("homesData", playerHomesDataMap.get(player).serialize().toString());

        homesCollection.accessOrThrow().replaceOne(new Document("player", playerName), document, new ReplaceOptions().upsert(true));
    }

    public HomesData getHomesDataFromDatabase(ISyncPlayer player) {
        Document document = homesCollection.accessOrThrow().find(new Document("player", player.getName())).first();
        if(document != null){
            return HomesData.deserialize(SerializationUtil.gson.fromJson(document.get("homesData").toString(), JsonObject.class));
        }

        return new HomesData(Optional.empty(), new ArrayList<>());
    }

    public void loadGlobalHomesLimitFromDatabase(){
        Document document = null;
        try {
            document = homesCollection.access().find(new Document("global", "homesLimit")).first();
        } catch (SharedDataStoreNotReadyException e) {
            DatasyncHomes.LOGGER.error("Error while trying to access homes, loading of homes happens too early");
        }

        if(document != null){
            int limit = document.getInteger("limit");
            maxHomes = limit;
            return;
        }

        saveGlobalHomesLimitToDatabase();
    }

    public void saveGlobalHomesLimitToDatabase() {
        Document document = new Document("global", "homesLimit").append("limit", maxHomes);

        homesCollection.accessOrThrow().replaceOne(new Document("global", "homesLimit"), document, new ReplaceOptions().upsert(true));
    }

    public void createHome(ISyncPlayer player, String homeName, int x, int y, int z, String dimension) {
        Home home = new Home(homeName, x, y, z, dimension, CrossServerCoreAPI.getServerName());
        addCachedHome(player, home);
        savePlayerHomesData(player);
        CrossServerCoreAPI.sendCrossMessageToAllOtherServer(new SetHome(player, SetHome.ActionType.SET, home));
    }

    public void addCachedHome(ISyncPlayer player, Home home){
        HomesData playerHomesData = playerHomesDataMap.get(player);

        if(playerHomesData != null)
            playerHomesData.homes().add(home);
    }

    public void deleteHome(ISyncPlayer player, String homeName) {
        removeCachedHome(player, homeName);
        savePlayerHomesData(player);
        CrossServerCoreAPI.sendCrossMessageToAllOtherServer(new SetHome(player, SetHome.ActionType.DELETE, new Home(homeName, 0, 0, 0, "", "")));
    }

    public void removeCachedHome(ISyncPlayer player, String homeName){
        HomesData playerHomesData = playerHomesDataMap.get(player);

        if(playerHomesData != null)
            playerHomesData.homes().removeIf(home -> home.name().equals(homeName));
    }

    public void setPlayerHomesLimit(ISyncPlayer player, int count){
        setCachedPlayerHomesLimit(player, count);
        propagatePlayerHomesLimit(player);
    }

    public void setCachedPlayerHomesLimit(ISyncPlayer player, int count){
        HomesData playerHomesData = playerHomesDataMap.get(player);
        if(playerHomesData != null)
            playerHomesData.setHomesLimit(count);
    }

    private void propagatePlayerHomesLimit(ISyncPlayer player){
        savePlayerHomesData(player);
        CrossServerCoreAPI.sendCrossMessageToAllOtherServer(new ChangePlayerHomesLimit(player, getPlayerHomesLimitOptional(player)));
    }

    public void unsetPlayerHomesLimit(ISyncPlayer player){
        unsetCachedPlayerHomesLimit(player);
        propagateGlobalHomesLimit();
    }

    public void unsetCachedPlayerHomesLimit(ISyncPlayer player){
        HomesData playerHomesData = playerHomesDataMap.get(player);
        if(playerHomesData != null)
            playerHomesData.unsetHomesLimit();
    }

    public void setGlobalHomesLimit(int count){
        maxHomes = count;
    }

    public void propagateGlobalHomesLimit(){
        saveGlobalHomesLimitToDatabase();
        CrossServerCoreAPI.sendCrossMessageToAllOtherServer(new ChangeGlobalHomesLimit(maxHomes));
    }

    public boolean homeExists(ISyncPlayer player, String homeName) {
        return playerHomesDataMap.get(player).homes().stream().anyMatch(home -> home.name().equals(homeName));
    }

    public int getGlobalHomesLimit() {
        return maxHomes;
    }

    public record Home(String name, int x, int y, int z, String dimension, String server) {
        public JsonObject serialize(){
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("name", name);
            jsonObject.addProperty("x", x);
            jsonObject.addProperty("y", y);
            jsonObject.addProperty("z", z);
            jsonObject.addProperty("dimension", dimension);
            jsonObject.addProperty("server", server);
            return jsonObject;
        }

        public static Home deserialize(JsonObject jsonObject){
            return new Home(
                    jsonObject.get("name").getAsString(),
                    jsonObject.get("x").getAsInt(),
                    jsonObject.get("y").getAsInt(),
                    jsonObject.get("z").getAsInt(),
                    jsonObject.get("dimension").getAsString(),
                    jsonObject.get("server").getAsString()
            );
        }
    }

    public boolean isLocalHome(ISyncPlayer player, String homeName) {
        return getHomeByName(player, homeName).server().equals(CrossServerCoreAPI.getServerName());
    }

    public static class HomesData {
        private Optional<Integer> homesLimit;
        private List<Home> homes;

        public HomesData(Optional<Integer> homesLimit, List<Home> homes) {
            this.homesLimit = homesLimit;
            this.homes = homes;
        }

        public void setHomesLimit(int homesLimit) {
            this.homesLimit = Optional.of(homesLimit);
        }

        public void unsetHomesLimit() {
            this.homesLimit = Optional.empty();
        }

        public Optional<Integer> homesLimit() {
            return homesLimit;
        }

        public List<Home> homes() {
            return homes;
        }

        public JsonObject serialize(){
            JsonObject jsonObject = new JsonObject();
            homesLimit.ifPresent(integer -> jsonObject.addProperty("homesLimit", integer));
            JsonArray homesJsonArray = new JsonArray();
            for (Home home : homes) {
                homesJsonArray.add(home.serialize());
            }
            jsonObject.add("homes", homesJsonArray);
            return jsonObject;
        }

        public static HomesData deserialize(JsonObject jsonObject){
            List<Home> homes = new ArrayList<>();
            JsonArray homesJsonArray = SerializationUtil.gson.fromJson(jsonObject.get("homes"), JsonArray.class);
            for (int i = 0; i < homesJsonArray.size(); i++) {
                homes.add(Home.deserialize(homesJsonArray.get(i).getAsJsonObject()));
            }

            JsonElement homesLimitData = jsonObject.get("homesLimit");

            return new HomesData(
                    homesLimitData == null ? Optional.empty() : Optional.of(homesLimitData.getAsInt()),
                    homes
            );
        }
    }
    public record PendingHomeTp(Home home, int time) {}
}
