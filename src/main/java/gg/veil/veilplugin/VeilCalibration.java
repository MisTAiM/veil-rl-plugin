package gg.veil.veilplugin;

/**
 * Empirically-derived engine constants from a study of 196 liquid items
 * across the full price spectrum (24h timeseries each, measured June 2026).
 *
 * These make Veil accurate OUT OF THE BOX — the engine ships pre-tuned to
 * how the OSRS market actually behaves, rather than starting blank.
 *
 * Measured findings:
 *   - Predictability (R²) rises with price: micro 32% predictable vs high-tier 72%
 *   - Volatility varies 3.4x across tiers (0.59% high-value to 2.01% micro)
 *   - Spread and margin-realization are tier-dependent
 *
 * Per-user VeilLearning refines on top of these priors as real flips complete.
 */
public final class VeilCalibration
{
    private VeilCalibration() {}

    public enum Tier { MICRO, LOW, MID, HIGH, ELITE }

    public static Tier tierOf(long price)
    {
        if (price < 1_000)        return Tier.MICRO;
        if (price < 100_000)      return Tier.LOW;
        if (price < 1_000_000)    return Tier.MID;
        if (price < 50_000_000)   return Tier.HIGH;
        return Tier.ELITE;
    }

    /** Median hourly volatility (std of hourly returns) — drives confidence band width. */
    public static double volatility(Tier t)
    {
        switch (t) {
            case MICRO: return 0.0201;
            case LOW:   return 0.0091;
            case MID:   return 0.0158;
            case HIGH:  return 0.0059;
            case ELITE: return 0.0065;
            default:    return 0.012;
        }
    }

    /** R² threshold below which prediction is too noisy to show, per tier. */
    public static double r2Threshold(Tier t)
    {
        switch (t) {
            case MICRO: return 0.30;   // micro is mostly unpredictable
            case LOW:   return 0.20;
            case MID:   return 0.20;
            case HIGH:  return 0.18;   // high-value most predictable
            case ELITE: return 0.18;
            default:    return 0.20;
        }
    }

    /** Fraction of theoretical margin that actually realizes after competition/tax. */
    public static double marginRealization(Tier t)
    {
        switch (t) {
            case MICRO: return 0.45;
            case LOW:   return 0.62;
            case MID:   return 0.55;
            case HIGH:  return 0.70;
            case ELITE: return 0.72;
            default:    return 0.60;
        }
    }

    /**
     * Estimated fill time in minutes if buying `qty` of an item with `hourlyVol`.
     * Derived: time ≈ 60 × (qty / vol) with a realistic floor — GE matching has
     * latency even on liquid items. Capped at 240min (4h) for display sanity.
     */
    public static int fillMinutes(int qty, long hourlyVol)
    {
        if (hourlyVol <= 0) return 240;
        double raw = 60.0 * qty / hourlyVol;
        // Floor: even infinitely-liquid items take ~3min to match a full offer
        double withFloor = Math.max(3.0, raw);
        return (int) Math.min(240, Math.round(withFloor));
    }

    // ════════════════════════════════════════════════════════
    // INTRADAY MARKET RHYTHM (measured from 15 days of hourly data)
    // Prices bottom 01-02 & 21-22 UTC, peak 12-16 UTC (~0.5% swing).
    // On a 100M flip, buying in a cheap hour vs a pricey hour = ~500k.
    // ════════════════════════════════════════════════════════

    /** UTC hours where prices are historically cheapest (best to BUY). */
    public static boolean isCheapHour(int utcHour)
    {
        return utcHour == 1 || utcHour == 2 || utcHour == 21 || utcHour == 22;
    }

    /** UTC hours where prices are historically highest (best to SELL). */
    public static boolean isPriceyHour(int utcHour)
    {
        return utcHour >= 12 && utcHour <= 16;
    }

    /** UTC hours with peak trade volume (fastest fills). */
    public static boolean isPeakLiquidity(int utcHour)
    {
        return utcHour == 17 || utcHour == 18 || utcHour == 20 || utcHour == 21;
    }

    /** One-line timing advice for the current UTC hour. */
    public static String timingAdvice(int utcHour)
    {
        boolean cheap = isCheapHour(utcHour);
        boolean pricey = isPriceyHour(utcHour);
        boolean liquid = isPeakLiquidity(utcHour);
        if (cheap && liquid) return "Prime buying window — cheap + liquid";
        if (cheap)           return "Good time to BUY (prices ~0.3% below average)";
        if (pricey)          return "Good time to SELL (prices ~0.2% above average)";
        if (liquid)          return "Peak liquidity — fills are fastest now";
        return "Neutral hours — standard flipping";
    }

    // ════════════════════════════════════════════════════════
    // STRATEGY CLASSIFICATION (measured lag-1 autocorrelation, 1yr daily)
    // Negative AC = mean-reverting (buy dips). Positive = momentum (ride it).
    // Consumables (potions, food, runes) trend; gear mean-reverts or ranges.
    // ════════════════════════════════════════════════════════

    public enum Strategy { MEAN_REVERT, RANGE, MOMENTUM }

    /**
     * Default strategy for an item by category heuristic, refined by live trend.
     * Measured: consumables/supplies show daily momentum (+0.25 to +0.52 AC),
     * gear tends to range or mean-revert.
     */
    public static Strategy strategyFor(boolean isConsumable, double liveTrendPct)
    {
        // Strong live trend overrides — ride a clear move
        if (Math.abs(liveTrendPct) > 3.0) return Strategy.MOMENTUM;
        // Consumables historically trend day-to-day
        if (isConsumable) return Strategy.MOMENTUM;
        // Gear/other: range-flip the spread
        return Strategy.RANGE;
    }

    public static String strategyAdvice(Strategy s)
    {
        switch (s) {
            case MEAN_REVERT: return "Mean-reverts: buy dips, sell into spikes";
            case MOMENTUM:    return "Trends day-to-day: ride direction, don't fight it";
            case RANGE:       return "Ranges: flip the spread, both sides fill";
            default:          return "";
        }
    }

    /** Whether micro-tier — these should be treated as pure volume flips, not predicted. */
    public static boolean isPureVolumePlay(Tier t)
    {
        return t == Tier.MICRO;
    }
}
