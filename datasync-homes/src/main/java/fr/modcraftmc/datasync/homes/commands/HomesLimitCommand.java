package fr.modcraftmc.datasync.homes.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import fr.modcraftmc.crossservercore.api.arguments.NetworkPlayerArgument;
import fr.modcraftmc.datasync.homes.DatasyncHomes;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class HomesLimitCommand extends CommandModule {

    @Override
    protected void buildCommand() {
        COMMANDS.add(Commands.literal("homeslimit")
                .requires(source -> source.hasPermission(4))
                .executes(context -> showHomeLimit(context.getSource()))
                .then(Commands.argument("limit", IntegerArgumentType.integer(0))
                        .executes(context -> setHomeLimit(context.getSource(), IntegerArgumentType.getInteger(context, "limit"))))
                .then(Commands.argument("player", NetworkPlayerArgument.networkPlayer())
                        .executes(context -> showHomeLimit(context.getSource(), NetworkPlayerArgument.getNetworkPlayer(context, "player")))
                        .then(Commands.argument("limit", IntegerArgumentType.integer(0))
                                .executes(context -> setHomeLimit(context.getSource(), NetworkPlayerArgument.getNetworkPlayer(context, "player"), IntegerArgumentType.getInteger(context, "limit"))))
                        .then(Commands.literal("default")
                                .executes(context -> unsetHomeLimit(context.getSource(), NetworkPlayerArgument.getNetworkPlayer(context, "player")))))
        );
    }

    private int setHomeLimit(CommandSourceStack source, String player, int limit) {
        DatasyncHomes.homeManager.setPlayerHomesLimit(player, limit);
        source.sendSuccess(Component.literal("The new homes limit for " + player + " is : " + limit).withStyle(style -> style.withColor(ChatFormatting.GOLD)), true);
        return 1;
    }

    private int unsetHomeLimit(CommandSourceStack source, String player) {
        DatasyncHomes.homeManager.unsetPlayerHomesLimit(player);
        source.sendSuccess(Component.literal("The homes limit for " + player + " has been unset").withStyle(style -> style.withColor(ChatFormatting.GOLD)), true);
        return 1;
    }

    private int setHomeLimit(CommandSourceStack source, int limit) {
        DatasyncHomes.homeManager.setGlobalHomesLimit(limit);
        DatasyncHomes.homeManager.propagateGlobalHomesLimit();
        source.sendSuccess(Component.literal("The new global homes limit is : " + limit).withStyle(style -> style.withColor(ChatFormatting.GOLD)), true);
        return 1;
    }

    private int showHomeLimit(CommandSourceStack source){
        source.sendSuccess(Component.literal("The global homes limit is : " + DatasyncHomes.homeManager.getGlobalHomesLimit()).withStyle(style -> style.withColor(ChatFormatting.GOLD)), true);
        return 1;
    }

    private int showHomeLimit(CommandSourceStack source, String player){
        source.sendSuccess(Component.literal("The homes limit for " + player + " is : " + DatasyncHomes.homeManager.getPlayerHomesLimit(player)).withStyle(style -> style.withColor(ChatFormatting.GOLD)), true);
        return 1;
    }


}
