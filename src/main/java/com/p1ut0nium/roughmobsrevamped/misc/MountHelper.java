package com.p1ut0nium.roughmobsrevamped.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.p1ut0nium.roughmobsrevamped.ai.misc.RoughAISearchForRider;
import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;
import com.p1ut0nium.roughmobsrevamped.features.HostileHorseFeatures;
import com.p1ut0nium.roughmobsrevamped.util.Constants;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

public class MountHelper {

	public static final Random RND = new Random();

	public static class Rider {

		public static final String RIDER = Constants.unique("isrider");

		private final String name;
		private final String[] defaultEntities;
		private final int defaultChance;

		private String[] entities;
		private int chance;
		private int randomRiderChance;
		private int rangeToSearchRider;
		private List<EntityType<?>> entries = new ArrayList<EntityType<?>>();

		public Rider(String name, String[] defaultEntities, int defaultChance) {
			this.name = name;
			this.defaultEntities = defaultEntities;
			this.defaultChance = defaultChance;
		}

		public void initConfigs() {
			chance = RoughConfig.getInteger(name, "RiderChance", defaultChance, 0, Short.MAX_VALUE, "Chance (1 in X) for a " + name + " to spawn with another entity riding it\nSet to 0 to disable this feature");
			entities = RoughConfig.getStringArray(name, "RiderEntities", defaultEntities, "Entities which may ride on " + name + "s");
			randomRiderChance = RoughConfig.getInteger(name, "RiderChanceRandom", 10, 0, Short.MAX_VALUE, "Chance (1 in X) that a randomly spawned entity from the RiderEntities list can start riding on random " + name + "s\nSet to 0 to disable this feature");
			rangeToSearchRider = RoughConfig.getInteger(name, "RangeToSearchRider", 32, 0, Short.MAX_VALUE, "Range for " + name + " to search rider");
		}

		public void postInit() {
			entries = FeatureHelper.getEntitiesFromNames(entities);
		}

		public void addAI(Mob mount) {
			if (randomRiderChance > 0)
				mount.goalSelector.addGoal(1, new RoughAISearchForRider(mount, getPossibleRiders(), rangeToSearchRider, randomRiderChance));
		}

		public void tryAddRider(LivingEntity mount) {
			if (chance <= 0 || mount == null || entries.isEmpty() || mount.getPersistentData().getBoolean(RIDER) || RND.nextInt(chance) != 0)
				return;

			EntityType<?> entry = entries.get(RND.nextInt(entries.size()));
			Entity entity = entry.create(mount.level());
			if (entity == null)
				return;

			entity.moveTo(mount.getX(), mount.getY(), mount.getZ(), mount.getYRot(), 0.0F);
			entity.invulnerableTime = 60;
			entity.getPersistentData().putBoolean(RIDER, true);
			mount.level().addFreshEntity(entity);

			if (!entity.isPassenger() && !entity.isVehicle() && !mount.isPassenger() && !mount.isVehicle())
				entity.startRiding(mount);
		}

		public boolean isPossibleRider(Entity entity) {
			for (EntityType<?> entry : entries)
				if (entry == entity.getType())
					return true;
			return false;
		}

		public List<EntityType<?>> getPossibleRiders() {
			return new ArrayList<EntityType<?>>(entries);
		}
	}

	public enum HorseType {

		NORMAL, ZOMBIE, SKELETON;

		public AbstractHorse createInstance(Level world) {
			switch(this)
			{
				case ZOMBIE: return EntityType.ZOMBIE_HORSE.create(world);
				case SKELETON: return EntityType.SKELETON_HORSE.create(world);
				default: return EntityType.HORSE.create(world);
			}
		}
	}

	public static AbstractHorse createHorse(Level world, Entity owner, HorseType type) {
		AbstractHorse horse = type.createInstance(world);
		if (horse == null)
			return null;
		horse.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0.0F);
		if (world instanceof ServerLevel)
			horse.finalizeSpawn((ServerLevel) world, world.getCurrentDifficultyAt(owner.blockPosition()), MobSpawnType.JOCKEY, null, null);
		horse.invulnerableTime = 60;
		horse.setTamed(true);
		horse.setAge(0);
		horse.getPersistentData().putBoolean(HostileHorseFeatures.ROUGH_HORSE, true);
		world.addFreshEntity(horse);

		return horse;
	}

	public static boolean tryMountHorse(Entity rider, HorseType type, int chance, int minY) {
		if (rider.getY() < minY)
			return false;

		if (!BossHelper.isBoss(rider) && (chance <= 0 || RND.nextInt(chance) != 0 || rider.isPassenger() || (rider instanceof Zombie && ((Zombie)rider).isBaby())))
			return false;

		if (rider.getPersistentData().getBoolean(Rider.RIDER))
			return false;

		AbstractHorse mount = createHorse(rider.level(), rider, type);
		if (mount == null)
			return false;
		rider.startRiding(mount);
		return true;
	}
}
