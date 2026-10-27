package com.p1ut0nium.roughmobsrevamped;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.p1ut0nium.roughmobsrevamped.classic.ClassicFeatures;
import com.p1ut0nium.roughmobsrevamped.classic.ClassicPreset;
import com.p1ut0nium.roughmobsrevamped.classic.LegacyConfigImporter;
import com.p1ut0nium.roughmobsrevamped.compat.CompatHandler;
import com.p1ut0nium.roughmobsrevamped.compat.GameStagesCompat;
import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;
import com.p1ut0nium.roughmobsrevamped.entities.IBoss;
import com.p1ut0nium.roughmobsrevamped.features.BlazeFeatures;
import com.p1ut0nium.roughmobsrevamped.features.CreeperFeatures;
import com.p1ut0nium.roughmobsrevamped.features.EndermanFeatures;
import com.p1ut0nium.roughmobsrevamped.features.EndermiteFeatures;
import com.p1ut0nium.roughmobsrevamped.features.EntityFeatures;
import com.p1ut0nium.roughmobsrevamped.features.GhastFeatures;
import com.p1ut0nium.roughmobsrevamped.features.GuardianFeatures;
import com.p1ut0nium.roughmobsrevamped.features.HostileHorseFeatures;
import com.p1ut0nium.roughmobsrevamped.features.MagmaCubeFeatures;
import com.p1ut0nium.roughmobsrevamped.features.SilverfishFeatures;
import com.p1ut0nium.roughmobsrevamped.features.SkeletonFeatures;
import com.p1ut0nium.roughmobsrevamped.features.SlimeFeatures;
import com.p1ut0nium.roughmobsrevamped.features.SpiderFeatures;
import com.p1ut0nium.roughmobsrevamped.features.WitchFeatures;
import com.p1ut0nium.roughmobsrevamped.features.WitchFeatures.WitchPotionFeatures;
import com.p1ut0nium.roughmobsrevamped.features.WitherFeatures;
import com.p1ut0nium.roughmobsrevamped.features.ZombieFeatures;
import com.p1ut0nium.roughmobsrevamped.features.ZombiePigmanFeatures;
import com.p1ut0nium.roughmobsrevamped.misc.AttributeHelper;
import com.p1ut0nium.roughmobsrevamped.misc.BossHelper;
import com.p1ut0nium.roughmobsrevamped.misc.EquipHelper;
import com.p1ut0nium.roughmobsrevamped.misc.SpawnHelper;
import com.p1ut0nium.roughmobsrevamped.misc.TargetHelper;
import com.p1ut0nium.roughmobsrevamped.util.Constants;
import com.p1ut0nium.roughmobsrevamped.util.DamageSourceFog;
import com.p1ut0nium.roughmobsrevamped.util.handlers.FogState;
import com.p1ut0nium.roughmobsrevamped.util.handlers.SoundHandler;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class RoughApplier {

	public static final String FEATURES_APPLIED = Constants.unique("featuresApplied");

	public static final List<EntityFeatures> FEATURES = new ArrayList<EntityFeatures>();

	// CHANGED: new in Rough Mobs 4, the Rough Mobs 1 and 2 features Revamped had dropped
	public static final ClassicFeatures CLASSIC = new ClassicFeatures();

	// Work that has to wait until the entity is in the level, run at the end of the next server tick
	private static final Queue<Runnable> NEXT_TICK = new ConcurrentLinkedQueue<Runnable>();

	// The mobs this pass already ran on, so a second join of the same object never stacks its AI goals
	private static final Set<Mob> APPLIED = Collections.newSetFromMap(new WeakHashMap<Mob, Boolean>());

	private static boolean gameStagesEnabled;
	private static boolean playerHasAbilsStage;
	private static boolean abilsStageEnabled;
	private static boolean equipStageEnabled;
	private static boolean playerHasEquipStage;

	public RoughApplier() {
		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.register(CLASSIC);

		FEATURES.add(new ZombieFeatures());
		FEATURES.add(new SkeletonFeatures());
		FEATURES.add(new HostileHorseFeatures());
		FEATURES.add(new CreeperFeatures());
		FEATURES.add(new SlimeFeatures());
		FEATURES.add(new EndermanFeatures());
		FEATURES.add(new SpiderFeatures());
		FEATURES.add(new WitchFeatures().addPotionHandler(FEATURES));
		FEATURES.add(new SilverfishFeatures());
		FEATURES.add(new ZombiePigmanFeatures());
		FEATURES.add(new BlazeFeatures());
		FEATURES.add(new GhastFeatures());
		FEATURES.add(new MagmaCubeFeatures());
		FEATURES.add(new WitherFeatures());
		FEATURES.add(new EndermiteFeatures());
		FEATURES.add(new GuardianFeatures());
	}

	// Runs once, in the mod constructor
	public void preInit() {

		for (EntityFeatures features : FEATURES)
			features.preInit();
	}

	/*
	 * CHANGED: was RoughConfig.loadFeatures() at the end of preInit. It runs twice: once in the mod
	 * constructor to define the config, once in the config Loading event to read it (see RoughConfig, RoughMobs).
	 */
	public void loadConfig() {

		// CHANGED: new in Rough Mobs 4, the Classic Preset (see classic.ClassicPreset) and the legacy config importer
		// (see classic.LegacyConfigImporter). Both set their values before any option they change is read; the file
		// is written after the read pass (RoughConfig.writePending).
		RoughConfig.addCustomCategoryComment("Classic", "Features and defaults from the original Rough Mobs (1 and 2)");
		String preset = presetName(RoughConfig.getString("Classic", "Preset", ClassicPreset.RMR, "Default numbers to start from: rmr (Rough Mobs Revamped), rm2 (Rough Mobs 2) or rm1 (Rough Mobs 1, also turns on slimes on heads and bat vampires)\nA change is written into this file at the next start: an option still at the old preset's number takes the new one, an option you changed keeps your value\nBosses stay off whatever the preset, see _BossGlobal_BossesEnabled", true), "_ClassicPreset");
		String applied = presetName(RoughConfig.getString("Classic", "PresetApplied", ClassicPreset.RMR, "The preset whose numbers this file holds, set by Rough Mobs 4 after it writes a preset. Leave it as it is", true), "_ClassicPresetApplied");
		String importName = RoughConfig.getString("Classic", "ImportLegacyConfig", "", "A Rough Mobs 2 roughmobs.cfg or Rough Mobs Revamped 1.12.2 roughmobsrevamped.cfg in the config folder, e.g. roughmobs.cfg\nIts values are copied into this file at the next start, the report goes to config/roughmobs4-import.log, and this option is cleared", true).trim();

		if (!RoughConfig.isDefining()) {
			for (String key : RoughConfig.unknownKeys(ClassicPreset.values(preset)))
				RoughMobs.logger.warn("Rough Mobs 4: Classic Preset {} names {}, which is not a config option", preset, key);
			if (!preset.equals(applied)) {
				List<String> changed = RoughConfig.switchPreset(ClassicPreset.values(applied), ClassicPreset.values(preset));
				RoughConfig.set(RoughConfig.importantKey("Classic", "PresetApplied"), preset);
				RoughMobs.logger.info("Rough Mobs 4: Classic Preset {} set {}", preset, changed);
			}
			// After the preset, so an imported value wins
			if (!importName.isEmpty())
				LegacyConfigImporter.importConfig(importName);
		}

		for (EntityFeatures features : FEATURES)
			features.initConfig();

		CLASSIC.initConfig();
	}

	private static String presetName(String name, String key) {
		String preset = name.trim().toLowerCase(Locale.ROOT);
		if (ClassicPreset.NAMES.contains(preset))
			return preset;
		if (!RoughConfig.isDefining())
			RoughMobs.logger.warn("Rough Mobs 4: Classic {} {} is not rmr, rm2 or rm1, using rmr", key, name);
		return ClassicPreset.RMR;
	}

	public void postInit() {

		// The feature postInit methods resolve the configured names, so they wait for the read pass
		if (!RoughConfig.isDefining()) {
			for (EntityFeatures features : FEATURES)
				features.postInit();
		}

		AttributeHelper.initAttributeOption();

		SpawnHelper.initSpawnOption();
		// DROPPED: SpawnHelper.addEntries(); the biome modifier data/roughmobs4/forge/biome_modifier/spawn_entries.json adds the spawns

		BossHelper.initGlobalBossConfig();

		TargetHelper.init();

		// DROPPED: RoughConfig.saveFeatures(); ForgeConfigSpec writes the file itself
	}

	// CHANGED: new. True once the mob passed the spawn conditions (or is a boss) and got its features
	public static boolean passedSpawnConditions(Entity entity) {
		return entity.getPersistentData().getBoolean(FEATURES_APPLIED);
	}

	// CHANGED: new. passedSpawnConditions, and a feature group that is switched on lists the mob's type
	public static boolean hasFeatures(Entity entity) {
		if (!passedSpawnConditions(entity))
			return false;
		for (EntityFeatures features : FEATURES)
			if (features.isEntity(entity))
				return true;
		return false;
	}

	/*
	 * CHANGED: new. Feature code that needs the entity to be in the level (or must not touch the level while
	 * a chunk loads) queues itself here.
	 */
	public static void runNextTick(Runnable task) {
		NEXT_TICK.add(task);
	}

	@SubscribeEvent
	public void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;

		// Only what was queued before this tick; anything a task queues runs next tick
		for (int i = NEXT_TICK.size(); i > 0; i--) {
			Runnable task = NEXT_TICK.poll();
			if (task == null)
				break;
			try {
				task.run();
			} catch (RuntimeException e) {
				RoughMobs.logger.error("Rough Mobs 4: a deferred feature task failed", e);
			}
		}
	}

	@SubscribeEvent
	public void onServerStopped(ServerStoppedEvent event) {
		NEXT_TICK.clear();
	}

	/*
	 * Test if Game Stages is loaded, if any stages are enabled, and if the player has the stages
	 */
	private void getGameStages(Player player) {

		gameStagesEnabled = CompatHandler.isGameStagesLoaded();
		abilsStageEnabled = GameStagesCompat.useAbilitiesStage();
		equipStageEnabled = GameStagesCompat.useEquipmentStage();

		// Test to see if player has these stages unlocked.
		// CHANGED: no nearest player counts as not having the stages instead of throwing
		if (gameStagesEnabled && player != null) {
			playerHasEquipStage = GameStagesCompat.hasAnyOf(player, Constants.ROUGHMOBSALL, Constants.ROUGHMOBSEQUIP);
			playerHasAbilsStage = GameStagesCompat.hasAnyOf(player, Constants.ROUGHMOBSALL, Constants.ROUGHMOBSABILS);
		} else {
			playerHasEquipStage = playerHasAbilsStage = false;
		}
	}

	private void addAttributes(Mob entity) {
		// Test to see if abilities stage is disabled or if it is enabled and player has it
		if (abilsStageEnabled == false || abilsStageEnabled && playerHasAbilsStage) {
			AttributeHelper.addAttributes(entity);
		}
	}

	/*
	 * Add equipment and enchantments, and AI
	 * Also try to create bosses
	 */
	private void addFeatures(EntityJoinLevelEvent event, Mob entity) {

		boolean isBoss = entity instanceof IBoss;

		// DROPPED: the EntityPlayer class check; players never reach this method

		// Loop through the features list and add equipment and AI to the entity
		for (EntityFeatures features : FEATURES)
		{
			if (features.isEntity(entity))
			{
				// Don't attempt to add equipment to a boss. It has already been given equipment in the BossApplier class
				// Also test if baby zombies should have equipment
				if (!isBoss || entity.isBaby() && EquipHelper.disableBabyZombieEquipment() == false) {
					// Test to see if equip stage is disabled or if it is enabled and player has it
					if (equipStageEnabled == false || equipStageEnabled && playerHasEquipStage) {

						if (!entity.getPersistentData().getBoolean(FEATURES_APPLIED))
							features.addFeatures(event, entity);
					}
				}

				// Test to see if abilities stage is disabled or if it is enabled and player has it
				if (abilsStageEnabled == false || abilsStageEnabled && playerHasAbilsStage) {
					features.addAI(event, entity, entity.goalSelector, entity.targetSelector);
				}
			}
		}
	}

	//TODO Move this to fog handler?
	@SubscribeEvent
	public void onEntityHurt(LivingAttackEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			if (!player.isCreative()) {
				if (event.getSource().is(DamageSourceFog.POISONOUS_FOG)) {

					if (BossHelper.bossFogPlayerCough)
						playHurtSound(player);
				}
			}
		}
	}

	private void playHurtSound(Player player) {
		player.level().playSound(null, player.blockPosition(), SoundHandler.ENTITY_PLAYER_COUGH.get(), SoundSource.PLAYERS, 1.0F, (float)Math.max(0.75, Math.random()));
	}

	/*
	 * When an entity spawns, we do all the magic, such as adding equipment and AI, trying to turn it into a boss, etc.
	 */
	@SubscribeEvent
	public void onEntitySpawn(EntityJoinLevelEvent event) {

		Entity entity = event.getEntity();

		if (entity instanceof Player) {
			FogState.playerRespawned = true;
		}

		// Ignore spawn if on the client side, or if entity is the player.
		if (event.getLevel().isClientSide || entity instanceof Player)
			return;

		// CHANGED: a thrown potion is not a mob, so the witch potion handler gets it here
		if (entity instanceof ThrownPotion potion) {
			for (EntityFeatures features : FEATURES) {
				if (features instanceof WitchPotionFeatures potionFeatures && features.isEntity(potion))
					potionFeatures.onPotionJoin(potion);
			}
			return;
		}

		if (!(entity instanceof Mob))
			return;

		Mob mob = (Mob) entity;

		// CHANGED: an entity loaded from disk joins while its chunk is loading, when reading the level around it
		// can deadlock the server, so it is handled at the end of the tick with no event
		if (event.loadedFromDisk()) {
			runNextTick(() -> {
				if (mob.isAlive() && !mob.isRemoved())
					apply(null, mob);
			});
		}
		else {
			apply(event, mob);
		}
	}

	private void apply(EntityJoinLevelEvent event, Mob entity) {

		if (!APPLIED.add(entity))
			return;

		boolean isBoss = entity instanceof IBoss;
		boolean canSpawn = SpawnHelper.checkSpawnConditions(entity, event);

		// If enabled and the entity can spawn, add additional targets from config
		// Also add additional targets if the entity is ignoring spawn conditions
		if (TargetHelper.targetAttackerEnabled() && canSpawn || TargetHelper.targetAttackerEnabled() && TargetHelper.ignoreSpawnConditions)
			TargetHelper.setTargets(entity);

		// If entity failed spawn conditions, and isn't a boss, then exit event and spawn vanilla mob with no features added
		if (!isBoss && !canSpawn)
			return;

		getGameStages(entity.level().getNearestPlayer(entity, -1.0D));
		addAttributes(entity);
		addFeatures(event, entity);

		entity.getPersistentData().putBoolean(FEATURES_APPLIED, true);
	}

	/*
	 * CHANGED: 1.12.2 ran onAttack and onDefend from LivingAttackEvent. The features that change the damage
	 * dealt need LivingDamageEvent in 1.20.1, so onAttack and onDefend run here and the features that cancel
	 * the attack outright use onDefendAttack, below.
	 */
	@SubscribeEvent
	public void onAttack(LivingDamageEvent event) {

		Entity trueSource = event.getSource().getEntity();
		Entity immediateSource = event.getSource().getDirectEntity();
		Entity target = event.getEntity();

		if (trueSource == null || target == null || target instanceof FakePlayer || trueSource instanceof FakePlayer || immediateSource instanceof FakePlayer || target.level().isClientSide)
			return;

		for (EntityFeatures features : FEATURES) {
			if (trueSource instanceof Mob && features.isEntity(trueSource) && !(target instanceof Player && ((Player)target).isCreative())) {
				features.onAttack(trueSource, immediateSource, target, event);
			}

			if (target instanceof Mob && features.isEntity(target) && !(trueSource instanceof Player && ((Player)trueSource).isCreative())) {
				features.onDefend(target, trueSource, immediateSource, event);
			}
		}
	}

	@SubscribeEvent
	public void onDefendAttack(LivingAttackEvent event) {

		Entity trueSource = event.getSource().getEntity();
		Entity immediateSource = event.getSource().getDirectEntity();
		Entity target = event.getEntity();

		if (trueSource == null || target == null || target instanceof FakePlayer || trueSource instanceof FakePlayer || immediateSource instanceof FakePlayer || target.level().isClientSide)
			return;

		for (EntityFeatures features : FEATURES) {
			if (target instanceof Mob && features.isEntity(target) && !(trueSource instanceof Player && ((Player)trueSource).isCreative())) {
				features.onDefendAttack(target, trueSource, immediateSource, event);
			}
		}
	}

	@SubscribeEvent
	public void onDeath(LivingDeathEvent event) {

		Entity deadEntity = event.getEntity();

		if (deadEntity == null || deadEntity.level().isClientSide || !(deadEntity instanceof Mob))
			return;

		for (EntityFeatures features : FEATURES) {
			if (features.isEntity(deadEntity)) {
				features.onDeath(deadEntity, event.getSource());
			}
		}
	}

	@SubscribeEvent
	public void onFall(LivingFallEvent event) {

		Entity entity = event.getEntity();

		if (entity == null || entity.level().isClientSide || !(entity instanceof Mob))
			return;

		for (EntityFeatures features : FEATURES) {
			if (features.isEntity(entity)) {
				features.onFall(entity, event);
			}
		}
	}

	@SubscribeEvent
	public void onBlockBreak(BlockEvent.BreakEvent event) {

		Player player = event.getPlayer();

		if (player == null || player.level().isClientSide || player.isCreative())
			return;

		for (EntityFeatures features : FEATURES) {
			features.onBlockBreak(player, event);
		}
	}

	/*
	 * When an entity targets another entity
	 * CHANGED: LivingSetAttackTargetEvent is LivingChangeTargetEvent, which can replace the target itself, so
	 * the redirect goes through setNewTarget instead of a second setAttackTarget call from inside the event.
	 */
	@SubscribeEvent
	public void onTarget(LivingChangeTargetEvent event) {

		LivingEntity newTarget = event.getNewTarget();

		if (!TargetHelper.targetBlockerEnabled() || newTarget == null || !(newTarget instanceof Mob) || !(event.getEntity() instanceof Mob))
			return;

		Mob attacker = (Mob) event.getEntity();

		// The invalid attacker cannot attack the target
		if (TargetHelper.isTargetBlocked(attacker, newTarget)) {
			Player player = attacker.level().getNearestPlayer(TargetingConditions.forCombat().range(32.0D), attacker);

			// CHANGED: 1.12.2 returned here, which let the blocked target through when the nearest player was a teammate
			if (player == null || player.isAlliedTo(attacker)) {
				event.setNewTarget(null);
				return;
			}

			attacker.setLastHurtByMob(player);
			event.setNewTarget(player);

			// DROPPED: System.out.println("invalidAttacker = ...; blocked target")
		}
	}
}
