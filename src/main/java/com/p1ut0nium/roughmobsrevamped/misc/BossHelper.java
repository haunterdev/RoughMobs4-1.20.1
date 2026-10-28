package com.p1ut0nium.roughmobsrevamped.misc;

import java.util.List;
import java.util.Random;

import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;
import com.p1ut0nium.roughmobsrevamped.entities.IBoss;
import com.p1ut0nium.roughmobsrevamped.init.EntityInit;
import com.p1ut0nium.roughmobsrevamped.misc.EquipHelper.EquipmentApplier;
import com.p1ut0nium.roughmobsrevamped.util.Constants;
import com.p1ut0nium.roughmobsrevamped.util.handlers.SoundHandler;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;

public class BossHelper {

	public static final Random RND = new Random();
	public static final String BOSS = Constants.unique("isBoss");

	public static boolean bossesEnabled;
	private static boolean bossWarning;
	private static boolean bossWarningSound;
	private static int bossWarningDist;
	public static boolean bossLightning = true;
	private static String[] bossHonorifics = new String[0];

	public static boolean bossFogEnabled;
	public static String[] bossFogColor;
	// The fog colour parsed once from bossFogColor, red, green, blue
	public static float[] bossFogColorRGB = new float[] {0.1F, 0.4F, 0.1F};
	public static int bossFogMaxDistance;
	public static int bossFogFarPlane;
	public static float bossFogFarPlaneScale;
	public static int bossFogStartDistance;
	public static boolean bossFogPlayerCough;
	public static boolean bossFogDoTEnabled;
	public static int bossFogDoTDelay;
	public static boolean bossFogDoTWarning;
	public static int bossFogDoTWarningTime;
	public static int bossFogDoTDamage;

	public static boolean bossBatSwarmEnabled;
	public static int bossBatSwarmCount;
	public static int bossBatSwarmDelay;
	public static int bossBatSwarmRange;
	public static int bossBatSwarmDamage;
	public static int bossBatSwarmHealth;
	public static int bossBatSwarmAttackRange;

	public static void initGlobalBossConfig() {
		if (!hasDefaultConfig())
			return;

		RoughConfig.addCustomCategoryComment("BossGlobal", "Miscellaneous config options which affect all bosses");
		// CHANGED: new switch in Rough Mobs Redux, on by default; the 1.12.2 version always rolled bosses
		bossesEnabled = RoughConfig.getBoolean("BossGlobal", "_BossesEnabled", true, "Set to false to stop zombies, husks and skeletons spawning as bosses (poisonous fog, bat swarms, boss gear).\nEach mob's BossChance still applies.", true);
		bossWarning = RoughConfig.getBoolean("BossGlobal", "_SpawnWarning", true, "Enable this to have a chat message warning of a boss spawn.");
		bossWarningDist = RoughConfig.getInteger("BossGlobal", "_SpawnWarningDistance", 50, 0, Short.MAX_VALUE, "Max bos spawn distance from a player that will trigger a warning.\nUsed for both chat messages and sounds.");
		bossWarningSound = RoughConfig.getBoolean("BossGlobal", "_SpawnWarningSound", true, "Play a warning sound when a boss spawns.");
		// From Rough Mobs Revamped 1.16.5: BossConfig LightningBolt and Champion_Honorifics
		bossLightning = RoughConfig.getBoolean("BossGlobal", "_LightningBolt", true, "Enable this to show a lightning bolt where a boss spawns (only a visual, it sets nothing on fire).");
		bossHonorifics = RoughConfig.getStringArray("BossGlobal", "_Honorifics", Constants.BOSS_HONORIFICS, "Titles a boss may carry after its name (half of all bosses get one), e.g. Azog, The Foul\nLeave this empty for plain names");

		RoughConfig.addCustomCategoryComment("BossFog", "Configuration options for adjusting the poisonous fog that shrouds all bosses.");
		bossFogEnabled = RoughConfig.getBoolean("BossFog", "_Enabled", true, "Enable this to have thick colored fog around bosses.", true);
		bossFogColor = RoughConfig.getStringArray("BossFog", "_FogColor", Constants.FOG_COLORS, "Change these three values between 0.0 and 1.0 to change the fog color.\nRed, Green, Blue\n");
		bossFogMaxDistance = RoughConfig.getInteger("BossFog", "_FogMaxDist", 20, 0, 100, "Max distance from boss for fog to render.\nFog will only occur if you are within this distance");
		bossFogStartDistance = RoughConfig.getInteger("BossFog", "_FogStartDist", 1, 0, 50, "How far away from boss before fog begins to fade from maximum density.\nMust be a value lower than BossFogMaxDist");
		bossFogFarPlane = RoughConfig.getInteger("BossFog", "_FogFarPlane", 10, 0, 192, "This effects how far away from you before the fog is at maximum thickness.");
		bossFogFarPlaneScale = RoughConfig.getFloat("BossFog", "_FogFarPlaneScale", 0.2F, 0.0F, 0.8F, "This controls how thick/strong the fog is.");
		bossFogDoTEnabled = RoughConfig.getBoolean("BossFog", "_DoTEnabled", true, "If enabled, boss fog will cause poison damage over time.", true);
		bossFogDoTDelay = RoughConfig.getInteger("BossFog", "_DoTCoolDown", 10, 0, Short.MAX_VALUE, "The cooldown (in seconds) between each damage event while inside the fog.");
		bossFogDoTWarning = RoughConfig.getBoolean("BossFog", "_DoTWarning", true, "Should the player recieve a chat warning message when entering the poisonous fog?");
		bossFogDoTWarningTime = RoughConfig.getInteger("BossFog", "_DoTWarningTime", 60, 1, Short.MAX_VALUE, "Controls how frequent (in seconds) a player can be warned when entering boss fog.");
		bossFogDoTDamage = RoughConfig.getInteger("BossFog", "_DoTDamage", 1, 1, 127, "How many half hearts the fog DoT does per hit.");
		bossFogPlayerCough = RoughConfig.getBoolean("BossFog", "_PlayerCough", true, "Disable this if you find the player cough sound annoying.\nOnly happens in poisonous fog.");

		// CHANGED: parsed once here, the 1.12.2 fog handler parsed the strings when its class loaded
		bossFogColorRGB = new float[] {0.1F, 0.4F, 0.1F};
		for (int i = 0; i < 3 && i < bossFogColor.length; i++) {
			try {
				bossFogColorRGB[i] = Float.parseFloat(bossFogColor[i].trim());
			}
			catch (NumberFormatException e) {
				// keep the default for this channel
			}
		}

		RoughConfig.addCustomCategoryComment("BatSwarm", "Configuration options for the Bat Swarm attack.");
		bossBatSwarmEnabled = RoughConfig.getBoolean("BatSwarm", "_Enabled", true, "Disable this if you don't want bosses to use Bat Swarm attacks.", true);
		bossBatSwarmCount = RoughConfig.getInteger("BatSwarm", "_BatCount", 3, 0, Short.MAX_VALUE, "The number of bats that attack when a boss fires his Bat Swarm ability.");
		bossBatSwarmDelay = RoughConfig.getInteger("BatSwarm", "_CoolDown", 3, 0, Short.MAX_VALUE, "The cooldown (in seconds) before the boss can fire Bat Swarm again.");
		bossBatSwarmRange = RoughConfig.getInteger("BatSwarm", "_UseRange", 20, 0, Short.MAX_VALUE, "How close a player must be to the boss before it will fire off a Bat Swarm attack.");
		bossBatSwarmAttackRange = RoughConfig.getInteger("BatSwarm", "_BatAttackRange", 20, 0, Short.MAX_VALUE, "How close a player must be before the bat swarm attacks.");
		bossBatSwarmDamage = RoughConfig.getInteger("BatSwarm", "_Damage", 1, 0, Short.MAX_VALUE, "How many half hearts (minus damage immunity) each bat does on attack.");
		bossBatSwarmHealth = RoughConfig.getInteger("BatSwarm", "_Health", 6, 1, Short.MAX_VALUE, "How much health each bat in the swarm has.");
	}

	public static boolean hasDefaultConfig() {
		return true;
	}

	public static abstract class BossApplier {

		private EquipmentApplier equipApplier;

		private final String name;
		private final int defaultBossChance;
		private final float defaultEnchMultiplier;
		private final float defaultDropChance;
		private final String[] defaultBossNames;

		private int bossChance;
		private String[] bossNames;
		// DROPPED: bossColors (commented out WIP in the original)

		public BossApplier(String name, int defaultBossChance, float defaultEnchMultiplier, float defaultDropChance, String[] defaultBossNames) {
			this.name = name;
			this.defaultBossChance = defaultBossChance;
			this.defaultEnchMultiplier = defaultEnchMultiplier;
			this.defaultDropChance = defaultDropChance;
			this.defaultBossNames = defaultBossNames;
			equipApplier = new EquipmentApplier(name + " boss", 1, 1, 1, this.defaultEnchMultiplier, this.defaultDropChance);
		}

		public void initConfig() {
			equipApplier.initConfig(Constants.DEFAULT_BOSS_MAINHAND,
									Constants.DEFAULT_BOSS_OFFHAND,
									Constants.DEFAULT_BOSS_HELMETS,
									Constants.DEFAULT_BOSS_CHESTPLATES,
									Constants.DEFAULT_BOSS_LEGGINGS,
									Constants.DEFAULT_BOSS_BOOTS,
									Constants.DEFAULT_WEAPON_ENCHANTS,
									Constants.DEFAULT_ARMOR_ENCHANTS,
									true);
			bossChance = RoughConfig.getInteger(name, "_BossChance", defaultBossChance, 0, Short.MAX_VALUE, "Chance (1 in X) for a newly spawned " + name + " to become a boss " + name);
			bossNames = RoughConfig.getStringArray(name, "_BossNames", defaultBossNames, name + " boss names. Please be more creative than I am... :P");
		}

		public void postInit() {
			equipApplier.createPools();
		}

		/*
		 * CHANGED: the 1.12.2 trySetBoss rolled, spawned the boss and killed the original in one call, keyed on
		 * entity.getName(). Any zombie or skeleton type whose name was not "Zombie" or "Skeleton" (husk, stray,
		 * a named mob) was killed with no boss to replace it. The roll happens here, the boss type comes from the
		 * entity type, and other types are left alone. spawnBoss does the replacement a tick later.
		 */
		public EntityType<? extends Mob> rollBoss(Entity entity) {
			if (bossChance <= 0 || RND.nextInt(bossChance) != 0 || (entity instanceof Zombie && ((Zombie)entity).isBaby()))
				return null;

			if (entity.getType() == EntityType.ZOMBIE)
				return EntityInit.BOSS_ZOMBIE.get();
			// CHANGED: new in Rough Mobs Redux, the husk champion Rough Mobs Revamped 1.16.5 registered and never spawned
			if (entity.getType() == EntityType.HUSK)
				return EntityInit.BOSS_HUSK.get();
			if (entity.getType() == EntityType.SKELETON)
				return EntityInit.BOSS_SKELETON.get();
			return null;
		}

		public Mob spawnBoss(Mob entity, EntityType<? extends Mob> bossType) {
			if (entity.isRemoved())
				return null;

			// Despawn normal skeletons and zombies and replace with BossSkeleton or BossZombie respectively
			Mob boss = bossType.create(entity.level());
			if (boss == null)
				return null;

			boss.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
			entity.level().addFreshEntity(boss);

			// CHANGED: decided here with the server's config and sent once to the clients tracking the boss; 1.12.2
			// struck it on each client that added the boss (see BossZombie.onAddedToWorld)
			if (bossLightning && boss.getY() >= boss.level().getSeaLevel() && boss.level().canSeeSky(boss.blockPosition()))
				boss.level().broadcastEntityEvent(boss, IBoss.LIGHTNING_EVENT);

			// Remove vanilla mob
			entity.discard();

			// Add custom attributes
			AttributeHelper.applyAttributeModifier(boss, Attributes.MAX_HEALTH, name + "BossHealth", 0, boss.getMaxHealth()*2);
			AttributeHelper.applyAttributeModifier(boss, Attributes.KNOCKBACK_RESISTANCE, name + "BossKnock", 1, 1);

			// Add equipment
			boolean isBoss = true;
			equipApplier.equipEntity(boss, isBoss);

			// Set Bosses name
			String bossName = bossNames.length > 0 ? bossNames[RND.nextInt(bossNames.length)] : name;
			// From Rough Mobs Revamped 1.16.5 BossHelper: a 50% chance of a title after the name
			if (bossHonorifics.length > 0 && RND.nextInt(2) == 1)
				bossName += ", " + bossHonorifics[RND.nextInt(bossHonorifics.length)];
			boss.setCustomName(Component.literal(bossName));

			// Add chat message warning of new boss
			if (bossWarning) {
				MutableComponent bossWarningMsg = Component.literal(bossName)
						.setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true))
						.append(Component.translatable("translation.warning.bossdescriptor")
						.setStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(false)))
						.append(" ")
						.append(entity.getType().getDescription().copy()
						.setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withBold(true)))
						.append(" ")
						.append(Component.translatable("translation.warning.bossaction")
						.setStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(false)));

				List<Player> players = boss.level().getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(bossWarningDist));
				for (Player player : players)
					player.sendSystemMessage(bossWarningMsg);
			}

			// Play warning sound of boss spawn
			if (bossWarningSound) {
				boss.playSound(SoundHandler.ENTITY_BOSS_SPAWN.get(), (bossWarningDist / 16), 0.5F);
			}

			// Add Boss features
			boss.getPersistentData().putBoolean(BOSS, true);
			addBossFeatures(boss);

			return boss;
		}

		public abstract void addBossFeatures(Mob boss);
	}

	public static boolean isBoss(Entity entity) {
		return entity.getPersistentData().getBoolean(BOSS);
	}
}
