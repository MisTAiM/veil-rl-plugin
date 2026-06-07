package gg.veil.veilplugin;

import lombok.Data;
import java.util.List;

@Data
public class QuestState2
{
    public int  finished;    // count
    public int  inProgress;
    public int  notStarted;
    public int  totalQuests;
    public int  questPoints;
    public int  maxQuestPoints;
    public List<QuestEntry> quests;

    @Data
    public static class QuestEntry
    {
        public String name;
        public String state; // "FINISHED" | "IN_PROGRESS" | "NOT_STARTED"
        public boolean members;
        public int questPoints;
    }
}
