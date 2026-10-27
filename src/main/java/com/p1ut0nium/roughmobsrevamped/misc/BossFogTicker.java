package com.p1ut0nium.roughmobsrevamped.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.p1ut0nium.roughmobsrevamped.util.DamageSourceFog;
import com.p1ut0nium.roughmobsrevamped.util.Helpers;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/*
 * The poisonous fog damage over time around a boss, run from the boss's aiStep on the server.
 *
 * CHANGED: BossZombie and BossSkeleton each carried an identical copy of this code; it lives here once.
 * CHANGED: the 1.12.2 tick counter was static, so every boss in the world shared one damage timer.
 * Each boss owns its own ticker now.
 * CHANGED: the config values were copied into statics when the boss class loaded, before the config was
 * read; they are read from BossHelper on every tick.
 * CHANGED: the warning cooldown counts game time; world time stands still with doDaylightCycle off.
 */
public class BossFogTicker {

	private final HashMap<String, Long> playersWarned = new HashMap<>();
	private final List<Player> playersInFog = new ArrayList<Player>();
	private int fogDotTick = 0;

	public void tick(Mob boss) {
		if (!BossHelper.bossFogDoTEnabled)
			return;

		Level world = boss.level();
		List<Player> playersInRange = world.getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(BossHelper.bossFogMaxDistance));

		// Test to see if playersInRange actually contains players
		if (Helpers.containsInstance(playersInRange, Player.class)) {

			// Add new player to list of players in the fog
			for (Player player : playersInRange) {
				if (!playersInFog.contains(player)) {
					playersInFog.add(player);

					if (BossHelper.bossFogDoTWarning) {
						String name = player.getName().getString();
						long warningTime = BossHelper.bossFogDoTWarningTime * 20L;

						// Warn players on first entering the fog and add them to the players warned list
						if (!playersWarned.containsKey(name)) {
							playersWarned.put(name, world.getGameTime() + warningTime);
							player.sendSystemMessage(warning());
						}
						// If warned player hasn't been warned in a while, warn them again
						else if (world.getGameTime() >= playersWarned.get(name)) {
							playersWarned.replace(name, world.getGameTime() + warningTime);
							player.sendSystemMessage(warning());
						}
					}
				}
			}

			// Remove players from the "in fog" list if they are no longer in range
			for (Iterator<Player> playersInFogIterator = playersInFog.iterator(); playersInFogIterator.hasNext(); ) {
				Player playerInFog = playersInFogIterator.next();
				if (!Helpers.containsPlayer(playersInRange, playerInFog))
					playersInFogIterator.remove();
			}

			// Reset fog DoT delay
			if (fogDotTick == BossHelper.bossFogDoTDelay * 20)
				fogDotTick = 0;

			// Damage player while inside fog
			if (fogDotTick == 0) {
				for (Player player : playersInRange)
					player.hurt(DamageSourceFog.poisonousFog(world), BossHelper.bossFogDoTDamage);
			}

			fogDotTick += 1;
		}
		// If no players are in range, then clear the list that keeps track of all players in the fog
		else {
			playersInFog.clear();
		}
	}

	private static Component warning() {
		return Component.translatable("translation.warning.fog").withStyle(ChatFormatting.DARK_GREEN);
	}
}
