package com.p1ut0nium.roughmobsrevamped.init;

import com.p1ut0nium.roughmobsrevamped.entities.BossHusk;
import com.p1ut0nium.roughmobsrevamped.entities.BossSkeleton;
import com.p1ut0nium.roughmobsrevamped.entities.BossZombie;
import com.p1ut0nium.roughmobsrevamped.entities.EntityHostileBat;
import com.p1ut0nium.roughmobsrevamped.util.Constants;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/*
 * CHANGED: 1.12.2 registerModEntity took a tracking range of 50 blocks for all three. 1.20.1 counts
 * tracking range in chunks: 8 for the bosses (the vanilla zombie and skeleton value), 5 for the bat.
 */
public class EntityInit {

	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Constants.MODID);

	public static final RegistryObject<EntityType<BossZombie>> BOSS_ZOMBIE = ENTITIES.register("bosszombie",
			() -> EntityType.Builder.<BossZombie>of(BossZombie::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(8).build(Constants.unique("bosszombie")));

	// CHANGED: new in Rough Mobs Redux (see BossHusk), the size of the vanilla husk
	public static final RegistryObject<EntityType<BossHusk>> BOSS_HUSK = ENTITIES.register("bosshusk",
			() -> EntityType.Builder.<BossHusk>of(BossHusk::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(8).build(Constants.unique("bosshusk")));

	public static final RegistryObject<EntityType<BossSkeleton>> BOSS_SKELETON = ENTITIES.register("bossskeleton",
			() -> EntityType.Builder.<BossSkeleton>of(BossSkeleton::new, MobCategory.MONSTER).sized(0.6F, 1.99F).clientTrackingRange(8).build(Constants.unique("bossskeleton")));

	public static final RegistryObject<EntityType<EntityHostileBat>> HOSTILE_BAT = ENTITIES.register("hostilebat",
			() -> EntityType.Builder.<EntityHostileBat>of(EntityHostileBat::new, MobCategory.MONSTER).sized(0.5F, 0.9F).clientTrackingRange(5).build(Constants.unique("hostilebat")));

	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(BOSS_ZOMBIE.get(), Zombie.createAttributes().build());
		event.put(BOSS_HUSK.get(), Zombie.createAttributes().build());
		event.put(BOSS_SKELETON.get(), AbstractSkeleton.createAttributes().build());
		event.put(HOSTILE_BAT.get(), EntityHostileBat.createAttributes().build());
	}
}
