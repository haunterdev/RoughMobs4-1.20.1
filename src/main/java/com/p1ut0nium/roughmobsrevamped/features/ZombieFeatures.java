package com.p1ut0nium.roughmobsrevamped.features;

import java.util.Set;

import com.google.common.collect.Sets;
import com.p1ut0nium.roughmobsrevamped.RoughApplier;
import com.p1ut0nium.roughmobsrevamped.ai.combat.RoughAILeapAtTargetChanced;
import com.p1ut0nium.roughmobsrevamped.ai.misc.RoughAIBreakBlocks;
import com.p1ut0nium.roughmobsrevamped.ai.misc.RoughAISunlightBurn;
import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;
import com.p1ut0nium.roughmobsrevamped.entities.BossHusk;
import com.p1ut0nium.roughmobsrevamped.misc.BossHelper.BossApplier;
import com.p1ut0nium.roughmobsrevamped.misc.EquipHelper.EquipmentApplier;
import com.p1ut0nium.roughmobsrevamped.misc.FeatureHelper;
import com.p1ut0nium.roughmobsrevamped.misc.MountHelper;
import com.p1ut0nium.roughmobsrevamped.misc.MountHelper.HorseType;
import com.p1ut0nium.roughmobsrevamped.misc.SpawnHelper;
import com.p1ut0nium.roughmobsrevamped.util.Constants;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

public class ZombieFeatures extends EntityFeatures {

	public static final String BOSS_MINION = Constants.unique("bossMinion");

	private float leapHeight;
	private int leapChance;
	private int hungerDuration;
	private int hungerChance;
	private int horseChance;
	private int horseMinY;
	private boolean babyBurn;
	private boolean helmetBurn;

	private EquipmentApplier equipApplier;
	private BossApplier bossApplier;

	private String[] breakBlocks;
	private Set<Block> allowedBreakBlocks;

	public ZombieFeatures() {
		// Sends name and list of classes back to EntityFeatures
		super("zombie", EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.HUSK, EntityType.ZOMBIFIED_PIGLIN);
	}

	@Override
	public void preInit() {
		equipApplier = new EquipmentApplier(name, 3, 4, 8, 0.5f, 0.085F);
		bossApplier = new BossApplier(name, 200, 1F, 0.2F, new String[]{"Zombie King", "Flesh King", "Dr. Zomboss", "Azog", "Zon-Goku", "Amy", "Z0mb3y"}) {
			@Override
			public void addBossFeatures(Mob entity) {
				if (SpawnHelper.disableBabyZombies() == false) {
					// CHANGED: a husk boss (new in Rough Mobs 4) brings baby husks
					EntityType<? extends Zombie> minionType = entity instanceof BossHusk ? EntityType.HUSK : EntityType.ZOMBIE;
					for (int i = 0; i < 4; i++)
					{
						Zombie zombieMinion = minionType.create(entity.level());
						if (zombieMinion == null)
							continue;
						zombieMinion.setPos(entity.getX() + Math.random() - Math.random(), entity.getY() + Math.random(), entity.getZ() + Math.random() - Math.random());
						if (entity.level() instanceof ServerLevel)
							zombieMinion.finalizeSpawn((ServerLevel) entity.level(), entity.level().getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
						zombieMinion.setBaby(true);
						zombieMinion.getPersistentData().putBoolean(BOSS_MINION, true);
						entity.level().addFreshEntity(zombieMinion);
					}
				}
			}
		};
	}

	@Override
	public void postInit() {
		equipApplier.createPools();
		bossApplier.postInit();
		allowedBreakBlocks = FeatureHelper.getBlocksFromNames(breakBlocks, Sets.newIdentityHashSet());
	}

	@Override
	public void initConfig() {
		super.initConfig();

		leapChance = RoughConfig.getInteger(name, "LeapChance", 5, 0, MAX, "Chance (1 in X) for a %s to leap to the target\nSet to 0 to disable this feature");
		leapHeight = RoughConfig.getFloat(name, "LeapHeight", 0.2F, 0, MAX, "Amount of blocks the %s jumps on leap attack");

		hungerDuration = RoughConfig.getInteger(name, "HungerDuration", 200, 1, MAX, "Duration in ticks of the applied hunger effect (20 ticks = 1 second)");
		hungerChance = RoughConfig.getInteger(name, "HungerChance", 1, 0, MAX, "Chance (1 in X) for a %s to apply the hunger effect on attack\nSet to 0 to disable this feature");

		horseChance = RoughConfig.getInteger(name, "HorseChance", 10, 0, MAX, "Chance (1 in X) that a %s spawns riding a %s horse\nSet to 0 to disable this feature");
		horseMinY = RoughConfig.getInteger(name, "HorseMinY", 63, 0, MAX, "Minimal Y position above %s horses may spawn");

		babyBurn = RoughConfig.getBoolean(name, "BabyBurn", true, "Kept so Rough Mobs Revamped configs load. Baby %ss burn in sunlight in 1.20.1 whatever this is set to");
		helmetBurn = RoughConfig.getBoolean(name, "HelmetBurn", false, "Set this to true to make all %ss burn in sunlight even if they wear a helmet");

		breakBlocks = RoughConfig.getStringArray(name, "BreakBlocks", Constants.DEFAULT_DESTROY_BLOCKS, "Blocks which can be destroyed by %ss if they have no attack target\nDelete all lines to disable this feature");

		equipApplier.initConfig(
			Constants.DEFAULT_MAINHAND,
			Constants.DEFAULT_OFFHAND,
			Constants.DEFAULT_HELMETS,
			Constants.DEFAULT_CHESTPLATES,
			Constants.DEFAULT_LEGGINGS,
			Constants.DEFAULT_BOOTS,
			Constants.DEFAULT_WEAPON_ENCHANTS,
			Constants.DEFAULT_ARMOR_ENCHANTS,
			false
		);

		bossApplier.initConfig();
	}

	@Override
	public void addAI(EntityJoinLevelEvent event, Mob entity, GoalSelector tasks, GoalSelector targetTasks) {
		if (leapChance > 0)
			tasks.addGoal(1, new RoughAILeapAtTargetChanced(entity, leapHeight, leapChance));

		// DROPPED: if (babyBurn && baby && !fireImmune) tasks.addTask(0, new RoughAISunlightBurn(entity, false));
		// 1.20.1 Zombie.aiStep burns baby zombies itself (1.12.2 skipped children), so BabyBurn=true is vanilla now.
		// BabyBurn=false cannot stop the vanilla burn without a mixin; the key is kept so old configs load.

		if (helmetBurn)
			tasks.addGoal(0, new RoughAISunlightBurn(entity, true));

		if (!allowedBreakBlocks.isEmpty())
			tasks.addGoal(1, new RoughAIBreakBlocks(entity, 8, allowedBreakBlocks));
	}

	@Override
	public void addFeatures(EntityJoinLevelEvent event, Mob entity) {
		if (entity.getPersistentData().getBoolean(BOSS_MINION))
			return;

		if (super.bossesEnabled(entity)) {
			EntityType<? extends Mob> bossType = bossApplier.rollBoss(entity);
			if (bossType != null) {
				// CHANGED: the boss replaces the zombie a tick later instead of cancelling the join event
				RoughApplier.runNextTick(() -> {
					Mob boss = bossApplier.spawnBoss(entity, bossType);
					if (boss != null)
						MountHelper.tryMountHorse(boss, HorseType.ZOMBIE, horseChance, horseMinY);
				});
				return;
			}
		}

		// CHANGED: the original only equipped zombies when bosses were enabled for the nearest player,
		// so with the boss game stage in use, zombies spawned without equipment until a player had it
		equipApplier.equipEntity(entity);

		// Attempt to spawn zombie on a horse
		MountHelper.tryMountHorse(entity, HorseType.ZOMBIE, horseChance, horseMinY);
	}

	@Override
	public void onAttack(Entity attacker, Entity immediateAttacker, Entity target, LivingDamageEvent event) {
		if (target instanceof LivingEntity && hungerChance > 0)
			FeatureHelper.addEffect((LivingEntity)target, MobEffects.HUNGER, hungerDuration, 0, hungerChance, true, 4);
	}
}
