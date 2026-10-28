package com.p1ut0nium.roughmobsrevamped.util.handlers;

import java.util.List;

import com.p1ut0nium.roughmobsrevamped.entities.BossHusk;
import com.p1ut0nium.roughmobsrevamped.entities.BossSkeleton;
import com.p1ut0nium.roughmobsrevamped.entities.BossZombie;
import com.p1ut0nium.roughmobsrevamped.network.BossFogMessage;
import com.p1ut0nium.roughmobsrevamped.util.Helpers;

import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/*
 * The boss fog the player sees: the fog colour and fog distance close in as the player nears a boss and
 * fade back out over FOG_FADE_TIME frames when it dies.
 *
 * CHANGED: registered on the Forge bus by ClientSetup (was @EventBusSubscriber with Side.CLIENT).
 * CHANGED: the config copies were statics filled when this class loaded, before the config was read, from the
 * client's own config. The values are the server's, sent at login (network.BossFogMessage.client), read on every event.
 * CHANGED: bossDied and playerRespawned live in FogState, since the server side sets them too.
 * CHANGED: the viewer is the camera entity (1.12.2 EntityViewRenderEvent.getEntity()).
 */
@OnlyIn(Dist.CLIENT)
public class FogEventHandler {

	private static double FOG_RED = 0.7116436;
	private static double FOG_GREEN = 0.81895345;
	private static double FOG_BLUE = 0.99999976;

	private static float FOG_MAX_FARPLANE = 192;
	private static float FOG_MAX_FARPLANE_SCALE = 1.0F;

	private static int FOG_FADE_TIME = 60;
	private static int fog_colorfade_tick = 0;
	private static int fog_farplane_tick = 0;

	private static Mob closestBoss;
	private static float closestBossDistance;

	private static float currentFogColorRed;
	private static float currentFogColorGreen;
	private static float currentFogColorBlue;
	private static float addToRed = 0.0F;
	private static float addToGreen = 0.0F;
	private static float addToBlue = 0.0F;

	private static int currentFarPlane;
	private static float currentFarPlaneScale;
	private static float addToFarPlane = 0.0F;
	private static float addToFarPlaneScale = 0.0F;

	private static boolean outOfRange = true;

	@SubscribeEvent
	public static void onGetFogColor(ViewportEvent.ComputeFogColor event) {

		Entity viewer = event.getCamera().getEntity();

		// Set original fog color if no bosses are in range
		FOG_RED = event.getRed();
		FOG_GREEN = event.getGreen();
		FOG_BLUE = event.getBlue();

		// If player just died and respawned, reset fog color to normal
		if (viewer instanceof Player && FogState.playerRespawned) {
			event.setRed((float)FOG_RED);
			event.setGreen((float)FOG_GREEN);
			event.setBlue((float)FOG_BLUE);
		}

		// If player, then check for bosses in range.
		// CHANGED: no fog around a boss left in a world after bosses were switched off
		BossFogMessage fog = BossFogMessage.client;
		if (viewer instanceof Player player && fog.bossesEnabled() && fog.fogEnabled()) {

			Mob boss = checkForBossInRange(player, fog);

			// If there are bosses, then set the fog color based on the distance
			if (boss != null && !outOfRange && !FogState.bossDied) {

				float fogNearPlaneConfig = fog.startDistance();
				float fogMaxDistConfig = fog.maxDistance();

				currentFogColorRed = (float) Helpers.scaleValue(Math.max(fogNearPlaneConfig, closestBossDistance), fogNearPlaneConfig, fogMaxDistConfig, fog.red(), FOG_RED);
				currentFogColorGreen = (float) Helpers.scaleValue(Math.max(fogNearPlaneConfig, closestBossDistance), fogNearPlaneConfig, fogMaxDistConfig, fog.green(), FOG_GREEN);
				currentFogColorBlue = (float) Helpers.scaleValue(Math.max(fogNearPlaneConfig, closestBossDistance), fogNearPlaneConfig, fogMaxDistConfig, fog.blue(), FOG_BLUE);

				event.setRed(currentFogColorRed);
				event.setGreen(currentFogColorGreen);
				event.setBlue(currentFogColorBlue);
			}
			else if (FogState.bossDied && boss == closestBoss) {
				fadeOutFogColor(event);
			}
		}
	}

	@SubscribeEvent
	public static void onRenderFog(ViewportEvent.RenderFog event) {

		// CHANGED: 1.12.2 fired this event only for the normal linear air fog. 1.20.1 fires it for water, lava,
		// powder snow, blindness and darkness as well, so those keep their own fog
		if (event.getType() != FogType.NONE)
			return;

		Entity viewer = event.getCamera().getEntity();

		if (viewer instanceof LivingEntity living && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS)))
			return;

		// Set original fog far plane distance & scale
		FOG_MAX_FARPLANE = event.getFarPlaneDistance();

		// If player just died and respawned, reset fog density to normal
		if (viewer instanceof Player && FogState.playerRespawned) {
			renderFog(event, FOG_MAX_FARPLANE, FOG_MAX_FARPLANE_SCALE);
			FogState.playerRespawned = false;
		}

		BossFogMessage fog = BossFogMessage.client;
		if (viewer instanceof Player player && fog.bossesEnabled() && fog.fogEnabled()) {

			Mob boss = checkForBossInRange(player, fog);

			// If there are bosses, then set the fog far plane based on the distance
			if (boss != null && !outOfRange && !FogState.bossDied) {

				float fogNearPlaneConfig = fog.startDistance();
				float fogMaxDistConfig = fog.maxDistance();

				currentFarPlane = (int) Helpers.scaleValue(Math.max(fogNearPlaneConfig, closestBossDistance), fogNearPlaneConfig, fogMaxDistConfig, fog.farPlane(), FOG_MAX_FARPLANE);
				currentFarPlaneScale = (float) Helpers.scaleValue(Math.max(fogNearPlaneConfig, closestBossDistance), fogNearPlaneConfig, fogMaxDistConfig, fog.farPlaneScale(), FOG_MAX_FARPLANE_SCALE);

				renderFog(event, currentFarPlane, currentFarPlaneScale);
			}
			else if (FogState.bossDied && boss == closestBoss) {
				adjustFogFarPlane(event);
			}
		}
	}

	/*
	 * Updates the fog's far plane distance
	 * CHANGED: 1.12.2 called glFogf directly. Forge applies the event's planes only when it is cancelled.
	 */
	private static void renderFog(ViewportEvent.RenderFog event, float farPlaneDistance, float farPlaneDistanceScale) {
		if (event.getMode() == FogRenderer.FogMode.FOG_SKY) {
			event.setNearPlaneDistance(0.0F);
			event.setFarPlaneDistance(farPlaneDistance);
		} else {
			event.setNearPlaneDistance(farPlaneDistance * farPlaneDistanceScale);
			event.setFarPlaneDistance(farPlaneDistance);
		}
		event.setCanceled(true);
	}

	/*
	 * Finds the closest boss to the player
	 */
	private static Mob checkForBossInRange(Player player, BossFogMessage fog) {

		Level world = player.level();
		int fogMaxDistConfig = fog.maxDistance();

		List<Mob> entities = world.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(fogMaxDistConfig));

		// If there are no bosses in the list, then exit the function and reset distances
		// (BossHusk is a BossZombie)
		if (!(Helpers.containsInstance(entities, BossZombie.class, BossSkeleton.class))) {
			closestBoss = null;
			closestBossDistance = fogMaxDistConfig + 1;
			outOfRange = true;
			return closestBoss;
		}

		// Do while there are bosses in range of the player
		for (Mob entity : entities) {

			// Only test bosses in the list, ignore all other entity types
			// CHANGED: the husk boss is new in Rough Mobs Redux
			if (entity.getClass() == BossZombie.class || entity.getClass() == BossHusk.class || entity.getClass() == BossSkeleton.class) {

				float entityDistance = player.distanceTo(entity);

				// Is this boss closer than the last closest one and within the max fog distance?
				// If it is, then make it the new closest boss
				if (closestBoss == null && entityDistance <= fogMaxDistConfig || closestBoss != null && closestBoss != entity && entityDistance < player.distanceTo(closestBoss)) {
					closestBoss = entity;
					outOfRange = false;

					return closestBoss;
				}
				// Is this the current closest boss and is it now out of range?
				// If it is, then reset the closest entity to null
				else if (entity == closestBoss && entityDistance > fogMaxDistConfig) {
					closestBoss = null;
					closestBossDistance = fogMaxDistConfig + 1;
					outOfRange = true;

					return closestBoss;
				}
			}
		}

		// Update the distance of the closest boss
		if (closestBoss != null)
			closestBossDistance = player.distanceTo(closestBoss);

		return closestBoss;
	}

	/*
	 * This fades the fog color from the current color to the normal fog color over FOG_FADE_TIME ticks
	 */
	private static void fadeOutFogColor(ViewportEvent.ComputeFogColor event) {

		fog_colorfade_tick += 1;

		// On the first tick, figure out how much should be added to each color channel per tick
		if (!outOfRange && fog_colorfade_tick == 1) {

			float diff = (event.getRed() - currentFogColorRed);
			addToRed = diff / FOG_FADE_TIME;

			diff = (event.getGreen() - currentFogColorGreen);
			addToGreen = diff / FOG_FADE_TIME;

			diff = (event.getBlue() - currentFogColorBlue);
			addToBlue = diff / FOG_FADE_TIME;
		}

		// On all ticks, begin updating the current fog color for each color channel
		if (!outOfRange && fog_colorfade_tick <= FOG_FADE_TIME) {

			currentFogColorRed += addToRed;
			event.setRed(currentFogColorRed);

			currentFogColorGreen += addToGreen;
			event.setGreen(currentFogColorGreen);

			currentFogColorBlue += addToBlue;
			event.setBlue(currentFogColorBlue);
		}

		// After the last tick, reset
		else if (fog_colorfade_tick > FOG_FADE_TIME) {
			fog_colorfade_tick = 0;
			outOfRange = true;
			FogState.bossDied = false;
		}
	}

	/*
	 * Fades fog far plane from current distance to normal distance over FOG_FADE_TIME ticks
	 */
	private static void adjustFogFarPlane(ViewportEvent.RenderFog event) {

		fog_farplane_tick += 1;

		if (!outOfRange && fog_farplane_tick == 1) {
			float diff = (event.getFarPlaneDistance() - currentFarPlane);
			addToFarPlane = diff / FOG_FADE_TIME;

			diff = (FOG_MAX_FARPLANE_SCALE - currentFarPlaneScale);
			addToFarPlaneScale = diff / FOG_FADE_TIME;
		}

		if (!outOfRange && fog_farplane_tick <= FOG_FADE_TIME) {
			currentFarPlane += addToFarPlane;
			currentFarPlaneScale += addToFarPlaneScale;

			renderFog(event, currentFarPlane, Math.min(FOG_MAX_FARPLANE_SCALE, currentFarPlaneScale));
		}

		// After the last tick, reset
		else if (fog_farplane_tick > FOG_FADE_TIME) {
			fog_farplane_tick = 0;
			outOfRange = true;
			FogState.bossDied = false;

			renderFog(event, FOG_MAX_FARPLANE, FOG_MAX_FARPLANE_SCALE);
		}
	}
}
