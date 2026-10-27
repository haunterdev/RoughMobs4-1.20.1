package com.p1ut0nium.roughmobsrevamped.misc;

import java.util.ArrayList;
import java.util.List;

import com.p1ut0nium.roughmobsrevamped.RoughMobs;
import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;
import com.p1ut0nium.roughmobsrevamped.util.Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

public class TargetHelper {

	public static final String CATEGORY = "targetBlocker";

	private static boolean enableTargetBlock;
	private static final List<TargetEntry> BlockerList = new ArrayList<TargetEntry>();

	public static boolean enableTargetAlways;
	public static boolean ignoreSpawnConditions;
	private static final List<TargetEntry> AttackerList = new ArrayList<TargetEntry>();

	public static void init() {
		RoughConfig.addCustomCategoryComment(CATEGORY, "Entities which can't be targeted by other entities."
																+ "\ne.g. Skeletons can't target other Skeletons by shooting them accidentally"
																+ "\nTakes 2 arguments divided by a semicolon per entry. victim;attacker"
																+ "\nvictim: The entity which should not be targeted if attacked by the attacker (entity name)"
																+ "\nattacker: the attacker entity which can't target the victim (entity name)"
																+ "\nUse \"*\" instead of the victim or attacker if you want this for all entities except players");

		String[] blockers = RoughConfig.getStringArray(CATEGORY, "_List", Constants.DEFAULT_TARGET_BLOCKER, "");
		enableTargetBlock = RoughConfig.getBoolean(CATEGORY, "_Enabled", false, "Set to true to enable the target blocker feature.");

		if (!RoughConfig.isDefining())
			fillList(blockers, "targetblocker");

		RoughConfig.addCustomCategoryComment("targetAttacker", "Entities always attack these targets."
																+ "\ne.g. Zombies always attack pigs."
																+ "\nTakes 2 arguments divided by a semicolon per entry. victim;attacker"
																+ "\nvictim: The entity which should be attacked (entity name)"
																+ "\nattacker: the attacker entity (entity name)"
																+ "\nMultiple entries for the same victim or attacker are allowed"
																+ "\nUse \"*\" instead of the victim or attacker if you want this for all entities except players");

		String[] attackers = RoughConfig.getStringArray("targetAttacker", "_List", Constants.DEFAULT_TARGETS, "");
		enableTargetAlways = RoughConfig.getBoolean("targetAttacker", "_Enabled", false, "Set to true to enable the target attacker feature.");
		ignoreSpawnConditions = RoughConfig.getBoolean("targetAttacker", "_IgnoreSpawnConditions", true, "Disable to require spawn conditions be met in order for target attacker feature to work.");

		if (!RoughConfig.isDefining())
			fillList(attackers, "targetattacker");
	}

	private static void fillList(String[] options, String listType) {
		List<TargetEntry> list = listType.equals("targetblocker") ? BlockerList : AttackerList;
		list.clear();

		for (String option : options)
		{
			String[] split = option.split(";");
			if (split.length >= 2)
			{
				// null stands for "*" (any entity except players)
				EntityType<?>[] entities = new EntityType<?>[2];
				boolean success = true;
				for (int i = 0; i < 2; i++)
				{
					if (split[i].trim().equals("*"))
					{
						entities[i] = null;
					}
					else
					{
						ResourceLocation loc = Constants.legacyLocation(split[i]);
						if (loc == null || !ForgeRegistries.ENTITY_TYPES.containsKey(loc))
						{
							// CHANGED: the 1.12.2 check compared against "targetblock", so this error never printed
							RoughMobs.logger.error(listType + ": \"" + split[i] + "\" is not a valid entity!");
							success = false;
							break;
						}
						entities[i] = ForgeRegistries.ENTITY_TYPES.getValue(loc);
					}
				}

				if (success)
					list.add(new TargetEntry(entities[1], entities[0]));
			}
			else
				RoughMobs.logger.error(listType + ": each option needs at least 2 arguments! (" + option + ")");
		}
	}

	/*
	 * CHANGED: the 1.12.2 version returned the attacker class of the first entry whose victim class equalled
	 * the target's class, so "*" never matched as a victim and later entries for the same victim were skipped.
	 * Every entry is checked now and "*" matches any non-player entity on either side.
	 */
	public static boolean isTargetBlocked(Entity attacker, Entity targetedEntity) {
		for (TargetEntry entry : BlockerList)
		{
			if (entry.matchesVictim(targetedEntity) && entry.matchesAttacker(attacker))
				return true;
		}
		return false;
	}

	public static void setTargets(Mob attacker) {
		for (TargetEntry entry : AttackerList) {
			if (entry.matchesAttacker(attacker))
				attacker.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(attacker, LivingEntity.class, 10, true, false, entry::matchesVictim));
		}
	}

	public static boolean targetBlockerEnabled() {
		return enableTargetBlock;
	}

	public static boolean targetAttackerEnabled() {
		return enableTargetAlways;
	}

	static class TargetEntry {

		private final EntityType<?> attackerType;
		private final EntityType<?> victimType;

		public TargetEntry(EntityType<?> attackerType, EntityType<?> victimType) {
			this.attackerType = attackerType;
			this.victimType = victimType;
		}

		public EntityType<?> getAttackerType() {
			return attackerType;
		}

		public EntityType<?> getVictimType() {
			return victimType;
		}

		public boolean matchesAttacker(Entity entity) {
			return attackerType == null ? !(entity instanceof Player) : entity.getType() == attackerType;
		}

		public boolean matchesVictim(Entity entity) {
			return victimType == null ? !(entity instanceof Player) : entity.getType() == victimType;
		}
	}
}
