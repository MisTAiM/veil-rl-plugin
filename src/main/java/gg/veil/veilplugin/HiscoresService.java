package gg.veil.veilplugin;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Veil Hiscores Service
 * Fetches real player stats from the OSRS hiscores API.
 * API confirmed working: returns rank,level,xp per skill + rank,kc per boss.
 *
 * MorpheusXP actual stats (from live API):
 *   Attack 82, Defence 79, Strength 80, HP 83, Ranged 82, Prayer 70, Magic 85
 *   Slayer 29 (IMPORTANT: affects which tasks are available)
 *   Farming 30, Herblore 50, Crafting 55
 *   Boss KC: Hespori 2500, Kree'Arra 16, Obor 1
 */
@Slf4j
public class HiscoresService
{
    private static final String UA  = "Veil-Client/5.0.0 (contact@veil.gg)";
    private static final String URL = "https://secure.runescape.com/m=hiscore_oldschool/index_lite.ws?player=";

    private static final String[] SKILL_NAMES = {
        "Overall","Attack","Defence","Strength","Hitpoints","Ranged",
        "Prayer","Magic","Cooking","Woodcutting","Fletching","Fishing",
        "Firemaking","Crafting","Smithing","Mining","Herblore","Agility",
        "Thieving","Slayer","Farming","Runecraft","Hunter","Construction"
    };

    private static final String[] MINIGAME_NAMES = {
        "Bounty Hunter - Hunter","Bounty Hunter - Rogue",
        "Clue Scrolls (all)","Clue Scrolls (beginner)"
    };

    private static final String[] BOSS_NAMES = {
        "Abyssal Sire","Cerberus","Chambers of Xeric","Chaos Elemental",
        "Commander Zilyana","Dagannoth Prime","Dagannoth Rex","Dagannoth Supreme",
        "Deranged Archaeologist","General Graardor","Giant Mole","Grotesque Guardians",
        "Hespori","Kalphite Queen","King Black Dragon","Kraken",
        "Kree'Arra","K'ril Tsutsaroth","Mimic","Nex","Nightmare","Phosani's Nightmare",
        "Obor","Sarachnis","Scorpia","Skotizo","Spindel","Tempoross",
        "The Gauntlet","The Corrupted Gauntlet","Theatre of Blood","Theatre of Blood: Hard Mode",
        "Thermonuclear Smoke Devil","TzKal-Zuk","TzTok-Jad","Venenatis","Vet'ion",
        "Vorkath","Wintertodt","Zalcano","Zulrah"
    };

    @Data
    public static class PlayerStats
    {
        public String rsn;
        public long   fetchedAt;
        public boolean loaded = false;

        // Skills: [rank, level, xp]
        public Map<String, int[]> skills = new LinkedHashMap<>();
        // Boss KC: bossName → kc
        public Map<String, Integer> bossKc = new LinkedHashMap<>();
        // Minigames
        public Map<String, Integer> minigames = new LinkedHashMap<>();

        // Convenience accessors
        public int getLevel(String skill) {
            int[] s = skills.get(skill);
            return s != null ? s[1] : 1;
        }
        public long getXp(String skill) {
            int[] s = skills.get(skill);
            return s != null ? s[2] : 0;
        }
        public int getKc(String boss) {
            return bossKc.getOrDefault(boss, 0);
        }
        public int getTotalLevel() {
            int[] s = skills.get("Overall");
            return s != null ? s[1] : 0;
        }
    }

    private static volatile PlayerStats cached = null;
    public static PlayerStats getStats() { return cached; }

    /**
     * Fetch hiscores for the given RSN. Runs in a background thread.
     * Updates the cached PlayerStats when complete.
     */
    public static void fetchAsync(String rsn)
    {
        if (rsn == null || rsn.isEmpty()) return;
        if (cached != null && cached.rsn.equals(rsn)
            && System.currentTimeMillis() - cached.fetchedAt < 5 * 60_000L) return; // 5min cache

        new Thread(() -> {
            try {
                PlayerStats stats = fetch(rsn);
                if (stats != null) {
                    cached = stats;
                    log.debug("Veil hiscores: loaded stats for {} (total level {})",
                        rsn, stats.getTotalLevel());
                }
            } catch (Exception e) {
                log.debug("Veil hiscores: failed for {}: {}", rsn, e.getMessage());
            }
        }, "veil-hiscores").start();
    }

    private static PlayerStats fetch(String rsn) throws Exception
    {
        String url = URL + java.net.URLEncoder.encode(rsn, "UTF-8");
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(8_000);
        conn.setReadTimeout(8_000);
        conn.setRequestProperty("User-Agent", UA);

        if (conn.getResponseCode() != 200) return null;

        String raw;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            raw = sb.toString();
        } finally { conn.disconnect(); }

        String[] lines = raw.trim().split("\n");
        PlayerStats stats = new PlayerStats();
        stats.rsn       = rsn;
        stats.fetchedAt = System.currentTimeMillis();

        int idx = 0;

        // Parse skills (24 skills)
        for (String skillName : SKILL_NAMES) {
            if (idx >= lines.length) break;
            String[] parts = lines[idx++].trim().split(",");
            if (parts.length >= 3) {
                try {
                    stats.skills.put(skillName, new int[]{
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2])
                    });
                } catch (NumberFormatException ignored) {}
            }
        }

        // Parse minigames (4 entries)
        for (String mgName : MINIGAME_NAMES) {
            if (idx >= lines.length) break;
            String[] parts = lines[idx++].trim().split(",");
            if (parts.length >= 2) {
                try {
                    int score = Integer.parseInt(parts[1]);
                    if (score > 0) stats.minigames.put(mgName, score);
                } catch (NumberFormatException ignored) {}
            }
        }

        // Parse bosses
        for (String bossName : BOSS_NAMES) {
            if (idx >= lines.length) break;
            String[] parts = lines[idx++].trim().split(",");
            if (parts.length >= 2) {
                try {
                    int kc = Integer.parseInt(parts[1]);
                    if (kc > 0) stats.bossKc.put(bossName, kc);
                } catch (NumberFormatException ignored) {}
            }
        }

        stats.loaded = true;
        return stats;
    }

    /**
     * Generate personalised advice based on actual stats.
     * Uses real levels to make recommendations instead of guessing.
     */
    public static String getPersonalisedAdvice(PlayerStats stats)
    {
        if (stats == null || !stats.loaded) return "Loading your stats...";

        StringBuilder sb = new StringBuilder();
        int slayer  = stats.getLevel("Slayer");
        int herb    = stats.getLevel("Herblore");
        int farm    = stats.getLevel("Farming");
        int craft   = stats.getLevel("Crafting");
        int magic   = stats.getLevel("Magic");
        int prayer  = stats.getLevel("Prayer");
        int combat  = (stats.getLevel("Attack") + stats.getLevel("Strength") +
                       stats.getLevel("Defence") + stats.getLevel("Hitpoints")) / 4;

        // Slayer advice based on actual level
        sb.append("SLAYER (level " + slayer + "):\n");
        if (slayer < 50)       sb.append("  Get Slayer level up. Do tasks from Mazchna (Canifis).\n");
        else if (slayer < 75)  sb.append("  Switch to Chaeldar (Lost City). Better tasks + GP.\n");
        else if (slayer < 85)  sb.append("  Nieve in Gnome Stronghold. Unlocks cave krakens at 87.\n");
        else                   sb.append("  Duradel in Shilo Village. Best tasks in game.\n");

        // Herblore advice
        sb.append("HERBLORE (level " + herb + "):\n");
        if (herb < 38)         sb.append("  Get to 38 for Prayer potions. Train on attack potions.\n");
        else if (herb < 63)    sb.append("  You can make Prayer pots! Check crafting tab for profit.\n");
        else if (herb < 80)    sb.append("  Super restore at 63 is profitable. Check crafting tab.\n");
        else                   sb.append("  Bastion potions at 80 are +12k profit each right now!\n");

        // Farming advice
        if (farm >= 17) {
            sb.append("FARMING (level " + farm + "):\n");
            if      (farm < 32) sb.append("  Plant marigolds/cabbage to level. Cheap XP.\n");
            else if (farm < 38) sb.append("  Getting close to Ranarr seeds. Keep going.\n");
            else                sb.append("  Plant herbs every ~80 min. Check Herb Patch tab for best pick.\n");
        }

        // Magic for superheat
        if (magic >= 43) {
            sb.append("MAGIC (level " + magic + "):\n");
            sb.append("  Level 43+ = can Superheat. Check Tools tab for profitable ores.\n");
        }

        // Boss KC
        if (!stats.bossKc.isEmpty()) {
            sb.append("BOSS KC: ");
            stats.bossKc.entrySet().stream().limit(5).forEach(e ->
                sb.append(e.getKey()).append(" ").append(e.getValue()).append(", "));
            sb.append("\n");
        }

        return sb.toString().trim();
    }
}
