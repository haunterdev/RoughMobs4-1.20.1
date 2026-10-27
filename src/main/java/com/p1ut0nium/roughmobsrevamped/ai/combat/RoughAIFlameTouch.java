package com.p1ut0nium.roughmobsrevamped.ai.combat;

import java.util.EnumSet;
import java.util.List;

import com.p1ut0nium.roughmobsrevamped.misc.FeatureHelper;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

public class RoughAIFlameTouch extends Goal {

	protected Mob entity;
	private List<Player> players;
	// CHANGED: 1.12.2 hard-coded 8 seconds; blaze FlameTouchSeconds
	private final int seconds;

	public RoughAIFlameTouch(Mob entity, int seconds) {
		this.entity = entity;
		this.seconds = seconds;
		this.setFlags(EnumSet.of(Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		// CHANGED: 1.12.2 AxisAlignedBB.expand only grew the box in the positive direction; inflate grows every side
		return !(players = entity.level().getEntitiesOfClass(Player.class, entity.getBoundingBox().inflate(0.1))).isEmpty();
	}

	@Override
	public void tick() {
		for (Player player : players)
		{
			player.setSecondsOnFire(seconds);
			FeatureHelper.playSound(entity, SoundEvents.BLAZE_BURN);
			break;
		}
	}
}
