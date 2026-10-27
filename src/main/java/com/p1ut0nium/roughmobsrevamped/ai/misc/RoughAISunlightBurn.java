package com.p1ut0nium.roughmobsrevamped.ai.misc;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

/**
 * Sets a mob on fire in daylight under open sky.
 * helmetMode true: burns only while wearing a helmet (vanilla already burns the bare-headed undead).
 * helmetMode false: burns while bare-headed; a helmet takes durability instead.
 */
public class RoughAISunlightBurn extends Goal {

	protected Mob entity;
	protected boolean helmetMode;

	public RoughAISunlightBurn(Mob entity, boolean helmetMode) {
		this.entity = entity;
		this.helmetMode = helmetMode;
		this.setFlags(EnumSet.of(Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		float f = entity.getLightLevelDependentMagicValue();
		boolean flag = entity.level().isDay() && f > 0.5F && entity.level().canSeeSky(BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ()));

		if (!flag)
			return false;

		ItemStack itemstack = entity.getItemBySlot(EquipmentSlot.HEAD);

		if (!itemstack.isEmpty())
		{
			if (helmetMode)
				return !entity.isOnFire();

			if (itemstack.isDamageableItem())
			{
				itemstack.setDamageValue(itemstack.getDamageValue() + entity.level().random.nextInt(2));

				if (itemstack.getDamageValue() >= itemstack.getMaxDamage())
				{
					entity.broadcastBreakEvent(EquipmentSlot.HEAD);
					entity.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
				}
			}
			return false;
		}

		return !helmetMode && !entity.isOnFire();
	}

	@Override
	public void tick() {
		entity.setSecondsOnFire(8);
	}
}
