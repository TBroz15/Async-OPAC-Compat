package dev.tuxebro.async_opac_compat;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(value = AOCMain.MOD_ID, dist = Dist.DEDICATED_SERVER)
public class AOCMain {
    public static final String MOD_ID = "async_opac_compat";
    private static final Logger LOGGER = LogUtils.getLogger();

    public AOCMain(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Wassup, Async-OPAC Compat mod is not guaranteed to work well. So Here be (ender) dragons! or whatever minecraft screen says when you switch major versions on the same worlds idk");
    }
}
