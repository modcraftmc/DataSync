package fr.modcraftmc.datasync;

import com.mojang.logging.LogUtils;
import fr.modcraftmc.datasync.command.DataSyncCommand;
import fr.modcraftmc.datasync.message.MessageHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
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

        MinecraftForge.EVENT_BUS.addListener(this::commandResister);
        MessageHandler.init();
    }

    public void serverSetup(FMLDedicatedServerSetupEvent event){

    }

    public void commandResister(RegisterCommandsEvent event){
        new DataSyncCommand(event.getDispatcher());
    }
}
