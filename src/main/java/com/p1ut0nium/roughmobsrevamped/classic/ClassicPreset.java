package com.p1ut0nium.roughmobsrevamped.classic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;

/*
 * CHANGED: new in Rough Mobs 4. The Classic Preset option (Classic _ClassicPreset): the default numbers Rough Mobs 2
 * and Rough Mobs 1 shipped where Rough Mobs Revamped softened them.
 *   rmr  Rough Mobs Revamped defaults, nothing changed
 *   rm2  Rough Mobs 2: blaze push 4 and block damage on death, zombie pigmen angry on touch
 *   rm1  Rough Mobs 1: rm2 plus the RM1 horse, rider, witch, slime, enderman, endermite and gear numbers, and the
 *        slime-on-head and bat vampire features switched on
 * Each entry is a config key and its value. RoughConfig.switchPreset writes them into the file once, when
 * _ClassicPreset differs from _ClassicPresetApplied. A key no option defines is logged at start (RoughApplier.loadConfig).
 * Bosses are never part of a preset: _BossGlobal_BossesEnabled stays the only switch.
 */
public final class ClassicPreset {

	public static final String RMR = "rmr";
	public static final String RM2 = "rm2";
	public static final String RM1 = "rm1";
	public static final List<String> NAMES = List.of(RMR, RM2, RM1);

	private ClassicPreset() {
	}

	public static Map<List<String>, Object> values(String preset) {
		Map<List<String>, Object> values = new HashMap<>();

		if (preset.equals(RM2) || preset.equals(RM1)) {
			values.put(RoughConfig.key("blaze", "PushStrength"), 4.0D);
			values.put(RoughConfig.key("blaze", "DeathExplosionDestroyBlocks"), true);
			values.put(RoughConfig.key("zombie pigman", "AggressiveTouch"), true);
		}

		if (preset.equals(RM1)) {
			values.put(RoughConfig.key("zombie", "HorseChance"), 12);
			values.put(RoughConfig.key("skeleton", "HorseChance"), 12);
			values.put(RoughConfig.key("spider", "RiderChance"), 20);
			values.put(RoughConfig.key("witch", "LingeringChance"), 4);
			// RM1 witches dropped nextInt(5) + 3 bats
			values.put(RoughConfig.key("witch", "BatsOnDeath"), 3);
			values.put(RoughConfig.key("witch", "BatsOnDeathRandom"), 4);
			values.put(RoughConfig.key("blaze", "DeathExplosionStrength"), 0.5D);
			values.put(RoughConfig.key("blaze", "FlameTouchSeconds"), 5);
			values.put(RoughConfig.key("creeper", "InvisibleParticles"), 3);
			// RM1 slimeKnockback 0.5 used velocity += k * 1.5 * size; RMR knocks back by size * k
			values.put(RoughConfig.key("slime", "KnockBackMultiplier"), 0.75D);
			values.put(RoughConfig.key("slime", "OnHead"), true);
			values.put(RoughConfig.key("bat", "Vampire"), true);
			values.put(RoughConfig.key("enderman", "BlindChance"), 5);
			values.put(RoughConfig.key("enderman", "BlindOrNausea"), true);
			values.put(RoughConfig.key("endermite", "WitherRange"), 4.0D);

			values.put(RoughConfig.importantKey("zombieEquipment", "WeaponChance"), 5);
			values.put(RoughConfig.importantKey("zombieEquipment", "ArmorChance"), 5);
			values.put(RoughConfig.importantKey("zombieEquipment", "EnchantChance"), 5);
			values.put(RoughConfig.importantKey("zombieEquipment", "DropChance"), 0.01D);
			values.put(RoughConfig.importantKey("skeletonEquipment", "ArmorChance"), 3);
			values.put(RoughConfig.importantKey("skeletonEquipment", "DropChance"), 0.01D);
		}

		return values;
	}
}
