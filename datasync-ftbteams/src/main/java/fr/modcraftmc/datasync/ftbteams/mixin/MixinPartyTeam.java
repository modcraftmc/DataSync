package fr.modcraftmc.datasync.ftbteams.mixin;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbteams.data.PartyTeam;
import fr.modcraftmc.datasync.ftbteams.DatasyncFtbTeam;
import fr.modcraftmc.datasync.ftbteams.TeamsSynchronizer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(value = dev.ftb.mods.ftbteams.data.PartyTeam.class, remap = false)
public class MixinPartyTeam {
    @Inject(at = @At(value = "INVOKE_ASSIGN", target = "Ldev/ftb/mods/ftbteams/data/FTBTUtils;getPlayerByUUID(Lnet/minecraft/server/MinecraftServer;Ljava/util/UUID;)Lnet/minecraft/server/level/ServerPlayer;") , method = "invite")
    protected void onInvite(ServerPlayer from, Collection<GameProfile> players, CallbackInfoReturnable<Integer> cir){
        DatasyncFtbTeam.LOGGER.debug("FTBTeams PartyTeam inviting");
        PartyTeam team = (PartyTeam) (Object) this;
        DatasyncFtbTeam.teamsSynchronizer.syncTeam(team);
        for (GameProfile player : players) {
            TeamsSynchronizer.sendInvitationMessage(player.getId(), team, from);
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbteams/data/TeamManager;syncTeamsToAll([Ldev/ftb/mods/ftbteams/data/Team;)V") , method = "promote")
    protected void onPromote(ServerPlayer from, Collection<GameProfile> players, CallbackInfoReturnable<Integer> cir){
        DatasyncFtbTeam.LOGGER.debug("FTBTeams PartyTeam promoting");
        DatasyncFtbTeam.teamsSynchronizer.syncTeam((dev.ftb.mods.ftbteams.data.PartyTeam) (Object) this);
    }

    @Inject(at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbteams/data/TeamManager;syncTeamsToAll([Ldev/ftb/mods/ftbteams/data/Team;)V") , method = "demote")
    protected void onDemote(ServerPlayer from, Collection<GameProfile> players, CallbackInfoReturnable<Integer> cir){
        DatasyncFtbTeam.LOGGER.debug("FTBTeams PartyTeam demoting");
        DatasyncFtbTeam.teamsSynchronizer.syncTeam((dev.ftb.mods.ftbteams.data.PartyTeam) (Object) this);
    }
}
