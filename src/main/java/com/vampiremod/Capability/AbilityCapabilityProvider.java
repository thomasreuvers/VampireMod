package com.vampiremod.capability;

import com.vampiremod.ability.AbilityInstance;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class AbilityCapabilityProvider implements ICapabilitySerializable {
    public static final Capability<IAbilityCapability> ABILITY_CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>(){});

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
    public void deserializeNBT(Tag nbt) {
        instance.deserializeNBT(nbt);
    }

    public void invalidate() {
        holder.invalidate();
    }
}
