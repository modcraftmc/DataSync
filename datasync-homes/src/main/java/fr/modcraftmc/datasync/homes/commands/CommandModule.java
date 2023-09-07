package fr.modcraftmc.datasync.homes.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

import java.util.ArrayList;
import java.util.List;

public abstract class CommandModule {
    public static final List<LiteralArgumentBuilder<CommandSourceStack>> COMMANDS = new ArrayList<>();
    public static final List<LiteralArgumentBuilder<CommandSourceStack>> ROOT_COMMANDS = new ArrayList<>();

    public CommandModule() {
        buildCommand();
    }

    protected abstract void buildCommand();
}
