package com.p1ut0nium.roughmobsrevamped.ai.misc;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;

public class RoughAIAddEffect extends Goal {

	private final Mob entity;
	private final MobEffect effect;
	private final double range;

	public RoughAIAddEffect(Mob entity, MobEffect effect, double range) {
		this.entity = entity;
		this.effect = effect;
		this.range = range;
	}

	@Override
	public boolean canUse() {
		return !getEntities().isEmpty();
	}

	// The 1.12.2 updateTask ran every tick
	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		for (LivingEntity living : getEntities())
		{
			living.addEffect(new MobEffectInstance(effect, 200, 1));
		}
	}

	private List<LivingEntity> getEntities() {
		List<LivingEntity> allLiving = entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(range));
		List<LivingEntity> friendlyLiving = new ArrayList<>();

		for (LivingEntity living : allLiving)
			if (!(living instanceof Monster))
				friendlyLiving.add(living);

		return friendlyLiving;
	}
}
