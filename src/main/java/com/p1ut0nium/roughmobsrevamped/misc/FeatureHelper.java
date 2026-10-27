package com.p1ut0nium.roughmobsrevamped.misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.p1ut0nium.roughmobsrevamped.RoughMobs;
import com.p1ut0nium.roughmobsrevamped.util.Constants;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

public class FeatureHelper {

	public static boolean removeTask(Mob entity, Class<? extends Goal> aiClass) {
		return tryRemoveTask(entity.goalSelector, aiClass) || tryRemoveTask(entity.targetSelector, aiClass);
	}

	private static boolean tryRemoveTask(GoalSelector tasks, Class<? extends Goal> aiClass) {
		Goal found = null;
		for (WrappedGoal ai : tasks.getAvailableGoals())
		{
			if (aiClass.isInstance(ai.getGoal()))
			{
				found = ai.getGoal();
				break;
			}
		}
		if (found != null)
		{
			tasks.removeGoal(found);
			return true;
		}
		return false;
	}

	public static boolean addEffect(LivingEntity entity, MobEffect effect, int duration, int startAmplifier, int chance, boolean isIncreasing, int maxAmplifier) {
		if (entity == null || effect == null || startAmplifier < 0 || chance <= 0 || duration <= 0 || entity.getRandom().nextInt(chance) != 0)
			return false;

		int amplifier = startAmplifier;

		if (isIncreasing)
		{
			MobEffectInstance active = entity.getEffect(effect);
			if (active != null)
			{
				if (active.getDuration() == duration)
					return false;

				amplifier = Math.min(startAmplifier + active.getAmplifier() + 1, maxAmplifier);
			}
		}

		entity.addEffect(new MobEffectInstance(effect, duration, amplifier));
		return true;
	}

	public static boolean addEffect(LivingEntity entity, MobEffect effect, int duration, int amplifier, int chance) {
		return addEffect(entity, effect, duration, amplifier, chance, false, 127);
	}

	public static boolean addEffect(LivingEntity entity, MobEffect effect, int duration, int amplifier) {
		return addEffect(entity, effect, duration, amplifier, 1);
	}

	public static boolean addEffect(LivingEntity entity, MobEffect effect, int duration) {
		return addEffect(entity, effect, duration, 0);
	}

	/*
	 * CHANGED: the 1.12.2 call went through World.spawnParticle, which only reaches client listeners,
	 * so on a server these particles never showed. ServerLevel.sendParticles with a count of 0 sends
	 * one particle per call with the same motion vector the original computed.
	 */
	public static void spawnParticle(Entity entity, ParticleOptions type, float spread, int amount) {
		if (!(entity.level() instanceof ServerLevel))
			return;
		ServerLevel level = (ServerLevel) entity.level();
		RandomSource rnd = level.random;
		for (int i = 0; i < amount; i++)
		{
			double moveX = (rnd.nextDouble() - 0.5D) * 2.0D * spread;
			double moveY = -rnd.nextDouble() * spread;
			double moveZ = (rnd.nextDouble() - 0.5D) * 2.0D * spread;
			level.sendParticles(type, entity.getX() + (rnd.nextDouble() - 0.5D) * (double)entity.getBbWidth(), entity.getY() + rnd.nextDouble() * (double)entity.getBbHeight() - 0.25D, entity.getZ() + (rnd.nextDouble() - 0.5D) * (double)entity.getBbWidth(), 0, moveX, moveY, moveZ, 1.0D);
		}
	}

	public static void spawnParticle(Entity entity, ParticleOptions type, int amount) {
		spawnParticle(entity, type, 1, amount);
	}

	public static void spawnParticle(Entity entity, ParticleOptions type) {
		spawnParticle(entity, type, 1);
	}

	public static void playSound(Entity entity, SoundEvent event, float volume, float pitch) {
		entity.level().playSound(null, entity.xo, entity.yo, entity.zo, event, entity.getSoundSource(), volume, pitch);
		entity.playSound(event, volume, pitch);
	}

	public static void playSound(Entity entity, SoundEvent event) {
		playSound(entity, event, 1.0F, 1.0F);
	}

	public static void knockback(Entity attacker, LivingEntity target, float strength, float extraLeap) {
		double xRatio = attacker.getX() - target.getX();
		double zRatio = attacker.getZ() - target.getZ();
		target.knockback(strength, xRatio, zRatio);
		target.setDeltaMovement(target.getDeltaMovement().add(0, extraLeap, 0));
		// CHANGED: a server-side velocity change only reaches the client when hurtMarked is set
		target.hurtMarked = true;
	}

	public static <T extends Collection<Block>> T getBlocksFromNames(String[] blockNames, T blocks) {
		for (String name : blockNames)
		{
			ResourceLocation loc = Constants.legacyLocation(name);
			if (loc == null || !ForgeRegistries.BLOCKS.containsKey(loc))
				RoughMobs.logger.error(name + " isn't a valid block!");
			else
				blocks.add(ForgeRegistries.BLOCKS.getValue(loc));
		}
		return blocks;
	}

	public static List<EntityType<?>> getEntitiesFromNames(String[] entitieNames) {
		List<EntityType<?>> entities = new ArrayList<EntityType<?>>();
		for (String name : entitieNames)
		{
			ResourceLocation loc = Constants.legacyLocation(name);
			if (loc == null || !ForgeRegistries.ENTITY_TYPES.containsKey(loc))
				RoughMobs.logger.error(name + " isn't a valid entity!");
			else
				entities.add(ForgeRegistries.ENTITY_TYPES.getValue(loc));
		}
		return entities;
	}

	public static Map<MobEffect, Integer> getPotionsFromNames(String[] potionNames) {
		Map<MobEffect, Integer> potions = new HashMap<MobEffect, Integer>();
		for (String name : potionNames)
		{
			String[] parts = name.split(";");
			if (parts.length < 2)
				continue;

			ResourceLocation loc = Constants.legacyLocation(parts[0]);
			MobEffect potion = loc == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(loc);
			if (potion == null)
				RoughMobs.logger.error(parts[0] + " isn't a valid potion effect!");
			else
			{
				try
				{
					potions.put(potion, Integer.parseInt(parts[1].trim()));
				}
				catch(NumberFormatException e)
				{
					RoughMobs.logger.error(parts[1] + " isn't a valid number!");
				}
			}
		}
		return potions;
	}
}
