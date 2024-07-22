package fr.modcraftmc.datasync.waystones.mixin;

import com.mojang.datafixers.util.Either;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.CrossServerCoreProxyExtensionAPI;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncServer;
import fr.modcraftmc.datasync.waystones.DatasyncWaystones;
import fr.modcraftmc.datasync.waystones.message.TeleportToWaystone;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.api.WaystoneTeleportError;
import net.blay09.mods.waystones.core.PlayerWaystoneManager;
import net.blay09.mods.waystones.core.WarpMode;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = PlayerWaystoneManager.class, remap = false)
public class PlayerWaystoneManagerMixin {

    @Inject(method = "tryTeleportToWaystone", at = @At("HEAD"), cancellable = true)
    private static void checkServer(Entity entity, IWaystone waystone, WarpMode warpMode, @Nullable IWaystone fromWaystone, CallbackInfoReturnable<Either<List<Entity>, WaystoneTeleportError>> cir) {
        if (!DatasyncWaystones.waystoneManager.isWaystoneOnCurrentServer(waystone)) {
            if (entity instanceof ServerPlayer serverPlayer) {
                CrossServerCoreAPI.getPlayer(entity.getUUID()).ifPresent(syncPlayer -> {
                    ISyncServer syncServer = DatasyncWaystones.waystoneManager.getWaystoneServer(waystone);
                    syncServer.sendMessage(new TeleportToWaystone(syncPlayer, waystone.getWaystoneUid()));
                    CrossServerCoreProxyExtensionAPI.transferPlayer(syncPlayer, syncServer);
                });
            }
            cir.setReturnValue(null);
        }
    }
}
