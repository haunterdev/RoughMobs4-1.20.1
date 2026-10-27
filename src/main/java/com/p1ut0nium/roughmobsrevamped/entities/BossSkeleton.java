package com.p1ut0nium.roughmobsrevamped.entities;

import com.p1ut0nium.roughmobsrevamped.client.BossClientEffects;
import com.p1ut0nium.roughmobsrevamped.misc.BossFogTicker;
import com.p1ut0nium.roughmobsrevamped.misc.BossHelper;
import com.p1ut0nium.roughmobsrevamped.util.handlers.FogState;
import com.p1ut0nium.roughmobsrevamped.util.handlers.SoundHandler;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class BossSkeleton extends Skeleton implements IBoss {

	// Fog variables
	// CHANGED: the 1.12.2 skeleton fog hit for 0 damage (the zombie used BossFogDoTDamage); both use the config now
	private final BossFogTicker fog = new BossFogTicker();

	public BossSkeleton(EntityType<? extends Skeleton> type, Level worldIn) {
		super(type, worldIn);
		this.xpReward = 100;
	}

	// CHANGED: the lightning comes from the server once, at spawn (see BossZombie.onAddedToWorld)
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
				this.level().addParticle(ParticleTypes.FLAME, this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), 0.0D, 0.0D, 0.0D);
			}
		}

		// CHANGED: no fog from a boss left in a world after bosses were switched off
		if (!this.level().isClientSide && BossHelper.bossesEnabled)
			fog.tick(this);

		super.aiStep();
	}

	@Override
	public void die(DamageSource cause) {
		super.die(cause);

		// DROPPED: TODO "Add custom death effects" (no code in the original)

		FogState.bossDied = true;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	// CHANGED: 1.12.2 had no powder snow; a boss skeleton that froze would become a plain stray
	@Override
	protected void doFreezeConversion() {
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundHandler.ENTITY_BOSS_IDLE.get();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundHandler.ENTITY_BOSS_DEATH.get();
	}

	// CHANGED: the 1.12.2 skeleton boss used the zombie loot table (a copy and paste slip), so it dropped rotten flesh
	@Override
	protected ResourceLocation getDefaultLootTable() {
		return EntityType.SKELETON.getDefaultLootTable();
	}
}
