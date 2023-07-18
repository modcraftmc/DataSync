package fr.modcraftmc.datasync;

import com.mojang.logging.LogUtils;
import com.mongodb.client.MongoClient;
import fr.modcraftmc.datasync.command.DataSyncCommand;
import fr.modcraftmc.datasync.command.arguments.NetworkPlayerArgument;
import fr.modcraftmc.datasync.ftbsync.FTBSync;
import fr.modcraftmc.datasync.invsync.PlayerDataLoader;
import fr.modcraftmc.datasync.message.MessageHandler;
import fr.modcraftmc.datasync.networkidentity.PlayersLocation;
import fr.modcraftmc.datasync.networkidentity.ServerCluster;
import fr.modcraftmc.datasync.networking.Network;
import fr.modcraftmc.datasync.networking.packets.PacketUpdateClusterPlayers;
import fr.modcraftmc.datasync.rabbitmq.*;
import fr.modcraftmc.datasync.tpsync.TpRequestHandler;
import fr.modcraftmc.shared.mongodb.MongoDbConnectionBuilder;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerNegotiationEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Mod(DataSync.MOD_ID)
@Mod.EventBusSubscriber(modid = DataSync.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DataSync {
    public static final String MOD_ID = "datasync";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Client side
    public static List<String> playersOnCluster;

    // Server side
    public static List<Runnable> onConfigLoad;
    public static String serverName;

    public static final PlayersLocation playersLocation = new PlayersLocation();
    public static final ServerCluster serverCluster = new ServerCluster();

    //public static MongodbConnectionOLD mongodbConnection;
    public static MongoClient mongodbConnection;
    public static RabbitmqConnection rabbitmqConnection;

    public static SecurityWatcher dataSecurityWatcher;

    // Both side
    public static final Network network = new Network();
    public static DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES = DeferredRegister.create(Registry.COMMAND_ARGUMENT_TYPE_REGISTRY, MOD_ID);

    static {
        DataSync.ARGUMENT_TYPES.register("network_player", () -> ArgumentTypeInfos.registerByClass(NetworkPlayerArgument.class, SingletonArgumentInfo.contextFree(NetworkPlayerArgument::new)));
    }

    public DataSync(){
        DataSync.LOGGER.info("DataSync's here !");
        onConfigLoad = new ArrayList<>();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::serverSetup);

        MinecraftForge.EVENT_BUS.addListener(this::commandResister);



        ARGUMENT_TYPES.register(modEventBus);
        network.Init();
    }

    @SubscribeEvent
    public void serverSetup(FMLDedicatedServerSetupEvent event){
        dataSecurityWatcher = new SecurityWatcher("data security watcher");
        dataSecurityWatcher.registerOnInsecureEvent(() -> {
            kickAllPlayers("DataSync is not secure, you cannot join the server. Reason(s) : \n" + dataSecurityWatcher.getReason());
            DataSync.LOGGER.error("Data security is not ensured, server is now inaccessible.");
            DataSync.LOGGER.error("Reason(s) : \n" + dataSecurityWatcher.getReason());

            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                    while(!dataSecurityWatcher.isSecure()){
                        DataSync.LOGGER.error("Data security is not ensured.");
                        DataSync.LOGGER.error("Reason(s) : \n" + dataSecurityWatcher.getReason());

                        Thread.sleep(10000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        });
        dataSecurityWatcher.registerOnSecureEvent(() -> {
            DataSync.LOGGER.warn("Data security is ensured again, server is now accessible");
        });

        try {
            MinecraftForge.EVENT_BUS.addListener(this::onServerStop);
            MinecraftForge.EVENT_BUS.addListener(DataSync::onPreLogin);
            MinecraftForge.EVENT_BUS.addListener(DataSync::onPlayerJoin);
            MinecraftForge.EVENT_BUS.addListener(TpRequestHandler::onPlayerJoined);
            MinecraftForge.EVENT_BUS.addListener(PlayerDataLoader::onPlayerJoined);
            MinecraftForge.EVENT_BUS.addListener(PlayerDataLoader::onPlayerSave);
        }catch (Exception e){
            DataSync.LOGGER.error("Error while registering events server side", e);
        }

        DataSync.LOGGER.debug("Initializing main modules");
        initializeDatabaseConnection();
        initializeMessageSystem();
        MessageHandler.init();
        loadConfig();
        initializeFTBSync(); // must be after loadConfig because it use mongodb connection
        initializeNetworkIdentity();// must be after loadConfig because it use rabbitmq connection
        DataSync.LOGGER.info("Main modules initialized");
    }

    private void initializeFTBSync() {
        DataSync.LOGGER.debug("Initializing FTBSync");
        FTBSync.init();
        DataSync.LOGGER.info("FTBSync initialized");
    }

    private void initializeNetworkIdentity() {
        DataSync.LOGGER.debug("Initializing network identity");
        serverCluster.attach();
        DataSync.LOGGER.info("Network identity initialized");
    }

    public void onServerStop(ServerStoppingEvent event){
        serverCluster.detach();
    }

    public void commandResister(RegisterCommandsEvent event){
        DataSync.LOGGER.debug("Registering commands");
        new DataSyncCommand(event.getDispatcher());
    }

    private void initializeMessageSystem(){
        onConfigLoad.add(() -> {
            DataSync.LOGGER.debug("Connecting to RabbitMQ");
            ConfigManager.RabbitmqConfigData rabbitmqConfigData = ConfigManager.rabbitmqConfigData;

            if(rabbitmqConnection != null) rabbitmqConnection.close();
            rabbitmqConnection = new RabbitmqConnection(rabbitmqConfigData.host, rabbitmqConfigData.port, rabbitmqConfigData.username, rabbitmqConfigData.password, rabbitmqConfigData.vhost);
            DataSync.LOGGER.info("Connected to RabbitMQ");

            DataSync.LOGGER.debug("Initializing message streams");
            new RabbitmqPublisher(rabbitmqConnection);
            new RabbitmqSubscriber(rabbitmqConnection);
            new RabbitmqDirectPublisher(rabbitmqConnection);
            new RabbitmqDirectSubscriber(rabbitmqConnection);
            DataSync.LOGGER.debug("Message streams initialized");
        });
    }

    private void initializeDatabaseConnection(){
        onConfigLoad.add(() -> {
            DataSync.LOGGER.debug("Connecting to MongoDB");
            ConfigManager.MongodbConfigData mongodbConfigData = ConfigManager.mongodbConfigData;

            if(mongodbConnection != null) mongodbConnection.close();
            mongodbConnection = new MongoDbConnectionBuilder()
                    .host(mongodbConfigData.host)
                    .port(mongodbConfigData.port)
                    .username(mongodbConfigData.username)
                    .password(mongodbConfigData.password)
                    .authsource(mongodbConfigData.database)
                    .onHeartbeatFailed(() -> DataSync.dataSecurityWatcher.addIssue(SecurityWatcher.MONGODB_CONNECTION_ISSUE))
                    .onHeartbeatSucceeded(() -> DataSync.dataSecurityWatcher.removeIssue(SecurityWatcher.MONGODB_CONNECTION_ISSUE))
                    .build();
            PlayerDataLoader.databasePlayerData = mongodbConnection.getDatabase(mongodbConfigData.database).getCollection(References.PLAYER_DATA_COLLECTION_NAME);
            FTBSync.databaseTeamsData = mongodbConnection.getDatabase(mongodbConfigData.database).getCollection(References.TEAMS_DATA_COLLECTION_NAME);
            DataSync.LOGGER.info("Connected to MongoDB");
        });
    }

    public static void loadConfig(){
        ConfigManager.loadConfigFile();
        serverName = ConfigManager.serverName;

        onConfigLoad.forEach(Runnable::run);
    }

    public static void sendProxy(String message){
        try {
            RabbitmqDirectPublisher.instance.publish("proxy", message);
        } catch (IOException e) {
            LOGGER.error(String.format("Error while publishing message to rabbitmq cannot send message to proxy : %s", e.getMessage()));
        }
    }

    public static void updatePlayersLocationToClients(){
        if(ServerLifecycleHooks.getCurrentServer() == null) return;

        PacketUpdateClusterPlayers packetUpdateClusterPlayers = new PacketUpdateClusterPlayers(playersLocation.getAllPlayers());
        network.sendToAllPlayers(packetUpdateClusterPlayers);
    }

    public static void onPreLogin(PlayerNegotiationEvent event){
        if(!dataSecurityWatcher.isSecure()){
            event.getConnection().disconnect(Component.literal("DataSync is not secure, you cannot join the server. Reason(s) : \n" + dataSecurityWatcher.getReason()));
        }
    }

    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event){
        PacketUpdateClusterPlayers packetUpdateClusterPlayers = new PacketUpdateClusterPlayers(playersLocation.getAllPlayers());
        network.sendTo(packetUpdateClusterPlayers, (ServerPlayer) event.getEntity());
    }

    public static void kickAllPlayers(String reason){
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if(server == null) return;

        for(ServerPlayer player : server.getPlayerList().getPlayers()){
            player.connection.disconnect(Component.literal(reason));
        }
    }


}
