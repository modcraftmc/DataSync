package fr.modcraftmc.datasync.ftbteams;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import dev.ftb.mods.ftbteams.api.event.TeamManagerEvent;
import dev.ftb.mods.ftbteams.data.*;
import dev.ftb.mods.ftbteams.net.SendMessageResponseMessage;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.events.CrossServerCoreReadyEvent;
import fr.modcraftmc.crossservercore.api.events.PlayerJoinClusterEvent;
import fr.modcraftmc.crossservercore.api.message.SendMessage;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncPlayer;
import fr.modcraftmc.crossservercore.api.sharedpersistentdata.ISharedDataStore;
import fr.modcraftmc.crossservercore.api.sharedpersistentdata.SharedDataStore;
import fr.modcraftmc.datasync.ftbteams.message.SyncTeamMessage;
import fr.modcraftmc.datasync.ftbteams.message.SyncTeams;
import fr.modcraftmc.datasync.ftbteams.serialization.SerializationUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.bson.Document;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class TeamsSynchronizer {
    
    public boolean FTBTeamsLoaded = false;
    public ISharedDataStore databaseTeamsData = new SharedDataStore(References.TEAMS_DATA_COLLECTION_NAME);

    public static HolderLookup.Provider getLookupProvider(){
        return ServerLifecycleHooks.getCurrentServer().registryAccess();
    }

    public TeamsSynchronizer() {
        if(!ModList.get().isLoaded(References.FTBTEAMS_MOD_ID))
            return;

        DatasyncFtbTeam.LOGGER.info("FTBTeams is loaded, enabling FTBTeams sync");
        FTBTeamsLoaded = true;

        TeamManagerEvent.CREATED.register((event) -> {
            if(CrossServerCoreAPI.isLoaded()) {
                loadTeams();
                CrossServerCoreAPI.getAllPlayersOnCluster().forEach(TeamsSynchronizer::setPlayerTeamOnline);
            }
            else {
                NeoForge.EVENT_BUS.addListener(this::loadTeamsWhenCSCReady); //if ftbteams is loaded before crossservercore, we need to wait for it to be ready
            }
        });
        TeamEvent.PROPERTIES_CHANGED.register((event) -> syncTeam(event.getTeam()));
        TeamEvent.OWNERSHIP_TRANSFERRED.register((event) -> syncTeam(event.getTeam()));
        TeamEvent.PLAYER_CHANGED.register((event) -> {
            event.getPreviousTeam().ifPresent((team) -> syncTeam(team));
            syncTeam(event.getTeam());
        });
        TeamEvent.ADD_ALLY.register((event) -> syncTeam(event.getTeam()));
        TeamEvent.REMOVE_ALLY.register((event) -> syncTeam(event.getTeam()));
        TeamEvent.CREATED.register((event) -> syncTeam(event.getTeam()));
        TeamEvent.DELETED.register((event) -> removeTeam(event.getTeam()));

        NeoForge.EVENT_BUS.addListener(this::onPlayerJoinCluster);
    }

    private void loadTeamsWhenCSCReady(CrossServerCoreReadyEvent event){
        loadTeams();
    }

    private void onPlayerJoinCluster(PlayerJoinClusterEvent event){
        if (!FTBTeamsAPI.api().isManagerLoaded()){
            TeamManagerEvent.CREATED.register((e) -> {
                setPlayerTeamOnline(event.getPlayer());
            });

            return;
        }
        setPlayerTeamOnline(event.getPlayer());
    }

    public void removeTeam(Team team){
        AbstractTeam abstractTeam = (AbstractTeam) team; //pray for it to be an abstract team
        if(!FTBTeamsLoaded) return;
        DatasyncFtbTeam.LOGGER.debug(String.format("Removing team: %s", abstractTeam.getDisplayName()));
        CompoundTag teamsData = abstractTeam.serializeNBT(getLookupProvider());
        SyncTeams syncTeamsMessage = new SyncTeams(abstractTeam.getId(), abstractTeam.getType().name(), SerializationUtil.ToJsonElement(teamsData), true);

        CrossServerCoreAPI.sendCrossMessageToAllOtherServer(syncTeamsMessage);
        removeTeamFromDB(abstractTeam);
    }

    public void syncTeam(Team team) {
        AbstractTeam abstractTeam = (AbstractTeam) team; //pray for it to be an abstract team
        if(!FTBTeamsLoaded) return;
        DatasyncFtbTeam.LOGGER.debug(String.format("Syncing team: %s", abstractTeam.getDisplayName()));
        try{
            CompoundTag teamsData = abstractTeam.serializeNBT(getLookupProvider());
            SyncTeams syncTeamsMessage = new SyncTeams(abstractTeam.getId(), abstractTeam.getType().name(), SerializationUtil.ToJsonElement(teamsData));

            CrossServerCoreAPI.sendCrossMessageToAllOtherServer(syncTeamsMessage);
        } catch (Exception e){
            DatasyncFtbTeam.LOGGER.error(String.format("Error while syncing team: %s", abstractTeam.getDisplayName()));
        }

        saveTeamToDB(abstractTeam);
    }

    public void handleTeamSync(SyncTeams syncTeamMessage){
        if(!FTBTeamsLoaded) return;
        CompoundTag teamsData = SerializationUtil.GetNbt(syncTeamMessage.teamsData);
        AbstractTeam team = TeamManagerImpl.INSTANCE.getTeamMap().getOrDefault(syncTeamMessage.teamUUID, null);

        DatasyncFtbTeam.LOGGER.debug(String.format("team id: %s; team: %s", syncTeamMessage.teamUUID, team));
        if(team == null) {
            team = getNewTeam(syncTeamMessage.teamType, syncTeamMessage.teamUUID);
        }
        team.deserializeNBT(teamsData, getLookupProvider());

        AbstractTeam finalTeam = team;
        finalTeam.getMembers().forEach(uuid -> { //effectiveTeam is not serialized when a team is updated, so we need to do it manually
            if(FTBTeamsAPI.api().getManager().getTeamByID(uuid).get() != finalTeam) {
                ((PlayerTeam) FTBTeamsAPI.api().getManager().getPlayerTeamForPlayerID(uuid).get()).setEffectiveTeam(finalTeam);
            }
        });

        if (syncTeamMessage.remove && team.getMembers().isEmpty()) {
            TeamManagerImpl.INSTANCE.saveNow();
            TeamManagerImpl.INSTANCE.getTeamMap().remove(team.getId());
            String fn = team.getId() + ".snbt";

            try {
                Path dir = TeamManagerImpl.INSTANCE.getServer().getWorldPath(TeamManagerImpl.FOLDER_NAME).resolve("deleted");
                if (Files.notExists(dir, new LinkOption[0])) {
                    Files.createDirectories(dir);
                }

                Files.move(TeamManagerImpl.INSTANCE.getServer().getWorldPath(TeamManagerImpl.FOLDER_NAME).resolve("party/" + fn), dir.resolve(fn));
            } catch (IOException var10) {
                var10.printStackTrace();

                try {
                    Files.deleteIfExists(TeamManagerImpl.INSTANCE.getServer().getWorldPath(TeamManagerImpl.FOLDER_NAME).resolve("party/" + fn));
                } catch (IOException var9) {
                    var9.printStackTrace();
                }
            }
        }

        TeamManagerImpl.INSTANCE.syncToAll(team);
        if(team.getType() == TeamType.PLAYER) {
            ((PlayerTeam) team).updatePresence();
        }
//        ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().forEach(player -> {
//            Team playerTeam = FTBTeamsAPI.getPlayerTeam(player.getUUID());
//            if(finalTeam == playerTeam){
//                new SyncMessageHistoryMessage(playerTeam).sendTo(player);
//            }
//        });
    }

    public void syncTeamMessage(UUID teamUUID, UUID from, Component message){
        if(!FTBTeamsLoaded) return;
        SyncTeamMessage syncTeamMessage = new SyncTeamMessage(teamUUID, from, message);

        CrossServerCoreAPI.sendCrossMessageToAllOtherServer(syncTeamMessage);
    }

    public void handleTeamMessage(UUID teamUUID, UUID from, Component message){
        DatasyncFtbTeam.LOGGER.debug(String.format("Received team message: %s", message.getString()));
        if(!FTBTeamsLoaded) return;
        AbstractTeam team = TeamManagerImpl.INSTANCE.getTeamMap().getOrDefault(teamUUID, null);
        DatasyncFtbTeam.LOGGER.debug(String.format("team id: %s; team: %s", teamUUID, team));
        if(team == null) return;
        team.addMessage(new TeamMessageImpl(from, System.currentTimeMillis(), message));
        MutableComponent component = Component.literal("<");
        component.append(TeamManagerImpl.INSTANCE.getPlayerName(from));
        component.append(" @");
        component.append(team.getName());
        component.append("> ");
        component.append(message);

        for (ServerPlayer p : team.getOnlineMembers()) {
            p.displayClientMessage(component, false);
            PacketDistributor.sendToPlayer(p, new SendMessageResponseMessage(from, message));
        }
        team.markDirty();
    }

    public void saveTeamsToDB(){
        if(!FTBTeamsLoaded) return;
        DatasyncFtbTeam.LOGGER.debug("Saving teams");
        FTBTeamsAPI.api().getManager().getTeams().forEach(team -> saveTeamToDB((AbstractTeam) team));
    }

    public void saveTeamToDB(AbstractTeam team){
        DatasyncFtbTeam.LOGGER.debug(String.format("Saving team: %s", team.getDisplayName()));
        JsonObject teamData = SerializationUtil.ToJsonElement(team.serializeNBT(getLookupProvider())).getAsJsonObject();
        Date date = new Date();
        String uuid = team.getId().toString();
        Document document = new Document("uuid", uuid)
                .append("name", team.getDisplayName())
                .append("lastUpdated", new Timestamp(date.getTime()).toString())
                .append("teamData", teamData.toString());
        databaseTeamsData.accessOrThrow().replaceOne(new Document("uuid", uuid), document);
    }

    public void removeTeamFromDB(Team team) {
        if(!FTBTeamsLoaded) return;
        DatasyncFtbTeam.LOGGER.debug("Removing teams");
        databaseTeamsData.accessOrThrow().deleteOne(new Document("uuid", team.getId().toString()));
    }

    public void loadTeams(){
        Gson gson = new Gson();
        databaseTeamsData.accessOrThrow().find().forEach(data -> {
            JsonElement teamData = gson.fromJson(data.getString("teamData"), JsonElement.class);
            CompoundTag teamCompound = SerializationUtil.GetNbt(teamData);
            AbstractTeam team = (AbstractTeam) FTBTeamsAPI.api().getManager().getTeamByID(UUID.fromString(teamCompound.getString("id"))).get();
            if(team == null) {
                team = getNewTeam(teamCompound.getString("type"), UUID.fromString(teamCompound.getString("id")));
            }
            team.deserializeNBT(teamCompound, getLookupProvider());
        });
    }

    private static AbstractTeam getNewTeam(String teamType, UUID teamId){
        AbstractTeam team;
        TeamType type = null;
        switch (teamType){
            case "party", "PARTY" -> type = TeamType.PARTY;
            case "server", "SERVER" -> type = TeamType.SERVER;
            case "player", "PLAYER" -> type = TeamType.PLAYER;
            default -> DatasyncFtbTeam.LOGGER.error(String.format("Unknown team type: %s", teamType));
        }

        switch (type){
            case PARTY -> {
                team = new PartyTeam(TeamManagerImpl.INSTANCE, teamId);
            }
            case SERVER -> {
                team = new ServerTeam(TeamManagerImpl.INSTANCE, teamId);
            }
            case PLAYER -> {
                team = new PlayerTeam(TeamManagerImpl.INSTANCE, teamId);
                addKnownPlayerTeam((PlayerTeam) team);
            }
            default -> {
                return null;
            }
        }
        TeamManagerImpl.INSTANCE.getTeamMap().put(teamId, team);
        TeamManagerImpl.INSTANCE.markDirty();
        return team;
    }

    public static void addKnownPlayerTeam(PlayerTeam playerTeam){
        //use reflection to replace knownPlayers with a new map containing the new playerTeam
        if(TeamManagerImpl.INSTANCE.getKnownPlayerTeams().containsKey(playerTeam.getId())) return;

        try {
            Field knownPlayersField = TeamManagerImpl.class.getDeclaredField("knownPlayers");
            knownPlayersField.setAccessible(true);
            Map<UUID, PlayerTeam> newKnownPlayers = new LinkedHashMap<UUID, PlayerTeam>((Map<UUID, PlayerTeam>) TeamManagerImpl.INSTANCE.getKnownPlayerTeams());
            newKnownPlayers.put(playerTeam.getId(), playerTeam);
            knownPlayersField.set(TeamManagerImpl.INSTANCE, newKnownPlayers);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void setPlayerTeamOnline(ISyncPlayer player){
        PlayerTeam playerTeam = (PlayerTeam) FTBTeamsAPI.api().getManager().getKnownPlayerTeams().get(player.getUUID());

        if(playerTeam == null) return;

        DatasyncFtbTeam.LOGGER.debug(String.format("player team of %s set online", player.getName()));
        playerTeam.setOnline(true);
        playerTeam.updatePresence();
    }

    public static void sendInvitationMessage(UUID playerUUID, PartyTeam team, ServerPlayer sourcePlayer){
        CrossServerCoreAPI.getPlayer(playerUUID).ifPresent(playerInvited -> {
            new SendMessage(Component.translatable("ftbteams.message.invite_sent", sourcePlayer.getName().copy().withStyle(ChatFormatting.YELLOW)), playerInvited).send();
            Component acceptButton = Component.translatable("ftbteams.accept")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withClickEvent(
                            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ftbteams party join " + team.getId().toString()))
                    );
            Component declineButton = Component.translatable("ftbteams.decline")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withClickEvent(
                            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ftbteams party deny_invite " + team.getId().toString()))
                    );
            new SendMessage(Component.literal("[").append(acceptButton).append("] [").append(declineButton).append("]"), playerInvited).send();
        });
    }
}
