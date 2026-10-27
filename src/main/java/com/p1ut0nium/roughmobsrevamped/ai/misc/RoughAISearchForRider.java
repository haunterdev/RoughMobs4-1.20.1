package com.p1ut0nium.roughmobsrevamped.ai.misc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.p1ut0nium.roughmobsrevamped.util.Constants;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

public class RoughAISearchForRider extends Goal {

	public static final String MOUNT_SEARCHER = Constants.unique("mountSearcher");
	public static final int IS_SEARCHER = 2;
	public static final int NO_SEARCHER = 1;

	protected Mob entity;
	protected List<EntityType<?>> possibleRiders;
	protected int range;
	protected int chance;
	protected Mob mountSearcher;

	protected static Map<UUID, Integer> searcherCache = new HashMap<>();

	public RoughAISearchForRider(Mob entity, List<EntityType<?>> possibleRiders, int range, int chance) {
		this.entity = entity;
		this.possibleRiders = possibleRiders;
		this.range = range;
		this.chance = chance;
	}

	@Override
	public boolean canUse() {
		if (!entity.isAlive() || entity.isVehicle()) {
			mountSearcher = null;
			return false;
		}

		List<Mob> entities = entity.level().getEntitiesOfClass(Mob.class, entity.getBoundingBox().inflate(range));

		for (Mob searcher : entities) {
			// CHANGED: 1.12.2 declared isPossibleRider but never called it, so any mob (a creeper, another horse)
			// could climb on; the rider list from the config now applies
			if (searcher.isAlive() && this.entity != searcher && !searcher.isPassenger() && isPossibleRider(searcher)) {
				UUID uuid = searcher.getUUID();
				Integer searchState = searcherCache.get(uuid);

				if (searcherCache.size() >= 128) { searcherCache.clear(); }

				if (searchState == null) {
					int mountSearcherData = searcher.getPersistentData().getInt(MOUNT_SEARCHER);

					if (mountSearcherData == 0) {
						searchState = (chance > 0 && searcher.level().random.nextInt(chance) == 0) || searcher.isPassenger() ? IS_SEARCHER : NO_SEARCHER;
						searcher.getPersistentData().putInt(MOUNT_SEARCHER, searchState);
					} else {
						searchState = mountSearcherData;
					}

					if (searchState == IS_SEARCHER) {
						mountSearcher = searcher;
						searcherCache.put(uuid, searchState);
						return true;
					}
				} else if (searchState == IS_SEARCHER) {
					mountSearcher = searcher;
					return true;
				}
			}
		}

		mountSearcher = null;
		return false;
	}

	@Override
	public void tick() {
		this.mountSearcher.getNavigation().moveTo(entity, 1);

		if (this.entity.distanceToSqr(this.mountSearcher) <= 2 && this.mountSearcher != this.entity) {
			if (this.entity instanceof AbstractHorse horse) {
				horse.invulnerableTime = 60;
				horse.setPersistenceRequired();
				horse.setTamed(true);
			}

			this.mountSearcher.startRiding(this.entity);
			this.mountSearcher.getNavigation().stop();
		}
	}

	@Override
	public boolean canContinueToUse() {
		return this.mountSearcher != null
				&& (!(this.mountSearcher instanceof PathfinderMob) || ((PathfinderMob) this.mountSearcher).isWithinRestriction(entity.blockPosition()))
				&& this.mountSearcher.getTarget() == null && this.entity.isAlive() && !this.entity.isVehicle()
				&& !this.mountSearcher.isPassenger() && this.mountSearcher != this.entity;
	}

	@Override
	public void stop() {
		if (this.mountSearcher != null)
			this.mountSearcher.getNavigation().stop();
		this.mountSearcher = null;
	}

	private boolean isPossibleRider(LivingEntity entity) {
		return possibleRiders.contains(entity.getType());
	}
}
