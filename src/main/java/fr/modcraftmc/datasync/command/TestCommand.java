package fr.modcraftmc.datasync.command;

import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.modcraftmc.datasync.serialization.PlayerInventorySerializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

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
                    }));
    }

    private void saveInventory(CommandSourceStack source){
        inventory = PlayerInventorySerializer.serializeInventory(source.getPlayer().getInventory());
        source.sendSuccess(Component.literal("Inventory saved"), true);
    }

    private void clearInventory(CommandSourceStack source){
        source.getPlayer().getInventory().clearContent();
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
}
