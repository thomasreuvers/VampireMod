package com.vampiremod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.INBTSerializable;

public class PlayerVampireData implements INBTSerializable<CompoundTag> {
    private boolean vampire = false;
    private float blood = 20;

    public boolean isVampire() { return vampire; }
    public void setVampire(boolean v) { vampire = v; }

    public float getBlood() { return blood; }
    public void setBlood(float b) { blood = Math.max(0, Math.min(20, b)); }

    public void copyFrom(PlayerVampireData other) {
        this.vampire = other.vampire;
        this.blood = other.blood;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("vampire", vampire);
        tag.putFloat("blood", blood);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        vampire = nbt.getBoolean("vampire");
        blood = nbt.getFloat("blood");
    }

    public static PlayerVampireData get(Player p) {
        return p.getCapability(ModCapabilities.VAMPIRE_CAP)
                .orElseThrow(() -> new IllegalStateException("Missing vampire capability on player"));
    }
}
