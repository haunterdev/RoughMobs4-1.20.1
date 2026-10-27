package com.p1ut0nium.roughmobsrevamped.features;

import java.util.ArrayList;
import java.util.List;

import com.p1ut0nium.roughmobsrevamped.ai.misc.RoughAIDespawn;
import com.p1ut0nium.roughmobsrevamped.ai.misc.RoughAISearchForRider;
import com.p1ut0nium.roughmobsrevamped.ai.misc.RoughAISunlightBurn;
import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;
import com.p1ut0nium.roughmobsrevamped.util.Constants;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

public class HostileHorseFeatures extends EntityFeatures {

	public static final String ROUGH_HORSE = Constants.unique("roughhorse");

	private boolean horseBurn;
	private boolean burnWithRider;
	private int randomRiderChance;
	private boolean canDespawn;
	private int rangeToSearchRider;

	public HostileHorseFeatures() {
		super("hostileHorse", EntityType.ZOMBIE_HORSE, EntityType.SKELETON_HORSE);
	}

	@Override
	public void initConfig() {
		super.initConfig();

		horseBurn = RoughConfig.getBoolean(name, "Burn", true, "Set this to false to prevent undead horses from burning in sunlight (as long as they have no rider)");
		// RM1: horseUndeadBurn mode 0
		burnWithRider = RoughConfig.getBoolean(name, "BurnWithRider", false, "Set this to true to let undead horses burn in sunlight with a rider on too (needs Burn)");
		randomRiderChance = RoughConfig.getInteger(name, "RandomRiderChance", 3, 0, MAX, "Chance (1 in X) that a random skeleton or zombie starts riding unmounted hostile horses around it");
		canDespawn = RoughConfig.getBoolean(name, "CanDespawn", true, "Set to false to prevent undead horses summoned through this mod from despawning");
		rangeToSearchRider = RoughConfig.getInteger(name, "RangeToSearchRider", 32, 0, Short.MAX_VALUE, "Range for " + name + " to search rider");
	}

	@Override
	public void addAI(EntityJoinLevelEvent event, Mob entity, GoalSelector tasks, GoalSelector targetTasks) {
		if (horseBurn && !entity.fireImmune())
			tasks.addGoal(0, new RoughAISunlightBurn(entity, false) {
				@Override
				public boolean canUse() {
					return super.canUse() && (burnWithRider || !entity.isVehicle());
				}
			});

		if (randomRiderChance > 0)
			tasks.addGoal(1, new RoughAISearchForRider(entity, getRiders(entity), rangeToSearchRider, randomRiderChance));

		if (shouldDespawn(entity))
			tasks.addGoal(1, new RoughAIDespawn(entity));
	}

	@Override
	public void addFeatures(EntityJoinLevelEvent event, Mob entity) {
		if (shouldDespawn(entity))
			entity.persistenceRequired = false;
	}

	private boolean shouldDespawn(Entity entity) {
		return canDespawn && entity.getPersistentData().getBoolean(ROUGH_HORSE);
	}

	// CHANGED: riders are entity types, since 1.20.1 entity checks go by type; the class lists AbstractSkeleton and EntityZombie
	// matched every skeleton and every zombie kind, so the lists name each vanilla type
	private List<EntityType<?>> getRiders(Entity entity) {
		List<EntityType<?>> riders = new ArrayList<>();

		if (entity instanceof SkeletonHorse) {
			riders.add(EntityType.SKELETON);
			riders.add(EntityType.STRAY);
			riders.add(EntityType.WITHER_SKELETON);
		}
		else {
			riders.add(EntityType.ZOMBIE);
			riders.add(EntityType.ZOMBIE_VILLAGER);
			riders.add(EntityType.HUSK);
			riders.add(EntityType.DROWNED);
			riders.add(EntityType.ZOMBIFIED_PIGLIN);
		}

		return riders;
	}
}
