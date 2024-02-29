package fr.modcraftmc.datasync.ftbquests;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoCollection;
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
import dev.ftb.mods.ftbteams.data.Team;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.datasync.ftbquests.Serialization.SerializationUtil;
import fr.modcraftmc.datasync.ftbquests.message.SyncQuests;
import fr.modcraftmc.datasync.ftbquests.message.SyncTeamQuests;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.fml.ModList;
import org.apache.commons.lang3.mutable.MutableInt;
import org.bson.Document;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Date;
import java.util.UUID;

public class QuestsSynchronizer {

    public boolean FTBQuestsLoaded = false;

    public MongoCollection<Document> databaseTeamsQuestsData;

    public void register() {
        DatasyncFtbQuests.LOGGER.info("FTBQuests is loaded, enabling FTBQuests sync");
        FTBQuestsLoaded = true;
        databaseTeamsQuestsData = CrossServerCoreAPI.instance.getOrCreateMongoCollection(References.TEAMS_QUESTS_DATA_COLLECTION_NAME);
        ObjectCompletedEvent.QuestEvent.GENERIC.register((QuestEvent) -> {
            syncTeamQuests(QuestEvent.getData());
            return EventResult.pass();
        });
        ObjectCompletedEvent.TaskEvent.GENERIC.register((TaskEvent) -> {
            syncTeamQuests(TaskEvent.getData());
            return EventResult.pass();
        });
    }

    public void syncTeamQuests(TeamData teamData) {
        if(!FTBQuestsLoaded) return;
        DatasyncFtbQuests.LOGGER.debug(String.format("Syncing quests for team: %s", teamData.name));
        CompoundTag questsData = teamData.serializeNBT();
        JsonElement questsDataJson = SerializationUtil.ToJsonElement(questsData);

        Team team = FTBTeamsAPI.getManager().getTeamByID(teamData.uuid);
        saveTeamQuestsToDB(team, questsDataJson);
        SyncTeamQuests syncQuestsMessage = new SyncTeamQuests(teamData.uuid.toString(), questsDataJson);

        CrossServerCoreAPI.instance.sendCrossMessageToAllOtherServer(syncQuestsMessage);
    }

    public void handleTeamQuestsSync(SyncTeamQuests syncQuestsMessage){
        if(!FTBQuestsLoaded) return;
        CompoundTag questsData = SerializationUtil.GetNbt(syncQuestsMessage.questsData);
        Team team = FTBTeamsAPI.getManager().getTeamByID(UUID.fromString(syncQuestsMessage.teamUUID));
        TeamData teamData = FTBQuests.PROXY.getQuestFile(false).getData(team);

        teamData.deserializeNBT(SNBTCompoundTag.of(questsData));

        SyncTeamDataMessage syncMessageToPlayer = new SyncTeamDataMessage(teamData, true);
        team.getOnlineMembers().forEach(player -> syncMessageToPlayer.sendTo(player));
    }

    public void saveTeamsQuestsToDB(){
        if(!FTBQuestsLoaded) return;
        DatasyncFtbQuests.LOGGER.debug("Saving teams");
        FTBTeamsAPI.getManager().getTeams().forEach(team -> saveTeamQuestsToDB(team));
    }

    public void saveTeamQuestsToDB(Team team){
        TeamData teamQuestsData = FTBQuests.PROXY.getQuestFile(false).getData(team);
        JsonObject teamQuestsDataJson = SerializationUtil.ToJsonElement(teamQuestsData.serializeNBT()).getAsJsonObject();
        saveTeamQuestsToDB(team, teamQuestsDataJson);
    }

    public void saveTeamQuestsToDB(Team team, JsonElement questsDataJson){
        DatasyncFtbQuests.LOGGER.debug(String.format("Saving team: %s's quests", team.getDisplayName()));
        JsonObject teamQuestsDataJson = questsDataJson.getAsJsonObject();
        Date date = new Date();
        String uuid = team.getId().toString();
        Document document = new Document("uuid", uuid)
                .append("name", team.getDisplayName())
                .append("lastUpdated", new Timestamp(date.getTime()).toString())
                .append("teamQuestsData", teamQuestsDataJson.toString());
        databaseTeamsQuestsData.deleteMany(new Document("uuid", uuid));
        if(!databaseTeamsQuestsData.insertOne(document).wasAcknowledged()){
            DatasyncFtbQuests.LOGGER.error(String.format("Error while saving quests data for team %s", team.getDisplayName()));
        }
    }

    public void loadTeamsQuests(){
        DatasyncFtbQuests.LOGGER.debug("Loading teams quests");
        Gson gson = new Gson();
        databaseTeamsQuestsData.find().forEach(data -> {
            JsonElement teamQuestsDataJson = gson.fromJson(data.getString("teamQuestsData"), JsonElement.class);
            CompoundTag teamQuestsData = SerializationUtil.GetNbt(teamQuestsDataJson);

            Team team = FTBTeamsAPI.getManager().getTeamByID(UUID.fromString(data.getString("uuid")));
            TeamData teamData = FTBQuests.PROXY.getQuestFile(false).getData(team);

            DatasyncFtbQuests.LOGGER.debug(String.format("Loading team: %s's quests", team.getDisplayName()));

            teamData.deserializeNBT(SNBTCompoundTag.of(teamQuestsData));

            SyncTeamDataMessage syncMessageToPlayer = new SyncTeamDataMessage(teamData, true);
            team.getOnlineMembers().forEach(player -> syncMessageToPlayer.sendTo(player));
        });
    }

    public void syncQuests() {
        if(!FTBQuestsLoaded) return;
        DatasyncFtbQuests.LOGGER.debug("Syncing server quests");
        CompoundTag questsData = serializeQuests(FTBQuests.PROXY.getQuestFile(false));
        SyncQuests syncQuestsMessage = new SyncQuests(SerializationUtil.ToJsonElement(questsData));

        CrossServerCoreAPI.instance.sendCrossMessageToAllOtherServer(syncQuestsMessage);
    }

    public void handleSyncQuests(SyncQuests syncQuestsMessage){
        if(!FTBQuestsLoaded) return;
        CompoundTag questsData = SerializationUtil.GetNbt(syncQuestsMessage.questsData);
        deserializeQuests(FTBQuests.PROXY.getQuestFile(false), SNBTCompoundTag.of(questsData));

        SyncQuestsMessage syncMessageToPlayer = new SyncQuestsMessage(ServerQuestFile.INSTANCE);
        ServerQuestFile.INSTANCE.server.getPlayerList().getPlayers().forEach(player -> syncMessageToPlayer.sendTo(player));
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
                for (Quest quest : chapter.getQuests()) {
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
                for (QuestLink link : chapter.getQuestLinks()) {
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
                    chapter.getQuests().add(quest);

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
                    chapter.getQuestLinks().add(link);
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
                for (Quest quest : chapter.getQuests()) {
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
