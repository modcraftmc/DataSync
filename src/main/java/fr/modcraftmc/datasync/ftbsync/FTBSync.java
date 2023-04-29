package fr.modcraftmc.datasync.ftbsync;

import dev.architectury.event.EventResult;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbquests.events.CustomTaskEvent;
import dev.ftb.mods.ftbquests.events.ObjectCompletedEvent;
import dev.ftb.mods.ftbquests.net.SyncQuestsMessage;
import dev.ftb.mods.ftbquests.net.SyncTeamDataMessage;
import dev.ftb.mods.ftbquests.quest.*;
import dev.ftb.mods.ftbquests.quest.loot.RewardTable;
import dev.ftb.mods.ftbquests.quest.reward.CustomReward;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.task.CustomTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.*;
import dev.ftb.mods.ftbteams.event.TeamEvent;
import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.References;
import fr.modcraftmc.datasync.message.SyncQuests;
import fr.modcraftmc.datasync.message.SyncTeamQuests;
import fr.modcraftmc.datasync.message.SyncTeams;
import fr.modcraftmc.datasync.serialization.SerializationUtil;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.fml.ModList;
import org.apache.commons.lang3.mutable.MutableInt;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

public class FTBSync {
    private static boolean FTBTeamsLoaded = false;
    private static boolean FTBQuestsLoaded = false;

    public static void init() {
        if(ModList.get().isLoaded(References.FTBTEAMS_MOD_ID)) {
            DataSync.LOGGER.info("FTBTeams is loaded, enabling FTBTeams sync");
            FTBTeamsLoaded = true;

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
        }
        if(ModList.get().isLoaded(References.FTBQUESTS_MOD_ID)) {
            DataSync.LOGGER.info("FTBQuests is loaded, enabling FTBQuests sync");
            FTBQuestsLoaded = true;

            ObjectCompletedEvent.QuestEvent.GENERIC.register((event) -> {
                syncTeamQuests(event.getData());
                return EventResult.pass();
            });
            ObjectCompletedEvent.TaskEvent.GENERIC.register((event) -> {
                syncTeamQuests(event.getData());
                return EventResult.pass();
            });
        }
    }

    public static void syncTeam(Team team) {
        if(!FTBTeamsLoaded) return;
        DataSync.LOGGER.debug(String.format("Syncing team: %s", team.getDisplayName()));
        CompoundTag teamsData = FTBTeamsAPI.getManager().getTeamByID(team.getId()).serializeNBT();
        SyncTeams syncTeamsMessage = new SyncTeams(team.getId().toString(), team.getType().name(), SerializationUtil.ToJsonElement(teamsData));

        DataSync.serverCluster.sendMessageExceptCurrent(syncTeamsMessage.serializeToString());
    }

    public static void removeTeam(Team team){
        if(!FTBTeamsLoaded) return;
        DataSync.LOGGER.debug(String.format("Removing team: %s", team.getDisplayName()));
        CompoundTag teamsData = FTBTeamsAPI.getManager().getTeamByID(team.getId()).serializeNBT();
        SyncTeams syncTeamsMessage = new SyncTeams(team.getId().toString(), team.getType().name(), SerializationUtil.ToJsonElement(teamsData), true);

        DataSync.serverCluster.sendMessageExceptCurrent(syncTeamsMessage.serializeToString());
    }

    public static void handleTeamSync(SyncTeams syncQuestsMessage){
        if(!FTBTeamsLoaded) return;
        CompoundTag teamsData = SerializationUtil.GetNbt(syncQuestsMessage.teamsData);
        Team team = FTBTeamsAPI.getManager().getTeamByID(UUID.fromString(syncQuestsMessage.teamUUID));
        DataSync.LOGGER.debug(String.format("team id: %s; team: %s", syncQuestsMessage.teamUUID, team));
        if(team == null) {
            team = getNewTeam(syncQuestsMessage.teamType, syncQuestsMessage.teamUUID);
        }
        team.deserializeNBT(teamsData);

        Team finalTeam = team;
        finalTeam.getMembers().forEach(uuid -> { //actualTeam is not serialized when a team is updated, so we need to do it manually
            if(FTBTeamsAPI.getPlayerTeam(uuid) != finalTeam) {
                FTBTeamsAPI.getManager().getInternalPlayerTeam(uuid).actualTeam = finalTeam;
            }
        });

        if (syncQuestsMessage.remove && team.getMembers().isEmpty()) {
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

        if(team.getType() == TeamType.PLAYER) {
            ((PlayerTeam) team).updatePresence();
        }
        FTBTeamsAPI.getManager().syncTeamsToAll(team);
    }

    public static void syncTeamQuests(TeamData team) {
        if(!FTBQuestsLoaded) return;
        DataSync.LOGGER.debug(String.format("Syncing quests for team: %s", team.name));
        CompoundTag questsData = team.serializeNBT();
        SyncTeamQuests syncQuestsMessage = new SyncTeamQuests(team.uuid.toString(), SerializationUtil.ToJsonElement(questsData));

        DataSync.serverCluster.sendMessageExceptCurrent(syncQuestsMessage.serializeToString());
    }

    public static void handleTeamQuestsSync(SyncTeamQuests syncQuestsMessage){
        if(!FTBQuestsLoaded) return;
        CompoundTag questsData = SerializationUtil.GetNbt(syncQuestsMessage.questsData);
        Team team = FTBTeamsAPI.getManager().getTeamByID(UUID.fromString(syncQuestsMessage.teamUUID));
        TeamData teamData = FTBQuests.PROXY.getQuestFile(false).getData(team);
        teamData.deserializeNBT(SNBTCompoundTag.of(questsData));

        SyncTeamDataMessage syncMessageToPlayer = new SyncTeamDataMessage(teamData, true);
        team.getOnlineMembers().forEach(player -> syncMessageToPlayer.sendTo(player));
    }

    public static void syncQuests() {
        if(!FTBQuestsLoaded) return;
        DataSync.LOGGER.debug("Syncing server quests");
        CompoundTag questsData = serializeQuests(FTBQuests.PROXY.getQuestFile(false));
        SyncQuests syncQuestsMessage = new SyncQuests(SerializationUtil.ToJsonElement(questsData));

        DataSync.serverCluster.sendMessageExceptCurrent(syncQuestsMessage.serializeToString());
    }

    public static void handleSyncQuests(SyncQuests syncQuestsMessage){
        if(!FTBQuestsLoaded) return;
        CompoundTag questsData = SerializationUtil.GetNbt(syncQuestsMessage.questsData);
        deserializeQuests(FTBQuests.PROXY.getQuestFile(false), (SNBTCompoundTag) questsData);

        SyncQuestsMessage syncMessageToPlayer = new SyncQuestsMessage(ServerQuestFile.INSTANCE);
        ServerQuestFile.INSTANCE.server.getPlayerList().getPlayers().forEach(player -> syncMessageToPlayer.sendTo(player));
    }


    private static Team getNewTeam(String teamType, String id){
        Team team;
        TeamType type = TeamType.valueOf(teamType);
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
            DataSync.LOGGER.error("Error while setting team id");
        }
        FTBTeamsAPI.getManager().getTeamMap().put(teamId, team);
        return team;
    }

    // Code from QuestFile class adapted to write in SNBT instead of a file
    public static SNBTCompoundTag serializeQuests(QuestFile questFile){
        SNBTCompoundTag questsNBT = new SNBTCompoundTag();
        questsNBT.putInt("version", questFile.VERSION);
        questFile.writeData(questsNBT);

        ListTag chapterList = new ListTag();
        for (ChapterGroup group : questFile.chapterGroups) {
            for (int ci = 0; ci < group.chapters.size(); ci++) {
                Chapter chapter = group.chapters.get(ci);
                SNBTCompoundTag chapterNBT = new SNBTCompoundTag();
                chapterNBT.putString("id", chapter.getCodeString());
                chapterNBT.putString("group", group.isDefaultGroup() ? "" : group.getCodeString());
                chapterNBT.putInt("order_index", ci);
                chapter.writeData(chapterNBT);

                ListTag questList = new ListTag();
                for (Quest quest : chapter.quests) {
                    if (!quest.invalid) {
                        SNBTCompoundTag questNBT = new SNBTCompoundTag();
                        quest.writeData(questNBT);
                        questNBT.putString("id", quest.getCodeString());
                        if (!quest.tasks.isEmpty()) {
                            quest.writeTasks(questNBT);
                        }
                        if (!quest.rewards.isEmpty()) {
                            quest.writeRewards(questNBT);
                        }
                        questList.add(questNBT);
                    }
                }
                chapterNBT.put("quests", questList);

                ListTag linkList = new ListTag();
                for (QuestLink link : chapter.questLinks) {
                    if (link.getQuest().isPresent()) {
                        SNBTCompoundTag linkNBT = new SNBTCompoundTag();
                        link.writeData(linkNBT);
                        linkNBT.putString("id", link.getCodeString());
                        linkList.add(linkNBT);
                    }
                }
                chapterNBT.put("quest_links", linkList);

                chapterNBT.putString("filename", chapter.getFilename());
                chapterList.add(chapterNBT);
            }
        }
        questsNBT.put("chapters", chapterList);

        ListTag rewardTableList = new ListTag();
        for (int ri = 0; ri < questFile.rewardTables.size(); ri++) {
            RewardTable table = questFile.rewardTables.get(ri);
            SNBTCompoundTag tableNBT = new SNBTCompoundTag();
            tableNBT.putString("id", table.getCodeString());
            tableNBT.putInt("order_index", ri);
            table.writeData(tableNBT);
            tableNBT.putString("filename", table.getFilename());
            rewardTableList.add(tableNBT);
        }
        questsNBT.put("reward_tables", rewardTableList);

        ListTag chapterGroupTag = new ListTag();
        for (ChapterGroup group : questFile.chapterGroups) {
            if (!group.isDefaultGroup()) {
                SNBTCompoundTag groupTag = new SNBTCompoundTag();
                groupTag.singleLine();
                groupTag.putString("id", group.getCodeString());
                group.writeData(groupTag);
                chapterGroupTag.add(groupTag);
            }
        }

        SNBTCompoundTag groupNBT = new SNBTCompoundTag();
        groupNBT.put("chapter_groups", chapterGroupTag);
        questsNBT.put("chapter_groups", groupNBT);

        return questsNBT;
    }

    // Code from QuestFile class adapted to read from SNBT instead of a file
    public static void deserializeQuests(QuestFile questFile, SNBTCompoundTag questsNBT){
        questFile.clearCachedData();
        questFile.defaultChapterGroup.chapters.clear();
        questFile.chapterGroups.clear();
        questFile.chapterGroups.add(questFile.defaultChapterGroup);
        questFile.rewardTables.clear();

        MutableInt chapterCounter = new MutableInt();
        MutableInt questCounter = new MutableInt();

        Long2ObjectOpenHashMap<QuestObjectBase> objectMap = new Long2ObjectOpenHashMap<>();

        final Long2ObjectOpenHashMap<CompoundTag> dataCache = new Long2ObjectOpenHashMap<>();
        CompoundTag fileNBT = questsNBT;

        if (fileNBT != null) {
            questFile.fileVersion = fileNBT.getInt("version");
            objectMap.put(1, questFile);
            questFile.readData(fileNBT);
        }
        CompoundTag chapterGroupsTag = fileNBT.getCompound("chapter_groups");

        if (chapterGroupsTag != null) {
            ListTag groupListTag = chapterGroupsTag.getList("chapter_groups", Tag.TAG_COMPOUND);

            for (int i = 0; i < groupListTag.size(); i++) {
                CompoundTag groupNBT = groupListTag.getCompound(i);
                ChapterGroup chapterGroup = new ChapterGroup(questFile);
                chapterGroup.id = questFile.readID(groupNBT.get("id"));
                objectMap.put(chapterGroup.id, chapterGroup);
                dataCache.put(chapterGroup.id, groupNBT);
                questFile.chapterGroups.add(chapterGroup);
            }
        }

        Long2IntOpenHashMap objectOrderMap = new Long2IntOpenHashMap();
        objectOrderMap.defaultReturnValue(-1);

        ListTag chapterListTag = fileNBT.getList("chapters", Tag.TAG_COMPOUND);
        for (int i = 0; i < chapterListTag.size(); i++) {
            CompoundTag chapterNBT = chapterListTag.getCompound(i);

            if (chapterNBT != null) {
                Chapter chapter = new Chapter(questFile, questFile.getChapterGroup(questFile.getID(chapterNBT.get("group"))));
                chapter.id = questFile.readID(chapterNBT.get("id"));
                chapter.filename = chapterNBT.getString("filename");
                objectOrderMap.put(chapter.id, chapterNBT.getInt("order_index"));
                objectMap.put(chapter.id, chapter);
                dataCache.put(chapter.id, chapterNBT);
                chapter.group.chapters.add(chapter);

                ListTag questList = chapterNBT.getList("quests", Tag.TAG_COMPOUND);

                for (int x = 0; x < questList.size(); x++) {
                    CompoundTag questNBT = questList.getCompound(x);
                    Quest quest = new Quest(chapter);
                    quest.id = questFile.readID(questNBT.get("id"));
                    objectMap.put(quest.id, quest);
                    dataCache.put(quest.id, questNBT);
                    chapter.quests.add(quest);

                    ListTag taskList = questNBT.getList("tasks", Tag.TAG_COMPOUND);

                    for (int j = 0; j < taskList.size(); j++) {
                        CompoundTag taskNBT = taskList.getCompound(j);
                        Task task = TaskType.createTask(quest, taskNBT.getString("type"));

                        if (task == null) {
                            task = new CustomTask(quest);
                            task.title = "Unknown type: " + taskNBT.getString("type");
                        }

                        task.id = questFile.readID(taskNBT.get("id"));
                        objectMap.put(task.id, task);
                        dataCache.put(task.id, taskNBT);
                        quest.tasks.add(task);
                    }

                    ListTag rewardList = questNBT.getList("rewards", Tag.TAG_COMPOUND);

                    for (int j = 0; j < rewardList.size(); j++) {
                        CompoundTag rewardNBT = rewardList.getCompound(j);
                        Reward reward = RewardType.createReward(quest, rewardNBT.getString("type"));

                        if (reward == null) {
                            reward = new CustomReward(quest);
                            reward.title = "Unknown type: " + rewardNBT.getString("type");
                        }

                        reward.id = questFile.readID(rewardNBT.get("id"));
                        objectMap.put(reward.id, reward);
                        dataCache.put(reward.id, rewardNBT);
                        quest.rewards.add(reward);
                    }

                    questCounter.increment();
                }

                ListTag questLinks = chapterNBT.getList("quest_links", Tag.TAG_COMPOUND);
                for (int x = 0; x < questLinks.size(); x++) {
                    CompoundTag linkNBT = questLinks.getCompound(x);
                    QuestLink link = new QuestLink(chapter, questFile.readID(linkNBT.get("linked_quest")));
                    link.id = questFile.readID(linkNBT.get("id"));
                    chapter.questLinks.add(link);
                    objectMap.put(link.id, link);
                    dataCache.put(link.id, linkNBT);
                }

                chapterCounter.increment();
            }
        }

        ListTag rewardTableListTag = fileNBT.getList("reward_tables", Tag.TAG_COMPOUND);
        for(int i = 0; i < rewardTableListTag.size(); i++) {
            CompoundTag tableNBT = rewardTableListTag.getCompound(i);

            if (tableNBT != null) {
                RewardTable table = new RewardTable(questFile);
                table.id = questFile.readID(tableNBT.get("id"));
                table.filename = tableNBT.getString("filename");
                objectOrderMap.put(table.id, tableNBT.getInt("order_index"));
                objectMap.put(table.id, table);
                dataCache.put(table.id, tableNBT);
                questFile.rewardTables.add(table);
            }
        }


        for (QuestObjectBase object : objectMap.values()) {
            CompoundTag data = dataCache.get(object.id);

            if (data != null) {
                object.readData(data);
            }
        }

        for (ChapterGroup group : questFile.chapterGroups) {
            group.chapters.sort(Comparator.comparingInt(c -> objectOrderMap.get(c.id)));

            for (Chapter chapter : group.chapters) {
                for (Quest quest : chapter.quests) {
                    quest.removeInvalidDependencies();
                }
            }
        }

        questFile.rewardTables.sort(Comparator.comparingInt(c -> objectOrderMap.get(c.id)));
        questFile.updateLootCrates();

        for (QuestObjectBase object : questFile.getAllObjects()) {
            if (object instanceof CustomTask) {
                CustomTaskEvent.EVENT.invoker().act(new CustomTaskEvent((CustomTask) object));
            }
        }

        questFile.refreshIDMap();

        if (questFile.fileVersion != QuestFile.VERSION) {
            questFile.save();
        }

        FTBQuests.LOGGER.info("Loaded " + questFile.chapterGroups.size() + " chapter groups, " + chapterCounter + " chapters, " + questCounter + " quests, " + questFile.rewardTables.size() + " reward tables");
    }
}
