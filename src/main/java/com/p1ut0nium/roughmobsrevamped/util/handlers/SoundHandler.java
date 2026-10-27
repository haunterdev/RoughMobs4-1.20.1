package com.p1ut0nium.roughmobsrevamped.util.handlers;

import com.p1ut0nium.roughmobsrevamped.util.Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SoundHandler {

	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Constants.MODID);

	public static final RegistryObject<SoundEvent> ENTITY_BOSS_SPAWN = registerSound("entity.boss.boss_spawn");
	public static final RegistryObject<SoundEvent> ENTITY_BOSS_IDLE = registerSound("entity.boss.boss_idle");
	public static final RegistryObject<SoundEvent> ENTITY_BOSS_DEATH = registerSound("entity.boss.boss_death");
	public static final RegistryObject<SoundEvent> ENTITY_BOSS_BATSWARM = registerSound("entity.boss.boss_batswarm");
	public static final RegistryObject<SoundEvent> ENTITY_PLAYER_COUGH = registerSound("entity.player.player_cough");

	private static RegistryObject<SoundEvent> registerSound(String name) {
		ResourceLocation location = new ResourceLocation(Constants.MODID, name);
		return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(location));
	}
}
