package com.p1ut0nium.roughmobsrevamped.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

/*
 * Config access for every feature and helper.
 *
 * CHANGED: the 1.12.2 version wrapped a Forge Configuration, which could be read the moment a getter
 * was called. ForgeConfigSpec has to be fully defined before the file is read, so this wrapper runs in
 * two passes over the same feature code:
 *   1. define pass (mod constructor): every getter defines its value in the spec and returns the default.
 *   2. read pass (the config Loading event): every getter returns the value loaded from roughmobs4.toml.
 * The key, category and %s rules are the original ones, so the file keeps the 1.12.2 layout.
 * Changes to the file apply on the next restart.
 */
public class RoughConfig {

	public static final String FILE_NAME = "roughmobs4.toml";

	private static ForgeConfigSpec.Builder builder;
	private static ForgeConfigSpec spec;
	private static final Map<List<String>, ForgeConfigSpec.ConfigValue<?>> VALUES = new HashMap<>();
	private static final Set<String> CATEGORY_COMMENTS = new HashSet<>();
	private static final Map<List<String>, Object> PENDING = new LinkedHashMap<>();

	public static void beginDefine() {
		builder = new ForgeConfigSpec.Builder();
	}

	public static ForgeConfigSpec buildSpec() {
		spec = builder.build();
		builder = null;
		return spec;
	}

	public static boolean isDefining() {
		return builder != null;
	}

	public static void addCustomCategoryComment(String category, String comment) {
		if (builder == null || !CATEGORY_COMMENTS.add(category))
			return;
		builder.comment(comment).push(List.of(category));
		builder.pop();
	}

	private static List<String> path(String name, String id, boolean important) {
		return List.of(name, (important ? "_" : "") + name + id);
	}

	private static ForgeConfigSpec.Builder commented(String description, String name) {
		String desc = description.replace("%s", name);
		return desc.isEmpty() ? builder : builder.comment(desc);
	}

	/*
	 * CHANGED: new. Classic Preset (see classic.ClassicPreset), set once, in the read pass, when _ClassicPreset differs
	 * from _ClassicPresetApplied (RoughApplier.loadConfig), and written by writePending. After that the file is the truth:
	 *   an option the old preset set, still at the old preset's value, takes the new preset's value, or the Rough
	 *   Mobs Revamped default when the new preset does not name it;
	 *   an option only the new preset sets takes its value while it is at the default.
	 * An option changed in the file keeps its value. Returns the changes as "category.key = value".
	 */
	public static List<String> switchPreset(Map<List<String>, Object> from, Map<List<String>, Object> to) {
		List<String> changed = new ArrayList<>();
		for (Map.Entry<List<String>, Object> entry : from.entrySet()) {
			ForgeConfigSpec.ConfigValue<?> value = VALUES.get(entry.getKey());
			if (value != null && sameValue(getValue(entry.getKey()), entry.getValue()))
				setChanged(entry.getKey(), to.containsKey(entry.getKey()) ? to.get(entry.getKey()) : value.getDefault(), changed);
		}
		for (Map.Entry<List<String>, Object> entry : to.entrySet()) {
			ForgeConfigSpec.ConfigValue<?> value = VALUES.get(entry.getKey());
			if (value != null && !from.containsKey(entry.getKey()) && sameValue(getValue(entry.getKey()), value.getDefault()))
				setChanged(entry.getKey(), entry.getValue(), changed);
		}
		changed.sort(null);
		return changed;
	}

	private static void setChanged(List<String> path, Object newValue, List<String> changed) {
		if (sameValue(getValue(path), newValue))
			return;
		set(path, newValue);
		changed.add(String.join(".", path) + " = " + newValue);
	}

	// The keys no option defined, as "category.key"; empty in the define pass
	public static List<String> unknownKeys(Map<List<String>, Object> values) {
		List<String> unknown = new ArrayList<>();
		if (builder == null)
			for (List<String> path : values.keySet())
				if (!VALUES.containsKey(path))
					unknown.add(String.join(".", path));
		return unknown;
	}

	/*
	 * CHANGED: new. Writes in the read pass (Classic Preset, the legacy config importer). A number takes the
	 * option's own type, so 4 can go into a decimal option. The value waits in PENDING, where the rest of the read
	 * pass reads it, and writePending puts it in the file after the read pass.
	 */
	public static void set(List<String> path, Object newValue) {
		Object def = VALUES.get(path).getDefault();
		if (newValue instanceof Number && def instanceof Double)
			newValue = ((Number) newValue).doubleValue();
		else if (newValue instanceof Number && def instanceof Integer)
			newValue = ((Number) newValue).intValue();
		PENDING.put(path, newValue);
	}

	/*
	 * CHANGED: new. Forge's config saves the file on every ConfigValue.set, and each save makes Forge's file watcher
	 * reload it on another thread, clearing and refilling every value: a read during that gets the default, and a
	 * set during it can be lost. So nothing is written until the read pass is over (RoughMobs.onConfigLoad), and
	 * then the values go into their category tables, which do not save, and the file is saved once.
	 * Returns how many values were written.
	 */
	public static int writePending(ModConfig config) {
		int count = PENDING.size();
		if (count == 0)
			return 0;
		CommentedConfig data = config.getConfigData();
		for (Map.Entry<List<String>, Object> entry : PENDING.entrySet()) {
			List<String> path = entry.getKey();
			Object table = data.get(path.subList(0, 1));
			if (table instanceof Config category)
				category.set(path.subList(1, 2), entry.getValue());
			else
				data.set(path, entry.getValue());
		}
		PENDING.clear();
		config.save();
		return count;
	}

	// The option a 1.12 config names: the category matched without case (Forge 1.12 wrote it in lower case), the key exactly
	public static List<String> findOption(String category, String key) {
		for (List<String> path : VALUES.keySet())
			if (path.get(0).equalsIgnoreCase(category) && path.get(1).equals(key))
				return path;
		return null;
	}

	public static Object getValue(List<String> path) {
		if (PENDING.containsKey(path))
			return PENDING.get(path);
		ForgeConfigSpec.ConfigValue<?> value = VALUES.get(path);
		return value == null ? null : value.get();
	}

	public static Object getDefault(List<String> path) {
		return VALUES.get(path).getDefault();
	}

	// The option's type and range check, null for a path no option defined
	public static ForgeConfigSpec.ValueSpec valueSpec(List<String> path) {
		Object spec = RoughConfig.spec == null ? null : RoughConfig.spec.getSpec().get(path);
		return spec instanceof ForgeConfigSpec.ValueSpec ? (ForgeConfigSpec.ValueSpec) spec : null;
	}

	public static List<String> key(String name, String id) {
		return path(name, id, false);
	}

	public static List<String> importantKey(String name, String id) {
		return path(name, id, true);
	}

	@SuppressWarnings("unchecked")
	private static <T> T read(List<String> path, T defaultValue) {
		Object value = getValue(path);
		return value == null ? defaultValue : (T) value;
	}

	private static boolean sameValue(Object a, Object b) {
		if (a instanceof Number && b instanceof Number)
			return Float.compare(((Number) a).floatValue(), ((Number) b).floatValue()) == 0;
		return Objects.equals(a, b);
	}

	public static boolean getBoolean(String name, String id, boolean defaultValue, String description, boolean important) {
		List<String> path = path(name, id, important);
		if (builder != null) {
			if (!VALUES.containsKey(path))
				VALUES.put(path, commented(description, name).define(path, defaultValue));
			return defaultValue;
		}
		return read(path, defaultValue);
	}

	public static boolean getBoolean(String name, String id, boolean defaultValue, String description) {
		return getBoolean(name, id, defaultValue, description, false);
	}

	public static int getInteger(String name, String id, int defaultValue, int min, int max, String description, boolean important) {
		List<String> path = path(name, id, important);
		if (builder != null) {
			if (!VALUES.containsKey(path))
				VALUES.put(path, commented(description, name).defineInRange(path, defaultValue, min, max));
			return defaultValue;
		}
		return read(path, defaultValue);
	}

	public static int getInteger(String name, String id, int defaultValue, int min, int max, String description) {
		return getInteger(name, id, defaultValue, min, max, description, false);
	}

	public static float getFloat(String name, String id, float defaultValue, float min, float max, String description, boolean important) {
		List<String> path = path(name, id, important);
		if (builder != null) {
			if (!VALUES.containsKey(path))
				VALUES.put(path, commented(description, name).defineInRange(path, toDouble(defaultValue), toDouble(min), toDouble(max)));
			return defaultValue;
		}
		Number value = read(path, (Number) Double.valueOf(defaultValue));
		return value.floatValue();
	}

	public static float getFloat(String name, String id, float defaultValue, float min, float max, String description) {
		return getFloat(name, id, defaultValue, min, max, description, false);
	}

	public static String[] getStringArray(String name, String id, String[] defaultValue, String description, boolean important) {
		List<String> path = path(name, id, important);
		if (builder != null) {
			if (!VALUES.containsKey(path))
				VALUES.put(path, commented(description, name).defineList(path, Arrays.asList(defaultValue), o -> o instanceof String));
			return defaultValue;
		}
		List<?> list = read(path, Arrays.asList(defaultValue));
		List<String> out = new ArrayList<>();
		for (Object o : list)
			out.add(String.valueOf(o));
		return out.toArray(new String[0]);
	}

	public static String[] getStringArray(String name, String id, String[] defaultValue, String description) {
		return getStringArray(name, id, defaultValue, description, false);
	}

	public static String getString(String name, String id, String defaultValue, String description, boolean important) {
		List<String> path = path(name, id, important);
		if (builder != null) {
			if (!VALUES.containsKey(path))
				VALUES.put(path, commented(description, name).define(path, defaultValue));
			return defaultValue;
		}
		return read(path, defaultValue);
	}

	public static String getString(String name, String id, String defaultValue, String description) {
		return getString(name, id, defaultValue, description, false);
	}

	// Float.toString keeps the short decimal form (0.1F stays 0.1 in the file instead of 0.10000000149011612)
	private static double toDouble(float f) {
		return Double.parseDouble(Float.toString(f));
	}
}
