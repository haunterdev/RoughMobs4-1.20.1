package com.p1ut0nium.roughmobsrevamped.ai.misc;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

public class RoughAIDespawn extends Goal {

	protected LivingEntity entity;
	protected Player player = null;

	public RoughAIDespawn(LivingEntity entity) {
		this.entity = entity;
	}

	@Override
	public boolean canUse() {
		Player playerNew = entity.level().getNearestPlayer(entity, -1.0D);
		if (player == null || playerNew != null)
			player = playerNew;
		return player != null;
	}

	// The 1.12.2 updateTask ran every tick; the 1 in 800 despawn roll is per tick
	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (player != null)
		{
			double d3 = player.distanceToSqr(entity);

			if (d3 > 16384.0D)
			{
				entity.discard();
			}

			if (entity.getNoActionTime() > 600 && entity.level().random.nextInt(800) == 0 && d3 > 1024.0D)
			{
				entity.discard();
			}
			else if (d3 < 1024.0D)
			{
				entity.setNoActionTime(0);
			}
		}
	}
}
