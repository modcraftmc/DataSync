package fr.modcraftmc.datasync.inventory.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityMixin {

    @Accessor("persistentData")
    CompoundTag datasync_getPersistentData();

    @Accessor("persistentData")
    void datasync_setPersistentData(CompoundTag persistentData);
}
