package com.vampiremod;

import com.vampiremod.Entity.ModEntities;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;

public class ModEvents {
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.VAMPIRE.get(), Vindicator.createAttributes().build());
    }
}
