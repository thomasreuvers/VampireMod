package com.vampiremod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public class BloodData implements INBTSerializable<CompoundTag> {
    private int current;
    private int max;
    private boolean drinkable = true;

    public int getCurrent() { return current; }
    public int getMax() { return max; }
    public boolean isDrinkable() { return drinkable; }

    public void setCurrent(int value) { this.current = Math.max(0, Math.min(value, max)); }
    public void setMax(int value) {
        this.max = Math.max(0, value);
        this.current = Math.min(this.current, this.max);
    }
    public void setDrinkable(boolean drinkable) { this.drinkable = drinkable; }

    public int drain(int amount) {
        int drained = Math.min(amount, current);
        current -= drained;
        return drained;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("current", current);
        tag.putInt("max", max);
        tag.putBoolean("drinkable", drinkable);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        current = nbt.getInt("current");
        max = nbt.getInt("max");
        drinkable = nbt.getBoolean("drinkable");
    }
}
