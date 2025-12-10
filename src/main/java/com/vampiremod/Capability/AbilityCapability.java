package com.vampiremod.capability;

import com.vampiremod.ability.Ability;
import com.vampiremod.ability.AbilityInstance;
import com.vampiremod.ability.AbilityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AbilityCapability implements IAbilityCapability {
    private final Map<ResourceLocation, AbilityInstance> abilities = new HashMap<>();
    private int abilityPoints = 0;

    public AbilityCapability() {
        // Ensure instances exist for every registered ability so unlock state persists cleanly.
        ensureAbilitiesRegistered();
    }

    @Override
    public Collection<AbilityInstance> getAbilities() {
        return abilities.values();
    }

    @Override
    public AbilityInstance getAbility(ResourceLocation id) {
        return abilities.get(id);
    }

    @Override
    public void addAbility(AbilityInstance instance) {
        abilities.put(instance.getAbility().getId(), instance);
    }

    @Override
    public void ensureAbilitiesRegistered() {
        AbilityRegistry.getAllAbilities().forEach(ability -> {
            abilities.computeIfAbsent(ability.getId(), id -> new AbilityInstance(ability));
        });
    }

    @Override
    public int getAbilityPoints() {
        return abilityPoints;
    }

    @Override
    public void setAbilityPoints(int points) {
        this.abilityPoints = points;
    }

    @Override
    public boolean spendPoints(int amount) {
        if (abilityPoints >= amount) {
            abilityPoints -= amount;
            return true;
        }
        return false;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("points", abilityPoints);

        CompoundTag abilitiesTag = new CompoundTag();
        abilities.forEach((id, instance) -> {
            abilitiesTag.put(id.toString(), instance.save());
        });
        tag.put("abilities", abilitiesTag);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        abilityPoints = nbt.getInt("points");

        CompoundTag abilitiesTag = nbt.getCompound("abilities");
        for (String key : abilitiesTag.getAllKeys()) {
            CompoundTag abilityTag = abilitiesTag.getCompound(key);
            ResourceLocation id = new ResourceLocation(abilityTag.getString("ability"));

            Ability ability = AbilityRegistry.getAbility(id);
            if (ability != null) {
                AbilityInstance instance = new AbilityInstance(ability);
                instance.load(abilityTag);
                abilities.put(id, instance);
            }
        }

        // Add any new abilities introduced after this save
        ensureAbilitiesRegistered();
    }
}
