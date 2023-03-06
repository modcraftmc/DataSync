package fr.modcraftmc.datasync.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.modcraftmc.datasync.serialization.PlayerInventorySerializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosCapability;

public class TestCommand {
    private JsonObject inventory;

    private LiteralArgumentBuilder<CommandSourceStack> commandTree;

    public TestCommand(DataSyncCommand dataSyncCommand) {
        buildCommand();
        dataSyncCommand.registerCommand(commandTree);
    }

    public void buildCommand(){
        commandTree = Commands.literal("test")
                .then(Commands.literal("saveclearload")
                    .executes(context -> {
                        saveInventory(context.getSource());
                        clearInventory(context.getSource());
                        loadInventory(context.getSource());
                        return 1;
                    }))
                .then(Commands.literal("save")
                    .executes(context -> {
                        saveInventory(context.getSource());
                        return 1;
                    }))
                .then(Commands.literal("clear")
                    .executes(context -> {
                        clearInventory(context.getSource());
                        return 1;
                    }))
                .then(Commands.literal("load")
                    .executes(context -> {
                        loadInventory(context.getSource());
                        return 1;
                    }))
                .then(Commands.literal("showjson")
                    .executes(context -> {
                        showJson(context.getSource());
                        return 1;
                    }));
    }

    private void saveInventory(CommandSourceStack source){
        inventory = PlayerInventorySerializer.serializeInventory(source.getPlayer().getInventory());
        source.sendSuccess(Component.literal("Inventory saved"), true);
    }

    private void clearInventory(CommandSourceStack source){
        source.getPlayer().getInventory().clearContent();

        if(ModList.get().isLoaded("curios")){
            source.getPlayer().getCapability(CuriosCapability.INVENTORY).ifPresent((itemHandler) -> {
                itemHandler.saveInventory(true);
            });
        }

        source.sendSuccess(Component.literal("Inventory cleared"), true);
    }

    private void loadInventory(CommandSourceStack source){
        if(inventory != null) {
            PlayerInventorySerializer.deserializeInventory(inventory, source.getPlayer().getInventory());
            source.sendSuccess(Component.literal("Inventory loaded"), true);
        }
        else {
            source.sendFailure(Component.literal("Inventory is null"));
        }
    }

    public void showJson(CommandSourceStack source){
        String json;
        JsonObject jsonObject = PlayerInventorySerializer.serializeInventory(source.getPlayer().getInventory());
        Gson gson = new GsonBuilder()
                        .setPrettyPrinting()
                        .create();
        json = gson.toJson(jsonObject);

        source.sendSuccess(Component.literal(json), true);
    }
}
