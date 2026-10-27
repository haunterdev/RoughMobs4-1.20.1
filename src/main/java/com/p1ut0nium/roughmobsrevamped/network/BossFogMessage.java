package com.p1ut0nium.roughmobsrevamped.network;

import java.util.function.Supplier;

import com.p1ut0nium.roughmobsrevamped.misc.BossHelper;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/*
 * CHANGED: new in Rough Mobs 4. The server's boss fog settings (BossHelper), sent at login by RoughNetwork. The client
 * keeps the last one in client, and util.handlers.FogEventHandler draws from it. Before the first message and after
 * leaving a server, client is OFF: no boss fog.
 */
public record BossFogMessage(boolean bossesEnabled, boolean fogEnabled, float red, float green, float blue,
		int maxDistance, int startDistance, int farPlane, float farPlaneScale) {

	public static final BossFogMessage OFF = new BossFogMessage(false, false, 0.1F, 0.4F, 0.1F, 0, 0, 0, 1.0F);

	// What the client draws with, set on the client's main thread
	public static BossFogMessage client = OFF;

	public static BossFogMessage fromConfig() {
		float[] rgb = BossHelper.bossFogColorRGB;
		return new BossFogMessage(BossHelper.bossesEnabled, BossHelper.bossFogEnabled, rgb[0], rgb[1], rgb[2],
				BossHelper.bossFogMaxDistance, BossHelper.bossFogStartDistance, BossHelper.bossFogFarPlane, BossHelper.bossFogFarPlaneScale);
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeBoolean(bossesEnabled);
		buf.writeBoolean(fogEnabled);
		buf.writeFloat(red);
		buf.writeFloat(green);
		buf.writeFloat(blue);
		buf.writeVarInt(maxDistance);
		buf.writeVarInt(startDistance);
		buf.writeVarInt(farPlane);
		buf.writeFloat(farPlaneScale);
	}

	public static BossFogMessage decode(FriendlyByteBuf buf) {
		return new BossFogMessage(buf.readBoolean(), buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
				buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readFloat());
	}

	public void handle(Supplier<NetworkEvent.Context> context) {
		client = this;
		context.get().setPacketHandled(true);
	}
}
