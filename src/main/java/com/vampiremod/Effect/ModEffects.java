package com.vampiremod.effect;

import com.vampiremod.VampireMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, VampireMod.MOD_ID);

    public static final RegistryObject<MobEffect> VAMPIRISM_BITE =
            EFFECTS.register("vampirism_bite", VampirismBiteEffect::new);

    public static final RegistryObject<MobEffect> SUNBURN =
            EFFECTS.register("sunburn", SunburnEffect::new);

    public static final RegistryObject<MobEffect> VAMPIRIC_WEAKNESS =
            EFFECTS.register("vampiric_weakness", VampiricWeaknessEffect::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}
