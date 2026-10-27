package com.p1ut0nium.roughmobsrevamped.ai.combat;

import com.p1ut0nium.roughmobsrevamped.entities.EntityHostileBat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class RoughAIBatAttack extends Goal {

	private final EntityHostileBat hostileBat;
	private Entity entityTarget;
	private int attackTick;

	public RoughAIBatAttack(EntityHostileBat bat) {
		hostileBat = bat;
		attackTick = 0;
	}

	@Override
	public boolean canUse() {
		if (hostileBat.getTarget() != null && hostileBat.getTarget().isAlive()) {
			entityTarget = hostileBat.getTarget();
			return true;
		}
		return false;
	}

	@Override
	public boolean canContinueToUse() {
		return entityTarget != null && entityTarget.isAlive() || canUse();
	}

	@Override
	public void stop() {
		entityTarget = null;
		attackTick = 0;
	}

	// The 1.12.2 updateTask ran every tick; the 20-tick attack cooldown counts ticks
	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		hostileBat.getLookControl().setLookAt(entityTarget, 30.0F, 30.0F);
		attackTick = Math.max(attackTick - 1, 0);
		double maxReach = hostileBat.getBbWidth() * hostileBat.getBbWidth() * 5.0D;

		if (hostileBat.distanceToSqr(entityTarget.getX(), entityTarget.getBoundingBox().maxY, entityTarget.getZ()) <= maxReach
				|| hostileBat.getBoundingBox().intersects(entityTarget.getBoundingBox())) {
			if (attackTick == 0) {
				attackTick = 20;
				hostileBat.doHurtTarget(entityTarget);

				// CHANGED: 1.12.2 called knockBack on the bat with strength 0, which only halved its horizontal
				// motion (and vertical when grounded); 1.20.1 knockback returns early at strength 0, so the halving is done here.
				Vec3 motion = hostileBat.getDeltaMovement();
				hostileBat.setDeltaMovement(motion.x / 2.0D, hostileBat.onGround() ? motion.y / 2.0D : motion.y, motion.z / 2.0D);
				hostileBat.hasImpulse = true;
			}
		}
	}
}
