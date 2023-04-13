package fr.modcraftmc.datasync.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

import java.util.ArrayList;
import java.util.List;

public abstract class CommandModule {
    public static final List<LiteralArgumentBuilder<CommandSourceStack>> COMMANDS = new ArrayList<>();
    protected LiteralArgumentBuilder<CommandSourceStack> commandTree;

    public CommandModule() {
        buildCommand();
        COMMANDS.add(commandTree);
    }

    protected abstract void buildCommand();
}
