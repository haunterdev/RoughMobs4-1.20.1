package com.p1ut0nium.roughmobsrevamped.entities;

import javax.annotation.Nullable;

import com.p1ut0nium.roughmobsrevamped.ai.combat.RoughAIBatAttack;
import com.p1ut0nium.roughmobsrevamped.ai.combat.RoughAIBatTarget;
import com.p1ut0nium.roughmobsrevamped.ai.misc.RoughAIBatFlight;
import com.p1ut0nium.roughmobsrevamped.misc.BossHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class EntityHostileBat extends Monster {

	// CHANGED: 1.12.2 created this key against EntityBat.class, which shifted the vanilla bat's own data ids
	private static final EntityDataAccessor<Byte> HANGING = SynchedEntityData.defineId(EntityHostileBat.class, EntityDataSerializers.BYTE);

	private BossZombie boss;

	public EntityHostileBat(EntityType<? extends EntityHostileBat> type, Level worldIn) {
		super(type, worldIn);
		this.setIsBatHanging(false);

		// CHANGED: 1.20.1 attributes are built once at registration, before the config is read, so the
		// configured damage and health go on as base values here (applyEntityAttributes did this per bat)
		this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(BossHelper.bossBatSwarmDamage);
		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(BossHelper.bossBatSwarmHealth);
		this.setHealth(this.getMaxHealth());
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes();
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new RoughAIBatAttack(this));
		this.goalSelector.addGoal(2, new RoughAIBatFlight(this));
		this.targetSelector.addGoal(1, new RoughAIBatTarget(this));
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(HANGING, (byte)0);
	}

	public void setBoss(BossZombie boss) {
		this.boss = boss;
	}

	public BossZombie getBoss() {
		return this.boss;
	}

	// Returns the volume for the sounds this mob makes.
	@Override
	protected float getSoundVolume() {
		return 0.1F;
	}

	// Gets the pitch of living sounds in living entities.
	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.95F;
	}

	@Nullable
	@Override
	public SoundEvent getAmbientSound() {
		return this.getIsBatHanging() && this.random.nextInt(4) != 0 ? null : SoundEvents.BAT_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
		return SoundEvents.BAT_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.BAT_DEATH;
	}

	// Returns true if this entity should push and be pushed by other entities when colliding.
	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entityIn) {
	}

	@Override
	protected void pushEntities() {
	}

	public boolean getIsBatHanging() {
		return (this.entityData.get(HANGING) & 1) != 0;
	}

	public void setIsBatHanging(boolean isHanging) {
		byte b0 = this.entityData.get(HANGING);

		if (isHanging) {
			this.entityData.set(HANGING, (byte)(b0 | 1));
		}
		else {
			this.entityData.set(HANGING, (byte)(b0 & -2));
		}
	}

	// Called to update the entity's position/logic.
	@Override
	public void tick() {
		super.tick();

		if (this.getIsBatHanging()) {
			this.setDeltaMovement(Vec3.ZERO);
			this.setPosRaw(this.getX(), (double)Mth.floor(this.getY()) + 1.0D - (double)this.getBbHeight(), this.getZ());
		}
		else {
			this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.6D, 1.0D));
		}
	}

	// canTriggerWalking false: no step sounds and no game events from moving
	@Override
	protected Entity.MovementEmission getMovementEmission() {
		return Entity.MovementEmission.NONE;
	}

	@Override
	public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
		return false;
	}

	@Override
	protected void checkFallDamage(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
	}

	// Return whether this entity should NOT trigger a pressure plate or a tripwire.
	@Override
	public boolean isIgnoringBlockTriggers() {
		return true;
	}

	// Called when the entity is attacked.
	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (this.isInvulnerableTo(source)) {
			return false;
		}
		else {
			if (!this.level().isClientSide && this.getIsBatHanging()) {
				this.setIsBatHanging(false);
			}

			return super.hurt(source, amount);
		}
	}

	// DROPPED: registerFixesBat (data fixers are vanilla only in 1.20.1)

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		this.entityData.set(HANGING, compound.getByte("BatFlags"));
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putByte("BatFlags", this.entityData.get(HANGING));
	}

	@Override
	protected float getStandingEyeHeight(Pose pose, EntityDimensions size) {
		return size.height / 2.0F;
	}

	@Override
	protected ResourceLocation getDefaultLootTable() {
		return EntityType.BAT.getDefaultLootTable();
	}
}
