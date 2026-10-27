package com.p1ut0nium.roughmobsrevamped.entities;

import java.util.ArrayList;
import java.util.List;

import com.p1ut0nium.roughmobsrevamped.client.BossClientEffects;
import com.p1ut0nium.roughmobsrevamped.init.EntityInit;
import com.p1ut0nium.roughmobsrevamped.misc.BossFogTicker;
import com.p1ut0nium.roughmobsrevamped.misc.BossHelper;
import com.p1ut0nium.roughmobsrevamped.util.handlers.FogState;
import com.p1ut0nium.roughmobsrevamped.util.handlers.SoundHandler;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.DistExecutor;

public class BossZombie extends Zombie implements IBoss {

	// Bat Swarm variables
	// CHANGED: the 1.12.2 config copies were statics filled at class load (before the config was read) and the
	// swarm timer was static, shared by every boss zombie; the config is read live and the timer is per boss
	private final List<EntityHostileBat> batMinions = new ArrayList<EntityHostileBat>();
	private int batSwarmTick;

	// Fog variables
	private final BossFogTicker fog = new BossFogTicker();

	public BossZombie(EntityType<? extends Zombie> type, Level worldIn) {
		super(type, worldIn);
		this.xpReward = 100;

		batSwarmTick = 0;
	}

	/*
	 * CHANGED: 1.12.2 struck the lightning on the client whenever the boss was added to a client world, using the
	 * client's config, so it struck again each time the boss came back into view. The server decides once, at
	 * spawn (BossHelper.spawnBoss), and sends LIGHTNING_EVENT.
	 */
	@Override
	public void onAddedToWorld() {
		super.onAddedToWorld();

		despawnBatMinions();
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == IBoss.LIGHTNING_EVENT)
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BossClientEffects.spawnLightning(this));
		else
			super.handleEntityEvent(id);
	}

	@Override
	public void aiStep() {

		if (this.level().isClientSide) {
			for (int i = 0; i < 2; ++i) {
				this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), 0.0D, 0.0D, 0.0D);
			}
		}

		// CHANGED: a boss left in a world after bosses were switched off keeps its name and gear, but no swarm or fog
		if (!this.level().isClientSide && BossHelper.bossesEnabled) {

			Player closestPlayer = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), BossHelper.bossFogMaxDistance, EntitySelector.NO_CREATIVE_OR_SPECTATOR);

			// Bat Swarm can only be fired every BATSWARM_DELAY ticks
			batSwarmTick = Math.max(batSwarmTick - 1, 0);

			if (batSwarmTick == 0) {
				batSwarmTick = BossHelper.bossBatSwarmDelay * 20;

				if (BossHelper.bossBatSwarmEnabled && closestPlayer != null && this.hasLineOfSight(closestPlayer) && closestPlayer.distanceTo(this) <= BossHelper.bossBatSwarmRange) {
					if (batMinions.isEmpty()) {
						for (int i = 0; i < BossHelper.bossBatSwarmCount; i++) {
							EntityHostileBat bat = EntityInit.HOSTILE_BAT.get().create(this.level());
							if (bat == null)
								continue;
							bat.setPos(this.getX() + Math.random() - Math.random(), this.getY() + Math.random(), this.getZ() + Math.random() - Math.random());
							ForgeEventFactory.onFinalizeSpawn(bat, (ServerLevel) this.level(), this.level().getCurrentDifficultyAt(this.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);

							// CHANGED: the boss link is set before the bat joins the level, so its target goal never sees a bat without a boss
							bat.setIsBatHanging(false);
							bat.setBoss(this);

							this.level().addFreshEntity(bat);

							batMinions.add(bat);
						}
						playSoundBatSwarm();
					}
				}
			}

			fog.tick(this);
		}

		// Remove any dead bats from the group of batMinions
		// CHANGED: 1.12.2 removed at most one dead bat per tick from inside a for-each
		if (!this.level().isClientSide)
			batMinions.removeIf(bat -> !bat.isAlive());

		super.aiStep();
	}

	private void playSoundBatSwarm() {
		if (!level().isClientSide)
			this.playSound(SoundHandler.ENTITY_BOSS_BATSWARM.get(), 1.0F, 1.0F);
	}

	@Override
	public void die(DamageSource cause) {
		super.die(cause);

		despawnBatMinions();

		FogState.bossDied = true;
	}

	public void removeBatMinion(EntityHostileBat bat) {
		bat.discard();
		batMinions.remove(bat);
	}

	public void despawnBatMinions() {
		// Max out batSwarmTick so no more bats can spawn
		batSwarmTick = BossHelper.bossBatSwarmDelay * 20;

		// Kill all bat minions when boss dies
		// CHANGED: 1.12.2 removed from the list it was iterating, which threw a ConcurrentModificationException
		// whenever the boss died with more than one bat out
		for (EntityHostileBat bat : batMinions)
			bat.discard();
		batMinions.clear();
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	// CHANGED: 1.12.2 had no drowned; a boss zombie that converted underwater would become a plain drowned
	@Override
	protected boolean convertsInWater() {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundHandler.ENTITY_BOSS_IDLE.get();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundHandler.ENTITY_BOSS_DEATH.get();
	}

	@Override
	protected ResourceLocation getDefaultLootTable() {
		return EntityType.ZOMBIE.getDefaultLootTable();
	}
}
