package com.p1ut0nium.roughmobsrevamped.features;

import com.p1ut0nium.roughmobsrevamped.ai.misc.RoughAIAddEffect;
import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

public class EndermiteFeatures extends EntityFeatures {

	private boolean teleportAttacker;
	private String immunityItem;
	private boolean witherMobs;
	private float witherRange;

	private Item immunityItemItem;

	public EndermiteFeatures() {
		super("endermite", EntityType.ENDERMITE);
	}

	@Override
	public void initConfig() {
		super.initConfig();

		teleportAttacker = RoughConfig.getBoolean(name, "TeleportAttacker", true, "Set this to false to prevent %ss from teleporting attackers away");
		immunityItem = RoughConfig.getString(name, "ImmunityItem", "minecraft:ender_eye", "If this item is somewhere in the players inventory, the player becomes immune to teleportation\nLeave this empty to disable this feature");
		witherMobs = RoughConfig.getBoolean(name, "WitherMobs", true, "Set this to false to prevent %ss from applying the wither effect to near entities");
		// RM1: endermiteWitherRange (4); RMR hard-coded 6
		witherRange = RoughConfig.getFloat(name, "WitherRange", 6F, 0F, 32F, "Range in blocks in which %ss wither entities that are not monsters");
	}

	@Override
	public void postInit() {
		immunityItemItem = EndermanFeatures.itemFromName(immunityItem);
	}

	@Override
	public void addAI(EntityJoinLevelEvent event, Mob entity, GoalSelector tasks, GoalSelector targetTasks) {
		if (witherMobs && witherRange > 0)
			tasks.addGoal(1, new RoughAIAddEffect(entity, MobEffects.WITHER, witherRange));
	}

	@Override
	public void onDefend(Entity target, Entity attacker, Entity immediateAttacker, LivingDamageEvent event) {
		if (teleportAttacker && attacker instanceof LivingEntity)
		{
			if (EndermanFeatures.isImmuneToTeleport(attacker, immunityItemItem))
			{
				// DROPPED: the second attacker.playSound call; level.playSound(null, ...) already reaches every player
				attacker.level().playSound(null, attacker.xo, attacker.yo, attacker.zo, SoundEvents.ENDER_EYE_DEATH, attacker.getSoundSource(), 1.0F, 1.0F);
			}
			else
				EndermanFeatures.teleportRandom((LivingEntity)attacker, 16);
		}
	}
}
