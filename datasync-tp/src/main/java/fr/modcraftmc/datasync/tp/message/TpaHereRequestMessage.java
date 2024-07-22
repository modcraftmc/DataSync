package fr.modcraftmc.datasync.tp.message;

import fr.modcraftmc.crossservercore.api.annotation.AutoRegister;
import fr.modcraftmc.crossservercore.api.annotation.AutoSerialize;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncPlayer;
import fr.modcraftmc.datasync.tp.tpsync.TpaHereRequest;
import fr.modcraftmc.datasync.tp.tpsync.TpaHereRequestHandler;

@AutoRegister("tpa_here_request_message")
public class TpaHereRequestMessage extends BaseMessage {
    @AutoSerialize
    public ISyncPlayer playerSourceName;
    @AutoSerialize
    public ISyncPlayer playerTargetName;
    @AutoSerialize
    public int time;

    private TpaHereRequestMessage() {}

    public TpaHereRequestMessage(ISyncPlayer playerSourceName, ISyncPlayer playerTargetName, int time) {
        this.playerSourceName = playerSourceName;
        this.playerTargetName = playerTargetName;
        this.time = time;
    }

    public TpaHereRequestMessage(TpaHereRequest tpaRequest) {
        this(tpaRequest.getPlayerSource(), tpaRequest.getPlayerTarget(), tpaRequest.getTime());
    }

    public TpaHereRequest getTpaHereRequest() {
        return new TpaHereRequest(playerSourceName, playerTargetName, time);
    }

    @Override
    public void handle() {
        TpaHereRequestHandler.handle(getTpaHereRequest());
    }


}
