package com.p1ut0nium.roughmobsrevamped.client;

import com.p1ut0nium.roughmobsrevamped.client.model.render.RenderHostileBat;
import com.p1ut0nium.roughmobsrevamped.init.EntityInit;
import com.p1ut0nium.roughmobsrevamped.network.BossFogMessage;
import com.p1ut0nium.roughmobsrevamped.util.handlers.FogEventHandler;

import net.minecraft.client.renderer.entity.HuskRenderer;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

/*
 * Client-only setup, called from the mod constructor on the client.
 * CHANGED: was ClientProxy. The proxy's packet helpers (getPlayerEntityFromContext, addScheduledTask) had no
 * callers and are DROPPED.
 */
@OnlyIn(Dist.CLIENT)
public class ClientSetup {

	public static void init(IEventBus modBus) {
		modBus.addListener(ClientSetup::registerRenderers);
		MinecraftForge.EVENT_BUS.register(FogEventHandler.class);
		// The next server sends its own boss fog settings at login
		MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> BossFogMessage.client = BossFogMessage.OFF);
	}

	// CHANGED: 1.12.2 registered only the bat renderer; the bosses fell back to the zombie and skeleton
	// renderers through their superclass. 1.20.1 has no fallback, so the vanilla renderers are named here.
	private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(EntityInit.BOSS_ZOMBIE.get(), ZombieRenderer::new);
		event.registerEntityRenderer(EntityInit.BOSS_HUSK.get(), HuskRenderer::new);
		event.registerEntityRenderer(EntityInit.BOSS_SKELETON.get(), SkeletonRenderer::new);
		event.registerEntityRenderer(EntityInit.HOSTILE_BAT.get(), RenderHostileBat::new);
	}
}
