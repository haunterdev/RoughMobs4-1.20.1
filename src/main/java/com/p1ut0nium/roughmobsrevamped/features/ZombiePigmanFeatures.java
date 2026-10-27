package com.p1ut0nium.roughmobsrevamped.features;

import java.util.List;

import com.p1ut0nium.roughmobsrevamped.ai.combat.RoughAIAggressiveTouch;
import com.p1ut0nium.roughmobsrevamped.ai.combat.RoughAIAlwaysAggressive;
import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;
import com.p1ut0nium.roughmobsrevamped.misc.FeatureHelper;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;

public class ZombiePigmanFeatures extends EntityFeatures {

	private boolean aggressiveTouch;
	private boolean alwaysAggressive;
	private int aggressiveRange;
	private float aggressiveBlockRange;
	private int aggressiveBlockChance;

	public ZombiePigmanFeatures() {
		super("zombie pigman", EntityType.ZOMBIFIED_PIGLIN);
	}

	@Override
	public void initConfig() {
		super.initConfig();

		aggressiveTouch = RoughConfig.getBoolean(name, "AggressiveTouch", false, "Set to false to prevent zombie pigman from getting aggressive if the player touches its hitbox");
		alwaysAggressive = RoughConfig.getBoolean(name, "AlwaysAggressive", true, "Set to true for zombie pigmen to always be aggressive");
		aggressiveRange = RoughConfig.getInteger(name, "AggressionRange", 10, 0, MAX, "The range at which zombie pigmen will be aggressive to the player.");
		aggressiveBlockRange = RoughConfig.getFloat(name, "AggressiveBlockRange", 20F, 1, MAX, "Block radius in which zombie pigman get aggressive if the player breaks blocks");
		aggressiveBlockChance = RoughConfig.getInteger(name, "AggressiveBlockChance", 10, 0, MAX, "Chance (1 in X) that a zombie pigman gets aggressive if the player breaks nearby blocks");
	}

	@Override
	public void addAI(EntityJoinLevelEvent event, Mob entity, GoalSelector tasks, GoalSelector targetTasks) {
		if (aggressiveTouch)
			tasks.addGoal(1, new RoughAIAggressiveTouch(entity));

		if (alwaysAggressive)
			tasks.addGoal(3, new RoughAIAlwaysAggressive(entity, aggressiveRange));
	}

	@Override
	public void onBlockBreak(Player player, BreakEvent event) {
		if (aggressiveBlockChance <= 0 || player.isCreative() || player.isSpectator())
			return;

		// CHANGED: 1.12.2 AxisAlignedBB.expand only grew the box in the positive direction, so only pigmen on one side
		// of the player reacted; inflate grows it on every side, matching the "Block radius" in the config text.
		// Creative and spectator players are skipped, as RMR 2.2.8 did for the touch aggression.
		List<Mob> entities = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(aggressiveBlockRange), this::isEntity);
		for (Mob living : entities)
		{
			if (player.level().random.nextInt(aggressiveBlockChance) == 0)
			{
				// CHANGED: setTarget on a neutral mob also starts its persistent anger, which the 1.12.2 pigman had built in
				RoughAIAggressiveTouch.anger(living, player);
				FeatureHelper.playSound(living, SoundEvents.ZOMBIFIED_PIGLIN_ANGRY);
			}
		}
	}
}
