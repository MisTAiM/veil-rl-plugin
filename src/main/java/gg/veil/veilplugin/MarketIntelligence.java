package gg.veil.veilplugin;

import lombok.Data;
import java.util.*;

/**
 * Veil Market Intelligence — beyond basic flipping.
 *
 * Contains:
 * - Category heat map (which market sectors are active)
 * - Supply shock detection (whale accumulation signals)
 * - Price spike detector (>5% moves in 30min)
 * - Time-of-day strategy advisor
 * - Slot optimizer (8-slot portfolio allocation)
 * - Thin market gems (bot-free high-margin items)
 * - Flip rotation signals (when to cancel slow fills)
 */
@Data
public class MarketIntelligence
{
    // Category heat map
    public List<CategoryHeat> categoryHeat = new ArrayList<>();

    // Supply shocks — someone is accumulating
    public List<SupplyShock> supplyShocks = new ArrayList<>();

    // Thin market gems — limit ≤ 10, high margin, bot-free
    public List<ThinMarketGem> thinMarketGems = new ArrayList<>();

    // Price spikes — >5% in 30min
    public List<PriceSpike> priceSpikes = new ArrayList<>();

    // Time-of-day context
    public TimeOfDayContext timeContext;

    // Session plan — what to do right now
    public String sessionPlan;

    // Last updated
    public long timestamp;

    // ── Inner classes ─────────────────────────────────────────

    @Data
    public static class CategoryHeat
    {
        public String category;
        public double avgPressure;   // average buy/sell ratio across items
        public double avgMomentum;   // average 5m momentum %
        public boolean isHot;        // pressure > 1.3 AND momentum > 0.2
        public String hotItems;      // comma-separated top 3 items in category
        public String advice;        // plain English action
    }

    @Data
    public static class SupplyShock
    {
        public int    itemId;
        public String itemName;
        public double pressure;     // > 5.0x = extreme accumulation
        public double momentum;     // % price change
        public int    buyPrice;
        public int    netMargin;
        public int    hourVol;
        public String alert;        // plain English
    }

    @Data
    public static class ThinMarketGem
    {
        public int    itemId;
        public String itemName;
        public int    buyLimit;      // ≤ 10
        public int    buyPrice;
        public int    sellPrice;
        public int    netMargin;
        public double roi;
        public int    hourVol;
        public String botRisk;       // "None — too illiquid for bots"
        public String strategy;      // plain English how to trade it
    }

    @Data
    public static class PriceSpike
    {
        public int    itemId;
        public String itemName;
        public double changePct;    // % change
        public int    currentPrice;
        public String direction;    // "UP" or "DOWN"
        public String possibleCause; // "Update? Streamer? Rare drop?"
    }

    @Data
    public static class TimeOfDayContext
    {
        public int    utcHour;
        public String session;       // "Peak", "Off-Peak", "Morning", "Transition"
        public String bestCategories; // what to flip now
        public String avoidCategories; // what to avoid now
        public String advice;
        public int    minutesUntilPeak; // if not peak
    }
}
