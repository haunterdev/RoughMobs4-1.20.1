package com.p1ut0nium.roughmobsrevamped.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/*
 * The lightning flash a boss arrives with, shown on the client only.
 *
 * CHANGED: 1.12.2 added a visual-only EntityLightningBolt through world.addWeatherEffect on the client.
 * 1.20.1 has no client weather list, so the bolt is a client-only entity with a negative id (server ids
 * are positive, so the two never collide), added on the next client task so it never lands while the
 * level is still adding the boss.
 * DROPPED: the extra entity.lightning.thunder playSound; the client bolt plays thunder and impact itself.
 */
@OnlyIn(Dist.CLIENT)
public class BossClientEffects {

	private static int nextBoltId = -1000;

	public static void spawnLightning(Entity boss) {
		final double x = boss.getX(), y = boss.getY(), z = boss.getZ();

		Minecraft.getInstance().tell(() -> {
			if (!(boss.level() instanceof ClientLevel clientLevel) || Minecraft.getInstance().level != clientLevel)
				return;

			LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, clientLevel);
			bolt.moveTo(x, y, z);
			bolt.setVisualOnly(true);
			int id = nextBoltId--;
			bolt.setId(id);
			clientLevel.putNonPlayerEntity(id, bolt);
		});
	}
}
