package com.p1ut0nium.roughmobsrevamped.classic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.p1ut0nium.roughmobsrevamped.RoughMobs;
import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;
import com.p1ut0nium.roughmobsrevamped.util.Constants;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.loading.FMLPaths;

/*
 * CHANGED: new in Rough Mobs 4 (lineage brief item B14). Copies a Rough Mobs 2 roughmobs.cfg or a Rough Mobs
 * Revamped 1.12.2 roughmobsrevamped.cfg into roughmobs4.toml. Both are Forge 1.12 Configuration text: "#" comments,
 * "name {" ... "}" categories (names with a space are quoted), "B:", "I:", "D:" and "S:" lines "name=value", and
 * lists "S:name <" with one entry per line, closed by ">".
 * Rough Mobs 4 kept Revamped's key names and Revamped kept most of Rough Mobs 2's, so a key maps to the option with
 * the same name in the same category (1.12 wrote categories in lower case). RENAMED holds the Rough Mobs 2 names
 * Revamped changed. List entries are copied as written: the read pass translates 1.12 names in them
 * (Constants.legacyName, legacyDimension), and the report lists the ones it will translate.
 * Runs in the read pass when Classic _ImportLegacyConfig names a file in the config folder (RoughApplier.loadConfig),
 * then clears that option and writes config/roughmobs4-import.log.
 */
public final class LegacyConfigImporter {

	public static final String LOG_NAME = "roughmobs4-import.log";

	// Rough Mobs 2 key -> Rough Mobs 4 key, same category. Every other key has the same name in both.
	private static final Map<String, String> RENAMED = Map.of(
			"zombieBossChance", "zombie_BossChance",
			"zombieBossNames", "zombie_BossNames",
			"skeletonBossChance", "skeleton_BossChance",
			"skeletonBossNames", "skeleton_BossNames",
			"spawnEntriesList", "spawnEntries_List",
			"targetBlockerEnabled", "targetBlocker_Enabled",
			"targetBlockerList", "targetBlocker_List");

	private static final Pattern VALUE = Pattern.compile("^([BIDS]):(\"?)(.+?)\\2\\s*(?:(<)|=(.*))$");

	private LegacyConfigImporter() {
	}

	// One value of the legacy file: list is null for a single value, value is null for a list
	public record Entry(String category, String key, String value, List<String> list) {
	}

	public static final class Report {
		public int entries;
		public final List<String> mapped = new ArrayList<>();
		public final List<String> renamed = new ArrayList<>();
		public final List<String> outOfRange = new ArrayList<>();
		public final List<String> invalid = new ArrayList<>();
		public final List<String> unmapped = new ArrayList<>();
		public final Set<String> translated = new TreeSet<>();
		public final List<String> notes = new ArrayList<>();
		// What importConfig sets: option path -> value of the option's type
		public final Map<List<String>, Object> values = new LinkedHashMap<>();

		public List<String> lines(String source) {
			List<String> out = new ArrayList<>();
			out.add("Rough Mobs 4 import of " + source + ", " + LocalDateTime.now().withNano(0));
			out.add("values in the file " + entries);
			section(out, "mapped", mapped);
			section(out, "renamed from Rough Mobs 2 names", renamed);
			section(out, "out of range, set to the nearest allowed value", outOfRange);
			section(out, "not a value the option takes, skipped", invalid);
			section(out, "unmapped, no Rough Mobs 4 option", unmapped);
			section(out, "translated when read, 1.12 names in list entries", translated);
			section(out, "notes", notes);
			return out;
		}

		private static void section(List<String> out, String title, java.util.Collection<String> items) {
			out.add("");
			out.add(title + " " + items.size());
			for (String item : items)
				out.add("  " + item);
		}
	}

	/*
	 * The read-pass entry point. name is the file under config/. The values are set, the option is cleared and the
	 * report written next to it; RoughConfig.writePending puts them in the toml after the read pass. Nothing is set
	 * when the file cannot be read or mapped, and a name that is not a file in the config folder stays set; both are
	 * logged at each start.
	 */
	public static void importConfig(String name) {
		Path configDir = FMLPaths.CONFIGDIR.get().toAbsolutePath().normalize();
		Path file = configDir.resolve(name).normalize();
		if (!file.startsWith(configDir) || !Files.isRegularFile(file)) {
			RoughMobs.logger.warn("Rough Mobs 4: Classic _ImportLegacyConfig {} is not a file in the config folder", name);
			return;
		}

		Report report;
		try {
			report = read(file);
		}
		catch (IOException | RuntimeException e) {
			RoughMobs.logger.error("Rough Mobs 4: could not import " + name + ", nothing was changed", e);
			return;
		}
		report.values.forEach(RoughConfig::set);
		RoughConfig.set(RoughConfig.importantKey("Classic", "ImportLegacyConfig"), "");
		try {
			Files.write(configDir.resolve(LOG_NAME), report.lines("config/" + configDir.relativize(file).toString().replace('\\', '/')), StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			RoughMobs.logger.error("Rough Mobs 4: could not write config/" + LOG_NAME, e);
		}
		RoughMobs.logger.info("Rough Mobs 4: imported {} values from {}, {} with no Rough Mobs 4 option, see config/{}",
				report.mapped.size(), name, report.unmapped.size(), LOG_NAME);
	}

	// Maps every value of a legacy file to an option, into report.values; sets nothing
	public static Report read(Path file) throws IOException {
		String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
		List<Entry> entries = parse(text.split("\\R"));
		Report report = new Report();
		report.entries = entries.size();
		Set<String> bosses = new TreeSet<>();

		for (Entry entry : entries) {
			String legacy = entry.category() + "." + entry.key();
			String key = RENAMED.getOrDefault(entry.key(), entry.key());
			List<String> path = RoughConfig.findOption(entry.category(), key);
			// The Classic options are Rough Mobs 4's own; no legacy file had them, and the importer's own key is there
			if (path == null || path.get(0).equals("Classic")) {
				report.unmapped.add(legacy + " = " + shown(entry));
				continue;
			}
			String option = String.join(".", path);
			if (!key.equals(entry.key()))
				report.renamed.add(legacy + " -> " + option);

			Object value = convert(entry, RoughConfig.getDefault(path));
			if (value == null) {
				report.invalid.add(option + " = " + shown(entry));
				continue;
			}
			ForgeConfigSpec.ValueSpec spec = RoughConfig.valueSpec(path);
			if (spec != null && !spec.test(value)) {
				Object corrected = spec.correct(value);
				report.outOfRange.add(option + " = " + value + ", set to " + corrected);
				value = corrected;
			}
			if (entry.list() != null)
				for (String item : entry.list())
					for (String part : item.split(";")) {
						String translated = Constants.legacyName(part);
						if (!translated.equals(part.trim()))
							report.translated.add(part.trim() + " -> " + translated);
					}

			report.values.put(path, value);
			report.mapped.add(option + " = " + (entry.list() != null ? entry.list() : value));
			if (path.get(1).endsWith("_BossChance") && value instanceof Integer && (Integer) value > 0)
				bosses.add(path.get(0));
		}

		// Rough Mobs 2 and Revamped 1.12.2 rolled bosses whenever a BossChance was above 0
		if (!bosses.isEmpty() && Boolean.FALSE.equals(RoughConfig.getValue(RoughConfig.importantKey("BossGlobal", "_BossesEnabled"))))
			report.notes.add("the file rolls " + String.join(" and ", bosses) + " bosses; they stay off until BossGlobal._BossGlobal_BossesEnabled = true");
		// Only a Rough Mobs 2 file has the Rough Mobs 2 key names
		String preset = String.valueOf(RoughConfig.getValue(RoughConfig.importantKey("Classic", "Preset"))).trim().toLowerCase(Locale.ROOT);
		if (!report.renamed.isEmpty() && !preset.equals(ClassicPreset.RM2) && !preset.equals(ClassicPreset.CLASSIC))
			report.notes.add("a Rough Mobs 2 file: Rough Mobs 2 had numbers it never wrote to its config (blaze push strength 4 and others); "
					+ "Classic._ClassicPreset = \"rm2\" sets them, and set together with _ClassicImportLegacyConfig the imported values still win");
		return report;
	}

	public static List<Entry> parse(String[] lines) {
		List<Entry> entries = new ArrayList<>();
		Deque<String> categories = new ArrayDeque<>();
		String listKey = null;
		List<String> list = null;

		for (String raw : lines) {
			String line = raw.trim();
			if (list != null) {
				if (line.equals(">")) {
					entries.add(new Entry(String.join(".", categories), listKey, null, list));
					list = null;
				}
				else if (!line.isEmpty())
					list.add(line);
				continue;
			}
			if (line.isEmpty() || line.startsWith("#"))
				continue;
			if (line.equals("}")) {
				categories.pollLast();
				continue;
			}
			// A value line first, so a value ending in "{" is not taken for a category
			Matcher m = VALUE.matcher(line);
			if (!m.matches()) {
				if (line.endsWith("{"))
					categories.addLast(unquote(line.substring(0, line.length() - 1).trim()));
				continue;
			}
			if (m.group(4) != null) {
				listKey = m.group(3);
				list = new ArrayList<>();
			}
			else
				entries.add(new Entry(String.join(".", categories), m.group(3), m.group(5), null));
		}
		return entries;
	}

	private static String unquote(String name) {
		return name.length() >= 2 && name.startsWith("\"") && name.endsWith("\"") ? name.substring(1, name.length() - 1) : name;
	}

	private static String shown(Entry entry) {
		return entry.list() != null ? entry.list().toString() : entry.value();
	}

	// The legacy text as the option's type, or null when it is not one; a float went into a 1.12 file as S:
	private static Object convert(Entry entry, Object def) {
		if (def instanceof List)
			return entry.list() != null ? new ArrayList<>(entry.list()) : null;
		if (entry.list() != null)
			return null;
		String value = entry.value().trim();
		if (def instanceof Boolean)
			return value.equalsIgnoreCase("true") ? Boolean.TRUE : value.equalsIgnoreCase("false") ? Boolean.FALSE : null;
		if (def instanceof String)
			return entry.value();
		try {
			if (def instanceof Double)
				return Double.parseDouble(value);
			// A whole number written as a decimal ("3.0") is taken too
			if (def instanceof Integer) {
				double number = Double.parseDouble(value);
				return number == Math.rint(number) && Math.abs(number) <= Integer.MAX_VALUE ? (Object) (int) number : null;
			}
		}
		catch (NumberFormatException e) {
			return null;
		}
		return null;
	}
}
