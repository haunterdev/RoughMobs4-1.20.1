package com.p1ut0nium.roughmobsrevamped.ai.combat;

import java.util.EnumSet;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.PrimedTnt;

public class RoughAIDropTNT extends Goal {

	// CHANGED: 1.12.2 dropped one TNT each time the target re-entered the column under the ghast, and a ghast
	// drifting over a player crossed that edge every few ticks; one drop per 40 ticks keeps it a threat, not a carpet.
	private static final int COOLDOWN = 40;

	protected Mob entity;
	private long nextDrop;

	public RoughAIDropTNT(Mob entity) {
		this.entity = entity;
		this.setFlags(EnumSet.of(Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = entity.getTarget();
		if (target == null || entity.level().getGameTime() < nextDrop)
			return false;
		return entity.getBoundingBox().inflate(3, 0, 3).expandTowards(0, -60, 0).intersects(target.getBoundingBox());
	}

	@Override
	public void start() {
		nextDrop = entity.level().getGameTime() + COOLDOWN;
		PrimedTnt tnt = new PrimedTnt(entity.level(), entity.getX(), entity.getY() - 1, entity.getZ(), entity);
		entity.level().addFreshEntity(tnt);
	}
}
