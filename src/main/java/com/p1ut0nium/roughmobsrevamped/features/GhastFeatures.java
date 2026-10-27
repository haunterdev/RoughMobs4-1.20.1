package com.p1ut0nium.roughmobsrevamped.features;

import com.p1ut0nium.roughmobsrevamped.ai.combat.RoughAIDropTNT;
import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;
import com.p1ut0nium.roughmobsrevamped.misc.FeatureHelper;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

public class GhastFeatures extends EntityFeatures {

	private boolean projectileImmunity;
	private boolean dropTnt;
	private int explosionRadius;

	public GhastFeatures() {
		super("ghast", EntityType.GHAST);
	}

	@Override
	public void initConfig() {
		super.initConfig();

		projectileImmunity = RoughConfig.getBoolean(name, "ProjectileImmunity", true, "Set to false to prevent %ss from being immune to projectiles");
		dropTnt = RoughConfig.getBoolean(name, "DropTnt", true, "Set to false to prevent %ss from dropping primed TNT on their targets");
		explosionRadius = RoughConfig.getInteger(name, "ExplosionRadius", 3, 0, MAX, "Ghast fireball explosion radius\nThe vanilla default is 1");
	}

	@Override
	public void addAI(EntityJoinLevelEvent event, Mob entity, GoalSelector tasks, GoalSelector targetTasks) {
		if (entity instanceof Ghast)
		{
			if (explosionRadius != 1)
				((Ghast)entity).explosionPower = explosionRadius;
		}

		if (dropTnt)
			tasks.addGoal(1, new RoughAIDropTNT(entity));
	}

	@Override
	public void onDefendAttack(Entity target, Entity attacker, Entity immediateAttacker, LivingAttackEvent event) {
		if (projectileImmunity && event.getSource().is(DamageTypeTags.IS_PROJECTILE) && !(immediateAttacker instanceof LargeFireball))
		{
			event.setCanceled(true);
			FeatureHelper.spawnParticle(target, ParticleTypes.LARGE_SMOKE, 5);
			FeatureHelper.playSound(target, SoundEvents.SNOW_BREAK);
		}
	}
}
