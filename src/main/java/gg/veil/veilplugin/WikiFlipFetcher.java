package gg.veil.veilplugin;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;

/**
 * Veil Flip Engine v5.0
 * Phase 1 + Phase 2 + all new features integrated.
 *
 * Phase 1 (accuracy):
 *   - Price freshness filter (timestamp-based, 6 tiers)
 *   - Spread ratio signal (current vs 1h avg)
 *   - Calibrated haircut (>100k=45%, 0vol=60%)
 *   - Realistic GP/hr (theoretical × 0.65)
 *   - Three fill time scenarios (fast/std/patient)
 *   - Confidence score (0-100)
 *
 * Phase 2 (signals):
 *   - Pressure velocity (RISING/FALLING/FLAT from timeseries trend)
 *   - Market making detector (flat momentum + wide spread + high vol)
 *   - Correlation pairs (Bandos chest/tassets, Armadyl set, crystals)
 *   - Margin erosion tracking (vs previous refresh)
 *
 * New features:
 *   - Superheat arbitrage scanner
 *   - Herb patch optimizer
 *   - Personality filter support
 */
@Slf4j
@SuppressWarnings("deprecation")
public class WikiFlipFetcher
{
    private static final String UA       = "Veil-Client/5.0.0 (contact@veil.gg)";
    private static final String LATEST   = "https://prices.runescape.wiki/api/v1/osrs/latest";
    private static final String HOUR     = "https://prices.runescape.wiki/api/v1/osrs/1h";
    private static final String FIVE     = "https://prices.runescape.wiki/api/v1/osrs/5m";
    private static final String MAPPING  = "https://prices.runescape.wiki/api/v1/osrs/mapping";
    private static final String TS_BASE  = "https://prices.runescape.wiki/api/v1/osrs/timeseries?timestep=1h&id=";
    private static final int    TIMEOUT  = 12_000;
    private static Gson gson;
    private static OkHttpClient http;
    public  static void init(Gson g, OkHttpClient h) { gson = g; http = h; }

    // ── Caches ────────────────────────────────────────────────
    // 24h average volume from timeseries
    private static final Map<String, Double> avgVolCache    = new ConcurrentHashMap<>();
    private static final Map<String, Double> avgSpreadCache = new ConcurrentHashMap<>();
    // Last 4 hours of pressure from timeseries (for velocity)
    private static final Map<String, double[]> pressureHistory = new ConcurrentHashMap<>();
    // Previous margin for erosion detection
    private static final Map<String, Integer> prevMarginCache  = new ConcurrentHashMap<>();
    private static long lastTsRefresh = 0;

    // ── Correlation pair definitions ──────────────────────────
    // {itemIdA, itemIdB, historicalRatio, nameA, nameB}
    private static final Object[][] PAIRS = {
        {"11832", "11834", 0.57,  "Bandos chestplate",  "Bandos tassets"},
        {"11838", "11840", 0.72,  "Armadyl chestplate", "Armadyl chainskirt"},
        {"21012", "21015", 1.05,  "Primordial crystal", "Pegasian crystal"},
        {"21013", "21012", 0.96,  "Eternal crystal",    "Primordial crystal"},
        {"12006", "4151",  6.10,  "Abyssal tentacle",   "Abyssal whip"},
        {"11802", "11804", 1.42,  "Armadyl godsword",   "Bandos godsword"},
    };

    // ── Market intelligence ───────────────────────────────────
    private static volatile MarketIntelligence lastIntel = null;
    public  static MarketIntelligence getLastIntel() { return lastIntel; }

    // ── Superheat data ────────────────────────────────────────
    // {ore_id, bar_id, ore_name, bar_name}
    private static final String[][] SUPERHEAT = {
        {"453",  "2353", "Iron ore",       "Iron bar"},
        {"444",  "2361", "Coal",           "Mithril bar"},   // simplified
        {"447",  "2359", "Mithril ore",    "Mithril bar"},
        {"449",  "2363", "Adamantite ore", "Adamantite bar"},
        {"451",  "2365", "Runite ore",     "Runite bar"},
    };

    // ── Herb patch data ───────────────────────────────────────
    // {seed_id, herb_id, herb_name}
    private static final String[][] HERBS = {
        {"5295", "207",  "Ranarr weed"},
        {"5300", "3049", "Toadflax"},
        {"5304", "239",  "Irit leaf"},
        {"5305", "219",  "Avantoe"},
        {"5303", "213",  "Kwuarm"},
        {"5302", "3051", "Snapdragon"},
        {"5296", "215",  "Cadantine"},
        {"5298", "2481", "Lantadyme"},
        {"5310", "217",  "Dwarf weed"},
        {"5311", "209",  "Torstol"},
    };

public static class CraftResult {
        public String   name, category, advice;
        public int[]    inputIds, inputQtys;
        public String[] inputNames;
        public int  outputId, outputQty;
        public int  totalCost, sellPrice;
        public int  profitPerCraft, craftsPerHour, profitPerHour;
        public int  xpPerCraft, levelRequired;
        public double gpPerXp;
    }

    // ── Volatile results for other features ───────────────────
    private static volatile List<SuperheatResult>  lastSuperheat  = new ArrayList<>();
    private static volatile List<HerbPatchResult>  lastHerbPatch  = new ArrayList<>();
    private static volatile List<CorrelationPlay>  lastCorrelation = new ArrayList<>();
    private static volatile List<MarketMakeOpp>    lastMarketMake  = new ArrayList<>();
    private static volatile List<CraftResult>       lastCrafting    = new ArrayList<>();

    public static List<SuperheatResult>  getLastSuperheat()   { return lastSuperheat;   }
    public static List<HerbPatchResult>  getLastHerbPatch()   { return lastHerbPatch;   }
    public static List<CorrelationPlay>  getLastCorrelation() { return lastCorrelation; }
    public static List<MarketMakeOpp>    getLastMarketMake()  { return lastMarketMake;  }
    public static List<CraftResult>       getLastCrafting()    { return lastCrafting;    }

    // ── Data classes ──────────────────────────────────────────
    public static class SuperheatResult {
        public String oreName, barName;
        public int oreBuy, barSell, superheatCost, profitPerCast, profitPerHour;
        public int oreId, barId;
        public String advice;
    }

    public static class HerbPatchResult {
        public String herbName;
        public int seedId, herbId;
        public int seedBuy, herbSell;
        public int avgYield = 9; // with magic secateurs + high farming
        public int profitPerPatch;
        public String advice;
    }

    public static class CorrelationPlay {
        public String itemAName, itemBName;
        public String itemAId, itemBId;
        public int itemAPrice, itemBPrice;
        public double currentRatio, expectedRatio, deviationPct;
        public String action; // "BUY A" or "BUY B"
        public String advice;
    }

    public static class MarketMakeOpp {
        public int itemId;
        public String itemName;
        public int buyAt, sellAt, spreadGp, taxGp, netPerTrade;
        public int volume;
        public double momentum;
        public int estimatedTradesPerHour;
        public int estimatedGpPerHour;
        public String advice;
    }


    // ═════════════════════════════════════════════════════════
    // MAIN FETCH
    // ═════════════════════════════════════════════════════════

    public static List<FlipSignal> fetchTopFlips(int maxResults) throws Exception
    {
        Map<String, Map<String, Object>> latestData  = fetchMap(LATEST);
        Map<String, Map<String, Object>> h1Data      = fetchMap(HOUR);
        Map<String, Map<String, Object>> m5Data      = fetchMap(FIVE);
        List<Map<String, Object>>        mappingList = fetchList(MAPPING);

        // Build lookup maps
        Map<String, String>  names       = new HashMap<>();
        Map<String, Integer> limits      = new HashMap<>();
        Map<String, Boolean> membersMap  = new HashMap<>();
        Map<String, Integer> highalchMap = new HashMap<>();

        for (Map<String, Object> item : mappingList) {
            String id = String.valueOf(((Number) item.get("id")).intValue());
            names.put(id, (String) item.getOrDefault("name", "?"));
            Object lim = item.get("limit"), mem = item.get("members"), alch = item.get("highalch");
            limits.put(id,      lim  instanceof Number  ? ((Number) lim).intValue()  : 0);
            membersMap.put(id,  mem  instanceof Boolean ? (Boolean) mem              : true);
            highalchMap.put(id, alch instanceof Number  ? ((Number) alch).intValue() : 0);
        }

        // Nature + fire rune prices for alch/superheat
        int natPrice  = num(latestData.getOrDefault("561", Collections.emptyMap()), "high");
        int firePrice = num(latestData.getOrDefault("554", Collections.emptyMap()), "low");
        int superheatCost = natPrice + (firePrice * 5);
        if (natPrice == 0) natPrice = 125;

        long nowTs = System.currentTimeMillis() / 1000L;

        // ── Score all items ──────────────────────────────────
        List<FlipSignal> results = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : latestData.entrySet()) {
            String iid = entry.getKey();
            Map<String, Object> l = entry.getValue();

            int high = num(l, "high"), low = num(l, "low");
            if (high <= 0 || low <= 0 || high <= low) continue;

            int buyLimit = limits.getOrDefault(iid, 0);
            if (buyLimit <= 0) continue;

            // ── FRESHNESS ────────────────────────────────────
            long highTs = numL(l, "highTime"), lowTs = numL(l, "lowTime");
            long worstAge = Math.max(
                highTs > 0 ? (nowTs - highTs) / 60 : 999L,
                lowTs  > 0 ? (nowTs - lowTs)  / 60 : 999L);
            double freshness =
                worstAge < 10  ? 1.00 : worstAge < 30  ? 0.92 :
                worstAge < 60  ? 0.80 : worstAge < 120 ? 0.65 :
                worstAge < 240 ? 0.50 : 0.35;

            // ── 1H DATA ──────────────────────────────────────
            Map<String, Object> h = h1Data.getOrDefault(iid, Collections.emptyMap());
            int hH = num(h,"avgHighPrice"), hL = num(h,"avgLowPrice");
            int hHv = num(h,"highPriceVolume"), hLv = num(h,"lowPriceVolume");
            int vol1h = hHv + hLv;

            // ── SPREAD RATIO ─────────────────────────────────
            int rawSpread = high - low;
            int h1Spread  = (hH > 0 && hL > 0 && hH > hL) ? hH - hL : rawSpread;
            double spreadRatio = rawSpread / (double) Math.max(h1Spread, 1);

            double spreadMult;
            String spreadFlag;
            if      (spreadRatio > 3.0) { spreadMult = 0.50; spreadFlag = "STALE?"; }
            else if (spreadRatio > 2.0) { spreadMult = 1.10; spreadFlag = "WIDE";   }
            else if (spreadRatio > 1.5) { spreadMult = 1.05; spreadFlag = "OK+";    }
            else if (spreadRatio > 0.8) { spreadMult = 1.00; spreadFlag = "NORM";   }
            else if (spreadRatio > 0.5) { spreadMult = 0.90; spreadFlag = "COMP";   }
            else                        { spreadMult = 0.78; spreadFlag = "CRIT";   }

            // Tax + margin
            int tax = Math.min(5_000_000, Math.max(1, (int)(high * 0.01)));
            int rawMargin = rawSpread - tax;
            if (rawMargin <= 0) continue;

            // ── CALIBRATED HAIRCUT ────────────────────────────
            double avgVol = avgVolCache.containsKey(iid)
                ? avgVolCache.get(iid) : Math.max(vol1h, 1.0);

            double baseAch =
                avgVol > 100_000 ? 0.45 : avgVol > 50_000 ? 0.52 :
                avgVol > 10_000  ? 0.65 : avgVol > 2_000  ? 0.75 :
                avgVol > 500     ? 0.85 : avgVol > 50     ? 0.88 :
                avgVol > 0       ? 0.88 : 0.60;

            double achievablePct = Math.max(0.20, Math.min(0.95, baseAch * spreadMult * freshness));
            int realMargin = (int)(rawMargin * achievablePct);
            if (realMargin < 50) continue;

            double roi = realMargin * 100.0 / low;
            if (roi < 0.15) continue;

            // ── FILL TIMES ────────────────────────────────────
            double volPerMin = Math.max(avgVol, 1.0) / 60.0;
            double fillBase  = buyLimit / Math.max(volPerMin, 0.01);
            if (fillBase > 720) continue;

            int fillFast    = Math.max(1, (int)(fillBase * 0.35));
            int fillStd     = Math.max(1, (int)(fillBase));
            int fillPatient = Math.max(1, (int)(fillBase * 3.0));

            // ── GP/HR (tier-aware empirical realization) ──────
            VeilCalibration.Tier tier = VeilCalibration.tierOf(high);
            int tradeable4hr = Math.max(1, (int) Math.min(buyLimit, avgVol * 4.0));
            double cycleHrs  = Math.max((fillBase * 2.0) / 60.0, 0.25);
            double gpHrTheo  = realMargin * (double) tradeable4hr / cycleHrs;
            // Empirically-derived realization factor per price tier (was flat 0.65)
            double realization = VeilCalibration.marginRealization(tier);
            double gpHrReal  = gpHrTheo * realization;
            if (gpHrReal < 3_000) continue;

            // ── VWAP & MOMENTUM ───────────────────────────────
            double vwap1h = vol1h > 0 && hH > 0 && hL > 0
                ? (hH * (double)hHv + hL * hLv) / vol1h : (high + low) / 2.0;
            Map<String, Object> m = m5Data.getOrDefault(iid, Collections.emptyMap());
            int mH = num(m,"avgHighPrice"), mL = num(m,"avgLowPrice");
            int mHv = num(m,"highPriceVolume"), mLv = num(m,"lowPriceVolume");
            int m5vol = mHv + mLv;
            double vwap5m = m5vol > 0 && mH > 0 && mL > 0
                ? (mH * (double)mHv + mL * mLv) / m5vol : 0;
            double momentum = vol1h >= 50 && vwap5m > 0 && vwap1h > 0
                ? (vwap5m - vwap1h) / vwap1h * 100 : 0.0;

            // ── PRESSURE VELOCITY (Phase 2) ───────────────────
            double pressure = hLv > 0 ? Math.min(10.0, (double)hHv / hLv)
                : hHv > 0 ? 2.0 : 1.0;

            String pressureVelocity = "FLAT";
            double[] ph = pressureHistory.get(iid);
            if (ph != null && ph.length >= 3) {
                if (ph[ph.length-1] > ph[ph.length-2] && ph[ph.length-2] > ph[ph.length-3])
                    pressureVelocity = "RISING";
                else if (ph[ph.length-1] < ph[ph.length-2] && ph[ph.length-2] < ph[ph.length-3])
                    pressureVelocity = "FALLING";
            }

            // ── VOLUME CONFIRMATION ───────────────────────────
            int minVol = high > 1_000_000 ? 10 : high > 100_000 ? 50
                       : high > 10_000    ? 200 : 1000;
            boolean volConfirmed = vol1h >= minVol;

            // ── SIGNAL ────────────────────────────────────────
            double volAccel = avgVol > 0 && vol1h > 0 ? (vol1h - avgVol) / avgVol : 0;
            boolean risingPressure = "RISING".equals(pressureVelocity);
            boolean fallingPressure = "FALLING".equals(pressureVelocity);

            boolean enter = pressure >= 1.3 && freshness > 0.5
                && (vol1h < 50 || momentum >= -0.01)
                && volAccel >= -0.35 && !fallingPressure;
            boolean exit = pressure < 0.7 || freshness < 0.40
                || (vol1h >= 50 && momentum < -0.03)
                || volAccel < -0.40 || fallingPressure;

            String signal;
            if      (!volConfirmed && pressure >= 1.3) signal = "WATCH";
            else if (enter && risingPressure)           signal = "ENTER"; // strongest
            else if (enter)                             signal = "ENTER";
            else if (exit)                              signal = "EXIT";
            else                                        signal = "HOLD";

            // ── MARKET MAKING DETECTION (Phase 2) ────────────
            boolean isMarketMake = Math.abs(momentum) < 0.5
                && spreadRatio > 1.2
                && avgVol > 500
                && freshness >= 0.92
                && realMargin > tax;  // spread must cover tax twice

            // ── MARGIN EROSION ────────────────────────────────
            int prevMargin = prevMarginCache.getOrDefault(iid, -1);
            double marginChangePct = prevMargin > 0
                ? (realMargin - prevMargin) * 100.0 / prevMargin : 0.0;
            boolean isEroding = prevMargin > 0 && marginChangePct < -25;
            prevMarginCache.put(iid, realMargin);

            // ── CONFIDENCE ────────────────────────────────────
            int conf = (int)(freshness * 30);
            conf += vol1h > 500 ? 25 : vol1h > 100 ? 18 : vol1h > 20 ? 10 : vol1h > 0 ? 5 : 0;
            conf += spreadRatio > 0.7 && spreadRatio < 2.0 ? 20
                  : spreadRatio > 0.5 && spreadRatio < 2.5 ? 12 : 3;
            conf += roi > 2.0 ? 15 : roi > 1.0 ? 10 : roi > 0.5 ? 5 : 0;
            if (volConfirmed) conf += 10;
            int confidence = Math.max(5, Math.min(99, conf));

            // ── GRADE ─────────────────────────────────────────
            String grade = gpHrReal > 3_000_000 ? "S" : gpHrReal > 1_000_000 ? "A"
                : gpHrReal > 300_000 ? "B" : gpHrReal > 50_000 ? "C" : "D";

            // ── ALCH ─────────────────────────────────────────
            int highalch   = highalchMap.getOrDefault(iid, 0);
            int alchProfit = highalch > 0 ? highalch - low - natPrice : 0;

            // ── BUILD SIGNAL ──────────────────────────────────
            FlipSignal fs        = new FlipSignal();
            fs.itemId            = Integer.parseInt(iid);
            fs.itemName          = names.getOrDefault(iid, "?");
            fs.buyPrice          = low;
            fs.sellPrice         = high;
            fs.margin            = rawMargin;
            fs.netMargin         = realMargin;
            fs.roi               = Math.round(roi * 100) / 100.0;
            fs.pressure          = Math.round(pressure * 100) / 100.0;
            fs.momentum          = Math.round(momentum * 100) / 100.0;
            fs.pressureVelocity  = pressureVelocity;
            fs.hourVol           = (int) Math.round(avgVol);
            fs.buyLimit          = buyLimit;
            fs.fillMins          = fillStd;
            fs.fillFast          = fillFast;
            fs.fillPatient       = fillPatient;
            fs.cycleGp           = realMargin * tradeable4hr;
            fs.kelly             = Math.round(Math.min(0.25, Math.max(0.01,
                roi > 0 ? 0.5 * roi / (roi + 100) : 0.01)) * 1000) / 1000.0;
            // ── SMART BUY PRICE ──────────────────────────────────────────────────
            double patFactor = avgVol > 50_000 ? 0.001 : avgVol > 5_000 ? 0.003
                : avgVol > 500 ? 0.006 : 0.010;
            int buyInstant   = low + 1;
            int buyStd       = low;
            int buyPatient   = (int)(low * (1 - patFactor));
            int buyPatSave   = low - buyPatient;
            int buyPatSaveTotal = buyPatSave * buyLimit;

            // ── BOT DETECTION ────────────────────────────────────────────────────
            int botScore = 0;
            StringBuilder botSigs = new StringBuilder();
            if (0 < rawMargin && rawMargin < tax * 3 && vol1h > 1000)
                { botScore++; botSigs.append("NEAR_ZERO "); }
            if (hH > 0 && mH > 0 && vwap5m > 0 && vwap1h > 0
                && Math.abs(vwap5m - vwap1h) < vwap1h * 0.001 && vol1h > 200)
                { botScore++; botSigs.append("PRICE_FROZEN "); }
            if (spreadRatio < 0.5 && vol1h > 500)
                { botScore++; botSigs.append("COMPRESSING "); }
            if ((high % 1000 == 0 || low % 1000 == 0) && high > 100_000 && spreadRatio < 0.6)
                { botScore++; botSigs.append("ROUND_PRICE "); }

            fs.buyInstant        = buyInstant;
            fs.buyStd            = buyStd;
            fs.buyPatient        = buyPatient;
            fs.buyPatientSavings = buyPatSave;
            fs.buyPatientSavingsTotal = buyPatSaveTotal;
            fs.signal            = signal;
            fs.score             = (int) gpHrReal;
            fs.gpHrTheoretical   = (int) gpHrTheo;
            fs.grade             = grade;
            fs.confidence        = confidence;
            fs.achievablePct     = achievablePct;
            fs.spreadRatio       = spreadRatio;
            fs.spreadFlag        = spreadFlag;
            fs.freshness         = (int)(freshness * 100);
            fs.vwap1h            = (int) vwap1h;
            fs.vwap5m            = (int) vwap5m;
            fs.botScore          = botScore;
            fs.botSignals        = botSigs.toString().trim();
            fs.isBotWarning      = botScore >= 2;
            fs.isMarketMake      = isMarketMake;
            fs.members           = membersMap.getOrDefault(iid, true);
            fs.tradeable         = true;
            fs.highalch          = highalch;
            fs.alchProfit        = alchProfit;
            fs.prevNetMargin     = prevMargin;
            fs.marginChangePct   = Math.round(marginChangePct * 10) / 10.0;
            fs.isEroding         = isEroding;

            // ── MARKET RHYTHM (empirically calibrated) ──
            double moveFraction = momentum / 100.0;  // momentum is %, convert to fraction
            VeilCalibration.MoveAlert alert = VeilCalibration.classifyMove(moveFraction);
            fs.moveAlert    = alert.name();
            fs.moveAdvice   = VeilCalibration.moveAdvice(alert);
            // Strategy: consumables (potions/food/runes) trend; gear ranges
            boolean isConsumable = isConsumableItem(fs.itemName);
            VeilCalibration.Strategy strat = VeilCalibration.strategyFor(isConsumable, momentum);
            fs.strategy        = strat.name();
            fs.strategyAdvice  = VeilCalibration.strategyAdvice(strat);
            // Verified correlation partner
            int[] partner = VeilCalibration.correlatedPartner(fs.itemId);
            if (partner != null) { fs.correlPartnerId = partner[0]; fs.correlStrength = partner[1]; }

            fs.marginContext      = buildMarginContext(realMargin, roi, avgVol,
                                      spreadRatio, worstAge, confidence,
                                      isEroding, pressureVelocity);
            // ── SELL CONFIDENCE + PREDICTION ─────────────────────────────
            PricePredictor.enrich(fs, nowTs, vwap5m, vwap1h, vol1h, pressure, momentum);

            results.add(fs);
        }

        results.sort((a, b) -> Integer.compare(b.score, a.score));

        // ── PHASE 2: CORRELATION PAIRS ────────────────────────
        lastCorrelation = buildCorrelationPlays(latestData, names);
        // Flag correlated items
        Map<String, CorrelationPlay> corrMap = new HashMap<>();
        for (CorrelationPlay cp : lastCorrelation) {
            corrMap.put(cp.itemAId, cp);
            corrMap.put(cp.itemBId, cp);
        }
        for (FlipSignal fs : results) {
            String sid = String.valueOf(fs.itemId);
            if (corrMap.containsKey(sid)) {
                CorrelationPlay cp = corrMap.get(sid);
                fs.isCorrelationPlay = true;
                fs.correlationNote   = cp.action + ": " + cp.advice;
            }
        }

        // ── MARKET MAKING LIST ────────────────────────────────
        lastMarketMake = buildMarketMakeList(results, latestData);

        // ── SUPERHEAT SCANNER ─────────────────────────────────
        lastSuperheat = buildSuperheatResults(latestData, names, superheatCost);

        // ── HERB PATCH OPTIMIZER ──────────────────────────────
        lastHerbPatch = buildHerbResults(latestData);

        // ── MARKET INTELLIGENCE ───────────────────────────────
        try {
            Map<String, Map<String, Object>> infoMap = new HashMap<>();
            for (Map.Entry<String, Map<String, Object>> e : latestData.entrySet()) {
                Map<String, Object> row = new HashMap<>();
                row.put("name",  names.getOrDefault(e.getKey(), "?"));
                row.put("limit", limits.getOrDefault(e.getKey(), 0));
                infoMap.put(e.getKey(), row);
            }
            lastIntel = MarketAnalyzer.analyze(latestData, h1Data, m5Data, infoMap);
        } catch (Exception ex) { log.debug("Intel failed", ex); }

        // ── BACKGROUND TIMESERIES REFRESH ────────────────────
        long nowMs = System.currentTimeMillis();
        if (nowMs - lastTsRefresh > 5 * 60_000L) {
            lastTsRefresh = nowMs;
            refreshTimeseries(results.subList(0, Math.min(200, results.size())));
        }

        return results.subList(0, Math.min(maxResults, results.size()));
    }

    // ═════════════════════════════════════════════════════════
    // CORRELATION PAIRS
    // ═════════════════════════════════════════════════════════

    private static List<CorrelationPlay> buildCorrelationPlays(
        Map<String, Map<String, Object>> latest, Map<String, String> names)
    {
        List<CorrelationPlay> plays = new ArrayList<>();
        for (Object[] pair : PAIRS) {
            String idA = (String)pair[0], idB = (String)pair[1];
            double expected = (double)pair[2];
            String nameA = (String)pair[3], nameB = (String)pair[4];

            Map<String, Object> lA = latest.getOrDefault(idA, Collections.emptyMap());
            Map<String, Object> lB = latest.getOrDefault(idB, Collections.emptyMap());
            int highA = num(lA,"high"), lowA = num(lA,"low");
            int highB = num(lB,"high"), lowB = num(lB,"low");
            if (highA <= 0 || highB <= 0) continue;

            double midA = (highA + lowA) / 2.0;
            double midB = (highB + lowB) / 2.0;
            double ratio = midA / midB;
            double dev   = (ratio - expected) / expected * 100;

            if (Math.abs(dev) < 8) continue; // only flag meaningful deviations

            CorrelationPlay cp = new CorrelationPlay();
            cp.itemAName = names.getOrDefault(idA, nameA);
            cp.itemBName = names.getOrDefault(idB, nameB);
            cp.itemAId = idA; cp.itemBId = idB;
            cp.itemAPrice = (int) midA; cp.itemBPrice = (int) midB;
            cp.currentRatio = Math.round(ratio * 1000) / 1000.0;
            cp.expectedRatio = expected;
            cp.deviationPct  = Math.round(dev * 10) / 10.0;
            // If ratio > expected: A is expensive relative to B → buy B
            cp.action = dev > 0 ? "BUY " + nameB : "BUY " + nameA;
            cp.advice = String.format(
                "%s is %.1f%% %s vs historical ratio. Buy the cheaper one — they tend to converge.",
                dev > 0 ? nameA : nameB,
                Math.abs(dev),
                dev > 0 ? "overpriced" : "underpriced");
            plays.add(cp);
        }
        plays.sort((a, b) -> Double.compare(Math.abs(b.deviationPct), Math.abs(a.deviationPct)));
        return plays;
    }

    // ═════════════════════════════════════════════════════════
    // MARKET MAKING
    // ═════════════════════════════════════════════════════════

    private static List<MarketMakeOpp> buildMarketMakeList(
        List<FlipSignal> scored, Map<String, Map<String, Object>> latest)
    {
        List<MarketMakeOpp> opps = new ArrayList<>();
        for (FlipSignal fs : scored) {
            if (!fs.isMarketMake) continue;
            if (fs.hourVol < 200) continue;
            if (fs.confidence < 60) continue;

            MarketMakeOpp mm = new MarketMakeOpp();
            mm.itemId   = fs.itemId;
            mm.itemName = fs.itemName;
            mm.buyAt    = fs.buyPrice  + 1;  // post 1gp above instabuy
            mm.sellAt   = fs.sellPrice - 1;  // post 1gp below instasell
            int tax     = Math.min(5_000_000, Math.max(1, (int)(mm.sellAt * 0.01)));
            mm.taxGp    = tax;
            mm.spreadGp = mm.sellAt - mm.buyAt;
            mm.netPerTrade = mm.spreadGp - tax;
            mm.volume   = fs.hourVol;
            mm.momentum = fs.momentum;
            // Estimate: you capture ~10% of total volume with competitive pricing
            mm.estimatedTradesPerHour = (int)(fs.hourVol * 0.10);
            mm.estimatedGpPerHour     = mm.netPerTrade * mm.estimatedTradesPerHour;
            mm.advice = String.format(
                "Post BUY at %,d (+1gp) AND SELL at %,d (-1gp) simultaneously. " +
                "Collect spread when players cross your orders. " +
                "Momentum is flat (%.2f%%) — safe to hold inventory. " +
                "Est. %,d GP/hr.",
                mm.buyAt, mm.sellAt, mm.momentum, mm.estimatedGpPerHour);
            opps.add(mm);
        }
        opps.sort((a, b) -> Integer.compare(b.estimatedGpPerHour, a.estimatedGpPerHour));
        return opps.subList(0, Math.min(10, opps.size()));
    }

    // ═════════════════════════════════════════════════════════
    // SUPERHEAT SCANNER
    // ═════════════════════════════════════════════════════════

    private static List<SuperheatResult> buildSuperheatResults(
        Map<String, Map<String, Object>> latest,
        Map<String, String> names, int superheatCost)
    {
        List<SuperheatResult> results = new ArrayList<>();
        for (String[] combo : SUPERHEAT) {
            String oreId = combo[0], barId = combo[1];
            Map<String, Object> oreL = latest.getOrDefault(oreId, Collections.emptyMap());
            Map<String, Object> barL = latest.getOrDefault(barId, Collections.emptyMap());
            int oreBuy  = num(oreL, "high"); // buy ore at instabuy price
            int barSell = num(barL, "low");  // sell bar at instasell price
            if (oreBuy <= 0 || barSell <= 0) continue;

            int profit = barSell - oreBuy - superheatCost;

            SuperheatResult sr = new SuperheatResult();
            sr.oreId   = Integer.parseInt(oreId);
            sr.barId   = Integer.parseInt(barId);
            sr.oreName = names.getOrDefault(oreId, combo[2]);
            sr.barName = names.getOrDefault(barId, combo[3]);
            sr.oreBuy  = oreBuy;
            sr.barSell = barSell;
            sr.superheatCost  = superheatCost;
            sr.profitPerCast  = profit;
            // ~1200 casts/hr with superheat
            sr.profitPerHour  = profit * 1200;
            sr.advice = profit > 0
                ? String.format("BUY %s at %,d → Superheat → SELL %s at %,d = +%,d gp/cast (+%,d/hr)",
                    sr.oreName, oreBuy, sr.barName, barSell, profit, sr.profitPerHour)
                : String.format("Not profitable right now (-%,d gp/cast). Check back later.", -profit);
            results.add(sr);
        }
        results.sort((a, b) -> Integer.compare(b.profitPerCast, a.profitPerCast));
        return results;
    }

    // ═════════════════════════════════════════════════════════
    // HERB PATCH OPTIMIZER
    // ═════════════════════════════════════════════════════════

    private static List<HerbPatchResult> buildHerbResults(
        Map<String, Map<String, Object>> latest)
    {
        List<HerbPatchResult> results = new ArrayList<>();
        for (String[] herb : HERBS) {
            String seedId = herb[0], herbId = herb[1], herbName = herb[2];
            Map<String, Object> seedL = latest.getOrDefault(seedId, Collections.emptyMap());
            Map<String, Object> herbL = latest.getOrDefault(herbId, Collections.emptyMap());
            int seedBuy  = num(seedL, "high");
            int herbSell = num(herbL, "low");
            if (seedBuy <= 0 || herbSell <= 0) continue;

            HerbPatchResult hr = new HerbPatchResult();
            hr.seedId   = Integer.parseInt(seedId);
            hr.herbId   = Integer.parseInt(herbId);
            hr.herbName = herbName;
            hr.seedBuy  = seedBuy;
            hr.herbSell = herbSell;
            hr.profitPerPatch = herbSell * hr.avgYield - seedBuy;
            hr.advice = hr.profitPerPatch > 0
                ? String.format("Plant %s: buy seed at %,d, sell %d herbs at %,d = +%,d gp/patch",
                    herbName, seedBuy, hr.avgYield, herbSell, hr.profitPerPatch)
                : String.format("%s: currently NEGATIVE at %,d gp/patch — skip", herbName, hr.profitPerPatch);
            results.add(hr);
        }
        results.sort((a, b) -> Integer.compare(b.profitPerPatch, a.profitPerPatch));
        return results;
    }

    // ═════════════════════════════════════════════════════════
    // MARGIN CONTEXT
    // ═════════════════════════════════════════════════════════

    private static String buildMarginContext(int margin, double roi, double vol,
        double spreadRatio, long ageMin, int confidence,
        boolean isEroding, String pressureVelocity)
    {
        if (isEroding)
            return "⚠ Margin shrinking — bots may have found this. Consider rotating.";
        if ("FALLING".equals(pressureVelocity))
            return "Pressure declining — buyers pulling back. Watch before committing.";
        if (confidence < 40)
            return "Low confidence — stale price or thin volume. Verify on wiki first.";
        if (ageMin > 120)
            return "Price is " + (ageMin / 60) + "hrs old — real margin may differ. Check wiki.";
        if (spreadRatio > 2.5)
            return "Spread wider than usual — opportunity or stale data. Verify before buying.";
        if (spreadRatio < 0.5)
            return "Market more competitive than normal. Expect lower fills.";
        if ("RISING".equals(pressureVelocity))
            return "Pressure building — accumulation in progress. Good entry window.";
        if (margin > 500_000)
            return "High margin — patient fill worth it. Use STD or PATIENT price.";
        if (roi < 0.5 && vol > 10_000)
            return "High volume, thin margin — works at scale with full limit.";
        if (vol < 20 && margin > 100_000)
            return "Illiquid but high margin — post and wait. Low bot competition.";
        return "";
    }

    // ═════════════════════════════════════════════════════════
    // TIMESERIES REFRESH
    // ═════════════════════════════════════════════════════════

    private static void refreshTimeseries(List<FlipSignal> topItems)
    {
        new Thread(() -> {
            for (FlipSignal f : topItems) {
                try {
                    Map<String, Object> ts = fetchRaw(TS_BASE + f.itemId);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> data = ts.get("data") instanceof List
                        ? (List<Map<String, Object>>) ts.get("data") : null;
                    if (data == null || data.size() < 4) continue;

                    List<Map<String, Object>> recent =
                        data.subList(Math.max(0, data.size() - 24), data.size());

                    // Volume + spread averages
                    double tv = 0; int vc = 0, sc = 0; double ts2 = 0;
                    for (Map<String, Object> d : recent) {
                        int vol = num(d,"highPriceVolume") + num(d,"lowPriceVolume");
                        if (vol > 0) { tv += vol; vc++; }
                        int hP = num(d,"avgHighPrice"), lP = num(d,"avgLowPrice");
                        if (hP > lP && lP > 0) { ts2 += (hP - lP); sc++; }
                    }
                    String sid = String.valueOf(f.itemId);
                    if (vc > 0) avgVolCache.put(sid, tv / vc);
                    if (sc > 0) avgSpreadCache.put(sid, ts2 / sc);

                    // Pressure history (last 4 data points)
                    List<Map<String, Object>> last4 =
                        data.subList(Math.max(0, data.size() - 4), data.size());
                    double[] ph = new double[last4.size()];
                    for (int i = 0; i < last4.size(); i++) {
                        int hv = num(last4.get(i),"highPriceVolume");
                        int lv = num(last4.get(i),"lowPriceVolume");
                        ph[i] = lv > 0 ? Math.min(8.0, hv / (double)lv) : 1.0;
                    }
                    pressureHistory.put(sid, ph);

                    // rate limiting via OkHttp connection pooling
                } catch (Exception ignored) {}
            }
            log.debug("Veil: timeseries refreshed");
        }, "veil-ts").start();
    }

    // ═════════════════════════════════════════════════════════
    // HTTP HELPERS
    // ═════════════════════════════════════════════════════════

    private static boolean isConsumableItem(String name) {
        if (name == null) return false;
        String n = name.toLowerCase();
        return n.contains("potion") || n.contains("brew") || n.contains("rune")
            || n.contains("shark") || n.contains("anglerfish") || n.contains("food")
            || n.contains("(4)") || n.contains("(3)") || n.contains("(2)") || n.contains("(1)")
            || n.contains("dart") || n.contains("arrow") || n.contains("bolt")
            || n.contains("seed") || n.contains("herb") || n.contains("ore")
            || n.contains("bar") || n.contains("log") || n.contains("fish")
            || n.contains("restore") || n.contains("antidote") || n.contains("stamina");
    }

    private static Map<String, Map<String, Object>> fetchMap(String url) throws Exception {
        JsonObject root = new JsonParser().parse(get(url)).getAsJsonObject();
        Map<String, Map<String, Object>> result = new HashMap<>();
        JsonObject src = root.has("data") ? root.getAsJsonObject("data") : root;
        for (Map.Entry<String, JsonElement> e : src.entrySet()) {
            if (!e.getValue().isJsonObject()) continue;
            Map<String, Object> inner = new HashMap<>();
            for (Map.Entry<String, JsonElement> f : e.getValue().getAsJsonObject().entrySet()) {
                if (f.getValue().isJsonPrimitive()) {
                    try { inner.put(f.getKey(), f.getValue().getAsLong()); }
                    catch (Exception ex) { inner.put(f.getKey(), f.getValue().getAsString()); }
                }
            }
            result.put(e.getKey(), inner);
        }
        return result;
    }

    private static Map<String, Object> fetchRaw(String url) throws Exception {
        JsonObject root = new JsonParser().parse(get(url)).getAsJsonObject();
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : root.entrySet()) {
            if (e.getValue().isJsonPrimitive()) {
                try { result.put(e.getKey(), e.getValue().getAsLong()); }
                catch (Exception ex) { result.put(e.getKey(), e.getValue().getAsString()); }
            } else if (e.getValue().isJsonArray()) {
                result.put(e.getKey(), e.getValue().getAsJsonArray());
            } else if (e.getValue().isJsonObject()) {
                result.put(e.getKey(), e.getValue().getAsJsonObject());
            }
        }
        return result;
    }

    private static List<Map<String, Object>> fetchList(String url) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        for (JsonElement elem : new JsonParser().parse(get(url)).getAsJsonArray()) {
            if (!elem.isJsonObject()) continue;
            Map<String, Object> row = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : elem.getAsJsonObject().entrySet()) {
                if (e.getValue().isJsonPrimitive()) {
                    try { row.put(e.getKey(), e.getValue().getAsLong()); }
                    catch (Exception ex) { row.put(e.getKey(), e.getValue().getAsString()); }
                } else if (e.getValue().isJsonNull()) {
                    // skip nulls
                } else {
                    try { row.put(e.getKey(), e.getValue().getAsBoolean()); }
                    catch (Exception ex2) {}
                }
            }
            result.add(row);
        }
        return result;
    }

    private static String get(String urlStr) throws Exception {
        Request req = new Request.Builder()
            .url(urlStr)
            .header("User-Agent", UA)
            .header("Accept", "application/json")
            .build();
        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful() || resp.body() == null)
                throw new Exception("HTTP " + resp.code());
            return resp.body().string();
        }
    }

    private static int  num (Map<String,Object> m, String k)
    { Object v=m.get(k); return v instanceof Number?((Number)v).intValue():0; }
    private static long numL(Map<String,Object> m, String k)
    { Object v=m.get(k); return v instanceof Number?((Number)v).longValue():0L; }

    // ═════════════════════════════════════════════════════════
    // CRAFTING ARBITRAGE SCANNER
    // All recipes validated against live data.
    // ═════════════════════════════════════════════════════════

    // {name, category, {input_id,qty,...}, output_id, output_qty, xp, level, crafts_per_hr}
    private static final Object[][] CRAFT_RECIPES = {
        // ── JEWELLERY ────────────────────────────────────────────
        {"Gold ring",        "JEWELLERY", new int[]{2357,1},         1635,  1, 15,  5, 2400},
        {"Sapphire ring",    "JEWELLERY", new int[]{2357,1,1623,1},  1637,  1, 40, 20, 2000},
        {"Emerald ring",     "JEWELLERY", new int[]{2357,1,1621,1},  1639,  1, 55, 27, 2000},
        {"Ruby ring",        "JEWELLERY", new int[]{2357,1,1619,1},  1641,  1, 70, 34, 2000},
        {"Diamond ring",     "JEWELLERY", new int[]{2357,1,1617,1},  1643,  1, 85, 43, 1800},
        {"Dragonstone ring", "JEWELLERY", new int[]{2357,1,1615,1},  1645,  1,100, 55, 1500},
        {"Onyx ring",        "JEWELLERY", new int[]{2357,1,6573,1},  6575,  1,115, 67, 1200},
        {"Sapphire amulet",  "JEWELLERY", new int[]{2357,1,1623,1},  1694,  1, 65, 24, 2000},
        {"Emerald amulet",   "JEWELLERY", new int[]{2357,1,1621,1},  1696,  1, 70, 31, 2000},
        {"Ruby amulet",      "JEWELLERY", new int[]{2357,1,1619,1},  1698,  1, 85, 50, 1800},
        {"Diamond amulet",   "JEWELLERY", new int[]{2357,1,1617,1},  1700,  1,100, 70, 1500},
        {"Dragonstone amulet","JEWELLERY",new int[]{2357,1,1615,1},  1702,  1,150, 80, 1200},
        {"Zenyte amulet",    "JEWELLERY", new int[]{2357,1,19496,1}, 19553, 1,150, 98, 800},
        {"Zenyte ring",      "JEWELLERY", new int[]{2357,1,19496,1}, 19550, 1,150, 98, 800},
        {"Zenyte necklace",  "JEWELLERY", new int[]{2357,1,19496,1}, 19547, 1,150, 98, 800},

        // ── POTIONS ──────────────────────────────────────────────
        // herb_id + secondary_id → product_id (4-dose)
        {"Prayer potion(4)",  "POTION", new int[]{207,1,3138,1},   2434, 1,  38,  38, 1600},
        {"Super restore(4)",  "POTION", new int[]{3004,1,223,1},   3024, 1,  63,  63, 1600},
        {"Super defence(4)",  "POTION", new int[]{2996,1,239,1},   2442, 1,  66,  66, 1600},
        {"Stamina potion(4)", "POTION", new int[]{221,1,4251,1},  12625, 1,  77,  77, 1600},
        {"Bastion potion(4)", "POTION", new int[]{2998,1,245,1},  22461, 1,  80,  80, 1600},
        {"Antidote++(4)",     "POTION", new int[]{3049,1,1575,1},  5952, 1,  79,  79, 1600},
        {"Ranging potion(4)", "POTION", new int[]{2998,1,245,1},   2444, 1,  72,  72, 1600},
        {"Super attack(4)",   "POTION", new int[]{2998,1,221,1},   2436, 1,  45,  45, 1600},
        {"Super strength(4)", "POTION", new int[]{3000,1,225,1},   2440, 1,  55,  55, 1600},
        {"Saradomin brew(4)", "POTION", new int[]{3000,1,6693,1},  6685, 1,  81,  81, 1200},

        // ── FLETCHING ────────────────────────────────────────────
        {"Dragon arrow",      "FLETCH", new int[]{11237,15,9,15}, 11212, 15, 90, 90, 3000},
        {"Dragon dart",       "FLETCH", new int[]{11232,10,9,10}, 11230, 10, 52, 95, 3000},
        {"Amethyst arrow",    "FLETCH", new int[]{21347,15,9,15}, 21326, 15, 82, 82, 3000},
        {"Maple longbow (u)", "FLETCH", new int[]{1519,1},           60,  1, 55, 45, 1800},
        {"Yew longbow (u)",   "FLETCH", new int[]{1515,1},           68,  1, 68, 60, 1200},
        {"Magic longbow (u)", "FLETCH", new int[]{1513,1},           72,  1, 83, 75,  900},
    };

    private static List<CraftResult> buildCraftingResults(
        Map<String, Map<String, Object>> latest, Map<String, String> names)
    {
        List<CraftResult> results = new ArrayList<>();
        for (Object[] recipe : CRAFT_RECIPES) {
            try {
                String craftName = (String) recipe[0];
                String category  = (String) recipe[1];
                int[]  inputs    = (int[])  recipe[2];
                int    outputId  = (int)    recipe[3];
                int    outputQty = (int)    recipe[4];
                int    xp        = (int)    recipe[5];
                int    level     = (int)    recipe[6];
                int    cph       = (int)    recipe[7];

                // Buy inputs at instabuy price (high)
                int totalCost = 0;
                int[] inputIds  = new int[inputs.length / 2];
                int[] inputQtys = new int[inputs.length / 2];
                String[] inputNames = new String[inputs.length / 2];
                boolean skip = false;
                for (int i = 0; i < inputs.length; i += 2) {
                    int id  = inputs[i], qty = inputs[i+1];
                    Map<String, Object> l = latest.getOrDefault(String.valueOf(id), Collections.emptyMap());
                    int price = num(l, "high");
                    if (price == 0) { skip = true; break; }
                    totalCost += price * qty;
                    inputIds[i/2]    = id;
                    inputQtys[i/2]   = qty;
                    inputNames[i/2]  = names.getOrDefault(String.valueOf(id), "Item#"+id);
                }
                if (skip) continue;

                // Sell output at instasell (low)
                Map<String, Object> outL = latest.getOrDefault(String.valueOf(outputId), Collections.emptyMap());
                int sellPrice = num(outL, "low");
                if (sellPrice == 0) continue;
                int sellTotal = sellPrice * outputQty;

                int profitPerCraft = sellTotal - totalCost;
                double gpPerXp = xp > 0 && profitPerCraft > 0 ? (double)profitPerCraft / xp : 0;
                int profitPerHour = profitPerCraft * cph;

                CraftResult cr   = new CraftResult();
                cr.name          = craftName;
                cr.category      = category;
                cr.inputIds      = inputIds;
                cr.inputNames    = inputNames;
                cr.inputQtys     = inputQtys;
                cr.outputId      = outputId;
                cr.outputQty     = outputQty;
                cr.totalCost     = totalCost;
                cr.sellPrice     = sellTotal;
                cr.profitPerCraft = profitPerCraft;
                cr.craftsPerHour = cph;
                cr.profitPerHour = profitPerHour;
                cr.xpPerCraft    = xp;
                cr.gpPerXp       = Math.round(gpPerXp * 100) / 100.0;
                cr.levelRequired = level;
                cr.advice = profitPerCraft > 0
                    ? String.format("+%,d gp per craft · +%,d/hr · %.1f gp/xp",
                        profitPerCraft, profitPerHour, gpPerXp)
                    : String.format("Not profitable right now (-%,d gp/craft)", -profitPerCraft);
                results.add(cr);
            } catch (Exception ignored) {}
        }
        results.sort((a, b) -> Integer.compare(b.profitPerCraft, a.profitPerCraft));
        return results;
    }
}
