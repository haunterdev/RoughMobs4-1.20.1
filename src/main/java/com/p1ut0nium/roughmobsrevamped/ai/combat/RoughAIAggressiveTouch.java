package com.p1ut0nium.roughmobsrevamped.ai.combat;

import java.util.EnumSet;
import java.util.List;

import com.p1ut0nium.roughmobsrevamped.misc.FeatureHelper;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

public class RoughAIAggressiveTouch extends Goal {

	protected Mob entity;
	private List<Player> players;

	public RoughAIAggressiveTouch(Mob entity) {
		this.entity = entity;
		this.setFlags(EnumSet.of(Goal.Flag.JUMP));
	}

	/**
	 * Makes a mob hostile toward a player the way the 1.12.2 pigman's revenge plus attack target did.
	 * CHANGED: 1.20.1 neutral mobs (zombified piglins) drop a target that is not their persistent anger target,
	 * so the anger target and its timer are set as well.
	 */
	public static void anger(Mob mob, Player player) {
		mob.setLastHurtByMob(player);
		if (mob instanceof NeutralMob neutral)
		{
			neutral.setPersistentAngerTarget(player.getUUID());
			neutral.startPersistentAngerTimer();
		}
		mob.setTarget(player);
	}

	@Override
	public boolean canUse() {
		// CHANGED: 1.12.2 AxisAlignedBB.expand only grew the box in the positive direction; inflate grows every side.
		// Creative and spectator players are skipped, since a neutral mob angered at one would hold the anger forever.
		return !(players = entity.level().getEntitiesOfClass(Player.class, entity.getBoundingBox().inflate(0.1), EntitySelector.NO_CREATIVE_OR_SPECTATOR)).isEmpty();
	}

	@Override
	public void tick() {
		for (Player player : players)
		{
			anger(entity, player);
			FeatureHelper.playSound(entity, SoundEvents.ZOMBIFIED_PIGLIN_ANGRY);
			break;
		}
	}
}
