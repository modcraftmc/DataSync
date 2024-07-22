package fr.modcraftmc.datasync.tp.message;

import fr.modcraftmc.crossservercore.api.annotation.AutoRegister;
import fr.modcraftmc.crossservercore.api.annotation.AutoSerialize;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncPlayer;
import fr.modcraftmc.datasync.tp.tpsync.TpaRequest;
import fr.modcraftmc.datasync.tp.tpsync.TpaRequestHandler;

@AutoRegister("tpa_request_message")
public class TpaRequestMessage extends BaseMessage {
    @AutoSerialize
    public ISyncPlayer playerSource;
    @AutoSerialize
    public ISyncPlayer playerTarget;
    @AutoSerialize
    public int time;

    private TpaRequestMessage() {}

    public TpaRequestMessage(ISyncPlayer playerSource, ISyncPlayer playerTarget, int time) {
        this.playerSource = playerSource;
        this.playerTarget = playerTarget;
        this.time = time;
    }

    public TpaRequestMessage(TpaRequest tpaRequest) {
        this(tpaRequest.getPlayerSource(), tpaRequest.getPlayerTarget(), tpaRequest.getTime());
    }

    public TpaRequest getTpaRequest() {
        return new TpaRequest(playerSource, playerTarget, time);
    }

    @Override
    public void handle() {
        TpaRequestHandler.handle(getTpaRequest());
    }


}
