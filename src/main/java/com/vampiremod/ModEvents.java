package com.vampiremod;

import com.vampiremod.entity.ModEntities;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;

public class ModEvents {
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.VAMPIRE.get(),
                Vindicator.createAttributes()
                        .add(Attributes.MAX_HEALTH, 120.0D)
                        .build());
    }
}
