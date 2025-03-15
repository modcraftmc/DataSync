package fr.modcraftmc.datasync.ftbquests;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.client.model.ReplaceOptions;
import dev.architectury.event.EventResult;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
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
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.events.CrossServerCoreReadyEvent;
import fr.modcraftmc.crossservercore.api.sharedpersistentdata.ISharedDataStore;
import fr.modcraftmc.crossservercore.api.sharedpersistentdata.SharedDataStore;
import fr.modcraftmc.datasync.ftbquests.Serialization.SerializationUtil;
import fr.modcraftmc.datasync.ftbquests.message.SyncQuests;
import fr.modcraftmc.datasync.ftbquests.message.SyncTeamQuests;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.mutable.MutableInt;
import org.bson.Document;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Date;
import java.util.UUID;

public class QuestsSynchronizer {

    public boolean FTBQuestsLoaded = false;

    public ISharedDataStore databaseTeamsQuestsData = new SharedDataStore(References.TEAMS_QUESTS_DATA_COLLECTION_NAME);

    public QuestsSynchronizer() {
        if(!ModList.get().isLoaded(References.FTBQUESTS_MOD_ID))
            return;
        DatasyncFtbQuests.LOGGER.info("FTBQuests is loaded, enabling FTBQuests sync");
        FTBQuestsLoaded = true;

        ObjectCompletedEvent.QuestEvent.GENERIC.register((QuestEvent) -> {
            syncTeamQuests(QuestEvent.getData());
            return EventResult.pass();
        });
        ObjectCompletedEvent.TaskEvent.GENERIC.register((TaskEvent) -> {
            syncTeamQuests(TaskEvent.getData());
            return EventResult.pass();
        });

        NeoForge.EVENT_BUS.addListener(this::onCrossServerCoreReadyEvent);
    }

    private void onCrossServerCoreReadyEvent(CrossServerCoreReadyEvent event) {
        loadTeamsQuests();
    }

    public void syncTeamQuests(TeamData teamData) {
        if(!FTBQuestsLoaded) return;
        DatasyncFtbQuests.LOGGER.debug(String.format("Syncing quests for team: %s", teamData.getName()));
        CompoundTag questsData = teamData.serializeNBT();
        JsonElement questsDataJson = SerializationUtil.ToJsonElement(questsData);

        Team team = FTBTeamsAPI.api().getManager().getTeamByID(teamData.getTeamId()).get();
        saveTeamQuestsToDB(team, questsDataJson);
        SyncTeamQuests syncQuestsMessage = new SyncTeamQuests(teamData.getTeamId(), questsDataJson);

        CrossServerCoreAPI.sendCrossMessageToAllOtherServer(syncQuestsMessage);
    }

    public void handleTeamQuestsSync(SyncTeamQuests syncQuestsMessage){
        if(!FTBQuestsLoaded) return;
        CompoundTag questsData = SerializationUtil.GetNbt(syncQuestsMessage.questsData);
        Team team = FTBTeamsAPI.api().getManager().getTeamByID(syncQuestsMessage.teamUUID).get();
        TeamData teamData = FTBQuestsAPI.api().getQuestFile(false).getNullableTeamData(team.getId());
        

        teamData.deserializeNBT(SNBTCompoundTag.of(questsData));

        SyncTeamDataMessage syncMessageToPlayer = new SyncTeamDataMessage(teamData);
        PacketDistributor.sendToAllPlayers(syncMessageToPlayer);
    }

    public void saveTeamsQuestsToDB(){
        if(!FTBQuestsLoaded) return;
        DatasyncFtbQuests.LOGGER.debug("Saving teams");
        FTBTeamsAPI.api().getManager().getTeams().forEach(team -> saveTeamQuestsToDB(team));
    }

    public void saveTeamQuestsToDB(Team team){
        TeamData teamQuestsData = FTBQuestsAPI.api().getQuestFile(false).getNullableTeamData(team.getId());
        JsonObject teamQuestsDataJson = SerializationUtil.ToJsonElement(teamQuestsData.serializeNBT()).getAsJsonObject();
        saveTeamQuestsToDB(team, teamQuestsDataJson);
    }

    public void saveTeamQuestsToDB(Team team, JsonElement questsDataJson){
        DatasyncFtbQuests.LOGGER.debug(String.format("Saving team: %s's quests", team.getName().getString()));
        JsonObject teamQuestsDataJson = questsDataJson.getAsJsonObject();
        Date date = new Date();
        String uuid = team.getId().toString();
        Document document = new Document("uuid", uuid)
                .append("name", team.getName().getString())
                .append("lastUpdated", new Timestamp(date.getTime()).toString())
                .append("teamQuestsData", teamQuestsDataJson.toString());
        databaseTeamsQuestsData.accessOrThrow().replaceOne(new Document("uuid", uuid), document, new ReplaceOptions().upsert(true));
    }

    public void loadTeamsQuests(){
        DatasyncFtbQuests.LOGGER.debug("Loading teams quests");
        Gson gson = new Gson();
        databaseTeamsQuestsData.accessOrThrow().find().forEach(data -> {
            JsonElement teamQuestsDataJson = gson.fromJson(data.getString("teamQuestsData"), JsonElement.class);
            CompoundTag teamQuestsData = SerializationUtil.GetNbt(teamQuestsDataJson);

            Team team = FTBTeamsAPI.api().getManager().getTeamByID(UUID.fromString(data.getString("uuid"))).get();
            TeamData teamData = FTBQuestsAPI.api().getQuestFile(false).getNullableTeamData(team.getId());

            DatasyncFtbQuests.LOGGER.debug(String.format("Loading team: %s's quests", team.getName().getString()));

            teamData.deserializeNBT(SNBTCompoundTag.of(teamQuestsData));

            SyncTeamDataMessage syncMessageToPlayer = new SyncTeamDataMessage(teamData);
            PacketDistributor.sendToAllPlayers(syncMessageToPlayer);
        });
    }

    public void syncQuests() {
        if(!FTBQuestsLoaded) return;
        DatasyncFtbQuests.LOGGER.debug("Syncing server quests");
        CompoundTag questsData = serializeQuests(FTBQuestsAPI.api().getQuestFile(false));
        SyncQuests syncQuestsMessage = new SyncQuests(SerializationUtil.ToJsonElement(questsData));

        CrossServerCoreAPI.sendCrossMessageToAllOtherServer(syncQuestsMessage);
    }

    public void handleSyncQuests(SyncQuests syncQuestsMessage){
        if(!FTBQuestsLoaded) return;
        CompoundTag questsData = SerializationUtil.GetNbt(syncQuestsMessage.questsData);
        deserializeQuests(FTBQuestsAPI.api().getQuestFile(false), SNBTCompoundTag.of(questsData));

        SyncQuestsMessage syncMessageToPlayer = new SyncQuestsMessage(ServerQuestFile.INSTANCE);
        PacketDistributor.sendToAllPlayers(syncMessageToPlayer);
    }

    // Code from QuestFile class adapted to write in SNBT instead of a file
    public static SNBTCompoundTag serializeQuests(BaseQuestFile questFile){
        SNBTCompoundTag questsNBT = new SNBTCompoundTag();
        HolderLookup.Provider lookup = ServerLifecycleHooks.getCurrentServer().registryAccess();

        questsNBT.putInt("version", BaseQuestFile.VERSION);
        questFile.writeData(questsNBT, lookup);


        ListTag chapterList = new ListTag();
        for (ChapterGroup group : questFile.getChapterGroups()) {
            for (int ci = 0; ci < group.getChapters().size(); ci++) {
                Chapter chapter = group.getChapters().get(ci);
                SNBTCompoundTag chapterNBT = new SNBTCompoundTag();
                chapterNBT.putString("id", chapter.getCodeString());
                chapterNBT.putString("group", group.isDefaultGroup() ? "" : group.getCodeString());
                chapterNBT.putInt("order_index", ci);
                chapter.writeData(chapterNBT, lookup);

                ListTag questList = new ListTag();
                for (Quest quest : chapter.getQuests()) {
                    if (!quest.isValid()) {
                        SNBTCompoundTag questNBT = new SNBTCompoundTag();
                        quest.writeData(questNBT, lookup);
                        questNBT.putString("id", quest.getCodeString());
                        if (!quest.getTasks().isEmpty()) {
                            quest.writeTasks(questNBT, lookup);
                        }
                        if (!quest.getRewards().isEmpty()) {
                            quest.writeRewards(questNBT, lookup);
                        }
                        questList.add(questNBT);
                    }
                }
                chapterNBT.put("quests", questList);

                ListTag linkList = new ListTag();
                for (QuestLink link : chapter.getQuestLinks()) {
                    if (link.getQuest().isPresent()) {
                        SNBTCompoundTag linkNBT = new SNBTCompoundTag();
                        link.writeData(linkNBT, lookup);
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
        for (int ri = 0; ri < questFile.getRewardTables().size(); ri++) {
            RewardTable table = questFile.getRewardTables().get(ri);
            SNBTCompoundTag tableNBT = new SNBTCompoundTag();
            tableNBT.putString("id", table.getCodeString());
            tableNBT.putInt("order_index", ri);
            table.writeData(tableNBT, lookup);
            tableNBT.putString("filename", table.getFilename());
            rewardTableList.add(tableNBT);
        }
        questsNBT.put("reward_tables", rewardTableList);

        ListTag chapterGroupTag = new ListTag();
        for (ChapterGroup group : questFile.getChapterGroups()) {
            if (!group.isDefaultGroup()) {
                SNBTCompoundTag groupTag = new SNBTCompoundTag();
                groupTag.singleLine();
                groupTag.putString("id", group.getCodeString());
                group.writeData(groupTag, lookup);
                chapterGroupTag.add(groupTag);
            }
        }

        SNBTCompoundTag groupNBT = new SNBTCompoundTag();
        groupNBT.put("chapter_groups", chapterGroupTag);
        questsNBT.put("chapter_groups", groupNBT);

        return questsNBT;
    }

    // Code from QuestFile class adapted to read from SNBT instead of a file
    public static void deserializeQuests(BaseQuestFile questFile, SNBTCompoundTag questsNBT){
        HolderLookup.Provider lookup = ServerLifecycleHooks.getCurrentServer().registryAccess();

        questFile.clearCachedData();
        questFile.getDefaultChapterGroup().getChapters().clear();
        questFile.getChapterGroups().clear();
        questFile.getChapterGroups().add(questFile.getDefaultChapterGroup());
        questFile.getRewardTables().clear();

        MutableInt chapterCounter = new MutableInt();
        MutableInt questCounter = new MutableInt();

        Long2ObjectOpenHashMap<QuestObjectBase> objectMap = new Long2ObjectOpenHashMap<>();

        final Long2ObjectOpenHashMap<CompoundTag> dataCache = new Long2ObjectOpenHashMap<>();
        CompoundTag fileNBT = questsNBT;

        if (fileNBT != null) {
            BaseQuestFile.VERSION = fileNBT.getInt("version");
            objectMap.put(1, questFile);
            questFile.readData(fileNBT, lookup);
        }
        CompoundTag chapterGroupsTag = fileNBT.getCompound("chapter_groups");

        if (chapterGroupsTag != null) {
            ListTag groupListTag = chapterGroupsTag.getList("chapter_groups", Tag.TAG_COMPOUND);

            for (int i = 0; i < groupListTag.size(); i++) {
                CompoundTag groupNBT = groupListTag.getCompound(i);
                ChapterGroup chapterGroup = new ChapterGroup(groupNBT.getLong("id"), questFile);
                objectMap.put(chapterGroup.id, chapterGroup);
                dataCache.put(chapterGroup.id, groupNBT);
                questFile.getChapterGroups().add(chapterGroup);
            }
        }

        Long2IntOpenHashMap objectOrderMap = new Long2IntOpenHashMap();
        objectOrderMap.defaultReturnValue(-1);

        ListTag chapterListTag = fileNBT.getList("chapters", Tag.TAG_COMPOUND);
        for (int i = 0; i < chapterListTag.size(); i++) {
            CompoundTag chapterNBT = chapterListTag.getCompound(i);

            if (chapterNBT != null) {
                long chapterID = chapterNBT.getLong("id");
                String chapterFilename = chapterNBT.getString("filename");
                Chapter chapter = new Chapter(chapterID, questFile, questFile.getChapterGroup(questFile.getID(chapterNBT.get("group"))), chapterFilename);
                objectOrderMap.put(chapter.id, chapterNBT.getInt("order_index"));
                objectMap.put(chapter.id, chapter);
                dataCache.put(chapter.id, chapterNBT);
                chapter.getGroup().getChapters().add(chapter);

                ListTag questList = chapterNBT.getList("quests", Tag.TAG_COMPOUND);

                for (int x = 0; x < questList.size(); x++) {
                    CompoundTag questNBT = questList.getCompound(x);
                    Quest quest = new Quest(questNBT.getLong("id"), chapter);
                    objectMap.put(quest.id, quest);
                    dataCache.put(quest.id, questNBT);
                    chapter.getQuests().add(quest);

                    ListTag taskList = questNBT.getList("tasks", Tag.TAG_COMPOUND);

                    for (int j = 0; j < taskList.size(); j++) {
                        CompoundTag taskNBT = taskList.getCompound(j);
                        long taskId = taskNBT.getLong("id");
                        Task task = TaskType.createTask(taskId, quest, taskNBT.getString("type"));

                        if (task == null) {
                            task = new CustomTask(taskId, quest);
                            task.setRawTitle("Unknown type: " + taskNBT.getString("type"));
                        }
                        objectMap.put(task.id, task);
                        dataCache.put(task.id, taskNBT);
                        quest.getTasks().add(task);
                    }

                    ListTag rewardList = questNBT.getList("rewards", Tag.TAG_COMPOUND);

                    for (int j = 0; j < rewardList.size(); j++) {
                        CompoundTag rewardNBT = rewardList.getCompound(j);
                        long rewardID = rewardNBT.getLong("id");
                        Reward reward = RewardType.createReward(rewardID, quest, rewardNBT.getString("type"));

                        if (reward == null) {
                            reward = new CustomReward(rewardID, quest);
                            reward.setRawTitle("Unknown type: " + rewardNBT.getString("type"));
                        }

                        objectMap.put(reward.id, reward);
                        dataCache.put(reward.id, rewardNBT);
                        quest.getRewards().add(reward);
                    }

                    questCounter.increment();
                }

                ListTag questLinks = chapterNBT.getList("quest_links", Tag.TAG_COMPOUND);
                for (int x = 0; x < questLinks.size(); x++) {
                    CompoundTag linkNBT = questLinks.getCompound(x);
                    QuestLink link = new QuestLink(linkNBT.getLong("id"), chapter, questFile.readID(linkNBT.get("linked_quest")));
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
                RewardTable table = new RewardTable(tableNBT.getLong("id"), questFile, tableNBT.getString("filename"));
                objectOrderMap.put(table.id, tableNBT.getInt("order_index"));
                objectMap.put(table.id, table);
                dataCache.put(table.id, tableNBT);
                questFile.getRewardTables().add(table);
            }
        }


        for (QuestObjectBase object : objectMap.values()) {
            CompoundTag data = dataCache.get(object.id);

            if (data != null) {
                object.readData(data, lookup);
            }
        }

        for (ChapterGroup group : questFile.getChapterGroups()) {
            group.getChapters().sort(Comparator.comparingInt(c -> objectOrderMap.get(c.id)));

            for (Chapter chapter : group.getChapters()) {
                for (Quest quest : chapter.getQuests()) {
                    quest.removeInvalidDependencies();
                }
            }
        }

        questFile.getRewardTables().sort(Comparator.comparingInt(c -> objectOrderMap.get(c.id)));
        questFile.updateLootCrates();

        for (QuestObjectBase object : questFile.getAllObjects()) {
            if (object instanceof CustomTask) {
                CustomTaskEvent.EVENT.invoker().act(new CustomTaskEvent((CustomTask) object));
            }
        }

        questFile.refreshIDMap();

        //fileVersion isn't accessible so we mark it dirty every time
//        if (questFile.fileVersion != BaseQuestFile.VERSION) {
            questFile.markDirty();
//        }

        FTBQuests.LOGGER.info("Loaded " + questFile.getChapterGroups().size() + " chapter groups, " + chapterCounter + " chapters, " + questCounter + " quests, " + questFile.getRewardTables().size() + " reward tables");
    }
}
