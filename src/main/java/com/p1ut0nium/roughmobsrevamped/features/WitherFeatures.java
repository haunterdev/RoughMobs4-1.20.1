package com.p1ut0nium.roughmobsrevamped.features;

import com.p1ut0nium.roughmobsrevamped.ai.combat.RoughAISummonSkeleton;
import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;
import com.p1ut0nium.roughmobsrevamped.misc.FeatureHelper;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

public class WitherFeatures extends EntityFeatures {

	private boolean pushAttackersAway;
	private boolean enableKnockBackDamage;
	private int summonSkeletonTimer;

	public WitherFeatures() {
		super("wither", EntityType.WITHER);
	}

	@Override
	public void initConfig() {
		super.initConfig();

		pushAttackersAway = RoughConfig.getBoolean(name, "PushAttackersAway", true, "Set to false to prevent %ss from pushing attackers away.");
		enableKnockBackDamage = RoughConfig.getBoolean(name, "EnableKnockBackDamage", true, "Set to false to turn off knockback damage.");
		summonSkeletonTimer = RoughConfig.getInteger(name, "SummonSkeletonTimer", 200, 0, MAX, "Delay in ticks between each spawned Skeleton\nSet to 0 to disable this feature");
	}

	@Override
	public void addAI(EntityJoinLevelEvent event, Mob entity, GoalSelector tasks, GoalSelector targetTasks) {
		if (summonSkeletonTimer > 0)
			tasks.addGoal(1, new RoughAISummonSkeleton(entity, summonSkeletonTimer, true));
	}

	@Override
	public void onDefend(Entity target, Entity attacker, Entity immediateAttacker, LivingDamageEvent event) {
		if (pushAttackersAway && attacker instanceof LivingEntity && attacker == immediateAttacker)
		{
			FeatureHelper.knockback(target, (LivingEntity) attacker, 1F, 0.05F);
			if (enableKnockBackDamage)
				attacker.hurt(target.damageSources().generic(), 4F);
			FeatureHelper.playSound(target, SoundEvents.WITHER_BREAK_BLOCK, 0.7f, 1.0f);
		}
	}
}
