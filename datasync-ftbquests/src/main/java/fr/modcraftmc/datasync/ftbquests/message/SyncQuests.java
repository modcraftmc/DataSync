package fr.modcraftmc.datasync.ftbquests.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.modcraftmc.crossservercore.api.annotation.AutoRegister;
import fr.modcraftmc.crossservercore.api.annotation.AutoSerialize;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.datasync.ftbquests.DatasyncFtbQuests;

@AutoRegister("sync_quests")
public class SyncQuests extends BaseMessage {
    @AutoSerialize
    public JsonElement questsData;

    public SyncQuests() {}

    public SyncQuests(JsonElement questsData) {
        this.questsData = questsData;
    }

    @Override
    public void handle() {
        DatasyncFtbQuests.questsSynchronizer.handleSyncQuests(this);
    }
}
