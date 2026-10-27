package com.p1ut0nium.roughmobsrevamped.ai.misc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.p1ut0nium.roughmobsrevamped.util.Constants;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.ForgeEventFactory;

public class RoughAIBreakBlocks extends Goal {

	public static final String BANS = Constants.unique("breakbans");
	public static final String LONG_POS = Constants.unique("breakpos");

	protected Mob breaker;
	protected int range;
	protected Level world;
	protected Set<Block> allowedBlocks;
	protected BlockPos target;
	protected int breakingTime;
	protected int previousBreakProgress = -1;
	protected Block block;
	protected int neededTime;
	protected int idleTime;
	protected int curDistance;
	private long nextScan;

	protected static Map<BlockPos, Boolean> blockCache = new HashMap<>();

	public RoughAIBreakBlocks(Mob breaker, int range, Set<Block> allowedBlocks) {
		this.breaker = breaker;
		this.range = range;
		this.world = breaker.level();
		this.allowedBlocks = allowedBlocks;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		// CHANGED: the Forge mob griefing event replaces the bare game rule read; it returns the game rule unless another mod decides
		if (!ForgeEventFactory.getMobGriefingEvent(world, breaker) || this.breaker.getTarget() != null) { return false; }

		// CHANGED: 1.12.2 scanned the (2 * range + 1) cube every 3 ticks per zombie; 1.20.1 would scan every 2,
		// so a scan now waits 10 to 20 ticks after the last one
		long now = world.getGameTime();
		if (now < nextScan) { return false; }
		nextScan = now + 10 + breaker.getRandom().nextInt(11);

		BlockPos target = getFirstTarget();

		if (target != null) {
			this.target = target;
			return true;
		}
		return false;
	}

	@Override
	public void start() {
		this.breakingTime = 0;
		this.idleTime = 0;
		this.previousBreakProgress = -1;
		this.block = world.getBlockState(target).getBlock();
		this.neededTime = getBreakSpeed();
		this.breaker.getNavigation().moveTo(this.breaker.getNavigation().createPath(target, 0), 1);
	}

	private int getBreakSpeed() {
		float multiplier = 100;
		ItemStack held = breaker.getMainHandItem();
		BlockState state = world.getBlockState(target);

		if (held.getItem() instanceof DiggerItem) {
			multiplier -= held.getDestroySpeed(state) * 10;
		}

		return (int) (state.getDestroySpeed(world, target) * Math.max(0, multiplier) + 10);
	}

	// The 1.12.2 updateTask ran every tick: break time and the idle ban count ticks
	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		// A banished target is cleared mid-run, and an every-tick goal can tick once more before the continue check
		if (this.target == null) { return; }

		Entity breakerEntity = breaker.getRootVehicle();

		++this.idleTime;
		double distance = this.target.distToLowCornerSqr(breakerEntity.getX(), breakerEntity.getY(), breakerEntity.getZ());
		if ((int) Math.round(distance) == curDistance) { this.idleTime++; } else { this.idleTime = 0; }
		if (this.idleTime >= 160) { banishTarget(); }
		curDistance = (int) Math.round(distance);

		if (this.target != null) {
			if (distance <= 2) {
				++this.breakingTime;
				int i = (int) ((float) this.breakingTime / (float) this.neededTime * 10.0F);

				if (i != this.previousBreakProgress) {
					this.world.destroyBlockProgress(this.breaker.getId(), this.target, i);
					this.previousBreakProgress = i;
				}

				if (this.breakingTime >= this.neededTime) {
					BlockState state = world.getBlockState(this.target);
					this.world.removeBlock(this.target, false);
					this.world.levelEvent(2001, this.target, Block.getId(state));
					this.breakingTime = 0;
				}
			// CHANGED: 1.12.2 keyed the re-path on world time, which stands still with doDaylightCycle off
			} else if (world.getGameTime() % 40 == 0) {
				this.breaker.getNavigation().moveTo(this.breaker.getNavigation().createPath(target, 0), 1);
			}
		}
	}

	@Override
	public boolean canContinueToUse() {
		return target != null && (!(this.breaker instanceof PathfinderMob) || ((PathfinderMob) this.breaker)
				.isWithinRestriction(target)) && world.getBlockState(target).getBlock() == this.block
				&& this.breaker.getTarget() == null;
	}

	@Override
	public void stop() {
		this.breaker.getNavigation().stop();
		// CHANGED: clear the crack overlay on an interrupted break, as vanilla BreakDoorGoal does
		if (this.target != null && this.previousBreakProgress >= 0)
			this.world.destroyBlockProgress(this.breaker.getId(), this.target, -1);
		this.previousBreakProgress = -1;
	}

	private BlockPos getFirstTarget() {
		List<BlockPos> positions = new ArrayList<>();
		BlockPos curPos, target = null;
		Entity breakerEntity = breaker.getRootVehicle();

		int j, k;
		for (int i = -range; i <= range; i++) {
			for (j = -range; j <= range; j++) {
				for (k = -range; k <= range; k++) {
					curPos = BlockPos.containing(breakerEntity.getX() + i, breakerEntity.getY() + j, breakerEntity.getZ() + k);
					// CHANGED: skip unloaded positions so a scan at a chunk edge never loads a chunk
					if (!world.isLoaded(curPos)) { continue; }

					if (!blockCache.containsKey(curPos) || blockCache.get(curPos)) {
						Block block = world.getBlockState(curPos).getBlock();
						if (block instanceof SpawnerBlock) { return null; }

						if (blockCache.size() >= 256) blockCache.clear();

						if (allowedBlocks.contains(block) && !isBanned(curPos)) {
							positions.add(curPos);
							blockCache.put(curPos, true);
						} else {
							blockCache.put(curPos, false);
						}
					}
				}
			}
		}

		BlockPos breakerPos = breakerEntity.blockPosition();
		target = positions.stream()
				.min(Comparator.comparingDouble(blockPos -> blockPos.distSqr(breakerPos)))
				.orElse(null);
		return target;
	}

	private void banishTarget() {
		CompoundTag data = breaker.getPersistentData();
		ListTag list = data.getList(BANS, Tag.TAG_COMPOUND);

		CompoundTag comp = new CompoundTag();
		comp.putLong(LONG_POS, target.asLong());
		list.add(comp);
		data.put(BANS, list);

		stop();
		this.target = null;
	}

	private boolean isBanned(BlockPos pos) {
		long l = pos.asLong();
		ListTag list = breaker.getPersistentData().getList(BANS, Tag.TAG_COMPOUND);

		for (int i = 0; i < list.size(); i++) {
			if (list.getCompound(i).getLong(LONG_POS) == l) { return true; }
		}
		return false;
	}
}
