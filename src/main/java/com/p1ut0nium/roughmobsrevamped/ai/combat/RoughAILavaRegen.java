package com.p1ut0nium.roughmobsrevamped.ai.combat;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

public class RoughAILavaRegen extends Goal {

	protected Mob entity;

	public RoughAILavaRegen(Mob entity) {
		this.entity = entity;
	}

	@Override
	public boolean canUse() {
		return entity.isInLava();
	}

	@Override
	public void tick() {
		entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1, false, false));
	}
}
