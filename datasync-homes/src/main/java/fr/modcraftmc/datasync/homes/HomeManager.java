package fr.modcraftmc.datasync.homes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoCollection;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.CrossServerCoreProxyExtensionAPI;
import fr.modcraftmc.datasync.homes.messages.ChangeGlobalHomesLimit;
import fr.modcraftmc.datasync.homes.messages.HomeTpRequest;
import fr.modcraftmc.datasync.homes.serialization.SerializationUtil;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class HomeManager {

    private final HashMap<String, HomesData> playerHomesDataMap = new HashMap<>();
    private final HashMap<String, PendingHomeTp> pendingHomeTpList = new HashMap<>();

    private static final String homesCollectionName = "homes";
    private MongoCollection<Document> homesCollection;

    private static int pendingHomeTpTimeout = 120;
    private int maxHomes = 5;

    public HomeManager() {
        CrossServerCoreAPI.runWhenCSCIsReady(() -> {
            homesCollection = CrossServerCoreAPI.instance.getOrCreateMongoCollection(homesCollectionName);
            getGlobalHomesLimitFromDatabase();

            CrossServerCoreAPI.instance.registerOnPlayerJoinedCluster((playerName, SyncServer) -> {
                loadPlayerHomesData(playerName);
            });

            CrossServerCoreAPI.instance.registerOnPlayerLeftCluster((playerName, SyncServer) -> {
                unloadPlayerHomesData(playerName);
            });
        });
    }

    public List<String> getHomeNames(String player) {
        List<String> homesNames = new ArrayList<>();
        for (Home home : playerHomesDataMap.get(player).homes()) {
            homesNames.add(home.name());
        }
        return homesNames;
    }

    public int getPlayerHomesLimit(String player) {
        return playerHomesDataMap.get(player).homesLimit().orElse(maxHomes);
    }

    public int getRemainingHomes(String player) {
        return  getPlayerHomesLimit(player) - playerHomesDataMap.get(player).homes().size();
    }

    public boolean canCreateHome(String player) {
        return getRemainingHomes(player) > 0;
    }

    public void tryTeleportPlayerToHome(String playerToTeleport, String playerHomeOwner, String targetHome) {
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

    private void tryTeleportPlayerToHome(String playerToTeleport, Home target) {
        if(!target.server.equals(CrossServerCoreAPI.instance.getServerName())){
            CrossServerCoreProxyExtensionAPI.instance.transferPlayer(playerToTeleport, target.server);
            addPendingHomeTp(playerToTeleport, target);
        } else {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerPlayer serverPlayer = server.getPlayerList().getPlayerByName(playerToTeleport);
            if (serverPlayer != null) {
                ServerLevel serverLevel = server.getLevel(SerializationUtil.GetResourceKey(SerializationUtil.StringToJsonElement(target.dimension), Registry.DIMENSION_REGISTRY));
                if (serverLevel != null) {
                    server.execute(() -> serverPlayer.teleportTo(serverLevel, target.x, target.y, target.z, serverPlayer.getYRot(), serverPlayer.getXRot()));
                }
            }
        }
    }

    public void addPendingHomeTp(String playerToTeleport, Home home){
        CrossServerCoreAPI.instance.sendCrossMessageToServer(new HomeTpRequest(playerToTeleport, home), home.server());
    }

    public void addPendingHomeTp(HomeTpRequest homeTpRequest) {
        synchronized (pendingHomeTpList) {
            pendingHomeTpList.put(homeTpRequest.getPlayerName(), new PendingHomeTp(homeTpRequest.getHome(), (int) System.currentTimeMillis() / 1000));
        }
    }

    public void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event){
        synchronized (pendingHomeTpList) {
            pendingHomeTpList.entrySet().removeIf(pendingHomeTp -> pendingHomeTp.getValue().time() + pendingHomeTpTimeout < (int) System.currentTimeMillis() / 1000);

            if (pendingHomeTpList.containsKey(event.getEntity().getName().getString())) {
                PendingHomeTp pendingHomeTp = pendingHomeTpList.remove(event.getEntity().getName().getString());
                tryTeleportPlayerToHome(event.getEntity().getName().getString(), pendingHomeTp.home());
            }
        }
    }

    public void loadPlayerHomesData(String player){
        playerHomesDataMap.put(player, getHomesDataFromDatabase(player));
    }

    public void unloadPlayerHomesData(String player){
        savePlayerHomesData(player);
        playerHomesDataMap.remove(player);
    }

    public void savePlayerHomesData(String player){
        Document document = new Document("player", player).append("homesData", playerHomesDataMap.get(player).serialize().toString());

        homesCollection.deleteMany(new Document("player", player));
        homesCollection.insertOne(document);
    }

    public HomesData getHomesDataFromDatabase(String player){
        Document document = homesCollection.find(new Document("player", player)).first();
        if(document != null){
            return HomesData.deserialize(SerializationUtil.gson.fromJson(document.get("homesData").toString(), JsonObject.class));
        }

        return new HomesData(Optional.empty(), new ArrayList<>());
    }

    public int getGlobalHomesLimitFromDatabase(){
        Document document = homesCollection.find(new Document("global", "homesLimit")).first();
        if(document != null){
            int limit = document.getInteger("limit");
            maxHomes = limit;
            return limit;
        }
        saveGlobalHomesLimitToDatabase();
        return maxHomes;
    }

    public void saveGlobalHomesLimitToDatabase(){
        Document document = new Document("global", "homesLimit").append("limit", maxHomes);

        homesCollection.deleteMany(new Document("global", "homesLimit"));
        homesCollection.insertOne(document);
    }

    public void createHome(String playerName, String homeName, int x, int y, int z, String dimension) {
        Home home = new Home(homeName, x, y, z, dimension, CrossServerCoreAPI.instance.getServerName());
        playerHomesDataMap.get(playerName).homes().add(home);
        savePlayerHomesData(playerName);
    }

    public void deleteHome(String playerName, String homeName) {
        playerHomesDataMap.get(playerName).homes().removeIf(home -> home.name().equals(homeName));
        savePlayerHomesData(playerName);
    }

    public void setPlayerHomesLimit(String playerName, int count){
        playerHomesDataMap.get(playerName).setHomesLimit(count);
        savePlayerHomesData(playerName);
    }

    public void unsetPlayerHomesLimit(String playerName){
        playerHomesDataMap.get(playerName).unsetHomesLimit();
        savePlayerHomesData(playerName);
    }

    public void setGlobalHomesLimit(int count){
        maxHomes = count;
    }

    public void propagateGlobalHomesLimit(){
        saveGlobalHomesLimitToDatabase();
        CrossServerCoreAPI.instance.sendCrossMessageToAllOtherServer(new ChangeGlobalHomesLimit(maxHomes));
    }

    public boolean homeExists(String playerName, String homeName) {
        return playerHomesDataMap.get(playerName).homes().stream().anyMatch(home -> home.name().equals(homeName));
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
