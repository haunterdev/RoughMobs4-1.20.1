package com.p1ut0nium.roughmobsrevamped.features;

import com.p1ut0nium.roughmobsrevamped.ai.combat.RoughAIInvisibleTarget;
import com.p1ut0nium.roughmobsrevamped.ai.misc.RoughAIBurnExplosion;
import com.p1ut0nium.roughmobsrevamped.ai.misc.RoughAIInvisibleParticles;
import com.p1ut0nium.roughmobsrevamped.ai.misc.RoughAISunlightBurn;
import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

public class CreeperFeatures extends EntityFeatures {

	private float invisibleRange;
	private int invisibleParticles;
	private int fuseTime;
	private int explosionRadius;
	private boolean creeperBurn;
	private boolean creeperBurnExplosion;

	public CreeperFeatures() {
		super("creeper", EntityType.CREEPER);
	}

	@Override
	public void initConfig() {
		super.initConfig();

		invisibleRange = RoughConfig.getFloat(name, "InvisibleRange", 6F, 0, MAX, "Block range to the target in which %ss become invisible\nSet to 0 to prevent %ss from becoming invisible");
		// CHANGED: new in Rough Mobs Redux (3 in the classic Classic Preset; 0 keeps Revamped's fully invisible creeper)
		invisibleParticles = RoughConfig.getInteger(name, "InvisibleParticles", 0, 0, 50, "Green particles per tick around an invisible %s, so it can still be spotted\nSet to 0 for a fully invisible %s");
		fuseTime = RoughConfig.getInteger(name, "FuseTime", 20, 0, MAX, "Creeper fuse time (In ticks, 20 ticks = 1 second)\nThe vanilla default is 30");
		explosionRadius = RoughConfig.getInteger(name, "ExplosionRadius", 4, 0, MAX, "Creeper explosion radius\nThe vanilla default is 3");
		creeperBurn = RoughConfig.getBoolean(name, "CreeperBurn", false, "If true %ss burn in sunlight");
		creeperBurnExplosion = RoughConfig.getBoolean(name, "CreeperBurnExplosion", false, "If true %ss explode if they catch fire");
	}

	@Override
	public void addAI(EntityJoinLevelEvent event, Mob entity, GoalSelector tasks, GoalSelector targetTasks) {
		if (invisibleRange > 0)
			tasks.addGoal(2, new RoughAIInvisibleTarget(entity, invisibleRange));
		if (invisibleParticles > 0)
			tasks.addGoal(2, new RoughAIInvisibleParticles(entity, invisibleParticles));

		if (entity instanceof Creeper)
		{
			Creeper creeper = (Creeper) entity;
			if (fuseTime != 30)
				creeper.maxSwell = fuseTime;
			if (explosionRadius != 3)
				creeper.explosionRadius = explosionRadius;
			if (creeperBurnExplosion)
				tasks.addGoal(0, new RoughAIBurnExplosion(creeper));
		}

		if (creeperBurn)
			tasks.addGoal(0, new RoughAISunlightBurn(entity, false));
	}
}
