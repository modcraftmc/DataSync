package fr.modcraftmc.datasync;

import com.mojang.logging.LogUtils;
import fr.modcraftmc.datasync.command.DataSyncCommand;
import fr.modcraftmc.datasync.invsync.PlayerDataLoader;
import fr.modcraftmc.datasync.message.MessageHandler;
import fr.modcraftmc.datasync.mongodb.MongodbConnection;
import fr.modcraftmc.datasync.networkidentity.PlayersLocation;
import fr.modcraftmc.datasync.networkidentity.ServerCluster;
import fr.modcraftmc.datasync.rabbitmq.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Mod(DataSync.MOD_ID)
@Mod.EventBusSubscriber(modid = DataSync.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DataSync {
    public static final String MOD_ID = "datasync";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final PlayersLocation playersLocation = new PlayersLocation();
    public static final ServerCluster serverCluster = new ServerCluster();
    public static List<Runnable> onConfigLoad;
    public static String serverName;

    public static MongodbConnection mongodbConnection;
    public static RabbitmqConnection rabbitmqConnection;

    public DataSync(){
        onConfigLoad = new ArrayList<>();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::serverSetup);

        MinecraftForge.EVENT_BUS.addListener(this::commandResister);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStop);
    }

    @SubscribeEvent
    public void serverSetup(FMLDedicatedServerSetupEvent event){
        DataSync.LOGGER.debug("Initializing main modules");
        initializeDatabaseConnection();
        initializeMessageSystem();
        MessageHandler.init();

        loadConfig();
        initializeNetworkIdentity();// must be after loadConfig because it use rabbitmq connection
        DataSync.LOGGER.info("Main modules initialized");
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
            mongodbConnection = new MongodbConnection(mongodbConfigData.host, mongodbConfigData.port, mongodbConfigData.username, mongodbConfigData.password, mongodbConfigData.database);
            PlayerDataLoader.databasePlayerData = mongodbConnection.getClient().getDatabase(mongodbConfigData.database).getCollection(References.PLAYER_DATA_COLLECTION_NAME);
            DataSync.LOGGER.info("Connected to MongoDB");
        });
    }

    public static void loadConfig(){
        ConfigManager.loadConfigFile();
        serverName = ConfigManager.serverName;

        onConfigLoad.forEach(Runnable::run);
    }
}
