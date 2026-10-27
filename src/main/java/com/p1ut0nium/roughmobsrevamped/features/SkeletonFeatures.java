package com.p1ut0nium.roughmobsrevamped.features;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import com.p1ut0nium.roughmobsrevamped.RoughApplier;
import com.p1ut0nium.roughmobsrevamped.ai.combat.RoughAIWeaponSwitch;
import com.p1ut0nium.roughmobsrevamped.ai.misc.RoughAISunlightBurn;
import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;
import com.p1ut0nium.roughmobsrevamped.misc.BossHelper.BossApplier;
import com.p1ut0nium.roughmobsrevamped.misc.EquipHelper.EquipmentApplier;
import com.p1ut0nium.roughmobsrevamped.misc.MountHelper;
import com.p1ut0nium.roughmobsrevamped.misc.MountHelper.HorseType;
import com.p1ut0nium.roughmobsrevamped.util.Constants;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;

public class SkeletonFeatures extends EntityFeatures {

	private int horseChance;
	private int horseMinY;
	private int bowCooldown;
	private boolean changeWeapons;
	private boolean helmetBurn;

	private EquipmentApplier equipApplier;
	private BossApplier bossApplier;

	// Skeletons whose bow cooldown this mod set, so it can be set again after the vanilla reassessWeaponGoal resets it
	private final Set<AbstractSkeleton> cooldownSkeletons = Collections.newSetFromMap(new WeakHashMap<AbstractSkeleton, Boolean>());

	public SkeletonFeatures() {
		super("skeleton", EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON);
	}

	@Override
	public void preInit() {
		equipApplier = new EquipmentApplier(name, 1, 4, 8, 0.5f, 0.085F);
		bossApplier = new BossApplier(name, 200, 1F, 0.2F, new String[]{"Lich King", "Skeleton Lord", "Stallord", "Skeletron", "Skeletron Prime", "Krosis", "Wolnir", "Stalmaster"}) {
			@Override
			public void addBossFeatures(Mob entity) {
			}
		};
		MinecraftForge.EVENT_BUS.addListener(this::onEquipmentChange);
	}

	@Override
	public void postInit() {
		equipApplier.createPools();
		bossApplier.postInit();
	}

	@Override
	public void initConfig() {
		super.initConfig();

		changeWeapons = RoughConfig.getBoolean(name, "ChangeWeapons", true, "Set this to false to prevent %ss from switching their weapons");

		horseChance = RoughConfig.getInteger(name, "HorseChance", 10, 0, MAX, "Chance (1 in X) that a %s spawns riding a %s horse\nSet to 0 to disable this feature");
		horseMinY = RoughConfig.getInteger(name, "HorseMinY", 63, 0, MAX, "Minimal Y position above %s horses may spawn");

		bowCooldown = RoughConfig.getInteger(name, "BowCooldown", 0, 0, MAX, "Bow cooldown of %ss in ticks (The vanilla default is 20)");

		helmetBurn = RoughConfig.getBoolean(name, "HelmetBurn", false, "Set this to true to make all %ss burn in sunlight even if they wear a helmet");

		equipApplier.initConfig(
			Constants.SKELETON_MAINHAND,
			Constants.DEFAULT_MAINHAND,
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
		if (changeWeapons)
			targetTasks.addGoal(1, new RoughAIWeaponSwitch(entity, 12D));

		if (helmetBurn)
			tasks.addGoal(0, new RoughAISunlightBurn(entity, true));

		if (entity instanceof AbstractSkeleton) {
			AbstractSkeleton skeleton = (AbstractSkeleton) entity;
			skeleton.bowGoal.setMinAttackInterval(bowCooldown);
			cooldownSkeletons.add(skeleton);
		}
	}

	// CHANGED: 1.20.1 reassessWeaponGoal sets the bow interval back to 20 (40 on hard is 20 too) whenever the
	// skeleton's items change, which the 1.12.2 field write did not have to survive, so it is written again here
	private void onEquipmentChange(LivingEquipmentChangeEvent event) {
		if (event.getEntity() instanceof AbstractSkeleton && cooldownSkeletons.contains(event.getEntity())) {
			AbstractSkeleton skeleton = (AbstractSkeleton) event.getEntity();
			RoughApplier.runNextTick(() -> skeleton.bowGoal.setMinAttackInterval(bowCooldown));
		}
	}

	@Override
	public void addFeatures(EntityJoinLevelEvent event, Mob entity) {
		if (entity instanceof Skeleton && entity.getType() == EntityType.SKELETON && entity.level().dimension() == Level.NETHER) {
			changeToWither(entity);
			return;
		}

		if (super.bossesEnabled(entity)) {
			EntityType<? extends Mob> bossType = bossApplier.rollBoss(entity);
			if (bossType != null) {
				// CHANGED: the boss replaces the skeleton a tick later instead of cancelling the join event
				RoughApplier.runNextTick(() -> {
					Mob boss = bossApplier.spawnBoss(entity, bossType);
					if (boss != null)
						MountHelper.tryMountHorse(boss, HorseType.SKELETON, horseChance, horseMinY);
				});
				return;
			}
		}

		// CHANGED: equipped when bosses are disabled too, as with zombies
		equipApplier.equipEntity(entity);

		// Attempt to spawn skeleton on a horse
		MountHelper.tryMountHorse(entity, HorseType.SKELETON, horseChance, horseMinY);
	}

	private void changeToWither(Mob entity) {
		// CHANGED: queued a tick so the replacement is never added while the chunk loads
		RoughApplier.runNextTick(() -> {
			if (!entity.isAlive())
				return;
			WitherSkeleton newSkeleton = EntityType.WITHER_SKELETON.create(entity.level());
			if (newSkeleton == null)
				return;
			newSkeleton.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
			if (entity.level() instanceof ServerLevel)
				newSkeleton.finalizeSpawn((ServerLevel) entity.level(), entity.level().getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.CONVERSION, null, null);

			entity.level().addFreshEntity(newSkeleton);
			entity.discard();
		});
	}
}
