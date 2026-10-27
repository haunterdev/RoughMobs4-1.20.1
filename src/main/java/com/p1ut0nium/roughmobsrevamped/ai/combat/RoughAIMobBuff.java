package com.p1ut0nium.roughmobsrevamped.ai.combat;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.p1ut0nium.roughmobsrevamped.misc.FeatureHelper;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;

public class RoughAIMobBuff extends Goal {

	protected LivingEntity entity;
	protected Map<MobEffect, Integer> effects;
	protected float range;
	// RM1: witchBoost picked the buffed mobs (hostile only, or all but players); the original buffed monsters
	protected Predicate<Mob> targets;

	public RoughAIMobBuff(LivingEntity entity, Map<MobEffect, Integer> effects, float range) {
		this(entity, effects, range, mob -> mob instanceof Monster);
	}

	public RoughAIMobBuff(LivingEntity entity, Map<MobEffect, Integer> effects, float range, Predicate<Mob> targets) {
		this.entity = entity;
		this.effects = effects;
		this.range = range;
		this.targets = targets;
		this.setFlags(EnumSet.of(Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		return !getEntitiesInRange().isEmpty() && !effects.isEmpty() && range != 0;
	}

	// The 1.12.2 updateTask ran every tick, so a mob walking into range is buffed on the tick it arrives
	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void tick() {
		Object[] entries = effects.entrySet().toArray();
		RandomSource rnd = entity.level().random;
		Map.Entry<MobEffect, Integer> entry;

		for (Mob mob : getEntitiesInRange())
		{
			if (mob == entity || isEffected(mob))
				continue;

			entry = (Map.Entry<MobEffect, Integer>) entries[rnd.nextInt(entries.length)];
			FeatureHelper.addEffect(mob, entry.getKey(), 600, entry.getValue()-1);
			FeatureHelper.spawnParticle(mob, ParticleTypes.WITCH, 10);
		}
	}

	private boolean isEffected(LivingEntity mob) {
		for (MobEffect potion : effects.keySet())
		{
			if (mob.hasEffect(potion))
				return true;
		}
		return false;
	}

	protected List<Mob> getEntitiesInRange() {
		// CHANGED: 1.12.2 AxisAlignedBB.expand only grew the box in the positive direction; inflate grows every side
		return entity.level().getEntitiesOfClass(Mob.class, entity.getBoundingBox().inflate(range), targets);
	}
}
