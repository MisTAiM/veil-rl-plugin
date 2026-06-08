package gg.veil.veilplugin;

import lombok.Data;

@Data
public class FlipSignal
{
    // ── Core price data ───────────────────────────────────────
    public int    itemId;
    public String itemName;
    public int    buyPrice;          // instabuy (low)
    public int    sellPrice;         // instasell (high)
    public int    margin;            // raw spread
    public int    netMargin;         // post-tax, post-haircut
    public double roi;               // return on investment %
    public int    highalch;          // high alch value
    public int    alchProfit;        // alch profit vs GE selling

    // ── Volume & liquidity ────────────────────────────────────
    public int    hourVol;           // 24h avg volume/hr
    public int    buyLimit;          // GE buy limit per 4hr
    public int    fillMins;          // fill time at instabuy-1 (standard)
    public int    buyInstant;         // buy at instabuy+1 (fills fastest)
    public int    buyStd;             // buy at instabuy (standard)
    public int    buyPatient;         // buy below instabuy (saves GP)
    public int    buyPatientSavings;  // GP saved per item vs standard
    public int    buyPatientSavingsTotal; // GP saved per full limit cycle
    public int    fillFast;          // fill time AT instabuy (fastest)
    public int    fillPatient;       // fill time at patient price (slowest)
    public int    cycleGp;           // expected GP per 4hr cycle
    public double kelly;             // Kelly position size fraction

    // ── Scoring ───────────────────────────────────────────────
    public int    score;             // GP/hr realistic (main sort key)
    public int    gpHrTheoretical;   // GP/hr with perfect execution
    public String grade;             // S/A/B/C/D
    public int    confidence;        // 0-100 data quality score
    public double achievablePct;     // actual achievable margin fraction

    // ── Market signals ────────────────────────────────────────
    public String signal;            // ENTER / HOLD / EXIT / WATCH
    public double pressure;          // buy vol / sell vol
    public double momentum;          // 5m VWAP vs 1h VWAP delta %
    public int    vwap1h;
    public int    vwap5m;
    public String pressureVelocity;  // RISING / FALLING / FLAT
    public double spreadRatio;       // current spread / 1h avg spread
    public String spreadFlag;        // NORM/WIDE/COMP/CRIT/STALE?/OK+
    public int    freshness;         // 0-100 price freshness
    // ── Bot competition detection ────────────────────────
    public int    botScore;         // 0-4 bot competition signals
    public String botSignals;       // which signals triggered
    public boolean isBotWarning;    // true if score >= 2

    public boolean isMarketMake;     // true = eligible for market-making strategy
    public boolean isCorrelationPlay;// true = pairs trade opportunity
    public String correlationNote;   // explanation of pairs signal

    // ── Metadata ──────────────────────────────────────────────
    public boolean members;
    public boolean tradeable;
    public String  marginContext;    // plain English why margin is what it is

    // ── Sell confidence ────────────────────────────────────
    public int    sellConfidence;    // 0-100 how likely sell price fills
    public String sellRisk;         // "LOW RISK" / "MEDIUM RISK" / "HIGH RISK"
    public String sellRiskReason;   // plain English why it's risky
    public int    safeSellPrice;    // if high risk: safer lower sell price

    // ── Price prediction ─────────────────────────────────────
    public int    pred1hrLow;       // price range low in 1 hour
    public int    pred1hrHigh;      // price range high in 1 hour
    public int    pred1hrCenter;    // center prediction in 1 hour
    public int    pred2hrLow;
    public int    pred2hrHigh;
    public int    pred2hrCenter;
    public int    pred4hrLow;
    public int    pred4hrHigh;
    public int    pred4hrCenter;
    public double predR2;           // prediction reliability (0-1)
    public String predTrend;        // UP / DOWN / FLAT
    public double predVolatilityPct;// ± uncertainty %
    public String bestTimeToSell;   // e.g. "18:00-22:00 UTC"
    public double timeOfDayBias;    // expected % return this UTC hour

    // ── Margin erosion tracking ───────────────────────────────────
    public int    prevNetMargin;     // margin from previous refresh
    public double marginChangePct;   // % change vs previous (-30% = eroding)
    public boolean isEroding;        // true if margin shrinking fast
}
