package fr.modcraftmc.datasync.command.arguments;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.modcraftmc.datasync.DataSync;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NetworkPlayerArgument implements ArgumentType<String> {
    public NetworkPlayerArgument() {
    }

    public static NetworkPlayerArgument networkPlayer() {
        return new NetworkPlayerArgument();
    }

    public static String getNetworkPlayer(CommandContext<CommandSourceStack> context, String player) {
        return context.getArgument(player, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if(DataSync.playersOnCluster == null)
            return Suggestions.empty();
        return context.getSource() instanceof SharedSuggestionProvider ? SharedSuggestionProvider.suggest(DataSync.playersOnCluster, builder) : Suggestions.empty();
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("Player");
    }
}
