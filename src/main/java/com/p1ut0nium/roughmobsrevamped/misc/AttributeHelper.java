package com.p1ut0nium.roughmobsrevamped.misc;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.UUID;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.p1ut0nium.roughmobsrevamped.RoughMobs;
import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;
import com.p1ut0nium.roughmobsrevamped.util.Constants;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.ForgeRegistries;

public class AttributeHelper {

	private static Multimap<EntityType<?>, AttributeEntry> map = ArrayListMultimap.create();

	public static final String KEY_ATTRIBUTES = Constants.unique("attributesApplied");

	public static class AttributeEntry {

		private UUID uuid;
		private Attribute attribute;
		private int operator;
		private double value;
		private String dimension;
		private int child;

		public AttributeEntry(UUID uuid, Attribute attribute, int operator, double value, String dimension, int child) {
			this.uuid = uuid;
			this.attribute = attribute;
			this.operator = operator;
			this.value = value;
			this.dimension = dimension;
			this.child = child;
		}

		public UUID getUuid() {
			return uuid;
		}

		public Attribute getAttribute() {
			return attribute;
		}

		public int getOperator() {
			return operator;
		}

		public double getValue() {
			return value;
		}

		public String getDimension() {
			return dimension;
		}

		public int getChild() {
			return child;
		}

		public boolean hasDimension() {
			return dimension != null;
		}

		public boolean checkChild(LivingEntity entity) {
			return child == 0 || (child == 1 && !entity.isBaby()) || (child == 2 && entity.isBaby());
		}

		// CHANGED: the 1.12.2 version parsed the dimension column and never checked it
		public boolean checkDimension(LivingEntity entity) {
			return !hasDimension() || entity.level().dimension().location().toString().equals(dimension);
		}
	}

	public static void initAttributeOption() {
		RoughConfig.addCustomCategoryComment("attributes", "Add attribute modifiers to entities to change their stats. Takes 4-6 values seperated by a semicolon:\n"
																		+ "Format: entity;attribute;operator;value;dimension;child\n"
																		+ "entity:\t\tentity name\n"
																		+ "attribute:\tattribute name (Possible attributes: " + getAttributesString() + ")\n"
																		+ "operator:\t\toperator type (0 = add, 1 = multiply and add)\n"
																		+ "value:\t\tvalue which will be used for the calculation\n"
																		+ "dimension:\tdimension id (e.g. minecraft:the_nether) in which the entity should get the boost (optional! Leave this blank or use a \"/\" for any dimension)\n"
																		+ "child:\t0 = the modifier doesn't care if the entity is a child or not, 1 = adults only, 2 = childs only (optional! Leave this blank for 0)");

		String[] options = RoughConfig.getStringArray("attributes", "Modifier", Constants.ATTRIBUTE_DEFAULT, "Attributes:");
		if (!RoughConfig.isDefining())
			fillMap(options);
	}

	/*
	 * CHANGED: the 1.12.2 version made a modifier with a random UUID, so hasModifier never matched.
	 * The UUID now comes from the name, so a modifier is only applied once.
	 */
	public static boolean applyAttributeModifier(LivingEntity entity, Attribute attribute, String name, int operator, double amount) {
		if (amount != 0)
		{
			AttributeInstance instance = entity.getAttribute(attribute);
			UUID uuid = UUID.nameUUIDFromBytes(Constants.unique(name).getBytes(StandardCharsets.UTF_8));
			if (instance != null && instance.getModifier(uuid) == null)
			{
				instance.addPermanentModifier(new AttributeModifier(uuid, Constants.unique(name), amount, AttributeModifier.Operation.fromValue(operator)));
				if (attribute == Attributes.MAX_HEALTH)
					entity.setHealth(entity.getMaxHealth());
				return true;
			}
		}
		return false;
	}

	public static void addAttributes(LivingEntity entity) {
		if (entity.getPersistentData().getBoolean(KEY_ATTRIBUTES))
			return;

		Collection<AttributeEntry> attributes = map.get(entity.getType());

		int i = 0;
		for (AttributeEntry attribute : attributes)
		{
			if (!attribute.checkChild(entity) || !attribute.checkDimension(entity))
				continue;

			AttributeInstance instance = entity.getAttribute(attribute.getAttribute());
			if (instance != null)
			{
				if (instance.getModifier(attribute.getUuid()) == null)
				{
					AttributeModifier modifier = new AttributeModifier(attribute.getUuid(), Constants.unique("mod" + i), attribute.getValue(), AttributeModifier.Operation.fromValue(attribute.getOperator()));
					instance.addPermanentModifier(modifier);
				}

				if (attribute.getAttribute() == Attributes.MAX_HEALTH)
					entity.setHealth(entity.getMaxHealth());
			}
			else
				RoughMobs.logger.error("Error on attribute modification: \"" + ForgeRegistries.ATTRIBUTES.getKey(attribute.getAttribute()) + "\" is not a valid attribute. Affected Entity: " + entity);
			i++;
		}
		entity.getPersistentData().putBoolean(KEY_ATTRIBUTES, true);
	}

	private static void fillMap(String[] options) {
		map = ArrayListMultimap.create();

		int index = 0;
		for (String line : options)
		{
			index++;
			String[] pars = line.split(";");
			if (pars.length >= 4)
			{
				ResourceLocation entityLoc = Constants.legacyLocation(pars[0]);
				EntityType<?> entityType = entityLoc != null && ForgeRegistries.ENTITY_TYPES.containsKey(entityLoc) ? ForgeRegistries.ENTITY_TYPES.getValue(entityLoc) : null;
				if (entityType != null)
				{
					ResourceLocation attributeLoc = Constants.legacyLocation(pars[1]);
					Attribute attribute = attributeLoc != null ? ForgeRegistries.ATTRIBUTES.getValue(attributeLoc) : null;
					if (attribute == null)
					{
						RoughMobs.logger.error("Error on attribute initialization: \"" + pars[1] + "\" is not a valid attribute: " + line);
						continue;
					}
					try
					{
						int operator = Integer.parseInt(pars[2].trim());
						double value = Double.parseDouble(pars[3].trim());
						String dimension = pars.length >= 5 && !pars[4].trim().equals("/") && !pars[4].trim().isEmpty() ? Constants.legacyDimension(pars[4]) : null;
						int child = pars.length >= 6 ? Integer.parseInt(pars[5].trim()) : 0;

						if (child < 0 || child > 2)
						{
							RoughMobs.logger.error("Error on attribute initialization: child is not between 0 and 2: " + line);
							continue;
						}
						if (operator < 0 || operator > 2)
						{
							RoughMobs.logger.error("Error on attribute initialization: operator is not between 0 and 2: " + line);
							continue;
						}

						// CHANGED: fixed UUID per config line instead of UUID.randomUUID(), so a saved modifier is recognised after a restart
						UUID uuid = UUID.nameUUIDFromBytes((Constants.MODID + ":attribute:" + index + ":" + line).getBytes(StandardCharsets.UTF_8));
						map.put(entityType, new AttributeEntry(uuid, attribute, operator, value, dimension, child));
					}
					catch(NumberFormatException e)
					{
						RoughMobs.logger.error("Error on attribute initialization: Invalid numbers: " + line);
					}
				}
				else
					RoughMobs.logger.error("Error on attribute initialization: Entity " + pars[0] + " does not exist");
			}
			else
				RoughMobs.logger.error("Error on attribute initialization: Wrong amount of arguments: " + line);
		}
	}

	// CHANGED: lists the attribute registry instead of reflecting over SharedMonsterAttributes
	private static String getAttributesString() {
		StringBuilder attributes = new StringBuilder();
		for (ResourceLocation key : BuiltInRegistries.ATTRIBUTE.keySet())
		{
			attributes.append(", ").append(key.getNamespace().equals("minecraft") ? key.getPath() : key.toString());
		}
		return attributes.length() > 2 ? attributes.substring(2) : "";
	}
}
