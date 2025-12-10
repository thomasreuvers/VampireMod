package com.vampiremod.capability;

import com.vampiremod.VampireMod;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AbilityCapabilityProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<IAbilityCapability> ABILITY_CAPABILITY = ModCapabilities.ABILITY_CAP;
    public static final ResourceLocation ID = new ResourceLocation(VampireMod.MOD_ID, "abilities");

    private final IAbilityCapability instance = new AbilityCapability();
    private final LazyOptional<IAbilityCapability> holder = LazyOptional.of(() -> instance);

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return ABILITY_CAPABILITY.orEmpty(cap, holder);
    }

    @Override
    public CompoundTag serializeNBT() {
        return instance.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        instance.deserializeNBT(nbt);
    }

    public void invalidate() {
        holder.invalidate();
    }
}
