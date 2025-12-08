package com.vampiremod.Capability;

import com.vampiremod.VampireMod;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class PlayerVampireProvider implements ICapabilitySerializable<CompoundTag> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(VampireMod.MOD_ID, "vampire_data");

    private final PlayerVampireData data = new PlayerVampireData();
    private final LazyOptional<PlayerVampireData> optional = LazyOptional.of(() -> data);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == ModCapabilities.VAMPIRE_CAP ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        data.deserializeNBT(nbt);
    }
}
