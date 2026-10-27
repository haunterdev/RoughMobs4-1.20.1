package com.p1ut0nium.roughmobsrevamped.entities;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/*
 * CHANGED: new in Rough Mobs 4. Rough Mobs Revamped 1.16.5 registered a husk_champion (HuskChampionEntity) and never
 * spawned it. This boss is built on BossZombie, so the bat swarm, fog, boss babies, gear, title and lightning event
 * are the boss zombie's and the three bosses match. From the vanilla husk: no sun burn, hunger on an empty-handed
 * hit, the husk hurt and step sounds (the idle and death sounds stay the boss ones), no head from a charged creeper,
 * and the husk loot table. A husk rolls it with the zombie _BossChance (BossHelper.rollBoss).
 */
public class BossHusk extends BossZombie {

	public BossHusk(EntityType<? extends Zombie> type, Level worldIn) {
		super(type, worldIn);
	}

	@Override
	protected boolean isSunSensitive() {
		return false;
	}

	// Vanilla Husk.doHurtTarget
	@Override
	public boolean doHurtTarget(Entity target) {
		boolean hit = super.doHurtTarget(target);
		if (hit && this.getMainHandItem().isEmpty() && target instanceof LivingEntity living) {
			float difficulty = this.level().getCurrentDifficultyAt(this.blockPosition()).getEffectiveDifficulty();
			living.addEffect(new MobEffectInstance(MobEffects.HUNGER, 140 * (int) difficulty), this);
		}
		return hit;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.HUSK_HURT;
	}

	@Override
	protected SoundEvent getStepSound() {
		return SoundEvents.HUSK_STEP;
	}

	@Override
	protected ItemStack getSkull() {
		return ItemStack.EMPTY;
	}

	@Override
	protected ResourceLocation getDefaultLootTable() {
		return EntityType.HUSK.getDefaultLootTable();
	}
}
