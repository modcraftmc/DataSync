package fr.modcraftmc.datasync.waystones.mixin;

import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.datasync.waystones.message.DeleteWaystone;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.block.WaystoneBlockBase;
import net.blay09.mods.waystones.block.entity.WaystoneBlockEntityBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WaystoneBlockBase.class, remap = false)
public class WaystoneBlockBaseMixin {
    @Inject(at = @At(value = "INVOKE", target = "Lnet/blay09/mods/waystones/block/entity/WaystoneBlockEntityBase;getWaystone()Lnet/blay09/mods/waystones/api/IWaystone;"), method = "onRemove")
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving, CallbackInfo ci) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        IWaystone waystone = ((WaystoneBlockEntityBase) (Object) blockEntity).getWaystone();
        CrossServerCoreAPI.instance.sendCrossMessageToAllOtherServer(new DeleteWaystone(waystone));
    }
}
