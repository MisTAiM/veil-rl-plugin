package gg.veil.veilplugin;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Per-user self-calibration. Ships on top of VeilCalibration's empirical priors.
 *
 * Every time a flip completes, we record predicted-vs-actual:
 *   - predicted fill minutes vs actual fill minutes
 *   - predicted net profit vs actual net profit
 *
 * The engine then learns a per-tier correction factor unique to THIS player's
 * worlds, timing, and habits. Starts at 1.0 (trust the empirical prior) and
 * converges toward the player's reality with an exponential moving average.
 *
 * Persists to ~/.runelite/veil/learning.json so it survives restarts and the
 * tool keeps getting sharper the more it's used.
 */
@Slf4j
@SuppressWarnings("deprecation")
public class VeilLearning
{
    private static final double EMA_ALPHA = 0.15;  // weight of each new observation
    private static final int    MIN_SAMPLES = 5;   // before we trust a learned factor

    // Per-tier learned correction factors (fill time and profit)
    private final Map<String, double[]> fillFactor   = new ConcurrentHashMap<>(); // [factor, sampleCount]
    private final Map<String, double[]> profitFactor = new ConcurrentHashMap<>();

    private final Gson gson;
    private File file;

    public VeilLearning(Gson gson)
    {
        this.gson = gson;
    }

    public void load()
    {
        try {
            File dir = new File(RuneLite.RUNELITE_DIR, "veil");
            if (!dir.exists()) dir.mkdirs();
            file = new File(dir, "learning.json");
            if (!file.exists()) return;
            try (FileReader fr = new FileReader(file)) {
                JsonObject root = new JsonParser().parse(fr).getAsJsonObject();
                loadMap(root, "fill", fillFactor);
                loadMap(root, "profit", profitFactor);
            }
            log.debug("Veil learning loaded: {} fill, {} profit factors", fillFactor.size(), profitFactor.size());
        } catch (Exception e) {
            log.debug("Could not load learning data", e);
        }
    }

    private void loadMap(JsonObject root, String key, Map<String, double[]> target)
    {
        if (!root.has(key)) return;
        JsonObject obj = root.getAsJsonObject(key);
        for (String tier : obj.keySet()) {
            JsonArray a = obj.getAsJsonArray(tier);
            target.put(tier, new double[]{ a.get(0).getAsDouble(), a.get(1).getAsDouble() });
        }
    }

    private void save()
    {
        if (file == null) return;
        try (FileWriter fw = new FileWriter(file)) {
            JsonObject root = new JsonObject();
            root.add("fill", mapToJson(fillFactor));
            root.add("profit", mapToJson(profitFactor));
            gson.toJson(root, fw);
        } catch (Exception e) {
            log.debug("Could not save learning data", e);
        }
    }

    private JsonObject mapToJson(Map<String, double[]> m)
    {
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, double[]> e : m.entrySet()) {
            JsonArray a = new JsonArray();
            a.add(e.getValue()[0]); a.add(e.getValue()[1]);
            obj.add(e.getKey(), a);
        }
        return obj;
    }

    /** Record a completed flip's predicted vs actual outcome. */
    public void recordFill(VeilCalibration.Tier tier, int predictedMin, int actualMin)
    {
        if (predictedMin <= 0 || actualMin <= 0) return;
        double ratio = (double) actualMin / predictedMin;
        updateEma(fillFactor, tier.name(), ratio);
        save();
    }

    public void recordProfit(VeilCalibration.Tier tier, long predictedNet, long actualNet)
    {
        if (predictedNet == 0) return;
        double ratio = (double) actualNet / predictedNet;
        // clamp outliers (a single weird flip shouldn't swing the model wildly)
        ratio = Math.max(0.2, Math.min(3.0, ratio));
        updateEma(profitFactor, tier.name(), ratio);
        save();
    }

    private void updateEma(Map<String, double[]> m, String tier, double obs)
    {
        double[] cur = m.computeIfAbsent(tier, k -> new double[]{1.0, 0});
        double factor = cur[1] == 0 ? obs : (1 - EMA_ALPHA) * cur[0] + EMA_ALPHA * obs;
        m.put(tier, new double[]{ factor, cur[1] + 1 });
    }

    /** Learned fill-time correction for this tier (1.0 until enough samples). */
    public double fillCorrection(VeilCalibration.Tier tier)
    {
        double[] f = fillFactor.get(tier.name());
        return (f != null && f[1] >= MIN_SAMPLES) ? f[0] : 1.0;
    }

    public double profitCorrection(VeilCalibration.Tier tier)
    {
        double[] f = profitFactor.get(tier.name());
        return (f != null && f[1] >= MIN_SAMPLES) ? f[0] : 1.0;
    }

    /** How many completed flips have informed this tier (for UI display). */
    public int sampleCount(VeilCalibration.Tier tier)
    {
        double[] f = fillFactor.get(tier.name());
        return f != null ? (int) f[1] : 0;
    }

    /** Human-readable calibration status for the UI. */
    public String statusFor(VeilCalibration.Tier tier)
    {
        int n = sampleCount(tier);
        if (n == 0)            return "using market defaults";
        if (n < MIN_SAMPLES)   return "learning (" + n + "/" + MIN_SAMPLES + ")";
        double fc = fillCorrection(tier);
        if (fc > 1.15)         return "your fills run " + Math.round((fc-1)*100) + "% slower";
        if (fc < 0.85)         return "your fills run " + Math.round((1-fc)*100) + "% faster";
        return "calibrated (" + n + " flips)";
    }
}
