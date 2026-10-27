package com.p1ut0nium.roughmobsrevamped.ai.combat;

import java.util.EnumSet;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

public class RoughAIInvisibleTarget extends Goal {

	protected Mob entity;
	protected double range;

	public RoughAIInvisibleTarget(Mob entity, double range) {
		this.entity = entity;
		this.range = range;
		this.setFlags(EnumSet.of(Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		return entity.getTarget() != null && entity.distanceTo(entity.getTarget()) < range && !entity.hasEffect(MobEffects.INVISIBILITY);
	}

	@Override
	public void start() {
		entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false));
	}
}
