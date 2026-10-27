package com.p1ut0nium.roughmobsrevamped.ai.misc;

import java.util.EnumSet;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;

public class RoughAIBurnExplosion extends Goal {

	private final Creeper creeper;

	public RoughAIBurnExplosion(Creeper creeper) {
		this.creeper = creeper;
		this.setFlags(EnumSet.of(Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		return creeper.isAlive() && creeper.isOnFire();
	}

	// Vanilla SwellGoal ticks every tick and sets the swell back to -1 with no target, so this has to as well
	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		creeper.setSwellDir(1);
	}
}
