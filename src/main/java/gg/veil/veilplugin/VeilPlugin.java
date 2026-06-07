package gg.veil.veilplugin;

import com.google.gson.Gson;
import com.google.inject.Provides;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.Notifier;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.ClientUI;
import java.awt.image.BufferedImage;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
    name        = "Veil Flipper",
    description = "GE tracking · quest sync · drop calculator · weapon charges · auto death risk · loot · slayer · XP · Veil mobile sync",
    tags        = {"ge","grand exchange","flip","flipper","merch","veil","mobile","slayer","loot","quest","charges"}
)
public class VeilPlugin extends Plugin
{
    private static final String VERSION = "3.0.0";
    private static final int GE_WIDGET_GROUP = 465;

    // Weapon charge thresholds for low-charge warnings
    private static final int BLOWPIPE_LOW  = 300;
    private static final int TRIDENT_LOW   = 100;
    private static final int SANGSTAFF_LOW = 100;
    private static final int SCYTHE_LOW    = 100;
    private static final int TUMEKENS_LOW  = 100;

    // Boss KC varplayer IDs (key bosses)
    // These are confirmed varplayer IDs from RL source
    private static final Map<String, Integer> BOSS_KC_VARPIDS = new LinkedHashMap<>();
    static {
        BOSS_KC_VARPIDS.put("Abyssal Sire",        526);
        BOSS_KC_VARPIDS.put("Cerberus",             523);
        BOSS_KC_VARPIDS.put("Hydra",                3981);
        BOSS_KC_VARPIDS.put("Kraken",               524);
        BOSS_KC_VARPIDS.put("Grotesque Guardians",  3981);
        BOSS_KC_VARPIDS.put("Zulrah",               443);
        BOSS_KC_VARPIDS.put("Vorkath",              524);
        BOSS_KC_VARPIDS.put("Dagannoth Rex",         1756);
        BOSS_KC_VARPIDS.put("Dagannoth Prime",       1757);
        BOSS_KC_VARPIDS.put("Dagannoth Supreme",     1758);
        BOSS_KC_VARPIDS.put("Thermy",               523);
        BOSS_KC_VARPIDS.put("Sarachnis",             523);
    }

    // Drop rates: bossName → {itemName → rate}
    private static final Map<String, Map<String, Integer>> DROP_RATES = new LinkedHashMap<>();
    static {
        drop("Abyssal Sire",    "Abyssal whip", 512, "Abyssal dagger", 256, "Abyssal head", 512);
        drop("Cerberus",        "Primordial crystal", 512, "Pegasian crystal", 512, "Eternal crystal", 512);
        drop("Hydra",           "Hydra tail", 512, "Hydra leather", 512, "Hydra's eye", 1000);
        drop("Kraken",          "Kraken tentacle", 512, "Trident of the seas", 512);
        drop("Zulrah",          "Tanzanite fang", 512, "Magic fang", 512, "Serpentine visage", 512);
        drop("Vorkath",         "Dragonbone necklace", 1000, "Skeletal visage", 5000);
        drop("Dagannoth Rex",   "Berserker ring", 128, "Dragon axe", 128);
        drop("Cerberus",        "Smouldering stone", 512);
        drop("Grotesque Guardians", "Granite gloves", 128, "Granite ring", 256, "Granite hammer", 256);
        drop("Thermy",          "Smoke battlestaff", 512, "Occult necklace", 512);
        drop("Sarachnis",       "Sarachnis cudgel", 384);
        drop("Basilisk Knight", "Basilisk jaw", 512);
        drop("Demonic Gorilla", "Zenyte shard", 300, "Ballista spring", 400);
        drop("Alchemical Hydra","Imbued heart", 512, "Hydra's claw", 512);
    }

    private static void drop(String boss, Object... pairs) {
        Map<String, Integer> m = DROP_RATES.computeIfAbsent(boss, k -> new LinkedHashMap<>());
        for (int i = 0; i < pairs.length - 1; i += 2)
            m.put((String) pairs[i], (Integer) pairs[i + 1]);
    }

    @Inject private Client        client;
    @Inject private ClientThread  clientThread;
    @Inject private VeilConfig    config;
    @Inject private ItemManager   itemManager;
    @Inject private OverlayManager overlayManager;
    @Inject private VeilOverlay   overlay;
    @Inject private Notifier      notifier;
    @Inject private ClientToolbar  clientToolbar;
    @Inject private VeilPanel      panel;

    private final Gson gson = new Gson();

    // ── GE Trade ─────────────────────────────────────────────
    private final Map<Integer, TradeRecord> activeOffers  = new ConcurrentHashMap<>();
    private final List<TradeRecord>         sessionTrades = new CopyOnWriteArrayList<>();
    private int sessionProfitGp = 0, sessionTradeCount = 0,
                sessionBuyCount = 0, sessionSellCount   = 0;
    private final Map<Integer, Long> buyLimitResetAt = new ConcurrentHashMap<>();

    // ── Loot ─────────────────────────────────────────────────
    private Map<Integer, Integer> inventorySnapshot = new HashMap<>();
    private final List<LootRecord> sessionLoot = new CopyOnWriteArrayList<>();
    private int sessionLootGp = 0;

    // ── Slayer ────────────────────────────────────────────────
    @Getter private SlayerState slayerState = new SlayerState();

    // ── XP ───────────────────────────────────────────────────
    private final Map<Skill, Integer> xpStart  = new EnumMap<>(Skill.class);
    private final Map<Skill, Integer> xpGained = new ConcurrentHashMap<>();
    private long sessionStartMs = 0;

    // ── Quests ───────────────────────────────────────────────
    @Getter private volatile QuestState2 questState = new QuestState2();
    private boolean questsDirty = true;

    // ── Equipment + Death Risk ────────────────────────────────
    @Getter private volatile EquipmentState equipmentState = new EquipmentState();

    // ── Weapon Charges ────────────────────────────────────────
    @Getter private volatile WeaponCharges weaponCharges = new WeaponCharges();

    // ── Boss KC + Drop Progress ───────────────────────────────
    private final Map<String, Integer> bossKcCache = new ConcurrentHashMap<>();
    @Getter private volatile BossDropProgress dropProgress = new BossDropProgress();

    // ── Flip signal + GE search ───────────────────────────────
    @Getter private volatile FlipSignal searchedItemSignal;
    private volatile List<FlipSignal>   cachedFlips = new ArrayList<>();

    // ── UI ────────────────────────────────────────────────────
    @Getter private boolean geOpen = false;
    @Getter private volatile int prayerPoints = 0;
    @Getter private volatile int specPercent  = 0;
    @Getter private volatile long coinStack = 0;

    // ── Sync ─────────────────────────────────────────────────
    private ScheduledExecutorService executor;
    private File tradeLogFile;

    // ─────────────────────────────────────────────────────────
    // LIFECYCLE
    // ─────────────────────────────────────────────────────────

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(overlay);
        sessionStartMs = System.currentTimeMillis();
        tradeLogFile = new File(System.getProperty("user.home"), ".runelite/veil/trades.jsonl");
        tradeLogFile.getParentFile().mkdirs();

        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "veil-worker"); t.setDaemon(true); return t; });

        // Side panel
        final BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        var g2 = icon.createGraphics();
        g2.setColor(new java.awt.Color(0xC9, 0xA8, 0x4C));
        g2.fillOval(2, 2, 12, 12);
        g2.dispose();
        NavigationButton navButton = NavigationButton.builder()
            .tooltip("Veil Flipper")
            .icon(icon)
            .priority(5)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navButton);

        // Sync server disabled — RuneLite plugin is standalone

        executor.scheduleAtFixedRate(this::refreshFlipCache, 5, 60, TimeUnit.SECONDS);

        log.info("Veil {} started", VERSION);
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        if (executor   != null) executor.shutdownNow();
        activeOffers.clear(); sessionTrades.clear(); sessionLoot.clear();
        xpStart.clear(); xpGained.clear(); buyLimitResetAt.clear();
        log.info("Veil stopped");
    }

    // ─────────────────────────────────────────────────────────
    // GE OFFER TRACKING
    // ─────────────────────────────────────────────────────────

    @Subscribe
    public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
    {
        final GrandExchangeOffer      offer = event.getOffer();
        final int                     slot  = event.getSlot();
        final GrandExchangeOfferState state = offer.getState();

        if (state == GrandExchangeOfferState.EMPTY) { activeOffers.remove(slot); return; }

        final int    itemId   = offer.getItemId();
        final String itemName = itemManager.getItemComposition(itemId).getName();
        final boolean isBuy   = state == GrandExchangeOfferState.BUYING
                              || state == GrandExchangeOfferState.BOUGHT
                              || state == GrandExchangeOfferState.CANCELLED_BUY;

        TradeRecord rec = activeOffers.computeIfAbsent(slot, s -> {
            TradeRecord r = new TradeRecord();
            r.slot = s; r.itemId = itemId; r.itemName = itemName;
            r.isBuy = isBuy; r.openedAt = Instant.now().toEpochMilli(); r.rsn = rsn();
            return r; });

        rec.quantityOffered = offer.getTotalQuantity();
        rec.quantityTraded  = offer.getQuantitySold();
        rec.pricePerUnit    = offer.getPrice();
        rec.avgFillPrice    = rec.quantityTraded > 0 ? offer.getSpent() / rec.quantityTraded : offer.getPrice();

        boolean done = state == GrandExchangeOfferState.BOUGHT   || state == GrandExchangeOfferState.SOLD
                    || state == GrandExchangeOfferState.CANCELLED_BUY || state == GrandExchangeOfferState.CANCELLED_SELL;

        if (done) {
            rec.complete = true; rec.closedAt = Instant.now().toEpochMilli();
            if (isBuy) {
                rec.totalSpent = offer.getSpent(); rec.totalReceived = rec.quantityTraded;
                rec.profitGp = 0; rec.taxGp = 0;
                if (state == GrandExchangeOfferState.BOUGHT && rec.quantityTraded > 0)
                    buyLimitResetAt.put(itemId, rec.openedAt + 4L * 60 * 60 * 1000);
            } else {
                int tax   = Math.min(5_000_000, Math.max(1, (int)(rec.avgFillPrice * rec.quantityTraded * 0.01)));
                int netGp = offer.getSpent() - tax;
                rec.totalReceived = netGp; rec.taxGp = tax;
                rec.profitGp = netGp - rec.pricePerUnit * rec.quantityTraded;
            }
            sessionProfitGp += rec.profitGp; sessionTradeCount++;
            if (isBuy) sessionBuyCount++; else sessionSellCount++;
            sessionTrades.add(0, rec);
            if (sessionTrades.size() > 300) sessionTrades.remove(sessionTrades.size() - 1);
            activeOffers.remove(slot);
            appendTradeToDisk(rec);
            if (panel != null) panel.updateSession();
            if (config.alertOnFill())
                notifier.notify(isBuy ? "Bought " + rec.quantityTraded + "× " + rec.itemName
                    : "Sold " + rec.quantityTraded + "× " + rec.itemName + " — " + fmt(rec.profitGp));
        }
    }

    @Subscribe
    public void onGrandExchangeSearched(GrandExchangeSearched event)
    {
        final String input = client.getVarcStrValue(VarClientID.MESLAYERINPUT);
        if (input == null || input.trim().isEmpty()) return;
        final String q = input.trim().toLowerCase();
        searchedItemSignal = cachedFlips.stream()
            .filter(f -> f.itemName.toLowerCase().contains(q))
            .findFirst().orElse(null);
    }

    // ─────────────────────────────────────────────────────────
    // WEAPON CHARGES — varbits fire on every change
    // ─────────────────────────────────────────────────────────

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        int varpId   = event.getVarpId();
        int varbitId = event.getVarbitId();
        int value    = event.getValue();

        // Prayer points (VarPlayer 709) and special attack % (VarPlayer 300)
        if (varpId == 709) prayerPoints = value;
        if (varpId == 300) specPercent  = value / 10; // stored as tenths

        // Slayer
        if (varpId == VarPlayerID.SLAYER_TARGET || varpId == VarPlayerID.SLAYER_COUNT)
            updateSlayerState();
        if (varbitId == VarbitID.SLAYER_POINTS)
            slayerState.points = value;
        if (varbitId == VarbitID.SLAYER_TASKS_COMPLETED)
            slayerState.streak = value;

        // Weapon charges
        WeaponCharges wc = new WeaponCharges();
        wc.blowpipeCharges  = client.getVarbitValue(VarbitID.CHARGES_TOXIC_BLOWPIPE_QUANTITY);
        wc.tridentCharges   = client.getVarbitValue(VarbitID.CHARGES_TRIDENT_OF_THE_SEAS_QUANTITY);
        wc.sangStaffCharges = client.getVarbitValue(VarbitID.CHARGES_SANGUINESTI_STAFF_QUANTITY);
        wc.tumekensCharges  = client.getVarbitValue(VarbitID.CHARGES_TUMEKENS_SHADOW_QUANTITY);

        wc.blowpipeLow  = wc.blowpipeCharges  > 0 && wc.blowpipeCharges  < BLOWPIPE_LOW;
        wc.tridentLow   = wc.tridentCharges   > 0 && wc.tridentCharges   < TRIDENT_LOW;
        wc.sangStaffLow = wc.sangStaffCharges > 0 && wc.sangStaffCharges < SANGSTAFF_LOW;
        wc.tumekensLow  = wc.tumekensCharges  > 0 && wc.tumekensCharges  < TUMEKENS_LOW;

        // Fire notifications for low charges
        WeaponCharges old = weaponCharges;
        if (!old.blowpipeLow  && wc.blowpipeLow)  notifier.notify("⚠ Blowpipe low — " + wc.blowpipeCharges + " darts left!");
        if (!old.tridentLow   && wc.tridentLow)   notifier.notify("⚠ Trident low — " + wc.tridentCharges + " charges!");
        if (!old.sangStaffLow && wc.sangStaffLow) notifier.notify("⚠ Sanguinesti staff low — " + wc.sangStaffCharges + " charges!");
        if (!old.tumekensLow  && wc.tumekensLow)  notifier.notify("⚠ Tumeken's Shadow low — " + wc.tumekensCharges + " charges!");

        weaponCharges = wc;

        // Quests dirty on any varbit (quests use varbits internally)
        questsDirty = true;
    }

    // ─────────────────────────────────────────────────────────
    // LOOT — inventory diff
    // ─────────────────────────────────────────────────────────

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        final int cid = event.getContainerId();

        // COINS — always read coin stack from inventory
        // Respect trackLoot config
        if (cid == InventoryID.INVENTORY.getId())
        {
            ItemContainer coinCheck = event.getItemContainer();
            if (coinCheck != null) {
                long coins = 0;
                for (Item item : coinCheck.getItems())
                    if (item.getId() == 995) coins += (long)item.getQuantity();
                coinStack = coins;
            }
        }

        // WORN equipment (94) → update death risk
        if (cid == InventoryID.EQUIPMENT.getId())
        {
            updateEquipmentState(event.getItemContainer());
            return;
        }

        // INV (93) → loot diff
        if (cid != InventoryID.INVENTORY.getId()) return;
        ItemContainer container = event.getItemContainer();
        if (container == null) return;

        Map<Integer, Integer> current = new HashMap<>();
        for (Item item : container.getItems())
            if (item.getId() > 0)
                current.merge(item.getId(), item.getQuantity(), Integer::sum);

        if (!inventorySnapshot.isEmpty()) {
            Map<Integer, Integer> gained = new HashMap<>();
            for (Map.Entry<Integer, Integer> e : current.entrySet()) {
                int delta = e.getValue() - inventorySnapshot.getOrDefault(e.getKey(), 0);
                if (delta > 0) gained.put(e.getKey(), delta);
            }
            if (!gained.isEmpty()) {
                int totalGp = 0;
                List<LootRecord.LootItem> items = new ArrayList<>();
                for (Map.Entry<Integer, Integer> e : gained.entrySet()) {
                    int price = itemManager.getItemPrice(e.getKey());
                    int val   = price * e.getValue();
                    totalGp  += val;
                    items.add(new LootRecord.LootItem(e.getKey(),
                        itemManager.getItemComposition(e.getKey()).getName(),
                        e.getValue(), price, val));
                }
                LootRecord record = new LootRecord();
                record.timestamp = Instant.now().toEpochMilli();
                record.totalGp   = totalGp; record.rsn = rsn(); record.items = items;
                sessionLoot.add(0, record);
                if (sessionLoot.size() > 100) sessionLoot.remove(sessionLoot.size() - 1);
                sessionLootGp += totalGp;
                if (panel != null) panel.updateSession();
                if (totalGp >= config.alertThresholdGp() && totalGp > 0)
                    notifier.notify("Loot: " + fmt(totalGp));
                    }
        }
        inventorySnapshot = current;
    }

    private void updateEquipmentState(ItemContainer worn)
    {
        if (worn == null) return;
        EquipmentState eq = new EquipmentState();
        eq.slots = new ArrayList<>();
        List<Integer> itemValues = new ArrayList<>();

        Item[] items = worn.getItems();
        for (int i = 0; i < items.length; i++) {
            Item item = items[i];
            if (item.getId() <= 0) continue;
            ItemComposition comp = itemManager.getItemComposition(item.getId());
            int price = itemManager.getItemPrice(item.getId()) * item.getQuantity();
            eq.totalValue += price;
            itemValues.add(price);
            EquipmentState.EquipSlot slot = new EquipmentState.EquipSlot();
            slot.itemId = item.getId(); slot.itemName = comp.getName();
            slot.quantity = item.getQuantity(); slot.geValue = price; slot.slot = i;
            eq.slots.add(slot);
        }
        // Top 3 kept on death (protect item adds 1 more — simplified: 3 kept)
        itemValues.sort(Collections.reverseOrder());
        int kept = Math.min(3, itemValues.size());
        eq.protectedValue = itemValues.subList(0, kept).stream().mapToInt(Integer::intValue).sum();
        eq.atRiskValue    = eq.totalValue - eq.protectedValue;
        equipmentState = eq;
    }

    // ─────────────────────────────────────────────────────────
    // STAT CHANGED — real-time XP
    // ─────────────────────────────────────────────────────────

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        final Skill skill = event.getSkill();
        final int   xp    = event.getXp();
        xpStart.computeIfAbsent(skill, k -> xp);
        int gained = xp - xpStart.getOrDefault(skill, xp);
        if (gained > 0) xpGained.put(skill, gained);
    }

    // ─────────────────────────────────────────────────────────
    // QUEST STATE — read from client thread on GameTick
    // ─────────────────────────────────────────────────────────

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        if (questsDirty)
        {
            questsDirty = false;
            refreshQuestState();
        }
    }

    private void refreshQuestState()
    {
        QuestState2 qs = new QuestState2();
        qs.quests = new ArrayList<>();
        int finished = 0, inProgress = 0, notStarted = 0, qp = 0;

        for (Quest quest : Quest.values())
        {
            try
            {
                net.runelite.api.QuestState state = quest.getState(client);
                QuestState2.QuestEntry entry = new QuestState2.QuestEntry();
                entry.name  = quest.getName();
                entry.state = state.name();
                qs.quests.add(entry);
                if (state == net.runelite.api.QuestState.FINISHED) { finished++; }
                else if (state == net.runelite.api.QuestState.IN_PROGRESS) { inProgress++; }
                else { notStarted++; }
            }
            catch (Exception ignored) {}
        }

        qs.finished    = finished;
        qs.inProgress  = inProgress;
        qs.notStarted  = notStarted;
        qs.totalQuests = finished + inProgress + notStarted;
        qs.questPoints = client.getVarpValue(VarPlayerID.QP);
        questState = qs;
    }

    // ─────────────────────────────────────────────────────────
    // GE OPEN / CLOSE
    // ─────────────────────────────────────────────────────────

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event)
    {
        if (event.getGroupId() == GE_WIDGET_GROUP) { geOpen = true; inventorySnapshot(); }
    }

    @Subscribe
    public void onWidgetClosed(WidgetClosed event)
    {
        if (event.getGroupId() == GE_WIDGET_GROUP) { geOpen = false; searchedItemSignal = null; }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN) {
            for (Skill s : Skill.values())
                xpStart.putIfAbsent(s, client.getSkillExperience(s));
            updateSlayerState();
            questsDirty = true;
        }
        if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
            geOpen = false;
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    private void updateSlayerState()
    {
        int count = client.getVarpValue(VarPlayerID.SLAYER_COUNT);
        slayerState.taskId   = client.getVarpValue(VarPlayerID.SLAYER_TARGET);
        slayerState.remaining = count;
        slayerState.locationId = client.getVarpValue(VarPlayerID.SLAYER_AREA);
        slayerState.points   = client.getVarbitValue(VarbitID.SLAYER_POINTS);
        slayerState.streak   = client.getVarbitValue(VarbitID.SLAYER_TASKS_COMPLETED);

        // Update drop progress for current slayer boss
        refreshDropProgress();
    }

    private void refreshDropProgress()
    {
        BossDropProgress progress = new BossDropProgress();
        for (Map.Entry<String, Map<String, Integer>> boss : DROP_RATES.entrySet())
        {
            Integer kc = bossKcCache.get(boss.getKey());
            if (kc == null || kc <= 0) continue;
            List<BossDropProgress.DropEntry> entries = new ArrayList<>();
            for (Map.Entry<String, Integer> drop : boss.getValue().entrySet())
            {
                int rate = drop.getValue();
                double prob = 1.0 - Math.pow(1.0 - 1.0 / rate, kc);
                boolean dry = kc > rate * 2;
                entries.add(new BossDropProgress.DropEntry(
                    drop.getKey(), rate, kc, prob,
                    String.format("%.1f%%", prob * 100), dry));
            }
            if (!entries.isEmpty()) progress.bosses.put(boss.getKey(), entries);
        }
        dropProgress = progress;
    }

    private void inventorySnapshot()
    {
        ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
        if (inv == null) return;
        inventorySnapshot = new HashMap<>();
        for (Item item : inv.getItems())
            if (item.getId() > 0) inventorySnapshot.merge(item.getId(), item.getQuantity(), Integer::sum);
    }

    }



    private void refreshFlipCache()
    {
        try {
            List<FlipSignal> fresh = WikiFlipFetcher.fetchTopFlips(50);
            if (!fresh.isEmpty()) { cachedFlips = fresh; if (panel != null) panel.refreshFlips(); }
        } catch (Exception e) { log.debug("Veil: flip refresh failed", e); }
    }

    private void appendTradeToDisk(TradeRecord rec)
    {
        try (FileWriter fw = new FileWriter(tradeLogFile, true))
        { fw.write(gson.toJson(rec) + "\n"); }
        catch (IOException e) { log.warn("Veil: persist failed", e); }
    }

    private String rsn() { Player p = client.getLocalPlayer(); return p != null ? p.getName() : "Unknown"; }

    private String fmt(int gp)
    {
        String sign = gp < 0 ? "-" : "+"; int abs = Math.abs(gp);
        if (abs >= 1_000_000) return sign + String.format("%.1fM", abs / 1_000_000.0);
        if (abs >= 1_000)     return sign + String.format("%.0fk", abs / 1_000.0);
        return sign + abs;
    }

    public List<TradeRecord> getActiveOffers() { return new ArrayList<>(activeOffers.values()); }
    public FlipSignal getTopFlip() { return cachedFlips.isEmpty() ? null : cachedFlips.get(0); }
    public List<FlipSignal> getCachedFlips() { return cachedFlips; }
    public MarketIntelligence getMarketIntelligence() { return WikiFlipFetcher.getLastIntel(); }
    public Map<Integer, Long> getBuyLimitResetAt() { return buyLimitResetAt; }
    public List<LootRecord> getSessionLoot() { return sessionLoot; }
    public List<TradeRecord> getSessionTrades() { return new ArrayList<>(sessionTrades.subList(0, Math.min(50, sessionTrades.size()))); }
    public int getSessionLootGp() { return sessionLootGp; }
    public Map<Skill, Integer> getXpGained() { return xpGained; }
    public long getSessionStartMs() { return sessionStartMs; }

    @Data public static class SessionStats { public int sessionProfitGp, tradeCount; }
    public SessionStats getSessionStats() {
        SessionStats s = new SessionStats();
        s.sessionProfitGp = sessionProfitGp; s.tradeCount = sessionTradeCount; return s; }

    @Provides VeilConfig provideConfig(ConfigManager cm) { return cm.getConfig(VeilConfig.class); }
}
