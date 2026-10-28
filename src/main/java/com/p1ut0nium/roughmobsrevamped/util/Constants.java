package com.p1ut0nium.roughmobsrevamped.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

public class Constants {

	public static final String MODID = "roughmobsredux";
	public static final String MODNAME = "Rough Mobs Redux";
	// DROPPED: MODVERSION, MODUPDATE, CLIENTPROXY, SERVERPROXY (mods.toml and DistExecutor replace them)
	// DROPPED: ENTITY_BOSSZOMBIE, ENTITY_BOSSSKELETON, ENTITY_HOSTILEBAT numeric ids (registry names only in 1.20.1)

	public static final String ROUGHMOBSALL = "roughmobsall";
	public static final String ROUGHMOBSEQUIP = "roughmobsequip";
	public static final String ROUGHMOBSBOSS = "roughmobsboss";
	public static final String ROUGHMOBSABILS = "roughmobsabils";
	public static final String ROUGHMOBSENCHANT = "roughmobsenchant";

	public static String unique(String id) {
		return MODID + ":" + id;
	}

	public static List<String> getRegNames(List<EntityType<?>> entityTypes) {
		List<String> regNames = new ArrayList<String>();
		for(EntityType<?> type : entityTypes) {
			regNames.add(ForgeRegistries.ENTITY_TYPES.getKey(type).toString());
		}
		return regNames;
	}

	/*
	 * CHANGED: 1.12.2 ids that were renamed by the 1.13 flattening or later, so a config or list copied
	 * from the 1.12.2 version still resolves. Keys are compared without the minecraft: namespace.
	 */
	private static final Map<String, String> LEGACY_NAMES = new HashMap<>();
	static {
		LEGACY_NAMES.put("zombie_pigman", "zombified_piglin");
		LEGACY_NAMES.put("vindication_illager", "vindicator");
		LEGACY_NAMES.put("evocation_illager", "evoker");
		LEGACY_NAMES.put("illusion_illager", "illusioner");
		LEGACY_NAMES.put("villager_golem", "iron_golem");
		LEGACY_NAMES.put("snowman", "snow_golem");
		LEGACY_NAMES.put("ender_crystal", "end_crystal");
		LEGACY_NAMES.put("reeds", "sugar_cane");
		LEGACY_NAMES.put("melon_block", "melon");
		LEGACY_NAMES.put("lit_redstone_lamp", "redstone_lamp");
		LEGACY_NAMES.put("lit_pumpkin", "jack_o_lantern");
		LEGACY_NAMES.put("web", "cobweb");
		LEGACY_NAMES.put("generic.maxHealth", "generic.max_health");
		LEGACY_NAMES.put("generic.followRange", "generic.follow_range");
		LEGACY_NAMES.put("generic.knockbackResistance", "generic.knockback_resistance");
		LEGACY_NAMES.put("generic.movementSpeed", "generic.movement_speed");
		LEGACY_NAMES.put("generic.flyingSpeed", "generic.flying_speed");
		LEGACY_NAMES.put("generic.attackDamage", "generic.attack_damage");
		LEGACY_NAMES.put("generic.attackSpeed", "generic.attack_speed");
		LEGACY_NAMES.put("generic.armorToughness", "generic.armor_toughness");
		LEGACY_NAMES.put("zombie.spawnReinforcements", "zombie.spawn_reinforcements");
		LEGACY_NAMES.put("horse.jumpStrength", "horse.jump_strength");
	}

	public static String legacyName(String id) {
		String trimmed = id.trim();
		String path = trimmed.startsWith("minecraft:") ? trimmed.substring("minecraft:".length()) : trimmed;
		String renamed = LEGACY_NAMES.get(path);
		return renamed == null ? trimmed : renamed;
	}

	public static ResourceLocation legacyLocation(String id) {
		return ResourceLocation.tryParse(legacyName(id));
	}

	/*
	 * CHANGED: 1.12.2 numeric dimension ids map to the 1.20.1 dimension keys.
	 * Anything else is returned as written (a dimension id such as minecraft:the_nether).
	 */
	public static String legacyDimension(String dimension) {
		switch (dimension.trim()) {
			case "0": return "minecraft:overworld";
			case "-1": return "minecraft:the_nether";
			case "1": return "minecraft:the_end";
		}
		return dimension.trim();
	}

	// From Rough Mobs Revamped 1.16.5 Constants.CHAMP_HONORIFICS
	public static final String[] BOSS_HONORIFICS = {
			"The Dread Knight",
			"The Foul",
			"The Deadly",
			"Life Stealer",
			"Death Dealer"
	};

	public static final String[] FOG_COLORS = {
			"0.1",
			"0.4",
			"0.1"
	};

	public static final String[] BOSS_COLORS = {
			"0.0",
			"1.0",
			"0.0"
	};

	public static final String[] SEASONS = {
			"SUMMER",
			"AUTUMN",
			"WINTER",
			"SPRING"
	};

	public static final String[] ATTRIBUTE_DEFAULT = {
			"zombie;generic.max_health;1;0.5;/;1",
			"zombified_piglin;generic.max_health;1;0.5;/;1",
			"zombie_villager;generic.max_health;1;0.5;/;1",
			"husk;generic.max_health;1;0.5;/;1",
			"skeleton;generic.max_health;1;0.5",
			"stray;generic.max_health;1;0.5",
			"wither_skeleton;generic.max_health;1;1",
			"slime;generic.max_health;1;0.5",
			"blaze;generic.max_health;1;0.5",
			"magma_cube;generic.max_health;1;0.5",
			"wither;generic.max_health;1;1",
			"vindicator;generic.max_health;1;0.5",
			"evoker;generic.max_health;1;1",
			"zombie;generic.follow_range;0;30",
			"zombified_piglin;generic.follow_range;0;30",
			"zombie_villager;generic.follow_range;0;30",
			"husk;generic.follow_range;0;30",
			"skeleton;generic.follow_range;0;30",
			"stray;generic.follow_range;0;30",
			"wither_skeleton;generic.follow_range;0;30",
			"creeper;generic.follow_range;0;30",
			"spider;generic.follow_range;0;30",
			"slime;generic.follow_range;0;30",
			"witch;generic.follow_range;0;30",
			"blaze;generic.follow_range;0;30",
			"ghast;generic.follow_range;0;30",
			"magma_cube;generic.follow_range;0;30",
			"vindicator;generic.follow_range;0;30",
			"evoker;generic.follow_range;0;30",
			"zombie;generic.knockback_resistance;0;0.5",
			"zombified_piglin;generic.knockback_resistance;0;0.5",
			"zombie_villager;generic.knockback_resistance;0;0.5",
			"husk;generic.knockback_resistance;0;0.5",
			"vindicator;generic.knockback_resistance;0;0.5",
			"creeper;generic.movement_speed;1;0.5",
			"slime;generic.movement_speed;1;0.5",
			"magma_cube;generic.movement_speed;1;0.5"
	};

	public static final String[] DEFAULT_DESTROY_BLOCKS = {
			"minecraft:carrots",
			"minecraft:potatoes",
			"minecraft:wheat",
			"minecraft:nether_wart",
			"minecraft:sugar_cane",
			"minecraft:beetroots",
			"minecraft:pumpkin_stem",
			"minecraft:melon_stem",
			"minecraft:pumpkin",
			"minecraft:hay_block",
			"minecraft:melon",
			"minecraft:torch",
			"minecraft:redstone_lamp",
			"minecraft:jack_o_lantern"
	};

	public static final String[] DEFAULT_MAINHAND = {
			"wooden_axe;3;minecraft:overworld",
			"wooden_sword;3;minecraft:overworld",
			"stone_pickaxe;2",
			"stone_axe;2",
			"stone_sword;2",
			"iron_pickaxe;1",
			"golden_sword;1",
			"golden_axe;1",
			"iron_axe;3;minecraft:the_nether",
			"iron_sword;3;minecraft:the_nether"
	};

	public static final String[] DEFAULT_OFFHAND = {
			"wooden_axe;3;minecraft:overworld",
			"wooden_sword;3;minecraft:overworld",
			"stone_pickaxe;2",
			"stone_axe;2",
			"stone_sword;2",
			"iron_pickaxe;1",
			"golden_sword;1",
			"golden_axe;1",
			"shield;5",
			"iron_axe;3;minecraft:the_nether",
			"iron_sword;3;minecraft:the_nether"
	};

	public static final String[] SKELETON_MAINHAND = {
			"bow;1"
	};

	public static final String[] DEFAULT_BOOTS = {
			"leather_boots;3;minecraft:overworld",
			"chainmail_boots;2",
			"golden_boots;1",
			"iron_boots;4;minecraft:the_nether"
	};

	public static final String[] DEFAULT_LEGGINGS = {
			"leather_leggings;3;minecraft:overworld",
			"chainmail_leggings;2",
			"golden_leggings;1",
			"iron_leggings;4;minecraft:the_nether"
	};

	public static final String[] DEFAULT_CHESTPLATES = {
			"leather_chestplate;3;minecraft:overworld",
			"chainmail_chestplate;2",
			"golden_chestplate;1",
			"iron_chestplate;4;minecraft:the_nether"
	};

	public static final String[] DEFAULT_HELMETS = {
			"leather_helmet;3;minecraft:overworld",
			"chainmail_helmet;2",
			"golden_helmet;1",
			"iron_helmet;4;minecraft:the_nether"
	};

	public static final String[] DEFAULT_WEAPON_ENCHANTS = {
			"sharpness;4",
			"knockback;2",
			"smite;2",
			"bane_of_arthropods;2",
			"looting;1",
			"sweeping;1",
			"fire_aspect;5;minecraft:the_nether"
	};

	public static final String[] DEFAULT_ARMOR_ENCHANTS = {
			"protection;4",
			"feather_falling;3",
			"respiration;3",
			"depth_strider;3",
			"thorns;2",
			"projectile_protection;2",
			"fire_protection;2",
			"blast_protection;2"
	};

	public static final String[] DEFAULT_BOSS_MAINHAND = {
			"diamond_sword;3",
			"diamond_axe;1"
	};

	public static final String[] DEFAULT_BOSS_OFFHAND = {
			"shield;2",
			"diamond_sword;1",
			"diamond_axe;1"
	};

	public static final String[] DEFAULT_BOSS_BOOTS = {
			"diamond_boots;1"
	};

	public static final String[] DEFAULT_BOSS_LEGGINGS = {
			"diamond_leggings;1"
	};

	public static final String[] DEFAULT_BOSS_CHESTPLATES = {
			"diamond_chestplate;1"
	};

	public static final String[] DEFAULT_BOSS_HELMETS = {
			"diamond_helmet;1"
	};

	// CHANGED: biome id 8 (Hell) is the minecraft:is_nether biome tag in 1.20.1
	public static final String[] DEFAULT_SPAWN_ENTRIES = {
			"wither_skeleton;30;1;5;MONSTER;#minecraft:is_nether",
			"blaze;30;1;5;MONSTER;#minecraft:is_nether",
			"magma_cube;30;1;3;MONSTER;#minecraft:is_nether"
	};

	public static final String[] DEFAULT_SPIDER_RIDERS = {
			"zombie",
			"skeleton",
			"witch",
			"cave_spider"
	};

	public static final String[] DEFAULT_WITCH_BUFFS = {
			"strength;1",
			"resistance;2",
			"speed;2"
	};

	public static final String[] DEFAULT_TARGET_BLOCKER = {
			"zombie;*",
			"zombified_piglin;*",
			"zombie_villager;*",
			"husk;*",
			"skeleton;*",
			"stray;*",
			"wither_skeleton;*",
			"slime;*",
			"blaze;*",
			"magma_cube;*",
			"wither;*",
			"vindicator;*",
			"evoker;*",
			"creeper;*",
			"spider;*",
			"cave_spider;*",
			"witch;*",
			"ghast;*"
	};

	public static final String[] DEFAULT_TARGETS = {
			"pig;zombie",
			"sheep;zombie",
			"sheep;skeleton"
	};
}
