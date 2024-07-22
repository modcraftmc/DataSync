package fr.modcraftmc.datasync.homes.messages;

import com.google.gson.JsonObject;
import fr.modcraftmc.crossservercore.api.annotation.AutoRegister;
import fr.modcraftmc.crossservercore.api.annotation.AutoSerialize;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.datasync.homes.DatasyncHomes;

@AutoRegister("change_global_homes_limit")
public class ChangeGlobalHomesLimit extends BaseMessage {
    @AutoSerialize
    private int limit;

    private ChangeGlobalHomesLimit() {}

    public ChangeGlobalHomesLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public void handle() {
        DatasyncHomes.homeManager.setGlobalHomesLimit(limit);
    }
}
