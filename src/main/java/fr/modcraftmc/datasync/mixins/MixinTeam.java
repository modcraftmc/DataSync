package fr.modcraftmc.datasync.mixins;

import dev.ftb.mods.ftbteams.data.Team;
import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.ftbsync.FTBSync;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = dev.ftb.mods.ftbteams.data.Team.class, remap = false)
public class MixinTeam {
    @Inject(at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbteams/data/TeamManager;syncTeamsToAll([Ldev/ftb/mods/ftbteams/data/Team;)V") , method = "denyInvite")
    protected void onDenyInvite(CommandSourceStack source, CallbackInfoReturnable<Integer> cir){
        DataSync.LOGGER.info("received Team denyInvite");
        FTBSync.syncTeam((dev.ftb.mods.ftbteams.data.Team) (Object) this);
    }

    @Inject(at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbteams/data/Team;getOnlineMembers()Ljava/util/List;") , method = "sendMessage")
    protected void onSendMessage(UUID from, Component text, CallbackInfo ci){
        DataSync.LOGGER.info("received Team sendMessage");
        Team team = (Team) (Object) this;
        FTBSync.syncTeamMessage(team.getId(), from, text);
    }
}
