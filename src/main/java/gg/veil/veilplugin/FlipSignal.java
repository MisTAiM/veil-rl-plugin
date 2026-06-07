package gg.veil.veilplugin;

import lombok.Data;

/**
 * Lightweight flip signal computed server-side.
 * Sent back to the Veil app alongside live trade data.
 */
@Data
public class FlipSignal
{
    public int    itemId;
    public String itemName;
    public int    buyPrice;     // instabuy (low)
    public int    sellPrice;    // instasell (high)
    public int    margin;       // raw
    public int    netMargin;    // post-tax
    public double roi;          // post-tax %
    public double pressure;     // buy vol / sell vol ratio
    public double momentum;     // 5m VWAP vs 1h VWAP delta %
    public int    hourVol;      // total hourly volume
    public int    buyLimit;     // GE buy limit per 4hr
    public int    fillMins;     // estimated minutes to fill
    public int    cycleGp;      // 4hr max GP (post-tax)
    public double kelly;        // Kelly fraction (0-0.25)
    public String signal;       // ENTER / HOLD / EXIT
    public int    score;        // composite score
    public String grade;
    public int    vwap1h;
    public int    vwap5m;
    public boolean members;     // true = members only item
    public boolean tradeable;   // true = can be traded on GE
    public int    highalch;     // high alch value
    public int    alchProfit;   // profit from high alching instead of selling on GE        // S/A/B/C/D
}
