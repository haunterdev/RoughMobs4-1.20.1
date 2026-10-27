package com.p1ut0nium.roughmobsrevamped.features;

import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;
import com.p1ut0nium.roughmobsrevamped.misc.FeatureHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Slime;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

public class SlimeFeatures extends EntityFeatures {

	private int splitChance;
	private float knockBackMultiplier;

	public SlimeFeatures() {
		super("slime", EntityType.SLIME, EntityType.MAGMA_CUBE);
	}

	@Override
	public void initConfig() {
		super.initConfig();

		splitChance = RoughConfig.getInteger(name, "SplitChance", 6, 0, MAX, "Chance (1 in X minus %s size) that a bigger %s summons a baby %s when hit\nSet to 0 to disable this feature");
		knockBackMultiplier = RoughConfig.getFloat(name, "KnockBackMultiplier", 0.2F, 0, MAX, "Amount of extra knockback a %s deals\nCalculated with this value times the slime size\nSet to 0 to disable this feature");
	}

	@Override
	public void onAttack(Entity attacker, Entity immediateAttacker, Entity target, LivingDamageEvent event) {
		if (knockBackMultiplier > 0 && target instanceof LivingEntity)
		{
			FeatureHelper.knockback(attacker, (LivingEntity)target, Math.max(getSlimeSize(attacker), 1)*knockBackMultiplier, 0.3F);
		}
	}

	@Override
	public void onDefend(Entity target, Entity attacker, Entity immediateAttacker, LivingDamageEvent event) {
		if (splitChance <= 0)
			return;

		int chance = Math.max(1, splitChance - getSlimeSize(target));
		if (!isSmallSlime(target) && attacker == immediateAttacker && RND.nextInt(chance) == 0)
		{
			Entity created = target.getType().create(target.level());
			if (!(created instanceof Slime))
				return;
			Slime slime = (Slime) created;
			slime.setPos(target.getX() + Math.random() - Math.random(), target.getY() + Math.random(), target.getZ() + Math.random() - Math.random());
			if (target.level() instanceof ServerLevel)
				slime.finalizeSpawn((ServerLevel) target.level(), target.level().getCurrentDifficultyAt(target.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
			// CHANGED: setSize is public in 1.20.1, the reflection fallback chain is gone
			slime.setSize(1, true);
			target.level().addFreshEntity(slime);
		}
	}

	private boolean isSmallSlime(Entity entity) {
		return entity instanceof Slime && ((Slime)entity).isTiny();
	}

	private int getSlimeSize(Entity entity) {
		return entity instanceof Slime ? ((Slime)entity).getSize() : 2;
	}
}
