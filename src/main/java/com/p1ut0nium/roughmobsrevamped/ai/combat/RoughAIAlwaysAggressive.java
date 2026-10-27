package com.p1ut0nium.roughmobsrevamped.ai.combat;

import java.util.EnumSet;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

public class RoughAIAlwaysAggressive extends Goal {

	private Mob entity;
	private Player targetEntity;
	private int aggressiveRange;
	private boolean isAngry;

	public RoughAIAlwaysAggressive(Mob entity, int aggressiveRange) {
		this.entity = entity;
		this.aggressiveRange = aggressiveRange;
		this.isAngry = true;
		this.setFlags(EnumSet.of(Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		// The 1.12.2 branch kept the attack target only when it already was the closest player, so both arms
		// resolve to the closest player.
		this.targetEntity = entity.level().getNearestPlayer(entity, aggressiveRange);

		if (this.targetEntity == null) {
			return false;
		} else if (this.targetEntity.distanceToSqr(this.entity) > (double)(this.aggressiveRange * this.aggressiveRange)) {
			return false;
		} else if (!entity.hasLineOfSight(targetEntity)) {
			return false;
		} else if (this.targetEntity.isCreative()) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void start() {
		entity.getNavigation().moveTo(this.targetEntity, 1);
		setAngry(true);
	}

	@Override
	public boolean canContinueToUse() {
		return !this.entity.getNavigation().isDone() && this.targetEntity.isAlive() && this.targetEntity.distanceToSqr(this.entity) < (double)(this.aggressiveRange * this.aggressiveRange) && this.isAngry;
	}

	@Override
	public void stop() {
		setAngry(false);
		this.targetEntity = null;
	}

	@Override
	public boolean isInterruptable() {
		return true;
	}

	// CHANGED: 1.12.2 reached EntityPigZombie.becomeAngryAt and angerLevel by reflection; 1.20.1 neutral mobs expose
	// their anger through NeutralMob, so the same two states map onto the persistent anger target and timer.
	private void setAngry(boolean isAngry) {
		if (isAngry) {
			if (targetEntity != null)
				RoughAIAggressiveTouch.anger(entity, targetEntity);
		} else if (entity instanceof NeutralMob neutral) {
			neutral.setRemainingPersistentAngerTime(0);
			neutral.setPersistentAngerTarget(null);
		}
		this.isAngry = isAngry;
	}
}
