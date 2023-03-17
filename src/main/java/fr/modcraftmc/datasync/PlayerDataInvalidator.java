package fr.modcraftmc.datasync;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PlayerDataInvalidator {
    private static final List<String> playerDataInvalidated = new ArrayList<>();
    private static final String FILE_NAME = "playerDataInvalidated.json";

    public static void invalidatePlayerData(String playerName) {
        playerDataInvalidated.add(playerName);
    }

    public static void validatePlayerData(String playerName) {
        playerDataInvalidated.remove(playerName);
    }

    public static boolean isPlayerDataInvalidated(String playerName) {
        return playerDataInvalidated.contains(playerName);
    }

    public static void savePlayerDataInvalidated() {
        File file = new File(FMLPaths.GAMEDIR.get().toFile(), FILE_NAME);
        if(!FileUtil.ensureFileExist(file)){
            DataSync.LOGGER.error(String.format("Error while creating %s", FILE_NAME));
            return;
        }

        JsonArray jsonArray = new JsonArray();
        for (String playerName : playerDataInvalidated) {
            jsonArray.add(playerName);
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("playerDataInvalidated", jsonArray);

        Gson gson = new Gson();
        String json = gson.toJson(jsonObject);

        if(!FileUtil.write(file, json)) {
            DataSync.LOGGER.error(String.format("Error while saving %s", FILE_NAME));
        }
    }

    public static void loadPlayerDataInvalidated(){
        File file = new File(FMLPaths.GAMEDIR.get().toFile(), FILE_NAME);
        if(!file.exists()) return;

        StringBuilder stringBuilder = new StringBuilder();
        if(!FileUtil.read(file, stringBuilder)){
            DataSync.LOGGER.error(String.format("Error while creating %s", FILE_NAME));
            return;
        }

        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(stringBuilder.toString(), JsonObject.class);

        JsonArray jsonArray = jsonObject.getAsJsonArray("playerDataInvalidated");
        for (int i = 0; i < jsonArray.size(); i++) {
            playerDataInvalidated.add(jsonArray.get(i).getAsString());
        }
    }
}
