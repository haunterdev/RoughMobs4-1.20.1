package com.p1ut0nium.roughmobsrevamped.util.handlers;

/*
 * CHANGED: these two flags lived on the client-only FogEventHandler in 1.12.2. The bosses and RoughApplier
 * set them from common code, so they sit in a class a dedicated server can load.
 */
public class FogState {

	// Set when a boss dies (server side, and on the client through the death entity event); starts the fog fade
	public static volatile boolean bossDied;
	// Set when a player joins a level; resets the fog to normal on the next frame
	public static volatile boolean playerRespawned;
}
