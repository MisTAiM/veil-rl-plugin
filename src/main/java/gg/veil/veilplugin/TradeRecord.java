package gg.veil.veilplugin;

import lombok.Data;

/**
 * One completed or in-progress GE trade.
 * Serialised to JSON and served via the sync server.
 */
@Data
public class TradeRecord
{
    // Core identity
    public int    itemId;
    public String itemName;
    public boolean isBuy;      // true = buy offer, false = sell offer

    // Offer state
    public int quantityOffered;
    public int quantityTraded; // how many have filled so far
    public int pricePerUnit;   // our offer price
    public int avgFillPrice;   // actual price fills happened at
    public boolean complete;   // full fill or cancelled

    // Financials (populated when complete = true)
    public int totalSpent;     // buy: gp out; sell: items * offer price
    public int totalReceived;  // buy: items received; sell: gp in
    public int profitGp;       // net GP gain (negative = loss)
    public int taxGp;          // GE tax paid (sell offers only)

    // Timing
    public long openedAt;      // epoch ms
    public long closedAt;      // epoch ms, 0 if still open

    // Slot (0-7)
    public int slot;

    // Session
    public String rsn;
}
