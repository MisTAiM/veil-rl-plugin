package gg.veil.veilplugin;

import lombok.Data;
import net.runelite.api.Skill;
import java.util.List;
import java.util.Map;

@Data
public class SyncPayload
{
    public String  rsn;
    public boolean loggedIn;
    public long    timestamp;

    // GE
    public List<TradeRecord> activeOffers;
    public List<TradeRecord> sessionTrades;
    public int  sessionProfitGp, sessionTradeCount, sessionBuyCount, sessionSellCount;
    public Map<Integer, Long> buyLimitResetAt;

    // Loot
    public List<LootRecord> sessionLoot;
    public int  sessionLootGp;

    // Slayer
    public SlayerState slayer;

    // XP
    public Map<Skill, Integer> xpGained;
    public double sessionHours;

    // NEW v3
    public QuestState2     questState;
    public EquipmentState  equipment;
    public WeaponCharges   weaponCharges;
    public BossDropProgress dropProgress;

    public String pluginVersion;
}
