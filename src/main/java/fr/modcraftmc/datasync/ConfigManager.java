package fr.modcraftmc.datasync;

import com.moandjiezana.toml.Toml;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class ConfigManager {
    public static final String CONFIG_FILE_NAME = "datasync-config.toml";
    private static Toml config;
    public static boolean isLoaded = false;

    public static String serverName;
    public static RabbitmqConfigData rabbitmqConfigData;
    public static MongodbConfigData mongodbConfigData;


    public static void loadConfigFile(){
        DataSync.LOGGER.info("Loading config file");

        try {
            config = readConfigFile();
        } catch (IOException e) {
            DataSync.LOGGER.error("Error while reading config file : %s".formatted(e.getMessage()));
            throw new RuntimeException(e);
        }

        try {
            serverName = config.getString("server_name");
            rabbitmqConfigData = config.getTable("rabbitmq").to(RabbitmqConfigData.class);
            mongodbConfigData = config.getTable("mongodb").to(MongodbConfigData.class);
        } catch (Exception e) {
            DataSync.LOGGER.error("Error while reading config file : %s".formatted(e.getMessage()));
            throw new RuntimeException(e);
        }

        isLoaded = true;
    }

    private static Toml readConfigFile() throws IOException {
        File configFile = new File(FMLPaths.CONFIGDIR.get().toFile(), CONFIG_FILE_NAME);
        if (!configFile.exists()) {
            configFile.mkdirs();
            DataSync.LOGGER.info("Config file not found, creating one !");
            if(!configFile.createNewFile())
                throw new IOException("Error while creating config file");
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(configFile));
            writer.write(defaultConfigFileContent());
            writer.close();
        }
        return new Toml().read(configFile);

    }

    private static String defaultConfigFileContent(){
        return """
                server_name = "default"
                
                [rabbitmq]
                host = "localhost"
                port = 5672
                username = "guest"
                password = "guest"
                vhost = "/"
                
                [mongodb]
                host = "localhost"
                port = 27017
                username = "root"
                password = "root"
                database = "datasync"
                """;
    }

    public static class RabbitmqConfigData {
        public String host;
        public int port;
        public String username;
        public String password;
        public String vhost;
    }

    public static class MongodbConfigData {
        public String host;
        public int port;
        public String username;
        public String password;
        public String database;
    }
}
