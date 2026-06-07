package gg.veil.veilplugin;

import java.util.*;

/**
 * Veil Knowledge Base — every piece of game knowledge the user needs.
 *
 * Contains:
 * - Slayer task database (method, location, block/extend, GP/hr)
 * - Boss unlock requirements (what quests/stats needed)
 * - Progression stages (what to do based on bankroll)
 * - Equipment upgrade path (next BiS upgrade from current gear)
 * - Daily routine generator (flip → boss → flip schedule)
 * - Quest chains for content access
 */
public class VeilKnowledge
{
    // ── PROGRESSION STAGES ────────────────────────────────────
    public static String getStage(long coins)
    {
        if (coins < 1_000_000)   return "BEGINNER";
        if (coins < 10_000_000)  return "EARLY";
        if (coins < 50_000_000)  return "MID";
        if (coins < 200_000_000) return "ADVANCED";
        return "ENDGAME";
    }

    public static String getStageAdvice(long coins)
    {
        if (coins < 1_000_000)
            return "Focus on high-volume low-cost items. Cannonballs, rune ore, coal. " +
                   "Aim for 3% per flip cycle. Goal: reach 1M GP.";
        if (coins < 10_000_000)
            return "Move into potions, food, and common supplies. Sharks, prayer potions, " +
                   "nature runes. 3 flips/day at 5% = doubles in 2 weeks. Goal: 10M GP.";
        if (coins < 50_000_000)
            return "Gear flips now viable. Bandos, Armadyl, whip, barrows. " +
                   "Combine with slayer for passive income. Goal: 50M GP.";
        if (coins < 200_000_000)
            return "Endgame gear flips. ToB/ToA items, Twisted bow, Scythe. " +
                   "Flip 2-3 items/day + daily bossing routine. Goal: 200M GP.";
        return "You're in the top tier. Flip high-margin rare items. " +
               "Consider bonds for extra accounts to multiply buy limits.";
    }

    public static String getNextMilestone(long coins)
    {
        if (coins < 1_000_000)   return "1M GP — unlock potion flipping";
        if (coins < 10_000_000)  return "10M GP — unlock gear flipping";
        if (coins < 50_000_000)  return "50M GP — unlock endgame items";
        if (coins < 200_000_000) return "200M GP — max efficiency tier";
        return "Maxed — focus on hourly GP/hr optimization";
    }

    // ── RECOMMENDED FLIPS BY STAGE ────────────────────────────
    public static List<String> getStageItems(long coins)
    {
        if (coins < 1_000_000)
            return Arrays.asList("Cannonball", "Coal", "Rune ore", "Cowhide",
                "Steel bar", "Molten glass", "Unpowered orb", "Bow string");
        if (coins < 10_000_000)
            return Arrays.asList("Shark", "Manta ray", "Anglerfish",
                "Prayer potion(4)", "Super restore(4)", "Nature rune",
                "Dragon bones", "Ranging potion(4)", "Stamina potion(4)");
        if (coins < 50_000_000)
            return Arrays.asList("Abyssal whip", "Rune platebody",
                "Bandos boots", "Berserker ring", "Dragon platelegs",
                "Barrows armour", "Kraken tentacle", "Dragon claws");
        return Arrays.asList("Bandos chestplate", "Bandos tassets",
            "Armadyl chestplate", "Armadyl chainskirt", "Armadyl godsword",
            "Twisted bow", "Scythe of vitur", "Tumeken's shadow");
    }

    // ── SLAYER TASK DATABASE ───────────────────────────────────
    public static class SlayerTask
    {
        public final String name;
        public final String location;
        public final String method;
        public final String gear;
        public final String gpHr;
        public final boolean extend;
        public final boolean block;
        public final String extendReason;
        public final String blockReason;
        public final String requirements;
        public final String tips;

        SlayerTask(String name, String location, String method, String gear,
                   String gpHr, boolean extend, boolean block,
                   String extendReason, String blockReason,
                   String requirements, String tips)
        {
            this.name = name; this.location = location;
            this.method = method; this.gear = gear;
            this.gpHr = gpHr; this.extend = extend; this.block = block;
            this.extendReason = extendReason; this.blockReason = blockReason;
            this.requirements = requirements; this.tips = tips;
        }
    }

    public static final Map<String, SlayerTask> SLAYER_DB = new LinkedHashMap<>();
    static {
        s("Abyssal demons", 85,
          "Slayer Tower (top floor) OR Catacombs of Kourend",
          "Melee — use whip or rapier. Arclight if you have it.",
          "Whip/Rapier, Proselyte/Void, Abyssal dagger spec",
          "400-600k GP/hr + 1/512 abyssal whip",
          true, false,
          "Best abyssal whip source. 85k slayer XP/hr. Prayer XP in Catacombs.",
          "",
          "85 Slayer required",
          "USE: Catacombs for prayer XP from ensouled heads. Bring herb sack."
        );
        s("Gargoyles", 75,
          "Slayer Tower (top floor, must be on task)",
          "Melee — they CANNOT be killed without rock hammer.",
          "Melee gear, Rock hammer (buy from slayer master for 50gp!)",
          "600-900k GP/hr — great bars + chance at granite hammer",
          true, false,
          "Excellent GP. Granite hammer worth 20M+. Fast AFK method.",
          "",
          "75 Slayer required. Rock hammer in inventory (mandatory).",
          "BUY rock hammer from slayer master before going. Without it you deal 0 damage."
        );
        s("Cave kraken", 87,
          "Kraken Cove (west of Piscatoris Fishing Colony)",
          "MAGIC ONLY — they are immune to melee and ranged.",
          "Trident of the seas, occult necklace, ancestral/ahrim",
          "500-700k GP/hr + chance at trident (2M) or tentacle",
          true, false,
          "Trident of the seas drops here. Kraken boss variant drops tentacle.",
          "",
          "87 Slayer. Fairy ring AKQ or spirit tree to Piscatoris.",
          "Protect from Magic always. Kraken boss variant in whirlpool — good for tentacle."
        );
        s("Cerberus", 91,
          "Taverley Dungeon (hellhound area, requires summoning souls)",
          "Melee — protect from Magic during triple ghost attack.",
          "Proselyte armour, whip or hasta, prayer potions",
          "500-800k GP/hr + primordial/pegasian/eternal crystals (50M+)",
          true, false,
          "Crystal drops worth 50M+ each. Best prayer XP/GP combo.",
          "",
          "91 Slayer. Must have completed 'Beneath Cursed Sands' quest for access.",
          "CRITICAL: When 3 ghosts appear — pray the colour that matches (blue=mage, green=range, red=melee)."
        );
        s("Hydra", 95,
          "Mount Karuulm (Iorwerth Dungeon)",
          "Ranged with ruby bolts(e) for DPS. Protect from range.",
          "Armadyl/void ranged, ruby bolts(e) for Alchemical Hydra boss",
          "1-2M GP/hr at Alchemical Hydra boss",
          true, false,
          "Alchemical Hydra variant drops claws (30M+). Best task in game.",
          "",
          "95 Slayer. Farmer's boots or Boots of stone required in dungeon.",
          "Switch phases: when color changes, move to correct side. Don't stand in AoE."
        );
        s("Bloodvelds", 50,
          "Slayer Tower / Catacombs of Kourend / God Wars Dungeon",
          "Magic attack style — they are weak to magic.",
          "Any magic gear — ibans blast or trident",
          "200-300k GP/hr + prayer XP in catacombs",
          true, false,
          "Fast task. Prayer XP from mutated bloodvelds in Catacombs is excellent.",
          "",
          "50 Slayer required",
          "Go to Catacombs for mutated bloodvelds — they give prayer XP from ensouled heads."
        );
        s("Drakes", 84,
          "Mount Karuulm (Iorwerth Dungeon)",
          "Melee or Ranged — bring antifire potions.",
          "Melee gear or ranged, antifire potions",
          "400-600k GP/hr",
          false, false,
          "", "",
          "84 Slayer. Farmer's boots or Boots of stone required.",
          "Antifires are essential. Can be off-task killed but task gives more drops."
        );
        s("Duradel's best tasks", 0,
          "Various locations",
          "Extend: Abyssal demons, Gargoyles, Cave kraken, Cerberus, Hydra, Nechryaels",
          "",
          "Varies",
          false, false, "", "",
          "Unlocked via completing 'Smoking Kills' quest for full slayer rewards",
          "BLOCK: Banshees, Brine rats, Cave bugs, Cockatrice, Desert lizards, Pyrefiends, Turoth, Vampyres"
        );
        s("Nechryaels", 80,
          "Slayer Tower (top floor) OR Catacombs of Kourend",
          "Melee — use cannon if possible. AFK friendly.",
          "Cannon (250gp/min cost), melee gear",
          "300-500k GP/hr",
          true, false,
          "AFK with cannon. Death rune drops are frequent.",
          "",
          "80 Slayer required",
          "Set cannon. AFK. Collect death runes. Very low effort task."
        );
        s("Greater demons", 0,
          "Brimhaven Dungeon / Catacombs / Entrana Dungeon",
          "Melee. Protect from Melee if needed.",
          "Standard melee gear",
          "200-350k GP/hr",
          false, false,
          "", "",
          "No slayer level required — assigned by lower masters",
          "USE: Iorwerth dungeon greater demons for best XP rate."
        );
        s("Black dragons", 0,
          "Taverley Dungeon / Evil Chicken's Lair / Myths' Guild basement",
          "Ranged or Melee with antifire. Baby black dragons much easier.",
          "Antifire potions + dragonfire shield, or void ranged",
          "200-400k GP/hr",
          false, false,
          "", "",
          "Dragon Slayer I quest required for antifire potions",
          "Baby black dragons are weaker — easier to kill but less XP."
        );
        s("Adamant/Rune dragons", 0,
          "Lithkren Vault (Dragon Slayer II required!)",
          "Ranged with ruby bolts(e). Extended antifires mandatory.",
          "Armadyl or void ranged, anti-dragon shield + extended antifire",
          "800k-1.5M GP/hr at rune dragons",
          true, false,
          "Draconic visage and dragon plateskirt/legs chance. Great GP.",
          "",
          "REQUIRES: Dragon Slayer II quest (hardest quest in game). 84 Smithing helps.",
          "MUST use extended antifire or dragon hunter crossbow. Without it you take constant damage."
        );
        s("Smoke devils", 93,
          "Smoke Devil Dungeon (south of Castle Wars)",
          "Ranged only — they are immune to melee.",
          "Void or armadyl ranged",
          "400-600k GP/hr + occult necklace chance",
          false, false, "", "",
          "93 Slayer required. Completion of Smoking Kills quest for points.",
          "Cannon works here. Occult necklace drop is the big ticket item."
        );
    }

    private static void s(String name, int slayerReq, String loc, String method,
                           String gear, String gpHr, boolean extend, boolean block,
                           String extendReason, String blockReason,
                           String requirements, String tips)
    {
        String fullReq = slayerReq > 0
            ? slayerReq + " Slayer required. " + requirements
            : requirements;
        SLAYER_DB.put(name.toLowerCase(), new SlayerTask(
            name, loc, method, gear, gpHr, extend, block,
            extendReason, blockReason, fullReq, tips));
    }

    // ── BOSS QUEST REQUIREMENTS ────────────────────────────────
    public static class BossInfo
    {
        public final String name;
        public final String questChain;    // shortest path to unlock
        public final String statsNeeded;
        public final String gearNeeded;
        public final String gpHr;
        public final String method;
        public final String topDrops;
        public final String beginnerGuide;

        BossInfo(String name, String questChain, String statsNeeded,
                 String gearNeeded, String gpHr, String method,
                 String topDrops, String beginnerGuide)
        {
            this.name = name; this.questChain = questChain;
            this.statsNeeded = statsNeeded; this.gearNeeded = gearNeeded;
            this.gpHr = gpHr; this.method = method;
            this.topDrops = topDrops; this.beginnerGuide = beginnerGuide;
        }
    }

    public static final Map<String, BossInfo> BOSS_DB = new LinkedHashMap<>();
    static {
        b("Zulrah",
          "Desert Treasure → Underground Pass → Regicide → Mourning's End Pt 1 → Pt 2 (12 quests total, ~10hrs)",
          "70+ Magic, 70+ Ranged, 43+ Prayer",
          "Trident of the seas (2M), void mage+range (20M), anguish, torture",
          "2-3M GP/hr",
          "Memorize 4 rotations. Use RuneLite Zulrah plugin to show rotation.",
          "Magic fang (9M), tanzanite fang (16M), serpentine visage (3M)",
          "BEGINNER: Watch YouTube 'Zulrah guide for beginners'. Use RuneLite Zulrah helper plugin."
        );
        b("Vorkath",
          "Dragon Slayer II (very long quest — 205 QP needed, 50+ stats across the board)",
          "70+ Attack/Strength/Defence, 55+ Prayer, antifire mandatory",
          "Dragon hunter crossbow (35M) OR Dragon hunter lance, antifire potions, Void or Bandos",
          "2-3M GP/hr",
          "Woox walk during acid phase (run diagonally). Protect from Magic always.",
          "Dragonbone necklace (1M), skeletal visage (17M), draconic visage (4M)",
          "BEGINNER: Learn Woox walk pattern. Use RuneLite Vorkath plugin. Always have extended antifire."
        );
        b("God Wars Dungeon (Bandos/Armadyl/Zamorak/Saradomin)",
          "Troll Stronghold (requires Death Plateau — 5min, no reqs) → then just 40 Kill Count in GWD",
          "70+ melee or 70+ ranged depending on boss",
          "Bandos/Armadyl gear, godsword, prayer potions",
          "500k-1.5M GP/hr",
          "Kill 40 of the right faction mobs to enter boss chamber. Protect from appropriate attack style.",
          "Bandos chestplate (23M), tassets (40M), Armadyl chestplate (34M), godswords",
          "BEGINNER: Do Death Plateau (10min, no reqs) → Troll Stronghold → you can access GWD."
        );
        b("Theatre of Blood",
          "Priest in Peril + Animal Magnetism quests (both short, easy). Then practice mechanics.",
          "90+ in relevant combat stats highly recommended",
          "Best in slot gear for your style — budget: ~200-300M",
          "3-5M GP/hr in group",
          "5-room gauntlet with team. Each room has unique mechanics. Learn in learning worlds.",
          "Twisted bow (1.6B), Scythe of vitur (750M), Justiciar armour (40-60M)",
          "BEGINNER: Join 'ToB learner' cc. Watch raid guides. Practice in story mode first."
        );
        b("Tombs of Amascut",
          "Beneath Cursed Sands quest (short — just need 62 Agility and 55 Crafting)",
          "Any levels — scales with your stats",
          "Good melee/ranged/mage gear. The raid scales to your stats.",
          "2-4M GP/hr",
          "5 rooms + boss. Set invocation level based on experience. Start at 0 for learning.",
          "Tumeken's shadow (1.5B), Osmumten's fang (40M), Masori armour (100M+)",
          "BEGINNER: Do Beneath Cursed Sands quest → start at 0 invocations → scale up gradually."
        );
        b("Chambers of Xeric",
          "No quest required! Just need 130 total level to enter.",
          "Any — scales with stats. 90+ combat recommended for efficiency.",
          "Best gear you have. Scythe or DHL for melee-heavy setup.",
          "1-3M GP/hr",
          "Multi-room raid. Different rooms each run. Use RuneLite CoX plugin.",
          "Twisted bow (1.6B), dragon claws (70M), ancestral robes (70M+)",
          "BEGINNER: No quest needed — just walk in. Watch 'Cox beginner guide' on YouTube."
        );
        b("Corporeal Beast",
          "No quest required. Located in the Spirit Realm — enter through cave north of Port Sarim.",
          "Any combat stats. 90+ recommended.",
          "Spec weapon (dragon warhammer or bandos godsword). Arclight works.",
          "500k-2M GP/hr (depending on team size)",
          "Solo or team. Spec down with DWH or BGS then kill. Use Arclight for 70% spec reduction.",
          "Elysian spirit shield (900M), arcane spirit shield (40M), divine spirit shield (1B)",
          "BEGINNER: Bring friends. Use BGS or DWH to drain defence. Use Arclight."
        );
    }

    private static void b(String name, String questChain, String stats,
                           String gear, String gpHr, String method,
                           String topDrops, String beginnerGuide)
    {
        BOSS_DB.put(name.toLowerCase(), new BossInfo(
            name, questChain, stats, gear, gpHr, method, topDrops, beginnerGuide));
    }

    // ── DAILY ROUTINE GENERATOR ────────────────────────────────
    public static String getDailyRoutine(long coins, boolean hasSlayerTask, int slayerRemaining)
    {
        String stage = getStage(coins);
        StringBuilder sb = new StringBuilder();
        sb.append("YOUR DAILY ROUTINE\n\n");

        // Morning flip
        sb.append("MORNING (30-45 min):\n");
        sb.append("  1. Log in, open GE\n");
        sb.append("  2. Check Veil → Flips tab → pick top ENTER signal item\n");
        sb.append("  3. Buy your full limit → log out while it fills\n\n");

        // Main activity
        sb.append("MAIN SESSION (1-3 hrs):\n");
        if (hasSlayerTask && slayerRemaining > 0) {
            sb.append("  You have a slayer task — do it now!\n");
            sb.append("  Check Veil → Tracker tab for method guide\n");
            sb.append("  Slayer = XP + GP at the same time\n\n");
        } else {
            switch (stage) {
                case "BEGINNER":
                    sb.append("  Train combat at crabs/nmz while flips run\n");
                    sb.append("  Goal: reach 70+ combat stats for bossing\n\n");
                    break;
                case "EARLY":
                    sb.append("  Get a slayer task from Duradel/Nieve\n");
                    sb.append("  Slayer is the best XP + GP combo in the game\n\n");
                    break;
                case "MID":
                    sb.append("  Boss for 1-2hrs: GWD or Zulrah\n");
                    sb.append("  Collect your morning flip GP on the way out\n\n");
                    break;
                default:
                    sb.append("  ToB/ToA runs for 2-3hrs\n");
                    sb.append("  Re-flip between raids\n\n");
                    break;
            }
        }

        // Evening flip
        sb.append("EVENING (30 min):\n");
        sb.append("  1. Collect morning flip\n");
        sb.append("  2. Post evening flip — pick different item from morning\n");
        sb.append("  3. Stagger sell prices to collect through the night\n\n");

        sb.append("EXPECTED DAILY GP: " + getDailyGpEstimate(coins));
        return sb.toString();
    }

    private static String getDailyGpEstimate(long coins)
    {
        if (coins < 1_000_000)   return "100-300k from flipping + skilling";
        if (coins < 10_000_000)  return "500k-2M from flipping + slayer";
        if (coins < 50_000_000)  return "2-5M from flipping + bossing";
        if (coins < 200_000_000) return "5-15M from flipping + raids";
        return "15M+ from optimal flip rotation + ToB/ToA";
    }

    // ── EQUIPMENT UPGRADE PATH ─────────────────────────────────
    public static String getUpgradeAdvice(long coins)
    {
        if (coins < 500_000)
            return "Priority: 1) Rune armour (40k) 2) Dragon scimitar (60k) 3) Berserker helm (100k)";
        if (coins < 5_000_000)
            return "Priority: 1) Barrows gloves (quest) 2) Fighter torso (minigame) 3) Dragon defender (Warriors Guild)";
        if (coins < 20_000_000)
            return "Priority: 1) Abyssal whip (1.1M) 2) Bandos boots (500k) 3) Berserker ring (3M)";
        if (coins < 100_000_000)
            return "Priority: 1) Bandos chestplate (23M) 2) Bandos tassets (40M) 3) Dragon claws (70M)";
        if (coins < 500_000_000)
            return "Priority: 1) Armadyl set (100M) 2) Twisted bow (1.6B if you can) 3) Ancestral (70M+)";
        return "Priority: 1) Twisted bow (1.6B) 2) Scythe of vitur (750M) 3) Tumeken's shadow (1.5B)";
    }

    // ── SLAYER TASK MATCHER ────────────────────────────────────
    public static SlayerTask findTask(String taskName)
    {
        if (taskName == null) return null;
        String lower = taskName.toLowerCase().trim();
        // Direct match
        if (SLAYER_DB.containsKey(lower)) return SLAYER_DB.get(lower);
        // Partial match
        for (Map.Entry<String, SlayerTask> e : SLAYER_DB.entrySet()) {
            if (lower.contains(e.getKey()) || e.getKey().contains(lower))
                return e.getValue();
        }
        return null;
    }
}
