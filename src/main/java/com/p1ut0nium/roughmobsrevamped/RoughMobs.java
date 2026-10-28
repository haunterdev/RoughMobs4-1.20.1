/*
 * Rough Mobs Redux
 * A 1.20.1 port of Rough Mobs Revamped by p1ut0nium_94, itself a revamp of Rough Mobs 2 by Lellson
 * Original Author: Lellson
 * This version is for Minecraft 1.20.1
 */

package com.p1ut0nium.roughmobsrevamped;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.p1ut0nium.roughmobsrevamped.client.ClientSetup;
import com.p1ut0nium.roughmobsrevamped.compat.CompatHandler;
import com.p1ut0nium.roughmobsrevamped.config.RoughConfig;
import com.p1ut0nium.roughmobsrevamped.init.EntityInit;
import com.p1ut0nium.roughmobsrevamped.misc.SpawnHelper;
import com.p1ut0nium.roughmobsrevamped.network.RoughNetwork;
import com.p1ut0nium.roughmobsrevamped.util.Constants;
import com.p1ut0nium.roughmobsrevamped.util.handlers.SoundHandler;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/*
 * CHANGED: @SidedProxy is ClientSetup, run on the client only. @Instance and instance() had no callers and are
 * DROPPED. DROPPED: updateJSON (the Rough Mobs Revamped update feed) and acceptableRemoteVersions.
 */
@Mod(Constants.MODID)
public class RoughMobs {

	public static final Logger logger = LogUtils.getLogger();

	private final RoughApplier applier;

	public RoughMobs() {

		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

		// CHANGED: was RegistryHandler.preInitRegistries (sounds and entities) plus the EntityRegistry spawn calls
		SoundHandler.SOUNDS.register(modBus);
		EntityInit.ENTITIES.register(modBus);
		SpawnHelper.BIOME_MODIFIERS.register(modBus);
		modBus.addListener(EntityInit::registerAttributes);
		modBus.addListener(this::onConfigLoad);
		RoughNetwork.register();

		CompatHandler.registerGameStages();
		CompatHandler.registerSereneSeasons();

		// CHANGED: new RoughConfig(event) opened the file here. The spec is defined by running the feature
		// config code once now and read by running it again when Forge loads the file (see RoughConfig).
		RoughConfig.beginDefine();

		applier = new RoughApplier();
		applier.preInit();
		applier.loadConfig();
		applier.postInit();

		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RoughConfig.buildSpec(), RoughConfig.FILE_NAME);

		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSetup.init(modBus));
	}

	/*
	 * CHANGED: was postInit. Runs in the config Loading event, after registries and before common setup.
	 * Forge saves the file right after this event, and its file watcher then reloads it on another thread;
	 * a read in common setup could meet an option mid-reload and get its default.
	 * The Classic Preset and the importer write into the file only after every read here (RoughConfig.writePending).
	 */
	private void onConfigLoad(ModConfigEvent.Loading event) {
		if (!Constants.MODID.equals(event.getConfig().getModId()))
			return;
		applier.loadConfig();
		applier.postInit();
		RoughConfig.writePending(event.getConfig());
	}
}
