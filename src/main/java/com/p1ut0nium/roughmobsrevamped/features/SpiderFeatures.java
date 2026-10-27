package com.p1ut0nium.roughmobsrevamped.features;

import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;
import com.p1ut0nium.roughmobsrevamped.misc.FeatureHelper;
import com.p1ut0nium.roughmobsrevamped.misc.MountHelper.Rider;
import com.p1ut0nium.roughmobsrevamped.util.Constants;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;

public class SpiderFeatures extends EntityFeatures {

	private float ignoreFallDamageMult;
	private int slownessChance;
	private int slownessDuration;
	private boolean slownessCreateWeb;

	private Rider rider;

	public SpiderFeatures() {
		super("spider", EntityType.SPIDER, EntityType.CAVE_SPIDER);
	}

	@Override
	public void preInit() {
		rider = new Rider(name, Constants.DEFAULT_SPIDER_RIDERS, 10);
	}

	@Override
	public void initConfig() {
		super.initConfig();

		ignoreFallDamageMult = RoughConfig.getFloat(name, "IgnoreFallDamageMult", 0.0F, 0F, 1F, "The fall damage %ss take is multiplied by this value (0.0 means no fall damage, 1.0 means normal full damage)");

		slownessDuration = RoughConfig.getInteger(name, "SlownessDuration", 200, 1, MAX, "Duration in ticks of the applied slowness effect (20 ticks = 1 second)");
		slownessChance = RoughConfig.getInteger(name, "SlownessChance", 1, 0, MAX, "Chance (1 in X) for a %s to apply the slowness effect on attack\nSet to 0 to disable this feature");
		slownessCreateWeb = RoughConfig.getBoolean(name, "SlownessCreateWeb", true, "Set this to false to prevent %ss from creating webs on slowed targets");

		rider.initConfigs();
	}

	@Override
	public void postInit() {
		rider.postInit();
	}

	@Override
	public void addAI(EntityJoinLevelEvent event, Mob entity, GoalSelector tasks, GoalSelector targetTasks) {
		rider.addAI(entity);
	}

	@Override
	public void addFeatures(EntityJoinLevelEvent event, Mob entity) {
		rider.tryAddRider(entity);
	}

	@Override
	public void onFall(Entity entity, LivingFallEvent event) {
		if (ignoreFallDamageMult == 1)
			return;

		if (ignoreFallDamageMult == 0)
			event.setCanceled(true);

		event.setDamageMultiplier(event.getDamageMultiplier() * ignoreFallDamageMult);
	}

	@Override
	public void onAttack(Entity attacker, Entity immediateAttacker, Entity target, LivingDamageEvent event) {
		if (target instanceof LivingEntity && slownessChance > 0)
		{
			LivingEntity living = (LivingEntity)target;
			int maxAmp = 4;
			FeatureHelper.addEffect(living, MobEffects.MOVEMENT_SLOWDOWN, slownessDuration, 0, slownessChance, true, maxAmp);

			MobEffectInstance active = living.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
			if (slownessCreateWeb && active != null && active.getAmplifier() >= maxAmp && RND.nextInt(slownessChance) == 0 && target.level().getBlockState(target.blockPosition()).isAir())
			{
				target.level().setBlockAndUpdate(target.blockPosition(), Blocks.COBWEB.defaultBlockState());
			}
		}
	}
}
