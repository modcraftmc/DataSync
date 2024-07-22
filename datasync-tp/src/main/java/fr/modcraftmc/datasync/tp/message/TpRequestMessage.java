package fr.modcraftmc.datasync.tp.message;

import fr.modcraftmc.crossservercore.api.annotation.AutoRegister;
import fr.modcraftmc.crossservercore.api.annotation.AutoSerialize;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncPlayer;
import fr.modcraftmc.datasync.tp.tpsync.TpRequest;
import fr.modcraftmc.datasync.tp.tpsync.TpRequestHandler;

@AutoRegister("tp_request_message")
public class TpRequestMessage extends BaseMessage {
    @AutoSerialize
    public ISyncPlayer playerSource;
    @AutoSerialize
    public ISyncPlayer playerTarget;
    @AutoSerialize
    public int time;

    private TpRequestMessage() {}

    public TpRequestMessage(TpRequest tpRequest) {
        this(tpRequest.getPlayerSource(), tpRequest.getPlayerTarget(), tpRequest.getTime());
    }

    public TpRequestMessage(ISyncPlayer playerSource, ISyncPlayer playerTarget, int time) {
        this.playerSource = playerSource;
        this.playerTarget = playerTarget;
        this.time = time;
    }

    @Override
    public void handle() {
        TpRequestHandler.handle(new TpRequest(playerSource, playerTarget, time));
    }
}
