package com.vampiremod.Sound;

import com.vampiremod.VampireMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, VampireMod.MOD_ID);

    public static final RegistryObject<SoundEvent> VAMPIRE_AMBIENT = registerSoundEvent("entity.vampire.ambient");
    public static final RegistryObject<SoundEvent> VAMPIRE_HURT = registerSoundEvent("entity.vampire.hurt");
    public static final RegistryObject<SoundEvent> VAMPIRE_DEATH = registerSoundEvent("entity.vampire.death");
    public static final RegistryObject<SoundEvent> VAMPIRE_STEP = registerSoundEvent("entity.vampire.step");

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(VampireMod.MOD_ID, name);
        System.out.println("Registering sound: " + location); // DEBUG
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(location));
    }

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
        System.out.println("ModSounds registered!"); // DEBUG
    }
}
