package fr.modcraftmc.datasync.waystones.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.datasync.waystones.DatasyncWaystones;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.core.Waystone;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

public class UpdateWaystone extends BaseMessage {

    public static final String MESSAGE_NAME = "update_waystones";

    private IWaystone iwaystone;
    private String serverName;

    public UpdateWaystone(IWaystone waystone) {
        this(waystone, CrossServerCoreAPI.instance.getServerName());
    }

    public UpdateWaystone(IWaystone waystone, String serverName) {
        super(MESSAGE_NAME);
        this.iwaystone = waystone;
        this.serverName = serverName;
    }

    @Override
    protected JsonObject serialize() {
        JsonObject object = super.serialize();
        CompoundTag tag = new CompoundTag();
        Waystone.write(iwaystone, tag);
        JsonElement waystoneJson = CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, tag).result().get();
        object.add("waystone", waystoneJson);
        object.addProperty("serverName", serverName);
        return object;
    }

    public static UpdateWaystone deserialize(JsonObject json) {
        CompoundTag waystoneTag = CompoundTag.CODEC.parse(JsonOps.INSTANCE, json.get("waystone")).result().get();
        String serverName = json.get("serverName").getAsString();
        return new UpdateWaystone(Waystone.read(waystoneTag), serverName);
    }

    @Override
    public String getMessageName() {
        return MESSAGE_NAME;
    }

    @Override
    public void handle() {
        DatasyncWaystones.LOGGER.info("Updating waystone " + iwaystone.getWaystoneUid() + " (" + iwaystone.getName() + ")");
        DatasyncWaystones.waystoneManager.enableWaystone(iwaystone, serverName);
    }
}
