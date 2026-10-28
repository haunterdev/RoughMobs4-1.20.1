package com.p1ut0nium.roughmobsrevamped.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;

/*
 * CHANGED: damage sources are data driven in 1.20.1. The poisonous_fog damage type lives in
 * data/roughmobsredux/damage_type/poisonous_fog.json and bypasses armor through the
 * minecraft:bypasses_armor tag, which is what setDamageBypassesArmor() did.
 */
public class DamageSourceFog {

	public static final ResourceKey<net.minecraft.world.damagesource.DamageType> POISONOUS_FOG =
			ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(Constants.MODID, "poisonous_fog"));

	public static DamageSource poisonousFog(Level level) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(POISONOUS_FOG));
	}
}
