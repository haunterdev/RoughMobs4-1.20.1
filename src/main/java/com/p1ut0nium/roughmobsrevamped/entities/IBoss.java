package com.p1ut0nium.roughmobsrevamped.entities;

// Marker for the boss entity classes; RoughApplier skips the normal equipment pass for them
public interface IBoss {

	// Entity event the server sends once when a boss spawns; the clients tracking it show the lightning
	byte LIGHTNING_EVENT = 100;

	// DROPPED: the commented out getBossColorTheme / setBossColorTheme pair (WIP in the original)
}
