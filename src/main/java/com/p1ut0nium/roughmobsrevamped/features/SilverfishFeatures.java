package com.p1ut0nium.roughmobsrevamped.features;

import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

public class SilverfishFeatures extends EntityFeatures {

	private int splitChance;

	public SilverfishFeatures() {
		super("silverfish", EntityType.SILVERFISH);
	}

	@Override
	public void initConfig() {
		super.initConfig();

		splitChance = RoughConfig.getInteger(name, "SplitChance", 3, 0, MAX, "Chance (1 in X) to split a %s in two when attacked\nSet to 0 to disable this feature");
	}

	@Override
	public void onDefend(Entity target, Entity attacker, Entity immediateAttacker, LivingDamageEvent event) {
		if (target instanceof Mob)
		{
			float health = ((Mob)target).getHealth();
			if (splitChance > 0 && health > 0 && target.level().random.nextInt(splitChance) == 0)
			{
				Entity created = target.getType().create(target.level());
				if (!(created instanceof Mob))
					return;
				Mob silverfish = (Mob) created;
				silverfish.setPos(target.getX(), target.getY(), target.getZ());
				if (target.level() instanceof ServerLevel)
					silverfish.finalizeSpawn((ServerLevel) target.level(), target.level().getCurrentDifficultyAt(target.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
				silverfish.setHealth(health);
				target.level().addFreshEntity(silverfish);
			}
		}
	}
}
