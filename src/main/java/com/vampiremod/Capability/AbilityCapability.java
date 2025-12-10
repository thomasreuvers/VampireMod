package com.vampiremod.capability;

import com.vampiremod.ability.Ability;
import com.vampiremod.ability.AbilityInstance;
import com.vampiremod.ability.AbilityRegistry;
import com.vampiremod.ability.network.AbilitySyncPacket;
import com.vampiremod.ability.network.NetworkHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AbilityCapability implements IAbilityCapability {
    private final Map<ResourceLocation, AbilityInstance> abilities = new HashMap<>();
    private int abilityPoints = 0;
    private boolean batForm;
    @Nullable
    private ResourceLocation activeAbility;

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
    public boolean isBatForm() {
        return batForm;
    }

    @Override
    public void setBatForm(Player player, boolean batForm) {
        if (this.batForm == batForm) {
            return;
        }

        this.batForm = batForm;
        if (player != null) {
            if (!player.level().isClientSide) {
                player.refreshDimensions();
            }
            sync(player);
        }
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
    public @Nullable ResourceLocation getActiveAbility() {
        return activeAbility;
    }

    @Override
    public void setActiveAbility(@Nullable ResourceLocation id) {
        this.activeAbility = id;
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
        tag.putBoolean("batForm", batForm);
        if (activeAbility != null) {
            tag.putString("activeAbility", activeAbility.toString());
        }

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
        batForm = nbt.getBoolean("batForm");
        if (nbt.contains("activeAbility")) {
            activeAbility = new ResourceLocation(nbt.getString("activeAbility"));
        } else {
            activeAbility = null;
        }

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

    private void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHandler.sendToTrackingAndSelf(serverPlayer,
                    new AbilitySyncPacket(serverPlayer.getId(), serializeNBT()));
        }
    }
}
