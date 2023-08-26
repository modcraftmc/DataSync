package fr.modcraftmc.datasync.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.modcraftmc.datasync.command.arguments.NetworkPlayerArgument;
import fr.modcraftmc.datasync.tpsync.TpaRequest;
import fr.modcraftmc.datasync.tpsync.TpaRequestHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class TpaCommand extends CommandModule{
    @Override
    protected void buildCommand() {
        LiteralArgumentBuilder<CommandSourceStack> tpa_command = Commands.literal("tpa")
                .then(Commands.argument("target", NetworkPlayerArgument.networkPlayer())
                        .executes(context -> {
                            tpa(context.getSource(), NetworkPlayerArgument.getNetworkPlayer(context, "target"));
                            return 1;
                        })
                );
        LiteralArgumentBuilder<CommandSourceStack> tpaaccept_command = Commands.literal("tpaaccept")
                .executes(context -> {
                    tpaccept(context.getSource());
                    return 1;
                });
        LiteralArgumentBuilder<CommandSourceStack> tpadeny_command = Commands.literal("tpadeny")
                .executes(context -> {
                    tpdeny(context.getSource());
                    return 1;
                });

        COMMANDS.add(tpa_command);
        COMMANDS.add(tpaaccept_command);
        COMMANDS.add(tpadeny_command);

        ROOT_COMMANDS.add(tpa_command);
        ROOT_COMMANDS.add(tpaaccept_command);
        ROOT_COMMANDS.add(tpadeny_command);
    }

    private void tpa(CommandSourceStack source, String target) {
        if(!source.isPlayer())
            source.sendFailure(Component.literal("You must be a player to use this command"));
        new TpaRequest(source.getPlayer().getName().getString(), target).fire();
    }

    private void tpaccept(CommandSourceStack source) {
        if(!source.isPlayer())
            source.sendFailure(Component.literal("You must be a player to use this command"));
        TpaRequestHandler.accept(source.getPlayer());
    }

    private void tpdeny(CommandSourceStack source) {
        if(!source.isPlayer())
            source.sendFailure(Component.literal("You must be a player to use this command"));
        TpaRequestHandler.deny(source.getPlayer());
    }
}