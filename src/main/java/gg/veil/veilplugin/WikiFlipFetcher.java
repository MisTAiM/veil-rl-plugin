package gg.veil.veilplugin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.Type;
import java.net.*;
import java.util.*;

/**
 * Fetches top flip signals from the OSRS Wiki prices API.
 * Runs on the executor thread, never on the game thread.
 * Same 8-signal scoring engine as the Flutter app — results synced to phone
 * AND used for the in-game GE search overlay.
 */
@Slf4j
public class WikiFlipFetcher
{
    private static final String UA      = "Veil-Client/2.0.0 (contact@veil.gg)";
    private static final String LATEST  = "https://prices.runescape.wiki/api/v1/osrs/latest";
    private static final String HOUR    = "https://prices.runescape.wiki/api/v1/osrs/1h";
    private static final String FIVE    = "https://prices.runescape.wiki/api/v1/osrs/5m";
    private static final String MAPPING = "https://prices.runescape.wiki/api/v1/osrs/mapping";
    private static final int    TIMEOUT = 10_000;

    private static final Gson gson = new Gson();

    // Single-shot fetch — called every 60s by executor
    public static List<FlipSignal> fetchTopFlips(int limit) throws Exception
    {
        Map<String, Object> latest  = fetchMap(LATEST + "?_=" + System.currentTimeMillis());
        Map<String, Object> h1data  = fetchMap(HOUR);
        Map<String, Object> m5data  = fetchMap(FIVE);
        List<Map<String, Object>> mapping = fetchList(MAPPING);

        // Build name + limit lookup
        Map<String, String>  names   = new HashMap<>();
        Map<String, Integer> limits  = new HashMap<>();
        Map<String, Integer> highalch = new HashMap<>();
        for (Map<String, Object> item : mapping)
        {
            String id = String.valueOf(((Number) item.get("id")).intValue());
            names.put(id,     (String) item.getOrDefault("name", "?"));
            Object lim = item.get("limit");
            limits.put(id,    lim != null ? ((Number) lim).intValue() : 0);
            Object ha = item.get("highalch");
            highalch.put(id,  ha  != null ? ((Number) ha).intValue()  : 0);
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
            String iid = entry.getKey();
            Map<String, Object> ldata = entry.getValue();

            int high = num(ldata, "high");
            int low  = num(ldata, "low");
            if (high <= 0 || low <= 0 || high <= low) continue;

            int margin = high - low;
            int tax    = Math.min(5_000_000, Math.max(1, (int)(high * 0.01)));
            int net    = margin - tax;
            if (net < 300) continue;

            double roi = (double) net / low;
            if (roi < 0.003) continue;

            Map<String, Object> h  = h1Items  != null ? h1Items.getOrDefault(iid, Map.of()) : Map.of();
            Map<String, Object> m5 = m5Items  != null ? m5Items.getOrDefault(iid, Map.of()) : Map.of();

            int hH  = num(h,  "avgHighPrice");
            int hL  = num(h,  "avgLowPrice");
            int hHv = num(h,  "highPriceVolume");
            int hLv = num(h,  "lowPriceVolume");
            int hourVol = hHv + hLv;
            if (hourVol < 300) continue;

            int buyLimitVal = limits.getOrDefault(iid, 0);
            if (buyLimitVal <= 0) continue;

            double vwap1h = (hH > 0 && hL > 0 && hourVol > 0)
                ? (hH * (double)hHv + hL * hLv) / hourVol
                : (high + low) / 2.0;
            double pressure = hLv > 0
                ? Math.min(10.0, (double)hHv / hLv)
                : (hHv > 0 ? 2.0 : 1.0);

            int mH  = num(m5, "avgHighPrice");
            int mL  = num(m5, "avgLowPrice");
            int mHv = num(m5, "highPriceVolume");
            int mLv = num(m5, "lowPriceVolume");
            int m5vol = mHv + mLv;
            double vwap5m = (mH > 0 && mL > 0 && m5vol > 0)
                ? (mH * (double)mHv + mL * mLv) / m5vol : 0;

            double momentum = (vwap5m > 0 && vwap1h > 0)
                ? (vwap5m - vwap1h) / vwap1h : 0.0;

            double pFill    = Math.min(0.90, (double)hourVol / (4 * buyLimitVal));
            double kelly    = Math.max(0.01, Math.min(0.25, pFill * roi / (roi + 1)));
            double fillMins = (double)buyLimitVal / Math.max(1, hourVol / 60.0);
            if (fillMins > 120) continue;

            int tradeable4hr = Math.min(buyLimitVal, hourVol * 4);
            int cycleGp      = net * Math.max(1, tradeable4hr);

            double stability = 1.0;
            if (hH > 0 && hL > 0) {
                int spread1h = hH - hL;
                if (spread1h > 0)
                    stability = Math.min((double)margin, spread1h) / Math.max((double)margin, spread1h);
            }
            double fillPenalty = 1.0 / (1 + fillMins / 60.0);
            double sharpe = roi * stability * fillPenalty * Math.sqrt(hourVol / 500.0);
            double score  = sharpe * Math.log(1 + cycleGp / 10_000.0) * kelly;

            String signal;
            if (pressure >= 1.2 && momentum >= 0.005)  signal = "ENTER";
            else if (pressure < 0.7 || momentum < -0.03) signal = "EXIT";
            else signal = "HOLD";

            String grade;
            int sc = (int)(score * 1000);
            if (sc > 300_000)   grade = "S";
            else if (sc > 100_000) grade = "A";
            else if (sc > 30_000)  grade = "B";
            else if (sc > 5_000)   grade = "C";
            else grade = "D";

            FlipSignal fs     = new FlipSignal();
            fs.itemId         = Integer.parseInt(iid);
            fs.itemName       = names.getOrDefault(iid, "?");
            fs.buyPrice       = low;
            fs.sellPrice      = high;
            fs.margin         = margin;
            fs.netMargin      = net;
            fs.roi            = roi * 100;
            fs.pressure       = pressure;
            fs.momentum       = momentum * 100;
            fs.hourVol        = hourVol;
            fs.buyLimit       = buyLimitVal;
            fs.fillMins       = (int) fillMins;
            fs.cycleGp        = cycleGp;
            fs.kelly          = kelly;
            fs.signal         = signal;
            fs.score          = sc;
            fs.grade          = grade;
            results.add(fs);
        }

        results.sort((a, b) -> Integer.compare(b.score, a.score));
        return results.subList(0, Math.min(limit, results.size()));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> fetchMap(String url) throws Exception
    {
        String body = get(url);
        Type t = new TypeToken<Map<String, Object>>(){}.getType();
        return gson.fromJson(body, t);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> fetchList(String url) throws Exception
    {
        String body = get(url);
        Type t = new TypeToken<List<Map<String, Object>>>(){}.getType();
        return gson.fromJson(body, t);
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
        finally
        {
            conn.disconnect();
        }
    }

    private static int num(Map<String, Object> m, String key)
    {
        Object v = m.get(key);
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }
}
