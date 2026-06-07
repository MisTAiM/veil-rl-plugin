package gg.veil.veilplugin;

import lombok.Data;
import java.util.*;

@Data
public class BossDropProgress
{
    // bossName → list of drop entries
    public Map<String, List<DropEntry>> bosses = new LinkedHashMap<>();

    @Data
    @lombok.AllArgsConstructor
    public static class DropEntry
    {
        public String itemName;
        public int    dropRate;   // 1 in X
        public int    kc;         // current kill count
        public double probability; // P(at least one by now) = 1-(1-1/rate)^kc
        public String pctStr;     // "54.3%"
        public boolean isDry;     // KC > 2× expected with no confirmed drop
    }
}
