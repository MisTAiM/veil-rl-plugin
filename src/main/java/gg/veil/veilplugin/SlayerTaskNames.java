package gg.veil.veilplugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps VarPlayerID.SLAYER_TARGET values to readable monster names.
 * Values sourced from OSRS wiki / RuneLite SlayerPlugin data.
 */
public class SlayerTaskNames
{
    private static final Map<Integer, String> NAMES = new HashMap<>();
    static {
        NAMES.put(1,  "Banshees");         NAMES.put(2,  "Bats");
        NAMES.put(3,  "Bears");            NAMES.put(4,  "Birds");
        NAMES.put(5,  "Cave bugs");        NAMES.put(6,  "Cave crawlers");
        NAMES.put(7,  "Cave slimes");      NAMES.put(8,  "Cows");
        NAMES.put(9,  "Crawling Hands");   NAMES.put(10, "Desert Lizards");
        NAMES.put(11, "Dogs");             NAMES.put(12, "Dwarves");
        NAMES.put(13, "Earthworms");       NAMES.put(14, "Elves");
        NAMES.put(15, "Flesh Crawlers");   NAMES.put(16, "Ghouls");
        NAMES.put(17, "Goblins");          NAMES.put(18, "Icefiends");
        NAMES.put(19, "Infernal Mages");   NAMES.put(20, "Jellies");
        NAMES.put(21, "Jungle Horrors");   NAMES.put(22, "Kalphites");
        NAMES.put(23, "Killerwatts");      NAMES.put(24, "Mogres");
        NAMES.put(25, "Monkeys");          NAMES.put(26, "Moss Giants");
        NAMES.put(27, "Ogres");            NAMES.put(28, "Pirates");
        NAMES.put(29, "Possessed Pickaxes");NAMES.put(30,"Pyrefiends");
        NAMES.put(31, "Rockslugs");        NAMES.put(32, "Sea Snakes");
        NAMES.put(33, "Shades");           NAMES.put(34, "Shadow Warriors");
        NAMES.put(35, "Skeletons");        NAMES.put(36, "Sourhogs");
        NAMES.put(37, "Spiders");          NAMES.put(38, "Spiritual creatures");
        NAMES.put(39, "Terror dogs");      NAMES.put(40, "Trolls");
        NAMES.put(41, "Turoth");           NAMES.put(42, "Vampyres");
        NAMES.put(43, "Wall Beasts");      NAMES.put(44, "Wolves");
        NAMES.put(45, "Zombies");          NAMES.put(46, "Abyssal demons");
        NAMES.put(47, "Cave horrors");     NAMES.put(48, "Cockatrice");
        NAMES.put(49, "Dagannoth");        NAMES.put(50, "Fire Giants");
        NAMES.put(51, "Gargoyles");        NAMES.put(52, "Greater Demons");
        NAMES.put(53, "Hellhounds");       NAMES.put(54, "Iron Dragons");
        NAMES.put(55, "Kurask");           NAMES.put(56, "Mutated Zygomites");
        NAMES.put(57, "Nechryael");        NAMES.put(58, "Red Dragons");
        NAMES.put(59, "Spiritual mages");  NAMES.put(60, "Steel Dragons");
        NAMES.put(61, "Suqahs");           NAMES.put(62, "Waterfiends");
        NAMES.put(63, "Black Demons");     NAMES.put(64, "Black Dragons");
        NAMES.put(65, "Blue Dragons");     NAMES.put(66, "Boss slayer tasks");
        NAMES.put(67, "Bronze Dragons");   NAMES.put(68, "Cave Krakens");
        NAMES.put(69, "Dark Beasts");      NAMES.put(70, "Dust Devils");
        NAMES.put(71, "Fossil Island Wyverns"); NAMES.put(72, "Ganodermic beasts");
        NAMES.put(73, "Smoke Devils");     NAMES.put(74, "Wyrms");
        NAMES.put(75, "Drakes");           NAMES.put(76, "Hydras");
        NAMES.put(77, "Cerberus");         NAMES.put(78, "Demonic Gorillas");
        NAMES.put(79, "Abyssal Sire");     NAMES.put(80, "Kalphite Queen");
        NAMES.put(81, "King Black Dragon");NAMES.put(82, "Barrows");
        NAMES.put(83, "Dagannoths (DKs)"); NAMES.put(84, "General Graardor");
        NAMES.put(85, "K'ril Tsutsaroth");NAMES.put(86, "Commander Zilyana");
        NAMES.put(87, "Kree'Arra");        NAMES.put(88, "Lizardman shamans");
        NAMES.put(89, "Deviant Spectres"); NAMES.put(90, "Grotesque Guardians");
        NAMES.put(91, "Skeletal Wyverns"); NAMES.put(92, "Bloodvelds");
        NAMES.put(93, "Black Knights");    NAMES.put(94, "Green Dragons");
        NAMES.put(95, "Ice Warriors");     NAMES.put(96, "Lesser Demons");
        NAMES.put(97, "Magic Axes");       NAMES.put(98, "Rock Crabs");
        NAMES.put(99, "Scorpions");        NAMES.put(100,"Sea Slugs");
        NAMES.put(101,"Spiritual Rangers");NAMES.put(102,"Spiritual Warriors");
        NAMES.put(103,"Werewolves");       NAMES.put(104,"White Dragons");
        NAMES.put(105,"Zulrah");           NAMES.put(106,"Vorkath");
        NAMES.put(107,"Rune Dragons");     NAMES.put(108,"Minions of Scabaras");
        NAMES.put(109,"Muspah");           NAMES.put(110,"Basilisks");
        NAMES.put(111,"Wyverns");          NAMES.put(112,"Scorpia");
        NAMES.put(113,"Venenatis");        NAMES.put(114,"Vet'ion");
        NAMES.put(115,"Callisto");         NAMES.put(116,"Chaos Elemental");
        NAMES.put(117,"Chaos Fanatic");    NAMES.put(118,"Crazy Archaeologist");
        NAMES.put(119,"Deranged Archaeologist");NAMES.put(120,"Giant Mole");
        NAMES.put(121,"Sarachnis");        NAMES.put(122,"Phosani's Nightmare");
        NAMES.put(123,"Spindel");          NAMES.put(124,"Artio");
        NAMES.put(125,"Calvar'ion");       NAMES.put(126,"Dusk (Grotesque Guardians)");
        NAMES.put(127,"Nechryarch");       NAMES.put(128,"Night Beast");
        NAMES.put(129,"Alchemical Hydra"); NAMES.put(130,"Duke Sucellus");
        NAMES.put(131,"The Leviathan");    NAMES.put(132,"The Whisperer");
        NAMES.put(133,"Vardorvis");        NAMES.put(134,"Tempoross");
        NAMES.put(135,"Wintertod");
    }

    public static String getName(int taskId)
    {
        return NAMES.getOrDefault(taskId, taskId > 0 ? "Task #" + taskId : "No task");
    }

    public static boolean hasName(int taskId) { return NAMES.containsKey(taskId); }
}
