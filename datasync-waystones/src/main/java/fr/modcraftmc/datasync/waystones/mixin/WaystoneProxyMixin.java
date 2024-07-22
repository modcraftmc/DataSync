package fr.modcraftmc.datasync.waystones.mixin;

import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncServer;
import fr.modcraftmc.datasync.waystones.DatasyncWaystones;
import net.blay09.mods.waystones.core.WaystoneProxy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WaystoneProxy.class, remap = false)
public class WaystoneProxyMixin {
    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
    protected void isValid(CallbackInfoReturnable<Boolean> cir) {
        ISyncServer waystoneServer = DatasyncWaystones.waystoneManager.getWaystoneServer((WaystoneProxy) (Object) this);
        if(waystoneServer != null && !waystoneServer.equals(CrossServerCoreAPI.getServer())) { // only override if the waystone is on another server
            cir.setReturnValue(true);
        }
    }
}
