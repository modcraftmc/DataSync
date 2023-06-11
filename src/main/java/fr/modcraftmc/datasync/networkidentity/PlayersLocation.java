package fr.modcraftmc.datasync.networkidentity;

import fr.modcraftmc.datasync.DataSync;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PlayersLocation {
    private final Map<String, SyncServer> playersLocation;

    public PlayersLocation() {
        this.playersLocation = new HashMap<>();
    }

    public Map<String, SyncServer> getPlayersLocation() {
        return playersLocation;
    }

    public void setPlayerLocation(String player, SyncServer location) {
        if(playersLocation.containsKey(player)) {
            playersLocation.get(player).removePlayer(player);
            this.playersLocation.replace(player, location);
            location.addPlayer(player);
        }
        else {
            this.playersLocation.put(player, location);
            location.addPlayer(player);
            DataSync.updatePlayersLocationToClients();
        }
    }

    public void removePlayer(String player) {
        if(playersLocation.containsKey(player)){
            playersLocation.get(player).removePlayer(player);
            this.playersLocation.remove(player);
            DataSync.updatePlayersLocationToClients();
        }
    }

    public Optional<SyncServer> getPlayerLocation(String player) {
        if(playersLocation.containsKey(player))
            return Optional.of(playersLocation.get(player));
        else
            return Optional.empty();
    }

    public List<String> getAllPlayers() {
        return this.playersLocation.keySet().stream().toList();
    }

    public void clear() {
        this.playersLocation.clear();
    }
}
