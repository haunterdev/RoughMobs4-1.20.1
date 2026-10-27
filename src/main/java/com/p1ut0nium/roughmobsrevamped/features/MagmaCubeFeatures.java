package com.p1ut0nium.roughmobsrevamped.features;

import com.p1ut0nium.roughmobsrevamped.ai.combat.RoughAILavaRegen;
import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

public class MagmaCubeFeatures extends EntityFeatures {

	private boolean regenInLava;
	private boolean dropLava;

	public MagmaCubeFeatures() {
		super("magma cube", EntityType.MAGMA_CUBE);
	}

	@Override
	public void initConfig() {
		super.initConfig();

		regenInLava = RoughConfig.getBoolean(name, "RegenInLava", true, "Set to false to prevent %ss from regenerating health while in lava");
		dropLava = RoughConfig.getBoolean(name, "DropLava", true, "Set to false to prevent small %ss from dropping lava on death");
	}

	@Override
	public void addAI(EntityJoinLevelEvent event, Mob entity, GoalSelector tasks, GoalSelector targetTasks) {
		if (regenInLava)
			tasks.addGoal(1, new RoughAILavaRegen(entity));
	}

	@Override
	public void onDeath(Entity deadEntity, DamageSource source) {
		if (dropLava && deadEntity instanceof Slime && ((Slime)deadEntity).getSize() <= 1)
		{
			BlockPos pos1 = deadEntity.blockPosition();
			BlockPos pos2 = pos1.above();

			if (deadEntity.level().getBlockState(pos1).isAir())
				deadEntity.level().setBlockAndUpdate(pos1, Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 11));
			if (deadEntity.level().getBlockState(pos2).isAir())
				deadEntity.level().setBlockAndUpdate(pos2, Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
		}
	}
}
