package com.p1ut0nium.roughmobsrevamped.classic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.p1ut0nium.roughmobsrevamped.RoughApplier;
import com.p1ut0nium.roughmobsrevamped.RoughMobs;
import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;
import com.p1ut0nium.roughmobsrevamped.entities.BossHusk;
import com.p1ut0nium.roughmobsrevamped.entities.BossSkeleton;
import com.p1ut0nium.roughmobsrevamped.entities.BossZombie;
import com.p1ut0nium.roughmobsrevamped.features.ZombieFeatures;
import com.p1ut0nium.roughmobsrevamped.misc.BossHelper;
import com.p1ut0nium.roughmobsrevamped.misc.SpawnHelper;
import com.p1ut0nium.roughmobsrevamped.util.Constants;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/*
 * CHANGED: new in Rough Mobs 4. Features Rough Mobs Revamped does not have, as Forge-bus handlers:
 *   slime OnHead / OnHeadNausea   small slimes and magma cubes ride the player's head
 *   bat Vampire                   vanilla bats ride the player and drink half a heart a second
 *   Classic SnowballMeltInNether  snowballs fizz out in ultra-warm dimensions
 *   zombie BossBabyChance         a hit boss zombie calls a baby zombie
 *   skeleton BossExplosion        a boss skeleton's hit explodes at the player
 *   Classic FireImmune            listed mobs ignore fire damage
 *   Classic ReduceSpawns          cancels a share of natural spawns per mob and dimension
 * The Classic Preset itself is read in RoughApplier.loadConfig. Smaller additions live in their feature class,
 * marked "CHANGED: new in Rough Mobs 4".
 */
public class ClassicFeatures {

	private static final Logger LOG = LogUtils.getLogger();
	private static final Random RND = new Random();
	public static final String RIDERS = Constants.unique("riders");

	private boolean slimeOnHead;
	private boolean slimeOnHeadNausea;
	private boolean batVampire;
	private boolean snowballMelt;
	private int zombieBossBabyChance;
	private float skeletonBossExplosion;
	private String[] fireImmuneNames;
	private String[] reduceSpawnNames;

	private final Set<EntityType<?>> fireImmune = new HashSet<>();
	private final List<SpawnReduction> spawnReductions = new ArrayList<>();

	private record SpawnReduction(EntityType<?> type, int percent, ResourceLocation dimension) {
	}

	public void initConfig() {
		RoughConfig.addCustomCategoryComment("bat", "Configuration options which affect vanilla bats");

		slimeOnHead = RoughConfig.getBoolean("slime", "OnHead", false, "Set to true to let the smallest slimes and magma cubes jump onto a player's head and ride it");
		slimeOnHeadNausea = RoughConfig.getBoolean("slime", "OnHeadNausea", true, "Set to false to keep the nausea off while a slime rides a player's head\nThe rider still slows the player down");
		batVampire = RoughConfig.getBoolean("bat", "Vampire", false, "Set to true to let bats latch onto players and drink half a heart every second until killed");
		zombieBossBabyChance = RoughConfig.getInteger("zombie", "BossBabyChance", 5, 0, Short.MAX_VALUE, "Chance (1 in X) per hit taken that a boss zombie calls a baby zombie to its side\nSet to 0 to disable this feature");
		skeletonBossExplosion = RoughConfig.getFloat("skeleton", "BossExplosion", 1.0F, 0F, 10F, "Strength of the explosion a boss skeleton's hit sets off at the player\nSet to 0 to disable this feature");

		snowballMelt = RoughConfig.getBoolean("Classic", "SnowballMeltInNether", true, "Snowballs melt in the Nether (any ultra-warm dimension) before they fly");
		fireImmuneNames = RoughConfig.getStringArray("Classic", "FireImmune", new String[0], "Entities which take no fire, lava or burning damage\nFormat: modid:entity, e.g. minecraft:zombie");
		reduceSpawnNames = RoughConfig.getStringArray("Classic", "ReduceSpawns", new String[0], "Share of natural spawns to cancel per entity, optionally in one dimension only\nFormat: modid:entity;percent or modid:entity;percent;dimension\ne.g. minecraft:creeper;50 or minecraft:zombie;25;minecraft:the_nether (-1, 0 and 1 mean the Nether, the Overworld and the End)");

		if (!RoughConfig.isDefining())
			parseLists();
	}

	private void parseLists() {
		fireImmune.clear();
		for (String name : fireImmuneNames) {
			EntityType<?> type = entityType(name.trim());
			if (type == null)
				LOG.warn("Rough Mobs 4: Classic FireImmune entry {} is not an entity", name);
			else
				fireImmune.add(type);
		}

		spawnReductions.clear();
		for (String entry : reduceSpawnNames) {
			String[] parts = entry.split(";");
			EntityType<?> type = parts.length >= 2 ? entityType(parts[0].trim()) : null;
			int percent = -1;
			try {
				percent = parts.length >= 2 ? Integer.parseInt(parts[1].trim()) : -1;
			}
			catch (NumberFormatException e) {
				// reported below
			}
			ResourceLocation dimension = parts.length >= 3 ? dimension(parts[2].trim()) : null;
			if (type == null || percent < 0 || percent > 100 || (parts.length >= 3 && dimension == null)) {
				LOG.warn("Rough Mobs 4: Classic ReduceSpawns entry {} is not modid:entity;percent[;dimension]", entry);
				continue;
			}
			spawnReductions.add(new SpawnReduction(type, percent, dimension));
		}
	}

	private static EntityType<?> entityType(String name) {
		ResourceLocation key = ResourceLocation.tryParse(name);
		if (key == null || !ForgeRegistries.ENTITY_TYPES.containsKey(key))
			return null;
		return ForgeRegistries.ENTITY_TYPES.getValue(key);
	}

	// Old configs named dimensions by number
	private static ResourceLocation dimension(String name) {
		switch (name) {
			case "-1": return Level.NETHER.location();
			case "0": return Level.OVERWORLD.location();
			case "1": return Level.END.location();
			default: return ResourceLocation.tryParse(name);
		}
	}

	@SubscribeEvent
	public void onLivingTick(LivingEvent.LivingTickEvent event) {
		LivingEntity entity = event.getEntity();
		if (entity.level().isClientSide)
			return;

		// A slime needs the slime or magma cube group to take it (FeaturesEnabled, Entities, spawn conditions);
		// bats have no group, so a bat needs the spawn conditions only
		if (slimeOnHead && entity instanceof Slime)
			tickRider(entity, ((Slime) entity).getSize() <= 1 && RoughApplier.hasFeatures(entity), entity.getBoundingBox(), 10);
		else if (batVampire && entity.getType() == EntityType.BAT)
			tickRider(entity, RoughApplier.passedSpawnConditions(entity), entity.getBoundingBox().inflate(0.25D), 20);
	}

	/*
	 * The rider mounts a player whose box it touches, then works
	 * on the player every interval ticks. It lets go once the player dies, leaves, switches to creative or
	 * spectator, or already carries something else.
	 */
	private void tickRider(LivingEntity rider, boolean canMount, AABB reach, int interval) {
		if (rider.getVehicle() instanceof Player) {
			Player player = (Player) rider.getVehicle();
			if (!canCarry(player, rider)) {
				rider.stopRiding();
				return;
			}
			if (rider.tickCount % interval == 0)
				feed(rider, player);
			return;
		}

		if (!canMount || rider.isPassenger() || rider.isVehicle() || !rider.isAlive())
			return;

		for (Player player : rider.level().getEntitiesOfClass(Player.class, reach, p -> canCarry(p, rider))) {
			rider.startRiding(player, true);
			return;
		}
	}

	/*
	 * CHANGED: new in Rough Mobs 4. Vanilla saves the vehicle a player rides, never the player's passengers, so a
	 * rider was let go at logout. It is saved with the player (PlayerPersisted, RIDERS) and put back at login. A
	 * dimension change still leaves it behind: vanilla ejects passengers on teleport.
	 */
	@SubscribeEvent
	public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		Player player = event.getEntity();
		if (player.level().isClientSide)
			return;

		ListTag riders = new ListTag();
		for (Entity passenger : new ArrayList<>(player.getPassengers())) {
			if (!(passenger instanceof Slime) && passenger.getType() != EntityType.BAT)
				continue;
			CompoundTag tag = new CompoundTag();
			if (passenger.saveAsPassenger(tag))
				riders.add(tag);
			passenger.discard();
		}
		if (!riders.isEmpty())
			persisted(player).put(RIDERS, riders);
	}

	@SubscribeEvent
	public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		Player player = event.getEntity();
		CompoundTag data = player.getPersistentData();
		if (player.level().isClientSide || !data.getCompound(Player.PERSISTED_NBT_TAG).contains(RIDERS, Tag.TAG_LIST))
			return;

		CompoundTag persisted = persisted(player);
		ListTag riders = persisted.getList(RIDERS, Tag.TAG_COMPOUND);
		// Removed first, so a rider is never put back twice
		persisted.remove(RIDERS);
		for (int i = 0; i < riders.size(); i++) {
			CompoundTag tag = riders.getCompound(i);
			Entity rider = EntityType.create(tag, player.level()).orElse(null);
			if (rider == null) {
				RoughMobs.logger.warn("Rough Mobs 4: could not put back the {} riding {}", tag.getString("id"), player.getGameProfile().getName());
				continue;
			}
			rider.moveTo(player.getX(), player.getY(), player.getZ(), rider.getYRot(), rider.getXRot());
			if (!player.level().addFreshEntity(rider)) {
				RoughMobs.logger.warn("Rough Mobs 4: could not put back the {} riding {}", tag.getString("id"), player.getGameProfile().getName());
				continue;
			}
			// With the feature switched off since, the rider is set down next to the player
			if (rider instanceof Slime ? slimeOnHead : batVampire)
				rider.startRiding(player, true);
		}
	}

	private static CompoundTag persisted(Player player) {
		CompoundTag data = player.getPersistentData();
		if (!data.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND))
			data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
		return data.getCompound(Player.PERSISTED_NBT_TAG);
	}

	private static boolean canCarry(Player player, Entity rider) {
		if (!player.isAlive() || player.isRemoved() || player.isCreative() || player.isSpectator())
			return false;
		// One rider per player, so riders never stack into a tower
		for (Entity passenger : player.getPassengers())
			if (passenger != rider)
				return false;
		return true;
	}

	private void feed(LivingEntity rider, Player player) {
		if (rider instanceof Bat) {
			player.hurt(rider.damageSources().mobAttack(rider), 0.5F);
			((ServerLevel) rider.level()).sendParticles(ParticleTypes.DAMAGE_INDICATOR, player.getX(), player.getY(0.5D), player.getZ(), 3, 0.2D, 0.2D, 0.2D, 0.0D);
			// The drinking sound at the player, PLAYERS category, volume 0.7, pitch 1
			rider.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.7F, 1.0F);
		}
		else {
			if (slimeOnHeadNausea)
				player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120));
			player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
		}
	}

	// A snowball in an ultra-warm dimension is removed with smoke and a fizz
	@SubscribeEvent
	public void onJoin(EntityJoinLevelEvent event) {
		if (!snowballMelt || !(event.getEntity() instanceof Snowball) || !(event.getLevel() instanceof ServerLevel))
			return;

		ServerLevel level = (ServerLevel) event.getLevel();
		if (!level.dimensionType().ultraWarm())
			return;

		Entity snowball = event.getEntity();
		event.setCanceled(true);
		level.sendParticles(ParticleTypes.SMOKE, snowball.getX(), snowball.getY(), snowball.getZ(), 4, 0.1D, 0.1D, 0.1D, 0.0D);
		level.playSound(null, snowball.getX(), snowball.getY(), snowball.getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.NEUTRAL, 0.5F, 1.0F);
	}

	@SubscribeEvent
	public void onDamage(LivingDamageEvent event) {
		LivingEntity target = event.getEntity();
		if (target.level().isClientSide)
			return;
		Entity attacker = event.getSource().getEntity();

		// Both are boss abilities, so a boss left in a world after bosses were switched off has neither
		if (!BossHelper.bossesEnabled)
			return;

		// No baby for the hit that kills the boss.
		if (zombieBossBabyChance > 0 && target instanceof BossZombie && attacker instanceof LivingEntity
				&& event.getAmount() < target.getHealth()
				&& !SpawnHelper.disableBabyZombies() && RND.nextInt(zombieBossBabyChance) == 0)
			spawnBossBaby((BossZombie) target);

		// The explosion runs after this hit has finished, and its own damage is an
		// explosion, so it never sets off another one.
		if (skeletonBossExplosion > 0 && attacker instanceof BossSkeleton && target instanceof Player
				&& !((Player) target).isCreative() && !target.isSpectator() && !event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
			float strength = skeletonBossExplosion;
			RoughApplier.runNextTick(() -> {
				if (attacker.isAlive() && target.isAlive() && target.level() == attacker.level())
					target.level().explode(attacker, target.getX(), target.getY(), target.getZ(), strength, Level.ExplosionInteraction.MOB);
			});
		}
	}

	private void spawnBossBaby(BossZombie boss) {
		if (!(boss.level() instanceof ServerLevel))
			return;
		ServerLevel level = (ServerLevel) boss.level();
		// A husk boss has husk babies
		Zombie baby = (boss instanceof BossHusk ? EntityType.HUSK : EntityType.ZOMBIE).create(level);
		if (baby == null)
			return;

		baby.moveTo(boss.getX() + RND.nextInt(7) - 3, boss.getY(), boss.getZ() + RND.nextInt(7) - 3, RND.nextFloat() * 360F, 0F);
		if (!level.noCollision(baby))
			baby.moveTo(boss.getX(), boss.getY(), boss.getZ(), RND.nextFloat() * 360F, 0F);
		// The group data makes it a baby and keeps finalizeSpawn from putting it on a chicken
		baby.finalizeSpawn(level, level.getCurrentDifficultyAt(baby.blockPosition()), MobSpawnType.MOB_SUMMONED, new Zombie.ZombieGroupData(true, false), null);
		baby.setBaby(true);
		baby.getPersistentData().putBoolean(ZombieFeatures.BOSS_MINION, true);
		level.addFreshEntity(baby);
	}

	// Classic FireImmune
	@SubscribeEvent
	public void onAttack(LivingAttackEvent event) {
		if (fireImmune.isEmpty())
			return;
		LivingEntity entity = event.getEntity();
		if (!entity.level().isClientSide && fireImmune.contains(entity.getType()) && event.getSource().is(DamageTypeTags.IS_FIRE)) {
			event.setCanceled(true);
			entity.clearFire();
		}
	}

	// Classic ReduceSpawns
	@SubscribeEvent
	public void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
		if (spawnReductions.isEmpty())
			return;
		if (event.getSpawnType() != MobSpawnType.NATURAL && event.getSpawnType() != MobSpawnType.CHUNK_GENERATION)
			return;

		EntityType<?> type = event.getEntity().getType();
		ResourceLocation dimension = event.getLevel().getLevel().dimension().location();
		for (SpawnReduction reduction : spawnReductions) {
			if (reduction.type() == type && (reduction.dimension() == null || reduction.dimension().equals(dimension))) {
				if (RND.nextInt(100) < reduction.percent())
					event.setSpawnCancelled(true);
				return;
			}
		}
	}
}
