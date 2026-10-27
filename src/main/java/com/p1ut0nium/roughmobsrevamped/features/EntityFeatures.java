package com.p1ut0nium.roughmobsrevamped.features;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.p1ut0nium.roughmobsrevamped.compat.GameStagesCompat;
import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;
import com.p1ut0nium.roughmobsrevamped.misc.BossHelper;
import com.p1ut0nium.roughmobsrevamped.util.Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.registries.ForgeRegistries;

/*
 * Base for every per-mob feature set.
 *
 * CHANGED: the event passed to addFeatures and addAI is null when the entity was loaded from disk.
 * Those entities are handled a tick after they join (adding entities while a chunk loads deadlocks
 * the server), so anything that replaced the original by cancelling the join event discards it instead.
 */
public abstract class EntityFeatures {

	public static final Random RND = new Random();
	public static final int MAX = Short.MAX_VALUE;

	//Default
	public String name;
	protected List<EntityType<?>> entityClasses;

	//Config
	protected boolean featuresEnabled;
	protected List<String> entityNames = new ArrayList<String>();

	public EntityFeatures(String name, EntityType<?>... entityClasses) {
		this.name = name;
		this.entityClasses = Arrays.asList(entityClasses);
	}

	public boolean isEntity(Entity creature) {
		ResourceLocation loc = ForgeRegistries.ENTITY_TYPES.getKey(creature.getType());
		return featuresEnabled && loc != null && entityNames.contains(loc.toString());
	}

	public void initConfig() {
		if (!hasDefaultConfig())
			return;

		RoughConfig.addCustomCategoryComment(name, "Configuration options which affect " + name + " features");

		featuresEnabled = RoughConfig.getBoolean(name, "FeaturesEnabled", true, "Set to false to disable ALL %s features", true);
		String[] names = RoughConfig.getStringArray(name, "Entities", Constants.getRegNames(entityClasses).toArray(new String[0]), "Entities which count as %s entities");

		// CHANGED: names are normalised to namespace:path, and 1.12.2 names are renamed, so "zombie" and "zombie_pigman" still match
		entityNames = new ArrayList<String>();
		for (String entityName : names) {
			ResourceLocation loc = Constants.legacyLocation(entityName);
			if (loc != null)
				entityNames.add(loc.toString());
		}
	}

	private boolean hasDefaultConfig() {
		return true;
	}

	public void addFeatures(EntityJoinLevelEvent event, Mob entity) {
	}

	public boolean bossesEnabled(Entity entity) {
		if (!BossHelper.bossesEnabled)
			return false;

		boolean bossStageEnabled = GameStagesCompat.useBossStage();

		if (bossStageEnabled) {
			Player playerClosest = entity.level().getNearestPlayer(entity, -1.0D);
			// CHANGED: no nearest player counts as not having the stage instead of throwing
			return playerClosest != null && GameStagesCompat.hasAnyOf(playerClosest, Constants.ROUGHMOBSALL, Constants.ROUGHMOBSBOSS);
		}

		// If boss game stage isn't enabled, then its ok to spawn bosses
		return true;
	}

	public void addAI(EntityJoinLevelEvent event, Mob entity, GoalSelector tasks, GoalSelector targetTasks) {
	}

	// CHANGED: on-hit effects run from LivingDamageEvent, so a hit that a shield or invulnerability frames stopped no longer counts
	public void onAttack(Entity attacker, Entity immediateAttacker, Entity target, LivingDamageEvent event) {
	}

	public void onDefend(Entity target, Entity attacker, Entity immediateAttacker, LivingDamageEvent event) {
	}

	// Runs from LivingAttackEvent, for features that cancel the attack outright
	public void onDefendAttack(Entity target, Entity attacker, Entity immediateAttacker, LivingAttackEvent event) {
	}

	public void onDeath(Entity deadEntity, DamageSource source) {
		// DROPPED: the empty player kill count branch (a TODO in the original)
	}

	public void onFall(Entity entity, LivingFallEvent event) {
	}

	public void onBlockBreak(Player player, BreakEvent event) {
	}

	public void preInit() {
	}

	public void postInit() {
	}
}
