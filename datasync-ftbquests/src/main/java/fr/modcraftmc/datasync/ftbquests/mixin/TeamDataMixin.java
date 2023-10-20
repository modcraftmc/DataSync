package fr.modcraftmc.datasync.ftbquests.mixin;

import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import fr.modcraftmc.datasync.ftbquests.DatasyncFtbQuests;
import fr.modcraftmc.datasync.ftbquests.QuestsSynchronizer;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import org.checkerframework.checker.units.qual.A;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = TeamData.class, remap = false)
public class TeamDataMixin {
    @Shadow Long2LongOpenHashMap completed;

    @Inject(method = "deserializeNBT", at = @At("HEAD"))
    protected void deserializeNBT(SNBTCompoundTag nbt, CallbackInfo ci) {
        this.completed.clear();
    }

    //add mixin to synchronize quests rewards because no event exist for that
    @Inject(method = "claimReward(Ljava/util/UUID;Ldev/ftb/mods/ftbquests/quest/reward/Reward;J)Z", at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbquests/quest/TeamData;save()V"))
    protected void onClaimReward(UUID player, Reward reward, long date, CallbackInfoReturnable<Boolean> cir) {
        DatasyncFtbQuests.questsSynchronizer.syncTeamQuests((TeamData) (Object) this);
    }
}
