package com.p1ut0nium.roughmobsrevamped.features;

import java.util.ArrayList;
import java.util.List;

import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;
import com.p1ut0nium.roughmobsrevamped.misc.FeatureHelper;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class EndermanFeatures extends EntityFeatures {

	private boolean dropEnderpearl;
	private boolean teleportAttacker;
	private String immunityItem;
	private int blindChance;
	private int blindDuration;
	private boolean blindOrNausea;
	private int stealItemChance;

	private Item immunityItemItem;

	public EndermanFeatures() {
		super("enderman", EntityType.ENDERMAN);
	}

	@Override
	public void initConfig() {
		super.initConfig();

		dropEnderpearl = RoughConfig.getBoolean(name, "DropEnderpearl", true, "Set this to false to prevent endermen from dropping an extra enderpearl on death");
		teleportAttacker = RoughConfig.getBoolean(name, "TeleportAttacker", true, "Set this to false to prevent endermen from teleporting attackers away");
		immunityItem = RoughConfig.getString(name, "ImmunityItem", "minecraft:ender_eye", "If this item is somewhere in the players inventory, the player becomes immune to teleportation and item stealing\nLeave this empty to disable this feature");

		blindChance = RoughConfig.getInteger(name, "BlindChance", 3, 0, MAX, "Chance (1 in X) that an %s applies the blindness effect to its target\nSet to 0 to disable this feature");
		blindDuration = RoughConfig.getInteger(name, "BlindDuration", 200, 1, MAX, "Duration in ticks of the applied blindness effect (20 ticks = 1 second)");
		// RM1: endermanPotionChance gave blindness or nausea
		blindOrNausea = RoughConfig.getBoolean(name, "BlindOrNausea", false, "Set this to true to make each BlindChance hit give blindness or nausea at random");

		stealItemChance = RoughConfig.getInteger(name, "StealItemChance", 3, 0, MAX, "Chance (1 in X) that an %s steals the targets held or equipped item to drop it on the ground\nSet to 0 to disable this feature");
	}

	@Override
	public void postInit() {
		immunityItemItem = itemFromName(immunityItem);
	}

	static Item itemFromName(String itemName) {
		if (itemName == null || itemName.isEmpty())
			return null;
		ResourceLocation loc = ResourceLocation.tryParse(itemName);
		if (loc == null || !ForgeRegistries.ITEMS.containsKey(loc))
			return null;
		Item item = ForgeRegistries.ITEMS.getValue(loc);
		return item == Items.AIR ? null : item;
	}

	@Override
	public void onDeath(Entity deadEntity, DamageSource source) {
		if (dropEnderpearl)
			deadEntity.spawnAtLocation(Items.ENDER_PEARL, 1);
	}

	@Override
	public void onDefend(Entity target, Entity attacker, Entity immediateAttacker, LivingDamageEvent event) {
		if (teleportAttacker && attacker instanceof LivingEntity)
		{
			if (isImmuneToTeleport(attacker, immunityItemItem))
			{
				FeatureHelper.playSound(attacker, SoundEvents.ENDER_EYE_DEATH);
			}
			else
				teleportRandom((LivingEntity)attacker, 24);
		}
	}

	@Override
	public void onAttack(Entity attacker, Entity immediateAttacker, Entity target, LivingDamageEvent event) {
		if (!(target instanceof LivingEntity))
			return;

		if (blindChance > 0 && RND.nextInt(blindChance) == 0)
		{
			((LivingEntity)target).addEffect(new MobEffectInstance(blindOrNausea && RND.nextBoolean() ? MobEffects.CONFUSION : MobEffects.BLINDNESS, blindDuration));
		}

		if (stealItemChance > 0 && RND.nextInt(stealItemChance) == 0)
		{
			// CHANGED: the original checked the enderman's inventory for the immunity item, which is never a
			// player, so the item never protected anyone from theft; the config text says the player is immune
			if (isImmuneToTeleport(target, immunityItemItem))
			{
				FeatureHelper.playSound(target, SoundEvents.ENDER_EYE_DEATH);
			}
			else
				tryDropHeldItem((LivingEntity)target, attacker);
		}
	}

	private void tryDropHeldItem(LivingEntity target, Entity attacker) {
		List<InteractionHand> filledHands = new ArrayList<InteractionHand>();

		if (!target.getMainHandItem().isEmpty())
			filledHands.add(InteractionHand.MAIN_HAND);

		if (!target.getOffhandItem().isEmpty())
			filledHands.add(InteractionHand.OFF_HAND);

		if (filledHands.isEmpty())
			return;

		InteractionHand hand = filledHands.get(RND.nextInt(filledHands.size()));
		ItemStack heldStack = target.getItemInHand(hand).copy();

		target.setItemInHand(hand, ItemStack.EMPTY);
		attacker.spawnAtLocation(heldStack, (float)(Math.random() + 0.5f));
	}

	public static boolean teleportRandom(LivingEntity entity) {
		return teleportRandom(entity, 64);
	}

	public static boolean teleportRandom(LivingEntity entity, double multi) {
		if (entity == null || entity instanceof FakePlayer || !entity.isAlive())
			return false;

		double x = entity.getX() + (RND.nextDouble() - 0.5D) * multi;
		double y = entity.getY() + (RND.nextInt(64) - 32);
		double z = entity.getZ() + (RND.nextDouble() - 0.5D) * multi;

		EntityTeleportEvent.EnderEntity event = ForgeEventFactory.onEnderTeleport(entity, x, y, z);
		if (event.isCanceled())
			return false;

		boolean flag = entity.randomTeleport(event.getTargetX(), event.getTargetY(), event.getTargetZ(), true);
		if (flag)
		{
			FeatureHelper.playSound(entity, SoundEvents.ENDERMAN_TELEPORT);
		}

		return flag;
	}

	static boolean isImmuneToTeleport(Entity entity, Item item) {
		return item != null && entity instanceof Player && ((Player)entity).getInventory().contains(new ItemStack(item));
	}
}
