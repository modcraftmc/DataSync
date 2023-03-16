package fr.modcraftmc.datasync.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.modcraftmc.datasync.serialization.PlayerSerializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import top.theillusivec4.curios.api.CuriosCapability;

import java.io.*;
import java.util.Objects;

public class SerializerTestCommand {
    private JsonObject playerInfo;

    private LiteralArgumentBuilder<CommandSourceStack> commandTree;

    public SerializerTestCommand(DataSyncCommand dataSyncCommand) {
        buildCommand();
        dataSyncCommand.registerCommand(commandTree);
    }

    public void buildCommand(){
        commandTree = Commands.literal("test")
                .then(Commands.literal("saveclearload")
                    .executes(context -> {
                        savePlayerdata(context.getSource());
                        clearInventory(context.getSource());
                        loadPlayerData(context.getSource());
                        return 1;
                    }))
                .then(Commands.literal("save")
                    .executes(context -> {
                        savePlayerdata(context.getSource());
                        return 1;
                    }))
                .then(Commands.literal("clear")
                    .executes(context -> {
                        clearInventory(context.getSource());
                        return 1;
                    }))
                .then(Commands.literal("load")
                    .executes(context -> {
                        loadPlayerData(context.getSource());
                        return 1;
                    }))
                .then(Commands.literal("showjson")
                    .executes(context -> {
                        showJson(context.getSource());
                        return 1;
                    }))
                .then(Commands.literal("savejson")
                    .executes(context -> {
                        saveJson(context.getSource());
                        return 1;
                    }))
                .then(Commands.literal("loadjson")
                    .executes(context -> {
                        loadJson(context.getSource());
                        return 1;
                    }))
                .then(Commands.literal("test")
                    .executes(context -> {
                        test(context.getSource());
                        return 1;
                    }));
    }

    private void savePlayerdata(CommandSourceStack source){
        playerInfo = PlayerSerializer.serializePlayer(Objects.requireNonNull(source.getPlayer()));
        source.sendSuccess(Component.literal("Player saved"), true);
    }

    private void clearInventory(CommandSourceStack source){
        source.getPlayer().getInventory().clearContent();

        if(ModList.get().isLoaded("curios"))
            source.getPlayer().getCapability(CuriosCapability.INVENTORY).ifPresent((itemHandler) -> itemHandler.saveInventory(true));

        source.sendSuccess(Component.literal("Inventory cleared"), true);
    }

    private void loadPlayerData(CommandSourceStack source){
        if (playerInfo != null) {
            PlayerSerializer.deserializePlayer(playerInfo, Objects.requireNonNull(source.getPlayer()));
            source.sendSuccess(Component.literal("Player loaded"), true);
        } else {
            source.sendFailure(Component.literal("Player is null"));
        }
    }

    public void showJson(CommandSourceStack source){
        String json;
        Gson gson = new GsonBuilder()
                        .setPrettyPrinting()
                        .create();
        json = gson.toJson(playerInfo);

        source.sendSuccess(Component.literal(json), true);
    }

    public void saveJson(CommandSourceStack source){
        String json;
        Gson gson = new GsonBuilder()
                        .setPrettyPrinting()
                        .create();
        json = gson.toJson(playerInfo);

        // Get or create file
        File savefile = FMLPaths.GAMEDIR.get().resolve(String.format("%s.json", Objects.requireNonNull(source.getPlayer()).getName().getString())).toFile();
        if(!savefile.exists()) {
            try {
                savefile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Write to file
        try (OutputStreamWriter writer = new OutputStreamWriter(new java.io.FileOutputStream(savefile))) {
            writer.write(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        source.sendSuccess(Component.literal(String.format("saved to %s", savefile)), true);
    }

    public void loadJson(CommandSourceStack source){
        File savefile = FMLPaths.GAMEDIR.get().resolve(String.format("%s.json", Objects.requireNonNull(source.getPlayer()).getName().getString())).toFile();
        if(!savefile.exists()) {
            source.sendFailure(Component.literal("File doesn't exist"));
            return;
        }
        try {
            Reader reader = new BufferedReader(new FileReader(savefile));
            Gson gson = new Gson();
            playerInfo = gson.fromJson(reader, JsonObject.class);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        source.sendSuccess(Component.literal(String.format("loaded from %s", savefile)), true);
    }

    public void test(CommandSourceStack source){
        source.sendSuccess(Component.literal("testing"), true);
        source.sendSuccess(Component.literal("inventory test"), true);
        JsonObject inventory = new JsonObject();
        PlayerSerializer.savePlayerInventory(source.getPlayer(), inventory);
        PlayerSerializer.loadPlayerInventory(inventory, source.getPlayer());
        source.sendSuccess(Component.literal("inventory test done"), true);
        source.sendSuccess(Component.literal("curios test"), true);
        JsonObject curios = new JsonObject();
        PlayerSerializer.savePlayerCurios(source.getPlayer(), curios);
        PlayerSerializer.loadPlayerCurios(curios, source.getPlayer());
        source.sendSuccess(Component.literal("curios test done"), true);
    }
}
