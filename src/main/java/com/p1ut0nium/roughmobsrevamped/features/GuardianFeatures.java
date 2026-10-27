package com.p1ut0nium.roughmobsrevamped.features;

import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;

public class GuardianFeatures extends EntityFeatures {

	private boolean dropWater;

	public GuardianFeatures() {
		super("guardian", EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN);
	}

	@Override
	public void initConfig() {
		super.initConfig();

		dropWater = RoughConfig.getBoolean(name, "DropWater", true, "Set to false to prevent %ss from dropping water on death");
	}

	@Override
	public void onDeath(Entity deadEntity, DamageSource source) {
		if (dropWater)
		{
			BlockPos pos1 = deadEntity.blockPosition();
			BlockPos pos2 = pos1.above();

			if (deadEntity.level().getBlockState(pos1).isAir())
				deadEntity.level().setBlock(pos1, Blocks.WATER.defaultBlockState(), 11);
			if (deadEntity.level().getBlockState(pos2).isAir())
				deadEntity.level().setBlockAndUpdate(pos2, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
		}
	}
}
