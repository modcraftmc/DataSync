package fr.modcraftmc.datasync.mixins;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.PartyTeam;
import dev.ftb.mods.ftbteams.data.TeamRank;
import dev.ftb.mods.ftbteams.net.PlayerGUIOperationMessage;
import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.networkidentity.SyncServer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mixin(value = PlayerGUIOperationMessage.class, remap = false)
public class MixinPlayerGUIOperationMessage
{
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;m_6846_()Lnet/minecraft/server/players/PlayerList;", ordinal = 1) , method = "processTarget")
    protected void onProcessInvite(ServerPlayer sourcePlayer, TeamRank senderRank, PartyTeam partyTeam, UUID targetId, CallbackInfo ci){
        DataSync.LOGGER.debug("process invite");
        String playerInvited = FTBTeamsAPI.getManager().getInternalPlayerTeam(targetId).playerName;
        Optional<SyncServer> playerLocation = DataSync.playersLocation.getPlayerLocation(playerInvited);
        if(playerLocation.isPresent() && !playerLocation.get().getName().equals(DataSync.serverName)){
            Optional<GameProfile> gameProfile = ServerLifecycleHooks.getCurrentServer().getProfileCache().get(targetId);
            if(gameProfile.isPresent()) {
                try {
                    partyTeam.invite(sourcePlayer, List.of(gameProfile.get()));
                } catch (CommandSyntaxException e) {
                    sourcePlayer.displayClientMessage(Component.literal(e.getMessage()).withStyle(ChatFormatting.RED), false);
                }
            }
            else
                DataSync.LOGGER.error("Could not find GameProfile for player " + playerInvited);
        }

    }
}
