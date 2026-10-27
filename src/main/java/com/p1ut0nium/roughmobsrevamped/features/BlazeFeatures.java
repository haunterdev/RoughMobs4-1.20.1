package com.p1ut0nium.roughmobsrevamped.features;

import com.p1ut0nium.roughmobsrevamped.ai.combat.RoughAIFlameTouch;
import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;
import com.p1ut0nium.roughmobsrevamped.misc.FeatureHelper;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

public class BlazeFeatures extends EntityFeatures {

	private boolean pushAttackersAway;
	private float pushStrength;
	private boolean flameTouch;
	private int flameTouchSeconds;
	private float deathExplosionStrength;
	private boolean deathExplosionDestroyBlocks;

	public BlazeFeatures() {
		super("blaze", EntityType.BLAZE);
	}

	@Override
	public void initConfig() {
		super.initConfig();

		pushAttackersAway = RoughConfig.getBoolean(name, "PushAttackersAway", true, "Set to false to prevent %ss from pushing attackers away");
		pushStrength = RoughConfig.getFloat(name, "PushStrength", 1.0F, 0F, MAX, "Amount of damage done to attacker when pushed away.");
		flameTouch = RoughConfig.getBoolean(name, "FlameTouch", true, "Set to false to prevent %ss from igniting entities which touch their hitbox");
		// CHANGED: new in Rough Mobs 4, Revamped hard-coded 8; RM1's blazeFireSkin burned for 5 (the rm1 Classic Preset)
		flameTouchSeconds = RoughConfig.getInteger(name, "FlameTouchSeconds", 8, 1, MAX, "Seconds an entity burns after touching or hitting a %s");
		deathExplosionStrength = RoughConfig.getFloat(name, "DeathExplosionStrength", 1.0F, 0F, MAX, "Explosion strength of the explosions, which %ss create on death\nSet to 0 to disable this feature");
		deathExplosionDestroyBlocks = RoughConfig.getBoolean(name, "DeathExplosionDestroyBlocks", false, "Set to true to enable destoying blocks when a Blaze explodes on death.");
	}

	@Override
	public void addAI(EntityJoinLevelEvent event, Mob entity, GoalSelector tasks, GoalSelector targetTasks) {
		if (flameTouch)
			tasks.addGoal(1, new RoughAIFlameTouch(entity, flameTouchSeconds));
	}

	@Override
	public void onDefend(Entity target, Entity attacker, Entity immediateAttacker, LivingDamageEvent event) {
		if (pushAttackersAway && attacker instanceof LivingEntity && attacker == immediateAttacker) {
			FeatureHelper.knockback(target, (LivingEntity) attacker, 1F, 0.05F);
			attacker.hurt(target.damageSources().generic(), pushStrength);
			if (flameTouch)
				attacker.setSecondsOnFire(flameTouchSeconds);
			FeatureHelper.playSound(target, SoundEvents.FIRE_EXTINGUISH, 0.7f, 1.0f);
		}
	}

	@Override
	public void onDeath(Entity deadEntity, DamageSource source) {
		if (deathExplosionStrength > 0 && !(source.getEntity() instanceof FakePlayer)) {
			deadEntity.level().explode(deadEntity, deadEntity.getX(), deadEntity.getY(), deadEntity.getZ(), deathExplosionStrength, deathExplosionDestroyBlocks ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
		}
	}
}
