package com.p1ut0nium.roughmobsrevamped.ai.combat;

import java.util.EnumSet;

import com.p1ut0nium.roughmobsrevamped.entities.BossZombie;
import com.p1ut0nium.roughmobsrevamped.entities.EntityHostileBat;
import com.p1ut0nium.roughmobsrevamped.misc.BossHelper;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

public class RoughAIBatTarget extends TargetGoal {

	private EntityHostileBat hostileBat;
	private LivingEntity theTarget;
	private BossZombie boss;

	public RoughAIBatTarget(EntityHostileBat bat) {
		super(bat, false);
		this.hostileBat = bat;
		this.setFlags(EnumSet.of(Goal.Flag.TARGET));
	}

	private Player nearestPlayer(Entity center) {
		return hostileBat.level().getNearestPlayer(center.getX(), center.getY(), center.getZ(), BossHelper.bossBatSwarmAttackRange, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
	}

	@Override
	public boolean canUse() {
		// CHANGED: 1.12.2 read the boss only in startExecuting, so the first check with no player in range cast a null
		// boss and crashed. The boss is read here, and a bat whose boss is gone (dead, or unloaded with the chunk,
		// since the link is not saved) leaves the world instead of idling forever.
		BossZombie owner = hostileBat.getBoss();
		if (owner == null || !owner.isAlive()) {
			hostileBat.discard();
			return false;
		}

		theTarget = nearestPlayer(owner);

		if (theTarget == null) {
			owner.removeBatMinion(this.hostileBat);
			return false;
		}

		return this.canAttack(theTarget, TargetingConditions.forCombat().ignoreLineOfSight());
	}

	@Override
	public boolean canContinueToUse() {
		if (this.boss != null)
			theTarget = nearestPlayer(this.boss);

		return theTarget != null && theTarget.isAlive();
	}

	@Override
	public void stop() {
		super.stop();
		this.theTarget = null;
		this.hostileBat.setTarget(null);
	}

	@Override
	public void start() {
		this.boss = hostileBat.getBoss();
		this.hostileBat.setTarget(theTarget);
		super.start();
	}
}
