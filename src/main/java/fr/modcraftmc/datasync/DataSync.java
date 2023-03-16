package fr.modcraftmc.datasync;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.moandjiezana.toml.Toml;
import com.mojang.logging.LogUtils;
import fr.modcraftmc.datasync.command.DataSyncCommand;
import fr.modcraftmc.datasync.message.MessageHandler;
import fr.modcraftmc.datasync.rabbitmq.RabbitmqConnection;
import fr.modcraftmc.datasync.rabbitmq.RabbitmqDirectPublisher;
import fr.modcraftmc.datasync.rabbitmq.RabbitmqDirectSubscriber;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.TimeoutException;

@Mod(DataSync.MOD_ID)
public class DataSync {
    public static final String MOD_ID = "datasync";
    public static final Logger LOGGER = LogUtils.getLogger();
    private RabbitmqConnection rabbitmqConnection;
    private RabbitmqDirectSubscriber rabbitmqDirectSubscriber;
    private RabbitmqDirectPublisher rabbitmqDirectPublisher;
    public String serverName;

    public DataSync(){
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::serverSetup);

        MinecraftForge.EVENT_BUS.addListener(this::commandResister);
    }

    public void loadConfig(){
        LOGGER.info("Loading config file");
        if(rabbitmqConnection != null){
            rabbitmqConnection.close();
        }

        Toml config = readConfig();
        RabbitmqConfigData configData;
        try {
            serverName = config.getString("server_name");
            configData = config.getTable("rabbitmq").to(RabbitmqConfigData.class);
        } catch (Exception e) {
            LOGGER.error("Error while reading config file : %s".formatted(e.getMessage()));
            throw new RuntimeException(e);
        }

        try {
            this.rabbitmqConnection = new RabbitmqConnection(configData.host, configData.port, configData.username, configData.password, configData.vhost);
        } catch (IOException | TimeoutException e) {
            LOGGER.error("Error while connecting to RabbitMQ : %s".formatted(e.getMessage()));
            throw new RuntimeException(e);
        }
        LOGGER.info("Connected to RabbitMQ");

        initializeMessageSystem();
    }

    private Toml readConfig() {
        File configFile = new File(FMLPaths.CONFIGDIR.get().toFile(), "datasync-config.toml");
        if (!configFile.getParentFile().exists()) {
            configFile.getParentFile().mkdirs();
        }
        if (!configFile.exists()) {
            LOGGER.info("Config file not found, creating one !");
            try {
                configFile.createNewFile();
                OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(configFile));
                writer.write(defaultFileContent());
                writer.close();
            } catch (Exception e) {
                LOGGER.error("Error while creating config file : %s".formatted(e.getMessage()));
                throw new RuntimeException(e);
            }
        }
        return new Toml().read(configFile);
    }

    private String defaultFileContent(){
        return """
                server_name = "default"
                
                [rabbitmq]
                host = "localhost"
                port = 5672
                username = "guest"
                password = "guest"
                vhost = "/"
                """;
    }

    public void serverSetup(FMLDedicatedServerSetupEvent event){
        MessageHandler.init();
        loadConfig();
    }

    public void commandResister(RegisterCommandsEvent event){
        new DataSyncCommand(event.getDispatcher());
    }

    private void initializeMessageSystem(){
        this.rabbitmqDirectPublisher = new RabbitmqDirectPublisher(rabbitmqConnection);
        this.rabbitmqDirectSubscriber = new RabbitmqDirectSubscriber(rabbitmqConnection);
        this.rabbitmqDirectSubscriber.subscribe(serverName, (consumerTag, message) -> {
            DataSync.LOGGER.debug("Received message: " + new String(message.getBody()));
            String messageName = new String(message.getBody(), "UTF-8");
            Gson gson = new Gson();
            JsonObject jsonData = gson.fromJson(messageName, JsonObject.class);
            MessageHandler.handle(jsonData);
        });
    }

    private class RabbitmqConfigData {
        private String host;
        private int port;
        private String username;
        private String password;
        private String vhost;
    }
}
