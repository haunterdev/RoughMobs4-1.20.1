package com.p1ut0nium.roughmobsrevamped.network;

import com.p1ut0nium.roughmobsrevamped.util.Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/*
 * CHANGED: new in Rough Mobs Redux. Rough Mobs Revamped had no network channel, so each client drew the boss fog from its
 * own copy of the common config. The server sends its fog settings (BossFogMessage) when a player logs in,
 * singleplayer included, over the same path. The config is read once at start, so login is the only time to send.
 */
public final class RoughNetwork {

	private static final String VERSION = "1";
	public static final ResourceLocation NAME = new ResourceLocation(Constants.MODID, "main");
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(NAME, () -> VERSION, VERSION::equals, VERSION::equals);

	private RoughNetwork() {
	}

	// Called from the mod constructor
	public static void register() {
		CHANNEL.messageBuilder(BossFogMessage.class, 0, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(BossFogMessage::encode)
				.decoder(BossFogMessage::decode)
				.consumerMainThread(BossFogMessage::handle)
				.add();
		MinecraftForge.EVENT_BUS.addListener(RoughNetwork::onLogin);
	}

	private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), BossFogMessage.fromConfig());
	}
}
