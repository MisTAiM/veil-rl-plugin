package gg.veil.veilplugin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.Type;
import java.net.*;
import java.util.*;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Veil flip engine — research-backed, honest about competition.
 *
 * KEY IMPROVEMENTS over naive margin scanner:
 *
 * 1. REAL MARGIN with competition haircut
 *    High-volume items have more bots/players competing.
 *    We apply a liquidity discount: the margin you'll actually
 *    achieve is 55-90% of theoretical, based on volume.
 *
 * 2. GP/HR SCORING (not raw margin)
 *    What matters is how much you make PER HOUR, not per flip.
 *    GP/hr = real_margin × fills_per_hour
 *    This correctly ranks fast low-margin items vs slow high-margin.
 *
 * 3. VOLUME ACCELERATION
 *    Is volume growing or shrinking this hour?
 *    5m rate vs 1h rate tells us if a flip is dying or building.
 *
 * 4. MOMENTUM ONLY ON HIGH-VOLUME ITEMS
 *    Ranarr seed with 4,597 vol/hr has meaningless momentum.
 *    We only apply momentum signal when hourVol > 2,000.
 *    Below that threshold momentum = noise.
 *
 * 5. REALISTIC SIGNAL CONDITIONS
 *    ENTER: pressure ≥ 1.3 AND momentum non-negative (if applicable)
 *           AND volume not decelerating hard
 *    EXIT:  pressure < 0.7 OR momentum falling hard on liquid items
 *           OR volume collapsing
 *    HOLD:  everything else
 *
 * 6. GRADE BY GP/HR not arbitrary score thresholds
 *    S = >3M GP/hr achievable
 *    A = >1M GP/hr
 *    B = >300k GP/hr
 *    C = >50k GP/hr
 *    D = not worth your slot
 */
@Slf4j
public class WikiFlipFetcher
{
    private static final String UA      = "Veil-Client/3.0.0 (contact@veil.gg)";
    private static final String LATEST  = "https://prices.runescape.wiki/api/v1/osrs/latest";
    private static final String HOUR    = "https://prices.runescape.wiki/api/v1/osrs/1h";
    private static final String FIVE    = "https://prices.runescape.wiki/api/v1/osrs/5m";
    private static final String MAPPING = "https://prices.runescape.wiki/api/v1/osrs/mapping";
    private static final int    TIMEOUT = 12_000;

    private static final Gson gson = new Gson();

    // Last computed intelligence
    private static volatile MarketIntelligence lastIntel = null;
    public static MarketIntelligence getLastIntel() { return lastIntel; }

    public static List<FlipSignal> fetchTopFlips(int limit) throws Exception
    {
        Map<String, Object> latest  = fetchMap(LATEST);
        Map<String, Object> h1data  = fetchMap(HOUR);
        Map<String, Object> m5data  = fetchMap(FIVE);
        List<Map<String, Object>> mapping = fetchList(MAPPING);

        // Build lookup tables
        Map<String, String>  names  = new HashMap<>();
        Map<String, Integer> limits = new HashMap<>();
        Map<String, Boolean> membersMap = new HashMap<>();
        Map<String, Integer> highalchMap = new HashMap<>();
        for (Map<String, Object> item : mapping) {
            String id = String.valueOf(((Number) item.get("id")).intValue());
            names.put(id,  (String) item.getOrDefault("name", "?"));
            Object lim = item.get("limit");
            limits.put(id, lim != null ? ((Number) lim).intValue() : 0);
            Object mem = item.get("members");
            membersMap.put(id, mem instanceof Boolean ? (Boolean)mem : true);
            Object alch = item.get("highalch");
            highalchMap.put(id, alch instanceof Number ? ((Number)alch).intValue() : 0);
        }

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> latestItems =
            (Map<String, Map<String, Object>>) latest.get("data");
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> h1Items =
            (Map<String, Map<String, Object>>) h1data.get("data");
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> m5Items =
            (Map<String, Map<String, Object>>) m5data.get("data");

        if (latestItems == null) return Collections.emptyList();

        List<FlipSignal> results = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : latestItems.entrySet())
        {
            String iid  = entry.getKey();
            Map<String, Object> ldata = entry.getValue();

            int high = num(ldata, "high");
            int low  = num(ldata, "low");
            if (high <= 0 || low <= 0 || high <= low) continue;

            int buyLimit = limits.getOrDefault(iid, 0);
            if (buyLimit <= 0) continue;

            // ── GE tax ────────────────────────────────────────
            int tax        = Math.min(5_000_000, Math.max(1, (int)(high * 0.01)));
            int rawMargin  = high - low;
            int netMargin  = rawMargin - tax;
            if (netMargin <= 0) continue;

            // ── 1h data ───────────────────────────────────────
            Map<String, Object> h  = h1Items  != null ? h1Items.getOrDefault(iid, Collections.emptyMap()) : Collections.emptyMap();
            int hH  = num(h, "avgHighPrice");
            int hL  = num(h, "avgLowPrice");
            int hHv = num(h, "highPriceVolume");
            int hLv = num(h, "lowPriceVolume");
            int hourVol = hHv + hLv;

            // Minimum volume check — market must be real
            if (hourVol < 50) continue;

            // ── 5m data ───────────────────────────────────────
            Map<String, Object> m  = m5Items  != null ? m5Items.getOrDefault(iid, Collections.emptyMap()) : Collections.emptyMap();
            int mH  = num(m, "avgHighPrice");
            int mL  = num(m, "avgLowPrice");
            int mHv = num(m, "highPriceVolume");
            int mLv = num(m, "lowPriceVolume");
            int m5vol = mHv + mLv;

            // ── VWAP ──────────────────────────────────────────
            double vwap1h = (hH > 0 && hL > 0 && hourVol > 0)
                ? (hH * (double)hHv + hL * hLv) / hourVol
                : (high + low) / 2.0;
            double vwap5m = (mH > 0 && mL > 0 && m5vol > 0)
                ? (mH * (double)mHv + mL * mLv) / m5vol : 0;

            // ── Pressure ──────────────────────────────────────
            double pressure = hLv > 0
                ? Math.min(10.0, (double)hHv / hLv)
                : (hHv > 0 ? 2.0 : 1.0);

            // ── Momentum — ONLY if volume is meaningful ───────
            // Below 2000/hr the 5m sample is too small to trust
            double momentum = 0.0;
            if (hourVol >= 2000 && vwap5m > 0 && vwap1h > 0) {
                momentum = (vwap5m - vwap1h) / vwap1h;
            }

            // ── Volume acceleration ───────────────────────────
            // Is volume growing or shrinking vs the 1h rate?
            double volAccel = 0.0;
            if (hourVol > 0) {
                double m5Rate = m5vol * 12.0; // scale 5m to per-hour
                volAccel = (m5Rate - hourVol) / hourVol;
            }

            // ── REAL MARGIN with competition haircut ──────────
            // The theoretical margin is rarely achievable.
            // High-volume items have more competition → you get less.
            // These percentages are calibrated from real OSRS merching:
            double achievablePct;
            if (hourVol > 50_000)      achievablePct = 0.55; // very liquid = lots of bots
            else if (hourVol > 10_000) achievablePct = 0.65;
            else if (hourVol > 2_000)  achievablePct = 0.75;
            else if (hourVol > 500)    achievablePct = 0.85;
            else                       achievablePct = 0.92; // illiquid = less competition

            int realMargin = (int)(netMargin * achievablePct);
            if (realMargin < 50) continue;

            double roi = (double) realMargin / low * 100;
            if (roi < 0.25) continue;

            // ── Fill time ─────────────────────────────────────
            double volPerMin = hourVol / 60.0;
            double fillMins  = buyLimit / Math.max(volPerMin, 0.1);
            if (fillMins > 480) continue; // 8hr fill cap — only exclude truly untradeable-speed items

            // ── How many can you actually trade per 4hr? ──────
            int tradeable4hr = (int) Math.min(buyLimit, hourVol * 4.0);
            if (tradeable4hr <= 0) continue;

            // ── GP/HR — the real scoring metric ──────────────
            // Time per full cycle (buy + sell) in hours
            double cycleHrs = (fillMins * 2.0) / 60.0;
            double gpPerHr  = cycleHrs > 0
                ? (realMargin * (double) tradeable4hr) / Math.max(cycleHrs, 0.25)
                : 0;

            if (gpPerHr < 10_000) continue;

            int cycleGp = realMargin * tradeable4hr;

            // ── Kelly fraction ────────────────────────────────
            // Optimal position size: p×b - q / b where b = roi/100
            double pFill = Math.min(0.90, (double) hourVol / (4.0 * buyLimit));
            double b     = roi / 100.0;
            double kelly = b > 0 ? Math.min(0.25, Math.max(0.01, pFill * b / (b + 1))) : 0.01;

            // ── Grade by GP/HR ────────────────────────────────
            String grade;
            if (gpPerHr > 3_000_000)      grade = "S";
            else if (gpPerHr > 1_000_000) grade = "A";
            else if (gpPerHr > 300_000)   grade = "B";
            else if (gpPerHr > 50_000)    grade = "C";
            else grade = "D";

            // ── Signal — multi-condition ──────────────────────
            boolean volCrashing = volAccel < -0.35;
            boolean volBuilding  = volAccel > 0.15;

            boolean enter = pressure >= 1.3
                && !volCrashing
                && (hourVol < 2000 || momentum >= 0.0);

            boolean exit = pressure < 0.7
                || volCrashing
                || (hourVol >= 2000 && momentum < -0.03);

            String signal = enter ? "ENTER" : exit ? "EXIT" : "HOLD";

            // ── Build result ──────────────────────────────────
            FlipSignal fs     = new FlipSignal();
            fs.itemId         = Integer.parseInt(iid);
            fs.itemName       = names.getOrDefault(iid, "?");
            fs.buyPrice       = low;
            fs.sellPrice      = high;
            fs.margin         = rawMargin;
            fs.netMargin      = realMargin; // REAL margin after competition haircut
            fs.roi            = roi;
            fs.pressure       = pressure;
            fs.momentum       = momentum * 100;
            fs.hourVol        = hourVol;
            fs.buyLimit       = buyLimit;
            fs.fillMins       = (int) fillMins;
            fs.cycleGp        = cycleGp;
            fs.kelly          = kelly;
            fs.signal         = signal;
            fs.score          = (int) gpPerHr; // score IS gp/hr
            fs.grade          = grade;
            fs.vwap1h         = (int) vwap1h;
            fs.vwap5m         = (int) vwap5m;
            fs.members        = membersMap.getOrDefault(iid, true);
            fs.tradeable      = true;
            fs.highalch       = highalchMap.getOrDefault(iid, 0);
            int natureRunePrice = latest.getOrDefault("561", Collections.emptyMap())
                .containsKey("high") ? ((Number)latestItems.get("561").get("high")).intValue() : 125;
            fs.alchProfit     = fs.highalch > 0 ? fs.highalch - low - natureRunePrice : 0; // All items that pass our filters are GE tradeable

            results.add(fs);
        }

        // Build item info map for MarketAnalyzer
        Map<String, Map<String, Object>> infoMap = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> e : latestItems.entrySet()) {
            String sid = e.getKey();
            Map<String, Object> row = new HashMap<>();
            row.put("name",  names.getOrDefault(sid, "?"));
            row.put("limit", limits.getOrDefault(sid, 0));
            infoMap.put(sid, row);
        }
        // Run market intelligence analysis
        try {
            lastIntel = MarketAnalyzer.analyze(latestItems, h1Items, m5Items, infoMap);
        } catch (Exception ex) {
            // Intelligence is optional — never crash flips because of it
        }

        // Sort by GP/hr descending
        results.sort((a, b2) -> Integer.compare(b2.score, a.score));
        return results.subList(0, Math.min(limit, results.size()));
    }

    // ── HTTP helpers ──────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static Map<String, Object> fetchMap(String url) throws Exception
    {
        Type t = new TypeToken<Map<String, Object>>(){}.getType();
        return gson.fromJson(get(url), t);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> fetchList(String url) throws Exception
    {
        Type t = new TypeToken<List<Map<String, Object>>>(){}.getType();
        return gson.fromJson(get(url), t);
    }

    private static String get(String urlStr) throws Exception
    {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);
        conn.setRequestProperty("User-Agent", UA);
        conn.setRequestProperty("Accept", "application/json");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream())))
        {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
        finally { conn.disconnect(); }
    }

    private static int num(Map<String, Object> m, String key)
    {
        Object v = m.get(key);
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }
}
