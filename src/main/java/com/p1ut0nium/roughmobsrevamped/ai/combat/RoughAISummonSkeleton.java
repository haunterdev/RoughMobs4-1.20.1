package com.p1ut0nium.roughmobsrevamped.ai.combat;

import java.util.EnumSet;

import com.p1ut0nium.roughmobsrevamped.misc.FeatureHelper;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraftforge.event.ForgeEventFactory;

public class RoughAISummonSkeleton extends Goal {

	protected int timer;
	protected Mob entity;
	protected int delay;
	protected boolean wither;
	private long lastStep = -1;

	public RoughAISummonSkeleton(Mob entity, int delay, boolean wither) {
		this.entity = entity;
		this.delay = delay;
		this.timer = delay;
		this.wither = wither;
		this.setFlags(EnumSet.of(Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		// CHANGED: the 1.12.2 timer stepped once per shouldExecute call, which ran every 3 ticks. 1.20.1 checks every
		// 2, so the timer steps once per 3 elapsed game ticks to keep the configured delay at its 1.12.2 length.
		long now = entity.level().getGameTime();
		if (lastStep < 0)
			lastStep = now;
		int steps = (int) ((now - lastStep) / 3);
		lastStep += steps * 3L;
		this.timer = Math.max(0, this.timer - steps);

		if (this.timer == 0)
		{
			LivingEntity target = entity.getTarget();
			if (target != null && entity.getBoundingBox().inflate(7, 5, 7).intersects(target.getBoundingBox()))
			{
				this.timer = this.delay;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean canContinueToUse() {
		return false;
	}

	@Override
	public void start() {
		if (!(entity.level() instanceof ServerLevel level))
			return;

		AbstractSkeleton skeleton = wither ? EntityType.WITHER_SKELETON.create(level) : EntityType.SKELETON.create(level);
		if (skeleton == null)
			return;

		skeleton.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
		ForgeEventFactory.onFinalizeSpawn(skeleton, level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
		FeatureHelper.spawnParticle(skeleton, ParticleTypes.SMOKE, 5);
		level.addFreshEntity(skeleton);
	}
}
