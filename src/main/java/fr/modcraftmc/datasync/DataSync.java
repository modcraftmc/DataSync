package fr.modcraftmc.datasync;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import fr.modcraftmc.datasync.command.DataSyncCommand;
import fr.modcraftmc.datasync.invsync.PlayerDataLoader;
import fr.modcraftmc.datasync.message.MessageHandler;
import fr.modcraftmc.datasync.mongodb.MongodbConnection;
import fr.modcraftmc.datasync.rabbitmq.RabbitmqConnection;
import fr.modcraftmc.datasync.rabbitmq.RabbitmqDirectPublisher;
import fr.modcraftmc.datasync.rabbitmq.RabbitmqDirectSubscriber;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;

@Mod(DataSync.MOD_ID)
@Mod.EventBusSubscriber(modid = DataSync.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DataSync {
    public static final String MOD_ID = "datasync";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DataSync(){
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::serverSetup);

        MinecraftForge.EVENT_BUS.addListener(this::commandResister);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStop);
    }

    @SubscribeEvent
    public void serverSetup(FMLDedicatedServerSetupEvent event){
        ConfigManager.loadConfigFile();

        MessageHandler.init();
        initializeMessageSystem();
        initializeDatabaseConnection();
    }

    public void onServerStop(ServerStoppingEvent event){
        DataSync.LOGGER.info("Server stopping, saving player data");
    }

    public void commandResister(RegisterCommandsEvent event){
        new DataSyncCommand(event.getDispatcher());
    }

    private void initializeMessageSystem(){
        String serverName = ConfigManager.serverName;
        ConfigManager.RabbitmqConfigData rabbitmqConfigData = ConfigManager.rabbitmqConfigData;
        RabbitmqConnection rabbitmqConnection = new RabbitmqConnection(rabbitmqConfigData.host, rabbitmqConfigData.port, rabbitmqConfigData.username, rabbitmqConfigData.password, rabbitmqConfigData.vhost);
        DataSync.LOGGER.info("Connected to RabbitMQ");

        new RabbitmqDirectPublisher(rabbitmqConnection);
        new RabbitmqDirectSubscriber(rabbitmqConnection);
        RabbitmqDirectSubscriber.instance.subscribe(serverName, (consumerTag, message) -> {
            DataSync.LOGGER.debug("Received message: " + new String(message.getBody()));
            String messageName = new String(message.getBody(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            JsonObject jsonData = gson.fromJson(messageName, JsonObject.class);
            MessageHandler.handle(jsonData);
        });
    }

    private void initializeDatabaseConnection(){
        ConfigManager.MongodbConfigData mongodbConfigData = ConfigManager.mongodbConfigData;
        MongodbConnection mongodbConnection = new MongodbConnection(mongodbConfigData.host, mongodbConfigData.port, mongodbConfigData.username, mongodbConfigData.password, mongodbConfigData.database);
        PlayerDataLoader.databasePlayerData = mongodbConnection.getClient().getDatabase(mongodbConfigData.database).getCollection(References.PLAYER_DATA_COLLECTION_NAME);
        DataSync.LOGGER.info("Connected to MongoDB");
    }
}
