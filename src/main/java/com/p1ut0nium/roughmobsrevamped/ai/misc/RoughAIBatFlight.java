package com.p1ut0nium.roughmobsrevamped.ai.misc;

import com.p1ut0nium.roughmobsrevamped.entities.EntityHostileBat;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class RoughAIBatFlight extends Goal {

	private final int BAT_OWNER_FOLLOW_Y_OFFSET = 3;
	private final EntityHostileBat hostileBat;
	private Mob boss;
	private BlockPos currentFlightTarget;
	private RandomSource rnd;

	public RoughAIBatFlight(EntityHostileBat hostileBat) {
		this.hostileBat = hostileBat;
		rnd = hostileBat.getRandom();
	}

	@Override
	public boolean canUse() {
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return true;
	}

	@Override
	public void start() {
		this.boss = hostileBat.getBoss();
	}

	// The 1.12.2 updateTask ran every tick and steers by nudging motion, so skipping ticks would halve its speed
	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		updateFlightTarget();
		performFlightMovement();
	}

	private void updateFlightTarget() {
		if (currentFlightTarget != null && !(hostileBat.getTarget() instanceof Player)
				&& (!hostileBat.level().isEmptyBlock(currentFlightTarget) || currentFlightTarget.getY() < 1)) {
			currentFlightTarget = null;
		}

		if (currentFlightTarget == null || rnd.nextInt(30) == 0
				|| currentFlightTarget.distToLowCornerSqr(hostileBat.getX(), hostileBat.getY(), hostileBat.getZ()) < 4.0F) {
			currentFlightTarget = getRandomFlightCoordinates();
		}
	}

	private void performFlightMovement() {
		double diffX, diffY, diffZ;
		LivingEntity target = hostileBat.getTarget();

		if (target == null || !target.isAlive()) {
			if (currentFlightTarget != null) {
				diffX = (double) currentFlightTarget.getX() + 0.5D - hostileBat.getX();
				diffY = (double) currentFlightTarget.getY() + 0.1D - hostileBat.getY();
				diffZ = (double) currentFlightTarget.getZ() + 0.5D - hostileBat.getZ();
			} else {
				diffX = diffY = diffZ = 0D;
			}
		}
		else {
			diffX = target.getX() - hostileBat.getX();
			diffY = target.getY() - hostileBat.getY() + 1.5D;
			diffZ = target.getZ() - hostileBat.getZ();
		}

		Vec3 motion = hostileBat.getDeltaMovement();
		double motionX = motion.x + (Math.signum(diffX) * 0.5D - motion.x) * 0.1D;
		double motionY = motion.y + (Math.signum(diffY) * 0.7D - motion.y) * 0.1D;
		double motionZ = motion.z + (Math.signum(diffZ) * 0.5D - motion.z) * 0.1D;
		hostileBat.setDeltaMovement(motionX, motionY, motionZ);

		float var7 = (float) (Math.atan2(motionZ, motionX) * 180.0D / Math.PI) - 90.0F;
		float var8 = Mth.wrapDegrees(var7 - hostileBat.getYRot());
		hostileBat.setZza(0.5F);
		hostileBat.setYRot(hostileBat.getYRot() + var8);
	}

	private BlockPos getRandomFlightCoordinates() {
		int x = 0;
		int y = 0;
		int z = 0;

		if (boss != null) {
			for (int i = 0; i < 10; i++) {
				BlockPos bossPos = boss.blockPosition();
				x = bossPos.getX() + rnd.nextInt(7) - rnd.nextInt(7);
				y = bossPos.getY() + rnd.nextInt(6) - 2 + BAT_OWNER_FOLLOW_Y_OFFSET;
				z = bossPos.getZ() + rnd.nextInt(7) - rnd.nextInt(7);

				Vec3 orig = hostileBat.position();
				Vec3 dest = new Vec3(x + 0.5D, y + 0.5D, z + 0.5D);
				HitResult result = hostileBat.level().clip(new ClipContext(orig, dest, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, hostileBat));

				// No collision detected, the path is free to traverse
				if (result.getType() == HitResult.Type.MISS) {
					break;
				}
			}
		}

		return new BlockPos(x, y, z);
	}
}
