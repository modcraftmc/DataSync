package fr.modcraftmc.datasync.waystones.mixin;

import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.datasync.waystones.DatasyncWaystones;
import fr.modcraftmc.datasync.waystones.message.DeleteWaystone;
import fr.modcraftmc.datasync.waystones.message.UpdateWaystone;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.core.WaystoneManager;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WaystoneManager.class, remap = false)
public class WaystoneManagerMixin {
    @Inject(method = "addWaystone", at = @At("HEAD"))
    protected void addWaystoneServer(IWaystone waystone, CallbackInfo ci) {
        //DatasyncWaystones.waystoneManager.setWaystoneServer(waystone, CrossServerCoreAPI.instance.getServerName());
    }

    @Inject(method = "updateWaystone", at = @At("HEAD"))
    protected void updateWaystoneServer(IWaystone waystone, CallbackInfo ci) {
        CrossServerCoreAPI.sendCrossMessageToAllOtherServer(new UpdateWaystone(waystone));
    }

    @Inject(method = "removeWaystone", at = @At("HEAD"))
    protected void removeWaystoneServer(IWaystone waystone, CallbackInfo ci) {
        if(WaystoneManager.get(ServerLifecycleHooks.getCurrentServer()).getWaystoneById(waystone.getWaystoneUid()).isPresent())
            CrossServerCoreAPI.sendCrossMessageToAllOtherServer(new DeleteWaystone(waystone));
        DatasyncWaystones.waystoneManager.dropWaystoneServer(waystone);
    }
}
