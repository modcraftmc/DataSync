package fr.modcraftmc.datasync.networkidentity;

import java.util.HashMap;
import java.util.Map;

public class PlayersLocation {
    private final Map<String, SyncServer> playersLocation;

    public PlayersLocation() {
        this.playersLocation = new HashMap<>();
    }

    public Map<String, SyncServer> getPlayersLocation() {
        return playersLocation;
    }

    public void setPlayerLocation(String player, SyncServer location) {
        if(playersLocation.containsKey(player))
            this.playersLocation.replace(player, location);
        else
            this.playersLocation.put(player, location);
    }

    public void removePlayer(String player) {
        this.playersLocation.remove(player);
    }

    public void clear() {
        this.playersLocation.clear();
    }
}
