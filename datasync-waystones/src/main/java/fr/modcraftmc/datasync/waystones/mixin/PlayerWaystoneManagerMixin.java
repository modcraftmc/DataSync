package fr.modcraftmc.datasync.waystones.mixin;

import com.mojang.datafixers.util.Either;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.CrossServerCoreProxyExtensionAPI;
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
        DatasyncWaystones.LOGGER.info("waystoneServerMap: ");
        DatasyncWaystones.waystoneManager.waystoneServerMap.forEach((iWaystone, s) -> DatasyncWaystones.LOGGER.info(iWaystone.getWaystoneUid() + "(" + iWaystone.getName() + ")" + " -> " + s));
        if (!DatasyncWaystones.waystoneManager.isWaystoneOnCurrentServer(waystone)) {
            DatasyncWaystones.LOGGER.info("Waystone is not on current server");
            if (entity instanceof ServerPlayer serverPlayer) {
                DatasyncWaystones.LOGGER.info(String.format("Teleporting %s to waystone %s on server %s", serverPlayer.getGameProfile().getName(), waystone.getName(), DatasyncWaystones.waystoneManager.getWaystoneServer(waystone)));
                CrossServerCoreAPI.instance.sendCrossMessageToAllOtherServer(new TeleportToWaystone(serverPlayer.getGameProfile().getName(), waystone.getWaystoneUid()));
                CrossServerCoreProxyExtensionAPI.instance.transferPlayer(serverPlayer.getGameProfile().getName(), DatasyncWaystones.waystoneManager.getWaystoneServer(waystone));
            }
            cir.setReturnValue(null);
        }
    }
}
