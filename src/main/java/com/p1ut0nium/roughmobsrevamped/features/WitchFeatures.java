package com.p1ut0nium.roughmobsrevamped.features;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.p1ut0nium.roughmobsrevamped.RoughMobs;
import com.p1ut0nium.roughmobsrevamped.ai.combat.RoughAIMobBuff;
import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;
import com.p1ut0nium.roughmobsrevamped.misc.FeatureHelper;
import com.p1ut0nium.roughmobsrevamped.util.Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class WitchFeatures extends EntityFeatures {

	private float applyEffectsRange;
	private String[] applyEffectNames;
	private Map<MobEffect, Integer> effects;
	private int lingeringChance;
	private int batsOnDeath;
	private int batsOnDeathRandom;
	private boolean buffAllMobs;
	private String[] buffBlacklist;
	private Set<EntityType<?>> buffBlacklistTypes = new HashSet<>();

	public WitchFeatures() {
		super("witch", EntityType.WITCH);
	}

	public EntityFeatures addPotionHandler(List<EntityFeatures> features) {
		features.add(new WitchPotionFeatures(this));
		return this;
	}

	@Override
	public void initConfig() {
		super.initConfig();

		applyEffectsRange = RoughConfig.getFloat(name, "ApplyEffectsRange", 10, 0, MAX, "Range in each direction from the %ses position in which allied mobs get buffed\nSet to 0 to disable this feature");
		applyEffectNames = RoughConfig.getStringArray(name, "ApplyEffectsNames", Constants.DEFAULT_WITCH_BUFFS, "Potion effects which may be added to nearby mobs\nFormat: effect;amplifier");
		// CHANGED: new in Rough Mobs Redux
		buffAllMobs = RoughConfig.getBoolean(name, "BuffAllMobs", false, "Set this to true to let %ses buff every mob around them (animals and villagers too), not only monsters");
		buffBlacklist = RoughConfig.getStringArray(name, "BuffBlacklist", new String[0], "Entities %ses never buff\nFormat: modid:entity");

		lingeringChance = RoughConfig.getInteger(name, "LingeringChance", 5, 0, MAX, "Chance (1 in X) for a %ses thrown potion to become a lingering potion\nSet to 0 to disable this feature");

		batsOnDeath = RoughConfig.getInteger(name, "BatsOnDeath", 5, 0, MAX, "Amount of bats which spawn on %ses death\nSet to 0 to disable this feature");
		// CHANGED: new in Rough Mobs Redux (the classic Classic Preset sets 3 plus up to 4, so 3 to 7)
		batsOnDeathRandom = RoughConfig.getInteger(name, "BatsOnDeathRandom", 0, 0, MAX, "Up to this many extra bats on top of BatsOnDeath, a random number each death");
	}

	@Override
	public void addAI(EntityJoinLevelEvent event, Mob entity, GoalSelector tasks, GoalSelector targetTasks) {
		if (applyEffectsRange > 0 && !effects.isEmpty())
			tasks.addGoal(1, new RoughAIMobBuff(entity, effects, applyEffectsRange,
					mob -> (buffAllMobs || mob instanceof Monster) && !buffBlacklistTypes.contains(mob.getType())));
	}

	@Override
	public void onDeath(Entity deadEntity, DamageSource source) {
		// CHANGED: plus BatsOnDeathRandom extra bats
		int bats = batsOnDeath + (batsOnDeathRandom > 0 ? deadEntity.level().random.nextInt(batsOnDeathRandom + 1) : 0);
		for (int i = 0; i < bats; i++)
		{
			Bat bat = EntityType.BAT.create(deadEntity.level());
			if (bat == null)
				continue;
			bat.setPos(deadEntity.getX() + Math.random() - Math.random(), deadEntity.getY() + Math.random(), deadEntity.getZ() + Math.random() - Math.random());
			if (deadEntity.level() instanceof ServerLevel)
				bat.finalizeSpawn((ServerLevel) deadEntity.level(), deadEntity.level().getCurrentDifficultyAt(deadEntity.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);

			deadEntity.level().addFreshEntity(bat);
		}
	}

	@Override
	public void postInit() {
		effects = FeatureHelper.getPotionsFromNames(applyEffectNames);

		buffBlacklistTypes = new HashSet<>();
		for (String entityName : buffBlacklist) {
			ResourceLocation key = ResourceLocation.tryParse(entityName.trim());
			if (key != null && ForgeRegistries.ENTITY_TYPES.containsKey(key))
				buffBlacklistTypes.add(ForgeRegistries.ENTITY_TYPES.getValue(key));
			else
				RoughMobs.logger.warn("Rough Mobs Redux: witch BuffBlacklist entry {} is not an entity", entityName);
		}
	}

	public static class WitchPotionFeatures extends EntityFeatures {

		private WitchFeatures features;

		public WitchPotionFeatures(WitchFeatures features) {
			super("potion", EntityType.POTION);
			this.features = features;
		}

		// CHANGED: thrown potions are not mobs, so RoughApplier calls this instead of addFeatures
		public void onPotionJoin(ThrownPotion entity) {
			if (features.lingeringChance <= 0 || !(entity.getOwner() instanceof Witch))
				return;

			if (entity.level().random.nextInt(features.lingeringChance) == 0)
			{
				ItemStack potion = new ItemStack(Items.LINGERING_POTION);
				PotionUtils.setPotion(potion, PotionUtils.getPotion(entity.getItem()));
				entity.setItem(potion);
			}
		}
	}
}
