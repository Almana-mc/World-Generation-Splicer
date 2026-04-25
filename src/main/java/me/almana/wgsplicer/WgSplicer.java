package me.almana.wgsplicer;

import me.almana.wgsplicer.config.WgSplicerConfig;
import me.almana.wgsplicer.event.ModEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(WgSplicer.MODID)
public class WgSplicer {
    public static final String MODID = "wgsplicer";

    public WgSplicer(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, WgSplicerConfig.SPEC);
        NeoForge.EVENT_BUS.register(ModEvents.class);
    }
}
