package fr.modcraftmc.datasync;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(DataSync.MOD_ID)
public class DataSync {
    public static final String MOD_ID = "datasync";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DataSync(){
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::serverSetup);
    }

    public void serverSetup(FMLDedicatedServerSetupEvent event){
        LOGGER.info("Server setup");
    }
}
