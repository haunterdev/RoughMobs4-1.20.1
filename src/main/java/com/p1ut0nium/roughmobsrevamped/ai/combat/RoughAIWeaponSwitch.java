package com.p1ut0nium.roughmobsrevamped.ai.combat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RoughAIWeaponSwitch extends Goal {

	protected Mob entity;
	// The 1.12.2 near and far bounds are compared against the squared distance (so 12 swaps at about 3.3 blocks); kept as is
	protected double rangeNear, rangeFar;
	protected ItemStack melee, ranged;

	public RoughAIWeaponSwitch(Mob entity, double range) {
		this.entity = entity;
		rangeNear = range * 0.9;
		rangeFar = range * 1.1;

		ranged = entity.getMainHandItem();
		melee = entity.getOffhandItem();

		if (melee.getItem() == Items.BOW) {
			ItemStack swap = melee;
			melee = ranged;
			ranged = swap;
		}
	}

	@Override
	public boolean canUse() {
		return !melee.isEmpty() && !ranged.isEmpty();
	}

	@Override
	public void start() {
		LivingEntity target = entity.getTarget();
		if (target == null || !entity.getSensing().hasLineOfSight(target)) return;

		double distSq = entity.distanceToSqr(target);

		if (distSq < rangeNear) {
			if (entity.getMainHandItem() == ranged) {
				entity.setItemSlot(EquipmentSlot.MAINHAND, melee);
				entity.setItemSlot(EquipmentSlot.OFFHAND, ranged);
			}
		}
		else if (distSq > rangeFar) {
			if (entity.getMainHandItem() == melee) {
				entity.setItemSlot(EquipmentSlot.MAINHAND, ranged);
				entity.setItemSlot(EquipmentSlot.OFFHAND, melee);
			}
		}
	}

	@Override
	public boolean canContinueToUse() {
		return false;
	}
}
