package com.p1ut0nium.roughmobsrevamped.ai.misc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/*
 * CHANGED: new in Rough Mobs Redux. An invisible creeper gives off a trail of happy villager particles (creeper
 * InvisibleParticles), so a sharp-eyed player can still spot it. No flags, so it runs beside every other goal.
 */
public class RoughAIInvisibleParticles extends Goal {

	private final Mob entity;
	private final int count;

	public RoughAIInvisibleParticles(Mob entity, int count) {
		this.entity = entity;
		this.count = count;
	}

	@Override
	public boolean canUse() {
		return entity.isInvisible() && entity.level() instanceof ServerLevel;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		((ServerLevel) entity.level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY(0.5D), entity.getZ(), count,
				entity.getBbWidth() * 0.4D, entity.getBbHeight() * 0.3D, entity.getBbWidth() * 0.4D, 0.1D);
	}
}
