package com.p1ut0nium.roughmobsrevamped.ai.combat;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;

public class RoughAILeapAtTargetChanced extends LeapAtTargetGoal {

	private int chance;
	protected Mob leaperProtected;

	public RoughAILeapAtTargetChanced(Mob leapingEntity, float leapMotionYIn, int chance) {
		super(leapingEntity, leapMotionYIn);
		this.leaperProtected = leapingEntity;
		this.chance = chance;
	}

	@Override
	public boolean canUse() {
		// CHANGED: 1.12.2 rolled this every 3 ticks, 1.20.1 every 2, so the bound is scaled by 3/2 to keep the
		// configured 1 in X per check at the same rate per second
		return super.canUse() && chance > 0 && leaperProtected.getRandom().nextInt(Math.max(1, chance * 3 / 2)) == 0;
	}
}
