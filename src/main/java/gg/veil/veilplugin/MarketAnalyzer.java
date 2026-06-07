package gg.veil.veilplugin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.Type;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;

/**
 * Veil Market Analyzer — the intelligence layer.
 *
 * Runs alongside WikiFlipFetcher every 60s.
 * Produces MarketIntelligence: category heat, supply shocks,
 * thin market gems, time-of-day context, session plan.
 *
 * THIS is what beats bots:
 * Bots see individual items. We see the whole market.
 */
@Slf4j
public class MarketAnalyzer
{
    private static final String UA      = "Veil-Client/5.0.0 (contact@veil.gg)";
    private static final String LATEST  = "https://prices.runescape.wiki/api/v1/osrs/latest";
    private static final String HOUR    = "https://prices.runescape.wiki/api/v1/osrs/1h";
    private static final String FIVE    = "https://prices.runescape.wiki/api/v1/osrs/5m";
    private static final int    TIMEOUT = 12_000;

    private static final Gson gson = new Gson();

    // Item categories — when one heats up, ALL items in it move
    private static final Map<String, int[]> CATEGORIES = new LinkedHashMap<>();
    static {
        CATEGORIES.put("Combat Potions",    new int[]{2428,2436,2440,139,113,2432,3040,111,12140,22461,12142});
        CATEGORIES.put("Food",              new int[]{385,391,379,361,7946,7060,5003,6685,3144,12167});
        CATEGORIES.put("Runes",             new int[]{554,555,556,557,558,559,560,561,562,563,564,565,566});
        CATEGORIES.put("Herb Seeds",        new int[]{5295,5300,5304,5305,207,5298,5310,5303,5302});
        CATEGORIES.put("Ores & Bars",       new int[]{440,453,447,449,451,2349,1436,1439,2357,2359,2361});
        CATEGORIES.put("Logs",              new int[]{1511,1519,1521,1515,1513,6333});
        CATEGORIES.put("God Wars Gear",     new int[]{11832,11834,11836,13652,11802,11804,11838,11840,11842});
        CATEGORIES.put("Barrows",           new int[]{4708,4710,4712,4714,4716,4718,4720,4722,4724,4726,4732,4734});
        CATEGORIES.put("Dragon Items",      new int[]{4087,4585,1305,4151,1377,1434,11212});
        CATEGORIES.put("Slayer Items",      new int[]{4151,12006,13276,1712,11773,19547,12927,12924});
        CATEGORIES.put("PvP / BH Items",    new int[]{13652,20997,22978,22981,24511,26219});
        CATEGORIES.put("Skilling Supplies", new int[]{1779,1785,1787,8794,9763,5325,5329,5331});
    }

    // Previous prices for spike detection (price, timestamp)
    private static final Map<Integer, long[]> priceHistory = new HashMap<>();

    public static MarketIntelligence analyze(
        Map<String, Map<String, Object>> latestItems,
        Map<String, Map<String, Object>> h1Items,
        Map<String, Map<String, Object>> m5Items,
        Map<String, Map<String, Object>> itemInfo)
    {
        MarketIntelligence intel = new MarketIntelligence();
        intel.timestamp = System.currentTimeMillis();

        // 1. Category heat map
        intel.categoryHeat = buildCategoryHeat(latestItems, h1Items, m5Items, itemInfo);

        // 2. Supply shock detection
        intel.supplyShocks = detectSupplyShocks(latestItems, h1Items, m5Items, itemInfo);

        // 3. Thin market gems
        intel.thinMarketGems = findThinMarketGems(latestItems, h1Items, itemInfo);

        // 4. Price spike detection
        intel.priceSpikes = detectPriceSpikes(latestItems, itemInfo);

        // 5. Time-of-day context
        intel.timeContext = buildTimeContext(intel.categoryHeat);

        // 6. Session plan
        intel.sessionPlan = buildSessionPlan(intel);

        return intel;
    }

    // ── Category heat map ─────────────────────────────────────

    private static List<MarketIntelligence.CategoryHeat> buildCategoryHeat(
        Map<String, Map<String, Object>> latest,
        Map<String, Map<String, Object>> h1,
        Map<String, Map<String, Object>> m5,
        Map<String, Map<String, Object>> info)
    {
        List<MarketIntelligence.CategoryHeat> result = new ArrayList<>();

        for (Map.Entry<String, int[]> cat : CATEGORIES.entrySet())
        {
            String catName = cat.getKey();
            int[]  ids     = cat.getValue();

            List<Double> pressures = new ArrayList<>();
            List<Double> momentums = new ArrayList<>();
            List<String> hotItemNames = new ArrayList<>();

            for (int id : ids)
            {
                String sid = String.valueOf(id);
                Map<String, Object> l  = latest.getOrDefault(sid, Collections.emptyMap());
                Map<String, Object> h  = h1.getOrDefault(sid, Collections.emptyMap());
                Map<String, Object> m  = m5.getOrDefault(sid, Collections.emptyMap());

                int hHv = num(h, "highPriceVolume");
                int hLv = num(h, "lowPriceVolume");
                int hvol = hHv + hLv;
                if (hvol < 10) continue;

                double pressure = hLv > 0 ? Math.min(8.0, (double)hHv / hLv) : 1.0;
                pressures.add(pressure);

                int hH = num(h, "avgHighPrice"), hL = num(h, "avgLowPrice");
                double vwap1h = (hH > 0 && hL > 0 && hvol > 0)
                    ? (hH * (double)hHv + hL * hLv) / hvol : 0;
                int mH = num(m, "avgHighPrice"), mL = num(m, "avgLowPrice");
                int mHv = num(m, "highPriceVolume"), mLv = num(m, "lowPriceVolume");
                int m5v = mHv + mLv;
                double vwap5m = (mH > 0 && mL > 0 && m5v > 0)
                    ? (mH * (double)mHv + mL * mLv) / m5v : 0;
                if (vwap1h > 0 && vwap5m > 0 && hvol > 200) {
                    momentums.add((vwap5m - vwap1h) / vwap1h * 100);
                }

                if (pressure > 1.5 && hotItemNames.size() < 3) {
                    Map<String, Object> inf = info.getOrDefault(sid, Collections.emptyMap());
                    String name = (String) inf.getOrDefault("name", sid);
                    hotItemNames.add(name);
                }
            }

            if (pressures.isEmpty()) continue;

            double avgP = pressures.stream().mapToDouble(Double::doubleValue).average().orElse(1.0);
            double avgM = momentums.isEmpty() ? 0.0
                : momentums.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            boolean isHot = avgP > 1.3 && avgM > 0.2;

            MarketIntelligence.CategoryHeat heat = new MarketIntelligence.CategoryHeat();
            heat.category   = catName;
            heat.avgPressure = Math.round(avgP * 100) / 100.0;
            heat.avgMomentum = Math.round(avgM * 100) / 100.0;
            heat.isHot      = isHot;
            heat.hotItems   = String.join(", ", hotItemNames);
            heat.advice     = buildCategoryAdvice(catName, avgP, avgM, isHot);
            result.add(heat);
        }

        result.sort((a, b) -> Double.compare(b.avgPressure * (b.avgMomentum + 1),
                                              a.avgPressure * (a.avgMomentum + 1)));
        return result;
    }

    private static String buildCategoryAdvice(String cat, double pressure, double momentum, boolean isHot)
    {
        if (isHot)
            return "🔥 Active now — check individual items for flips";
        if (pressure > 1.0 && momentum > 0)
            return "Warming up — watch for opportunities";
        if (pressure < 0.8)
            return "Sellers dominating — avoid buying in this category";
        return "Neutral — standard conditions";
    }

    // ── Supply shock detection ────────────────────────────────

    private static List<MarketIntelligence.SupplyShock> detectSupplyShocks(
        Map<String, Map<String, Object>> latest,
        Map<String, Map<String, Object>> h1,
        Map<String, Map<String, Object>> m5,
        Map<String, Map<String, Object>> info)
    {
        List<MarketIntelligence.SupplyShock> shocks = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : latest.entrySet())
        {
            String sid = entry.getKey();
            Map<String, Object> l = entry.getValue();
            Map<String, Object> h = h1.getOrDefault(sid, Collections.emptyMap());
            Map<String, Object> m = m5.getOrDefault(sid, Collections.emptyMap());

            int high = num(l, "high"), low = num(l, "low");
            if (high <= 0 || low <= 0) continue;

            int hHv = num(h, "highPriceVolume"), hLv = num(h, "lowPriceVolume");
            int hvol = hHv + hLv;
            if (hvol < 100 || hLv == 0) continue;

            double pressure = (double) hHv / hLv;
            if (pressure < 5.0) continue;  // Only extreme accumulation

            int hH = num(h, "avgHighPrice"), hL = num(h, "avgLowPrice");
            double vwap1h = (hH > 0 && hL > 0 && hvol > 0)
                ? (hH * (double)hHv + hL * hLv) / hvol : (high + low) / 2.0;
            int mH = num(m, "avgHighPrice"), mL = num(m, "avgLowPrice");
            int mHv = num(m, "highPriceVolume"), mLv = num(m, "lowPriceVolume");
            int m5v = mHv + mLv;
            double vwap5m = (mH > 0 && mL > 0 && m5v > 0)
                ? (mH * (double)mHv + mL * mLv) / m5v : 0;
            double momentum = (vwap5m > 0 && vwap1h > 0)
                ? (vwap5m - vwap1h) / vwap1h * 100 : 0;

            int tax = Math.min(5_000_000, Math.max(1, (int)(high * 0.01)));
            int netMargin = (high - low) - tax;
            if (netMargin <= 0) continue;

            Map<String, Object> inf = info.getOrDefault(sid, Collections.emptyMap());
            String name = (String) inf.getOrDefault("name", sid);

            MarketIntelligence.SupplyShock shock = new MarketIntelligence.SupplyShock();
            shock.itemId   = Integer.parseInt(sid);
            shock.itemName = name;
            shock.pressure = Math.round(pressure * 10) / 10.0;
            shock.momentum = Math.round(momentum * 100) / 100.0;
            shock.buyPrice = low;
            shock.netMargin = netMargin;
            shock.hourVol  = hvol;
            shock.alert    = buildShockAlert(name, pressure, momentum);
            shocks.add(shock);
        }

        shocks.sort((a, b) -> Double.compare(b.pressure, a.pressure));
        return shocks.subList(0, Math.min(10, shocks.size()));
    }

    private static String buildShockAlert(String name, double pressure, double momentum)
    {
        if (pressure > 50)
            return "EXTREME: " + (int)pressure + "× more buyers. Price spike likely imminent.";
        if (pressure > 15)
            return "HIGH: " + (int)pressure + "× buy pressure. Someone is accumulating fast.";
        return (int)pressure + "× buy pressure" + (momentum > 0 ? " + rising price." : ".");
    }

    // ── Thin market gems ──────────────────────────────────────

    private static List<MarketIntelligence.ThinMarketGem> findThinMarketGems(
        Map<String, Map<String, Object>> latest,
        Map<String, Map<String, Object>> h1,
        Map<String, Map<String, Object>> info)
    {
        List<MarketIntelligence.ThinMarketGem> gems = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : latest.entrySet())
        {
            String sid = entry.getKey();
            Map<String, Object> l = entry.getValue();
            Map<String, Object> h = h1.getOrDefault(sid, Collections.emptyMap());
            Map<String, Object> inf = info.getOrDefault(sid, Collections.emptyMap());

            int high = num(l, "high"), low = num(l, "low");
            if (high <= 0 || low <= 0 || high <= low) continue;

            Object limObj = inf.get("limit");
            int limit = limObj instanceof Number ? ((Number)limObj).intValue() : 0;
            if (limit == 0 || limit > 10) continue;

            int tax = Math.min(5_000_000, Math.max(1, (int)(high * 0.01)));
            int netMargin = (high - low) - tax;
            if (netMargin < 1_000_000) continue; // Only 1M+ margin gems

            double roi = (double) netMargin / low * 100;
            if (roi < 0.5) continue;

            int hvol = num(h, "highPriceVolume") + num(h, "lowPriceVolume");

            String name = (String) inf.getOrDefault("name", sid);

            MarketIntelligence.ThinMarketGem gem = new MarketIntelligence.ThinMarketGem();
            gem.itemId   = Integer.parseInt(sid);
            gem.itemName = name;
            gem.buyLimit = limit;
            gem.buyPrice = low;
            gem.sellPrice = high;
            gem.netMargin = netMargin;
            gem.roi      = Math.round(roi * 10) / 10.0;
            gem.hourVol  = hvol;
            gem.botRisk  = "None — only " + hvol + "/hr traded, bots skip this";
            gem.strategy = buildGemStrategy(name, low, high - 1, netMargin, limit, hvol);
            gems.add(gem);
        }

        gems.sort((a, b) -> Integer.compare(b.netMargin, a.netMargin));
        return gems.subList(0, Math.min(15, gems.size()));
    }

    private static String buildGemStrategy(String name, int buy, int sell, int profit, int limit, int vol)
    {
        double fillHrs = vol > 0 ? limit / (vol / 1.0) : 99;
        String fillStr = fillHrs < 1 ? "<1hr" : (int)fillHrs + "hrs";
        return String.format("Buy %d× @ %s. List at %s. Wait ~%s. Collect +%s. Repeat every 4hrs.",
            limit, fmtGp(buy), fmtGp(sell), fillStr, fmtGp(profit * limit));
    }

    // ── Price spike detection ─────────────────────────────────

    private static List<MarketIntelligence.PriceSpike> detectPriceSpikes(
        Map<String, Map<String, Object>> latest,
        Map<String, Map<String, Object>> info)
    {
        List<MarketIntelligence.PriceSpike> spikes = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Map.Entry<String, Map<String, Object>> entry : latest.entrySet())
        {
            String sid = entry.getKey();
            Map<String, Object> l = entry.getValue();
            int high = num(l, "high");
            if (high <= 0) continue;

            long[] prev = priceHistory.get(Integer.parseInt(sid));
            if (prev != null) {
                long prevPrice = prev[0];
                long prevTime  = prev[1];
                long ageMs = now - prevTime;
                // Only compare if within 30 minutes
                if (ageMs < 30 * 60_000 && prevPrice > 0) {
                    double changePct = (high - prevPrice) / (double) prevPrice * 100;
                    if (Math.abs(changePct) >= 5.0) {
                        Map<String, Object> inf = info.getOrDefault(sid, Collections.emptyMap());
                        String name = (String) inf.getOrDefault("name", sid);
                        MarketIntelligence.PriceSpike spike = new MarketIntelligence.PriceSpike();
                        spike.itemId       = Integer.parseInt(sid);
                        spike.itemName     = name;
                        spike.changePct    = Math.round(changePct * 10) / 10.0;
                        spike.currentPrice = high;
                        spike.direction    = changePct > 0 ? "UP" : "DOWN";
                        spike.possibleCause = Math.abs(changePct) > 15
                            ? "Major event? Check OSRS subreddit/Twitter"
                            : "Moderate move — watch for continuation";
                        spikes.add(spike);
                    }
                }
            }
            priceHistory.put(Integer.parseInt(sid), new long[]{high, now});
        }

        spikes.sort((a, b) -> Double.compare(Math.abs(b.changePct), Math.abs(a.changePct)));
        return spikes.subList(0, Math.min(5, spikes.size()));
    }

    // ── Time-of-day advisor ───────────────────────────────────

    private static MarketIntelligence.TimeOfDayContext buildTimeContext(
        List<MarketIntelligence.CategoryHeat> heat)
    {
        int utcHour = ZonedDateTime.now(ZoneOffset.UTC).getHour();

        MarketIntelligence.TimeOfDayContext ctx = new MarketIntelligence.TimeOfDayContext();
        ctx.utcHour = utcHour;

        // OSRS server population patterns (UTC):
        // 18-22: Peak EU evening + US afternoon = max players
        // 13-18: EU afternoon, US morning
        // 06-13: EU morning, low US
        // 22-06: Off-peak, AFK farmers active

        if (utcHour >= 18 && utcHour < 22) {
            ctx.session = "PEAK HOURS";
            ctx.bestCategories = "Combat Potions, Food, God Wars Gear, PvP Items";
            ctx.avoidCategories = "Ores, Logs (farmers not AFK now)";
            ctx.advice = "MAX volume period. PvMers and PvPers most active. " +
                "Flip consumables (potions, food, ammo). Gear demand is highest. " +
                "Spreads are tighter but fills happen in minutes.";
            ctx.minutesUntilPeak = 0;
        } else if (utcHour >= 13 && utcHour < 18) {
            ctx.session = "EU AFTERNOON";
            ctx.bestCategories = "Runes, Combat Potions, Slayer Items";
            ctx.avoidCategories = "Late-night resources";
            ctx.advice = "Building toward peak. Good for skilling supplies. " +
                "Slayer tasks active — check slayer equipment.";
            ctx.minutesUntilPeak = (18 - utcHour) * 60;
        } else if (utcHour >= 6 && utcHour < 13) {
            ctx.session = "EU MORNING";
            ctx.bestCategories = "Seeds, Herbs, Skilling Supplies";
            ctx.avoidCategories = "Gear (low PvM activity)";
            ctx.advice = "Lower activity. Good for patient flips with wide margins. " +
                "Herb seeds and farming supplies move overnight. " +
                "Thin market gems are best now — less competition.";
            ctx.minutesUntilPeak = (18 - utcHour) * 60;
        } else {
            ctx.session = "OFF-PEAK";
            ctx.bestCategories = "Ores, Logs, Raw Food, Seeds (AFK farmers active)";
            ctx.avoidCategories = "PvM gear (no demand right now)";
            ctx.advice = "AFK farmers and bots most active. Resources flood the market. " +
                "Buy resources cheap now, sell at peak hours. " +
                "Thin market gems and high-margin items are safest — bots avoid them.";
            ctx.minutesUntilPeak = ((24 - utcHour) + 18) * 60 % (24 * 60);
        }

        return ctx;
    }

    // ── Session plan ──────────────────────────────────────────

    private static String buildSessionPlan(MarketIntelligence intel)
    {
        StringBuilder plan = new StringBuilder();
        TimeZone tz = TimeZone.getDefault();

        plan.append("SESSION PLAN — ").append(intel.timeContext.session).append("\n\n");

        // Hot categories
        List<MarketIntelligence.CategoryHeat> hot = intel.categoryHeat.stream()
            .filter(c -> c.isHot).collect(java.util.stream.Collectors.toList());

        if (!hot.isEmpty()) {
            plan.append("🔥 HOT RIGHT NOW:\n");
            for (MarketIntelligence.CategoryHeat h : hot.subList(0, Math.min(3, hot.size()))) {
                plan.append("  → ").append(h.category)
                    .append(" (").append(h.avgPressure).append("× pressure)\n");
                if (!h.hotItems.isEmpty())
                    plan.append("    Check: ").append(h.hotItems).append("\n");
            }
            plan.append("\n");
        }

        // Supply shocks
        if (!intel.supplyShocks.isEmpty()) {
            plan.append("⚡ ACCUMULATION DETECTED:\n");
            for (MarketIntelligence.SupplyShock s : intel.supplyShocks.subList(0, Math.min(2, intel.supplyShocks.size()))) {
                plan.append("  → ").append(s.itemName)
                    .append(": ").append(s.alert).append("\n");
            }
            plan.append("\n");
        }

        // Time advice
        plan.append("⏰ TIME ADVICE:\n");
        plan.append("  ").append(intel.timeContext.advice).append("\n\n");

        // Thin market
        if (!intel.thinMarketGems.isEmpty()) {
            plan.append("💎 PATIENT PLAYS (no bot competition):\n");
            for (MarketIntelligence.ThinMarketGem g : intel.thinMarketGems.subList(0, Math.min(2, intel.thinMarketGems.size()))) {
                plan.append("  → ").append(g.itemName)
                    .append(": +").append(fmtGp(g.netMargin * g.buyLimit)).append(" per 4hr cycle\n");
            }
        }

        return plan.toString();
    }

    // ── Utilities ─────────────────────────────────────────────

    private static int num(Map<String, Object> m, String key)
    {
        Object v = m.get(key);
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }

    private static String fmtGp(long gp)
    {
        if (gp >= 1_000_000_000) return String.format("%.1fB", gp / 1_000_000_000.0);
        if (gp >= 1_000_000)     return String.format("%.1fM", gp / 1_000_000.0);
        if (gp >= 1_000)         return String.format("%.0fk", gp / 1_000.0);
        return String.valueOf(gp);
    }
}
