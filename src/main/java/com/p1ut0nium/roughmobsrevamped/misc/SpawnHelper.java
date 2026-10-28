package com.p1ut0nium.roughmobsrevamped.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mojang.serialization.Codec;
import com.p1ut0nium.roughmobsrevamped.RoughMobs;
import com.p1ut0nium.roughmobsrevamped.compat.CompatHandler;
import com.p1ut0nium.roughmobsrevamped.compat.SereneSeasonsCompat;
import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;
import com.p1ut0nium.roughmobsrevamped.util.Constants;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SpawnHelper {

	public static final List<SpawnEntry> ENTRIES = new ArrayList<SpawnEntry>();

	/*
	 * CHANGED: EntityRegistry.addSpawn/removeSpawn are gone. Spawn entries are applied by a biome modifier,
	 * data/roughmobsredux/forge/biome_modifier/spawn_entries.json, which reads ENTRIES when the server
	 * builds its biomes.
	 */
	public static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIERS = DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, Constants.MODID);
	public static final RegistryObject<Codec<SpawnEntriesModifier>> SPAWN_ENTRIES_CODEC = BIOME_MODIFIERS.register("spawn_entries", () -> Codec.unit(SpawnEntriesModifier::new));

	private static int playerSpawnLevel;
	private static boolean isUndergroundEnabled;
	private static int maxSpawnHeight;
	private static int minDistFromSpawn;
	private static boolean disableBabyZombies;

	public static boolean disableBabyZombies() {
		return disableBabyZombies;
	}

	public static class SpawnEntry {

		public static final String OW_TYPE = "OVERWORLD";
		public static final String DISABLE_KEY = "!";

		private boolean valid = true;
		private String error = "";

		public EntityType<?> entityType;
		public int prob;
		public int min;
		public int max;
		public MobCategory type;
		public List<BiomeMatcher> biomesAdd = new ArrayList<BiomeMatcher>();
		public List<BiomeMatcher> biomesRemove = new ArrayList<BiomeMatcher>();
		public boolean allBiomes;

		public SpawnEntry(String entityName, String prob, String min, String max, String type, String... biomes) {
			this.entityType = getEntityType(entityName);
			this.prob = getInteger(prob);
			this.min = getInteger(min);
			this.max = getInteger(max);
			this.type = getType(type);
			getBiomes(biomes);
		}

		/*
		 * CHANGED: BiomeDictionary types and numeric biome ids are gone. A biome is matched by
		 *   biome id (minecraft:plains), biome tag (#minecraft:is_nether), OVERWORLD (#minecraft:is_overworld),
		 *   a 1.12.2 type name such as FOREST (#minecraft:is_forest or #forge:is_forest),
		 *   or the 1.12.2 ids 8 (Hell, #minecraft:is_nether) and 9 (The End, #minecraft:is_end).
		 */
		private void getBiomes(String[] biomes) {
			for (String rawId : biomes)
			{
				String biomeId = rawId.trim();
				List<BiomeMatcher> list = biomeId.startsWith(DISABLE_KEY) ? biomesRemove : biomesAdd;
				if (biomeId.startsWith(DISABLE_KEY))
					biomeId = biomeId.substring(1).trim();

				if (biomeId.isEmpty())
					continue;

				if (biomeId.equals(OW_TYPE))
				{
					list.add(BiomeMatcher.tag("minecraft:is_overworld"));
				}
				else if (biomeId.startsWith("#"))
				{
					ResourceLocation loc = ResourceLocation.tryParse(biomeId.substring(1));
					if (loc == null)
					{
						this.valid = false;
						this.error = "Biome tag " + biomeId + " isn't a valid id!";
					}
					else
						list.add(BiomeMatcher.tag(loc.toString()));
				}
				else if (isInteger(biomeId))
				{
					int id = Integer.parseInt(biomeId);
					if (id == 8)
						list.add(BiomeMatcher.tag("minecraft:is_nether"));
					else if (id == 9)
						list.add(BiomeMatcher.tag("minecraft:is_end"));
					else
					{
						this.valid = false;
						this.error = "Numeric biome id " + id + " has no 1.20.1 equivalent, use a biome id or #tag instead!";
					}
				}
				else if (!biomeId.contains(":") && biomeId.equals(biomeId.toUpperCase()))
				{
					String path = "is_" + biomeId.toLowerCase();
					list.add(BiomeMatcher.anyTag("minecraft:" + path, "forge:" + path));
				}
				else
				{
					ResourceLocation loc = ResourceLocation.tryParse(biomeId);
					if (loc == null)
					{
						this.valid = false;
						this.error = "Biomes with the id/name " + biomeId + " don't exist!";
					}
					else
						list.add(BiomeMatcher.biome(loc));
				}
			}

			if (biomes.length == 0)
				allBiomes = true;
		}

		private MobCategory getType(String type) {
			String upper = type.trim().toUpperCase();
			for (MobCategory category : MobCategory.values())
			{
				if (category.name().equals(upper))
					return category;
			}

			this.valid = false;
			this.error = "Creature type " + type + " doesn't exist!";
			return null;
		}

		private int getInteger(String str) {
			try
			{
				return Integer.parseInt(str.trim());
			}
			catch(NumberFormatException e)
			{
				this.valid = false;
				error = str + " is not a valid number!";
				return -1;
			}
		}

		private static boolean isInteger(String str) {
			try
			{
				Integer.parseInt(str);
				return true;
			}
			catch(NumberFormatException e)
			{
				return false;
			}
		}

		private EntityType<?> getEntityType(String entityName) {
			ResourceLocation loc = Constants.legacyLocation(entityName);
			if (loc != null && ForgeRegistries.ENTITY_TYPES.containsKey(loc))
				return ForgeRegistries.ENTITY_TYPES.getValue(loc);

			this.valid = false;
			error = entityName + " is not a valid entity!";
			return null;
		}

		public boolean addsTo(Holder<Biome> biome) {
			if (allBiomes)
				return true;
			for (BiomeMatcher matcher : biomesAdd)
				if (matcher.matches(biome))
					return true;
			return false;
		}

		public boolean removesFrom(Holder<Biome> biome) {
			for (BiomeMatcher matcher : biomesRemove)
				if (matcher.matches(biome))
					return true;
			return false;
		}

		public boolean isValid() {
			return valid;
		}

		public String getError() {
			return error;
		}
	}

	public static class BiomeMatcher {

		private final List<TagKey<Biome>> tags;
		private final ResourceKey<Biome> biome;

		private BiomeMatcher(List<TagKey<Biome>> tags, ResourceKey<Biome> biome) {
			this.tags = tags;
			this.biome = biome;
		}

		public static BiomeMatcher tag(String id) {
			return new BiomeMatcher(List.of(TagKey.create(Registries.BIOME, new ResourceLocation(id))), null);
		}

		public static BiomeMatcher anyTag(String... ids) {
			List<TagKey<Biome>> tags = new ArrayList<TagKey<Biome>>();
			for (String id : ids)
				tags.add(TagKey.create(Registries.BIOME, new ResourceLocation(id)));
			return new BiomeMatcher(tags, null);
		}

		public static BiomeMatcher biome(ResourceLocation id) {
			return new BiomeMatcher(List.of(), ResourceKey.create(Registries.BIOME, id));
		}

		public boolean matches(Holder<Biome> holder) {
			if (biome != null)
				return holder.is(biome);
			for (TagKey<Biome> tag : tags)
				if (holder.is(tag))
					return true;
			return false;
		}
	}

	public static class SpawnEntriesModifier implements BiomeModifier {

		@Override
		public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
			if (phase == Phase.ADD)
			{
				for (SpawnEntry entry : ENTRIES)
				{
					if (entry.addsTo(biome))
					{
						// 1.12.2 addSpawn replaced an existing entry of the same entity, so the old one goes first
						List<MobSpawnSettings.SpawnerData> spawners = builder.getMobSpawnSettings().getSpawner(entry.type);
						spawners.removeIf(data -> data.type == entry.entityType);
						builder.getMobSpawnSettings().addSpawn(entry.type, new MobSpawnSettings.SpawnerData(entry.entityType, entry.prob, entry.min, entry.max));
					}
				}
			}
			else if (phase == Phase.REMOVE)
			{
				for (SpawnEntry entry : ENTRIES)
				{
					if (entry.removesFrom(biome))
						builder.getMobSpawnSettings().getSpawner(entry.type).removeIf(data -> data.type == entry.entityType);
				}
			}
		}

		@Override
		public Codec<? extends BiomeModifier> codec() {
			return SPAWN_ENTRIES_CODEC.get();
		}
	}

	public static void initSpawnOption() {
		if (!hasDefaultConfig())
			return;

		RoughConfig.addCustomCategoryComment("SpawnConditions", "Configuration options which affect when Rough Mobs can spawn");
		playerSpawnLevel = RoughConfig.getInteger("SpawnConditions", "_MinPlayerLevel", 0, 0, Short.MAX_VALUE, "Player's Minecraft Experience Level required before a Rough Mob will spawn.");
		isUndergroundEnabled = RoughConfig.getBoolean("SpawnConditions", "_MustBeUnderground", false, "Enable this to require Rough Mobs be underground in order to spawn.");
		// CHANGED: range is -64 to 320, the 1.20.1 overworld height
		maxSpawnHeight = RoughConfig.getInteger("SpawnConditions", "_MaxSpawnHeight", 256, -64, 320, "Set maximum height for Rough Mobs to spawn. Works in conjunction with MustBeUnderground.");
		minDistFromSpawn = RoughConfig.getInteger("SpawnConditions", "_MinDistanceFromSpawn", 0, 0, Integer.MAX_VALUE, "Set the minimum distance from the world spawn before a Rough Mob can spawn.");

		RoughConfig.addCustomCategoryComment("spawnEntries", "Add custom entity spawn entries or override old ones. Takes 5+ values seperated by a semicolon:\n" +
														"Format: entity;chance;min;max;type;biome1;biome2;...\n" +
														"entity:\tEntity name\n" +
														"chance:\tSpawn chance\n" +
														"min:\t\tMinimal group size. Must be greater than 0\n" +
														"max:\t\tMaximal group size\n" +
														"type:\t\tSpawn Type (AMBIENT = day and night, CREATURE = day only, MONSTER = night only and not in peaceful mode, WATER_CREATURE = only in water)\n" +
														"biomes:\tBiome id (minecraft:plains), biome tag (#minecraft:is_nether) or type (FOREST, Can be more than one). Put a \"!\" in front of the biome to revert this feature and disable entity spawning in the biome. Use " + SpawnEntry.OW_TYPE + " for all overworld biomes. Leave this blank for every biome!\n" +
														"Changes apply when a world is loaded.");

		String[] options = RoughConfig.getStringArray("spawnEntries", "_List", Constants.DEFAULT_SPAWN_ENTRIES, "");
		disableBabyZombies = RoughConfig.getBoolean("spawnEntries", "_DisableBabyZombies", false, "Set to true to disable spawning of baby zombies.");

		if (!RoughConfig.isDefining())
			fillEntries(options);
	}

	private static void fillEntries(String[] options) {
		ENTRIES.clear();
		for (String option : options)
		{
			String[] parts = option.split(";");
			if (parts.length >= 5)
			{
				String[] biomes = new String[parts.length - 5];
				for (int i = 0; i < biomes.length; i++)
					biomes[i] = parts[5+i];

				SpawnEntry entry = new SpawnEntry(parts[0], parts[1], parts[2], parts[3], parts[4], biomes);
				if (entry.isValid())
					ENTRIES.add(entry);
				else
					RoughMobs.logger.error("Spawn Entries: " + entry.getError());
			}
			else
			{
				RoughMobs.logger.error("Spawn Entries: Entry \"" + option + "\" needs at least 5 values!");
			}
		}
	}

	// DROPPED: getOverworldBiomes() and addEntries(), replaced by SpawnEntriesModifier

	/*
	 * event is null when the check runs for an entity loaded from disk (deferred a tick), in which case a
	 * disabled baby zombie is discarded instead of having its join cancelled.
	 */
	public static boolean checkSpawnConditions(Mob entity, EntityJoinLevelEvent event) {
		Level world = entity.level();
		Player playerClosest = world.getNearestPlayer(entity, -1.0D);

		// Test to see if the entity is a baby zombie
		if (entity.getType() == EntityType.ZOMBIE) {
			if (entity.isBaby() == true && disableBabyZombies == true) {
				if (event != null)
					event.setCanceled(true);
				else
					entity.discard();
				return false;
			}
		}

		// Test to see if it is the appropriate season to spawn rough mobs
		boolean sereneSeasonsEnabled = CompatHandler.isSereneSeasonsLoaded();
		String currentSeason = null;
		List<String> seasonWhiteList = null;

		if (sereneSeasonsEnabled) {
			currentSeason = SereneSeasonsCompat.getSeason(world);
			seasonWhiteList = Arrays.asList(SereneSeasonsCompat.getSeasonWhitelist());
		}

		if (!sereneSeasonsEnabled || seasonWhiteList.contains(currentSeason)) {

			// Test to see if mob spawn is far enough away from world spawn to be a rough mob.
			BlockPos spawn = world.getSharedSpawnPos();
			double distanceToSpawn = Math.sqrt(entity.distanceToSqr(spawn.getX(), spawn.getY(), spawn.getZ()));
			if (distanceToSpawn >= minDistFromSpawn) {

				// Test to see if closest player is high enough level to spawn as a rough mob
				if (playerClosest != null && playerClosest.experienceLevel >= playerSpawnLevel) {

					// Test to see if  mob is underground
					if(!isUndergroundEnabled || !world.canSeeSky(entity.blockPosition()) && isUndergroundEnabled) {

						// Test to see if mob is below maximum spawn height
						if (entity.blockPosition().getY() <= maxSpawnHeight) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public static boolean hasDefaultConfig() {
		return true;
	}

	public static int getMinDistFromSpawn() {
		return minDistFromSpawn;
	}
}
