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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

@Mod(DataSync.MOD_ID)
public class DataSync {
    public static final String MOD_ID = "datasync";
    public static final Logger LOGGER = LogUtils.getLogger();
    private RabbitmqConnection rabbitmqConnection;
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

        Toml config;
        try {
            config = readConfig();
        } catch (IOException e) {
            LOGGER.error("Error while reading config file : %s".formatted(e.getMessage()));
            throw new RuntimeException(e);
        }
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

    private Toml readConfig() throws IOException {
        File configFile = new File(FMLPaths.CONFIGDIR.get().toFile(), "datasync-config.toml");
        if (!configFile.getParentFile().exists()) {
            if(!configFile.getParentFile().mkdirs())
                throw new IOException("Error while creating config directory");
        }
        if (!configFile.exists()) {
            LOGGER.info("Config file not found, creating one !");
            if(!configFile.createNewFile())
                throw new IOException("Error while creating config file");
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(configFile));
            writer.write(defaultFileContent());
            writer.close();
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

    private static class RabbitmqConfigData {
        private String host;
        private int port;
        private String username;
        private String password;
        private String vhost;
    }
}
