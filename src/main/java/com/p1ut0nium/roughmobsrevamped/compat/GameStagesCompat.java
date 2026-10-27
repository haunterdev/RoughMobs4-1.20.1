package com.p1ut0nium.roughmobsrevamped.compat;

import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;

import net.darkhax.gamestages.GameStageHelper;
import net.minecraft.world.entity.player.Player;

public abstract class GameStagesCompat {

	private GameStagesCompat() {}

	private static boolean registered;

	private static Boolean useEquipmentStage = false;
	private static Boolean useAllStages = false;
	private static Boolean useBossStage = false;
	private static Boolean useEnchantStage = false;
	private static Boolean useAbilitiesStage = false;

	// CHANGED: register() runs once per config pass (define, then read), so the guard is per pass
	public static void register() {
		registered = true;
		preInit();
	}

	private static void preInit() {
		if (!hasDefaultConfig())
			return;

		// Get/Set config file values for Game Stages
		RoughConfig.addCustomCategoryComment("GameStages", "Configuration options for enabling/disabling Game Stages.\n"
																		+ "The mod Game Stages must be installed for these features to work.\n"
																		+ "\n"
																		+ "Stages to give player in-game: \n"
																		+ "\n"
																		+ "roughmobsall - gives all stages at once.\n"
																		+ "roughmobsequip - allows mobs to spawn with equipment.\n"
																		+ "roughmobsenchant - allows mob gear to be enchanted.\n"
																		+ "roughmobsboss - allows some mobs to be bosses.\n"
																		+ "roughmobsabils - allows mobs to have special abilities.");

		useAllStages = RoughConfig.getBoolean("GameStages", "_All", false, "Enable this Game Stage to allow all Rough Mobs stages at once. Or turn them on individually");
		useEquipmentStage = RoughConfig.getBoolean("GameStages", "_Equipment", false, "Enable this Game stage for Rough Mobs to have equipment.");
		useBossStage = RoughConfig.getBoolean("GameStages", "_Bosses", false, "Enable this Game Stage needed for Rough Mob Bosses to spawn. Must also enable Equipment stage for this to work.");
		useAbilitiesStage = RoughConfig.getBoolean("GameStages", "_Abilities", false, "Enable this Game Stage for Rough Mobs to have special combat AI and attributes.");
		useEnchantStage = RoughConfig.getBoolean("GameStages", "_Enchantments", false, "Enable this Game Stage for Rough Mob equipment to be enchanted.");

		// If useAllStages is true, then all other stages should also be set to true
		if (useAllStages) {
			useEquipmentStage = useBossStage = useAbilitiesStage = useEnchantStage = true;
		}
	}

	private static boolean hasDefaultConfig() {
		return true;
	}

	// CHANGED: the GameStages calls live here so no other class loads net.darkhax classes
	public static boolean hasAnyOf(Player player, String... stages) {
		return GameStageHelper.hasAnyOf(player, stages);
	}

	/*
	 * Getters
	 */
	public static Boolean useEquipmentStage() {
		if (registered) {
			return useEquipmentStage;
		}
		return false;
	}

	public static Boolean useAllStages() {
		if (registered) {
			return useAllStages;
		}
		return false;
	}

	public static Boolean useBossStage() {
		if (registered) {
			return useBossStage;
		}
		return false;
	}

	public static Boolean useEnchantStage() {
		if (registered) {
			return useEnchantStage;
		}
		return false;
	}

	public static Boolean useAbilitiesStage() {
		if (registered) {
			return useAbilitiesStage;
		}
		return false;
	}
}
