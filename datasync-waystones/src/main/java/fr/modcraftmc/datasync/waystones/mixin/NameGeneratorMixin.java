package fr.modcraftmc.datasync.waystones.mixin;

import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.worldgen.namegen.NameGenerationMode;
import net.blay09.mods.waystones.worldgen.namegen.NameGenerator;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NameGenerator.class, remap = false)
public class NameGeneratorMixin {

    @Inject(method = "getName", at = @At(value = "RETURN"), cancellable = true)
    public void addServerName(IWaystone waystone, RandomSource rand, NameGenerationMode nameGenerationMode, CallbackInfoReturnable<String> cir) {
        String serverName = CrossServerCoreAPI.instance.getServerName();
        cir.setReturnValue(serverName + ": " + cir.getReturnValue());
    }
}
