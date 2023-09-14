package fr.modcraftmc.datasync.ftbteams;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoCollection;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.*;
import dev.ftb.mods.ftbteams.event.TeamEvent;
import dev.ftb.mods.ftbteams.event.TeamManagerEvent;
import dev.ftb.mods.ftbteams.net.SendMessageResponseMessage;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.message.SendMessage;
import fr.modcraftmc.datasync.ftbteams.message.SyncTeamMessage;
import fr.modcraftmc.datasync.ftbteams.message.SyncTeams;
import fr.modcraftmc.datasync.ftbteams.serialization.SerializationUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import org.bson.Document;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

public class TeamsSynchronizer {
    
    public boolean FTBTeamsLoaded = false;
    public MongoCollection<Document> databaseTeamsData;
    
    public TeamsSynchronizer(){
        if (!ModList.get().isLoaded(References.FTBTEAMS_MOD_ID)) return;

        CrossServerCoreAPI.runWhenCSCIsReady(() -> {
            DatasyncFtbTeam.LOGGER.info("FTBTeams is loaded, enabling FTBTeams sync");
            FTBTeamsLoaded = true;

            databaseTeamsData = CrossServerCoreAPI.instance.getOrCreateMongoCollection(References.TEAMS_DATA_COLLECTION_NAME);
            CrossServerCoreAPI.instance.registerCrossMessage(SyncTeams.MESSAGE_NAME, SyncTeams::deserialize);
            CrossServerCoreAPI.instance.registerCrossMessage(SyncTeamMessage.MESSAGE_NAME, SyncTeamMessage::deserialize);

            TeamManagerEvent.CREATED.register((event) -> {
                loadTeams();
                CrossServerCoreAPI.instance.getPlayerLocationMap().keySet().forEach(TeamsSynchronizer::setPlayerTeamOnline);
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

            CrossServerCoreAPI.instance.registerOnPlayerJoinedCluster((playerName, syncServer) -> {
                if (!FTBTeamsAPI.isManagerLoaded()) return;
                setPlayerTeamOnline(playerName);
            });

            //todo: find why this piece of code existed
//            CrossServerCoreAPI.instance.registerOnPlayerLeftCluster((playerName, syncServer) -> {
//                if(!FTBTeamsAPI.isManagerLoaded()) return;
//                setPlayerTeamOnline(playerName);
//            });
        });
    }

    public void removeTeam(Team team){
        if(!FTBTeamsLoaded) return;
        DatasyncFtbTeam.LOGGER.debug(String.format("Removing team: %s", team.getDisplayName()));
        CompoundTag teamsData = team.serializeNBT();
        SyncTeams syncTeamsMessage = new SyncTeams(team.getId().toString(), team.getType().name(), SerializationUtil.ToJsonElement(teamsData), true);

        CrossServerCoreAPI.instance.sendCrossMessageToAllOtherServer(syncTeamsMessage);
        removeTeamFromDB(team);
    }

    public void syncTeam(Team team) {
        if(!FTBTeamsLoaded) return;
        DatasyncFtbTeam.LOGGER.debug(String.format("Syncing team: %s", team.getDisplayName()));
        try{
            CompoundTag teamsData = team.serializeNBT();
            SyncTeams syncTeamsMessage = new SyncTeams(team.getId().toString(), team.getType().name(), SerializationUtil.ToJsonElement(teamsData));

            CrossServerCoreAPI.instance.sendCrossMessageToAllOtherServer(syncTeamsMessage);
        } catch (Exception e){
            DatasyncFtbTeam.LOGGER.error(String.format("Error while syncing team: %s", team.getDisplayName()));
        }

        saveTeamToDB(team);
    }

    public void handleTeamSync(SyncTeams syncTeamMessage){
        if(!FTBTeamsLoaded) return;
        CompoundTag teamsData = SerializationUtil.GetNbt(syncTeamMessage.teamsData);
        Team team = FTBTeamsAPI.getManager().getTeamByID(UUID.fromString(syncTeamMessage.teamUUID));
        DatasyncFtbTeam.LOGGER.debug(String.format("team id: %s; team: %s", syncTeamMessage.teamUUID, team));
        if(team == null) {
            team = getNewTeam(syncTeamMessage.teamType, syncTeamMessage.teamUUID);
        }
        team.deserializeNBT(teamsData);

        Team finalTeam = team;
        finalTeam.getMembers().forEach(uuid -> { //actualTeam is not serialized when a team is updated, so we need to do it manually
            if(FTBTeamsAPI.getPlayerTeam(uuid) != finalTeam) {
                FTBTeamsAPI.getManager().getInternalPlayerTeam(uuid).actualTeam = finalTeam;
            }
        });

        if (syncTeamMessage.remove && team.getMembers().isEmpty()) {
            team.manager.saveNow();
            team.manager.getTeamMap().remove(team.getId());
            String fn = team.getId() + ".snbt";

            try {
                Path dir = team.manager.server.getWorldPath(TeamManager.FOLDER_NAME).resolve("deleted");
                if (Files.notExists(dir, new LinkOption[0])) {
                    Files.createDirectories(dir);
                }

                Files.move(team.manager.server.getWorldPath(TeamManager.FOLDER_NAME).resolve("party/" + fn), dir.resolve(fn));
            } catch (IOException var10) {
                var10.printStackTrace();

                try {
                    Files.deleteIfExists(team.manager.server.getWorldPath(TeamManager.FOLDER_NAME).resolve("party/" + fn));
                } catch (IOException var9) {
                    var9.printStackTrace();
                }
            }
        }

        FTBTeamsAPI.getManager().syncTeamsToAll(team);
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

        CrossServerCoreAPI.instance.sendCrossMessageToAllOtherServer(syncTeamMessage);
    }

    public void handleTeamMessage(UUID teamUUID, UUID from, Component message){
        DatasyncFtbTeam.LOGGER.debug(String.format("Received team message: %s", message.getString()));
        if(!FTBTeamsLoaded) return;
        Team team = FTBTeamsAPI.getManager().getTeamByID(teamUUID);
        DatasyncFtbTeam.LOGGER.debug(String.format("team id: %s; team: %s", teamUUID, team));
        if(team == null) return;
        team.addMessage(new TeamMessage(from, System.currentTimeMillis(), message));
        MutableComponent component = Component.literal("<");
        component.append(team.manager.getName(from));
        component.append(" @");
        component.append(team.getName());
        component.append("> ");
        component.append(message);

        for (ServerPlayer p : team.getOnlineMembers()) {
            p.displayClientMessage(component, false);
            new SendMessageResponseMessage(from, message).sendTo(p);
        }
        team.save();
    }

    public void saveTeamsToDB(){
        if(!FTBTeamsLoaded) return;
        DatasyncFtbTeam.LOGGER.debug("Saving teams");
        FTBTeamsAPI.getManager().getTeams().forEach(team -> saveTeamToDB(team));
    }

    public void saveTeamToDB(Team team){
        DatasyncFtbTeam.LOGGER.debug(String.format("Saving team: %s", team.getDisplayName()));
        JsonObject teamData = SerializationUtil.ToJsonElement(team.serializeNBT()).getAsJsonObject();
        Date date = new Date();
        String uuid = team.getId().toString();
        Document document = new Document("uuid", uuid)
                .append("name", team.getDisplayName())
                .append("lastUpdated", new Timestamp(date.getTime()).toString())
                .append("teamData", teamData.toString());
        databaseTeamsData.deleteMany(new Document("uuid", uuid));
        if(!databaseTeamsData.insertOne(document).wasAcknowledged()){
            DatasyncFtbTeam.LOGGER.error(String.format("Error while saving data for team %s", team.getDisplayName()));
        }

    }

    public void removeTeamFromDB(Team team) {
        if(!FTBTeamsLoaded) return;
        DatasyncFtbTeam.LOGGER.debug("Removing teams");
        databaseTeamsData.deleteOne(new Document("uuid", team.getId().toString()));
    }

    public void loadTeams(){
        Gson gson = new Gson();
        databaseTeamsData.find().forEach(data -> {
            JsonElement teamData = gson.fromJson(data.getString("teamData"), JsonElement.class);
            CompoundTag teamCompound = SerializationUtil.GetNbt(teamData);
            Team team = FTBTeamsAPI.getManager().getTeamByID(UUID.fromString(teamCompound.getString("id")));
            if(team == null) {
                team = getNewTeam(teamCompound.getString("type"), teamCompound.getString("id"));
            }
            team.deserializeNBT(teamCompound);
        });
    }

    private static Team getNewTeam(String teamType, String id){
        Team team;
        TeamType type = null;
        switch (teamType){
            case "party", "PARTY" -> type = TeamType.PARTY;
            case "server", "SERVER" -> type = TeamType.SERVER;
            case "player", "PLAYER" -> type = TeamType.PLAYER;
            default -> DatasyncFtbTeam.LOGGER.error(String.format("Unknown team type: %s", teamType));
        }
        UUID teamId = UUID.fromString(id);

        switch (type){
            case PARTY -> {
                team = new PartyTeam(FTBTeamsAPI.getManager());
            }
            case SERVER -> {
                team = new ServerTeam(FTBTeamsAPI.getManager());
            }
            case PLAYER -> {
                team = new PlayerTeam(FTBTeamsAPI.getManager());
                FTBTeamsAPI.getManager().getKnownPlayers().put(teamId, (PlayerTeam) team);
            }
            default -> {
                return null;
            }
        }
        try {
            Field idField = TeamBase.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(team, teamId);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            DatasyncFtbTeam.LOGGER.error("Error while setting team id");
        }
        FTBTeamsAPI.getManager().getTeamMap().put(teamId, team);
        return team;
    }

    public static void setPlayerTeamOnline(String playerName){
        FTBTeamsAPI.getManager().getKnownPlayers().forEach((uuid, playerTeam) -> {
            if(playerTeam.playerName.equals(playerName)){
                DatasyncFtbTeam.LOGGER.debug(String.format("player team of %s set online", playerName));
                playerTeam.online = true;
                playerTeam.updatePresence();
            }
        });
    }

    public static void sendInvitationMessage(UUID playerUUID, PartyTeam team, ServerPlayer sourcePlayer){
        String playerInvited = FTBTeamsAPI.getManager().getInternalPlayerTeam(playerUUID).playerName;
        new SendMessage(Component.translatable("ftbteams.message.invite_sent", sourcePlayer.getName().copy().withStyle(ChatFormatting.YELLOW)), playerInvited).send();
        Component acceptButton = Component.translatable("ftbteams.accept")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withClickEvent(
                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ftbteams party join " + team.getStringID()))
                );
        Component declineButton = Component.translatable("ftbteams.decline")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withClickEvent(
                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ftbteams party deny_invite " + team.getStringID()))
                );
        new SendMessage(Component.literal("[").append(acceptButton).append("] [").append(declineButton).append("]"), playerInvited).send();
    }
}
