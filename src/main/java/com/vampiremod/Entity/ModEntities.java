package com.vampiremod.entity;

import com.vampiremod.VampireMod;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, VampireMod.MOD_ID);

    public static final RegistryObject<EntityType<VampireEntity>> VAMPIRE =
            ENTITIES.register("vampire",
                    () -> EntityType.Builder.<VampireEntity>of(VampireEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .build("vampire")
            );

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
