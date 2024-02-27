package fr.modcraftmc.datasync.ftbteams.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.PartyTeam;
import dev.ftb.mods.ftbteams.data.TeamRank;
import dev.ftb.mods.ftbteams.net.PlayerGUIOperationMessage;
import fr.modcraftmc.datasync.ftbteams.DatasyncFtbTeam;
import fr.modcraftmc.datasync.ftbteams.TeamsSynchronizer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(value = PlayerGUIOperationMessage.class, remap = false)
public class MixinPlayerGUIOperationMessage
{
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getPlayerList()Lnet/minecraft/server/players/PlayerList;", ordinal = 1) , method = "processTarget", remap = true)
    protected void onProcessInvite(ServerPlayer sourcePlayer, TeamRank senderRank, PartyTeam partyTeam, UUID targetId, CallbackInfo ci){
        DatasyncFtbTeam.LOGGER.debug("FTBTeams GUI invite processing");
        String targetName = FTBTeamsAPI.getManager().getInternalPlayerTeam(targetId).playerName;
        if(ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(targetId) == null && targetName != null){
            try {
                partyTeam.invite(sourcePlayer, List.of(new GameProfile(targetId, targetName)));
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
