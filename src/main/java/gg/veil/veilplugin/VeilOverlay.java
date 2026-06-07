package gg.veil.veilplugin;

import net.runelite.api.*;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.*;
import javax.inject.Inject;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Veil 6.0 In-Game Overlay
 * Every section respects its config toggle.
 * Credits: MorpheusXP | Discord: Morpheus7239
 */
public class VeilOverlay extends OverlayPanel
{
    private static final Color GOLD   = new Color(0xC9, 0xA8, 0x4C);
    private static final Color GREEN  = new Color(0x3D, 0xAA, 0x6E);
    private static final Color RED    = new Color(0xC2, 0x54, 0x54);
    private static final Color AMBER  = new Color(0xBA, 0x75, 0x17);
    private static final Color PURPLE = new Color(0xAA, 0x00, 0xDD);
    private static final Color MUTED  = new Color(0x6B, 0x72, 0x80);
    private static final Color WHITE  = new Color(0xE8, 0xE6, 0xDE);
    private static final Color BG     = new Color(0x0C, 0x0E, 0x13, 215);

    private final VeilPlugin  plugin;
    private final VeilConfig  config;

    @Inject
    public VeilOverlay(Client client, VeilPlugin plugin, VeilConfig config)
    {
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.LOW);
        panelComponent.setPreferredSize(new Dimension(230, 0));
        panelComponent.setBorder(new Rectangle(6, 6, 6, 6));
        panelComponent.setBackgroundColor(BG);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showOverlay()) return null;

        panelComponent.getChildren().clear();

        // ── Header ────────────────────────────────────────────
        panelComponent.getChildren().add(TitleComponent.builder()
            .text("VEIL  by MorpheusXP")
            .color(GOLD)
            .build());

        // ── Coin stack ────────────────────────────────────────
        if (config.showCoins())
        {
            long coins = plugin.getCoinStack();
            if (coins > 0)
                row("Coins", fmtGp(coins), GOLD);
        }

        // ── GP Today ─────────────────────────────────────────
        if (config.showGpToday())
        {
            VeilPlugin.SessionStats stats = plugin.getSessionStats();
            int lootGp = plugin.getSessionLootGp();
            long total = stats.sessionProfitGp + lootGp;
            row("GP today", fmtSigned(total), total >= 0 ? GREEN : RED);
            if (stats.sessionProfitGp != 0)
                row("  GE",  fmtSigned(stats.sessionProfitGp), stats.sessionProfitGp >= 0 ? GREEN : RED);
            if (lootGp > 0)
                row("  Loot", "+" + fmtGp(lootGp), GREEN);
        }

        // ── Prayer + Spec ─────────────────────────────────────
        if (config.showPrayerSpec())
        {
            int prayer = plugin.getPrayerPoints();
            int spec   = plugin.getSpecPercent();
            if (prayer > 0 || spec > 0) gap();
            if (prayer > 0) row("Prayer", prayer + " pts", prayer < 20 ? RED : prayer < 50 ? AMBER : GREEN);
            if (spec > 0)   row("Special", spec + "%", spec >= 100 ? GREEN : spec >= 50 ? AMBER : MUTED);
        }

        // ── XP ───────────────────────────────────────────────
        if (config.showXp())
        {
            Map<Skill, Integer> xp = plugin.getXpGained();
            if (!xp.isEmpty()) {
                long ms = System.currentTimeMillis() - plugin.getSessionStartMs();
                double h = ms / 3_600_000.0;
                int total = xp.values().stream().mapToInt(Integer::intValue).sum();
                if (total > 0) {
                    gap();
                    Skill top = xp.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey).orElse(null);
                    if (top != null)
                        row(top.getName() + " XP", "+" + fmtXp(xp.get(top)), GOLD);
                    if (h > 0.02)
                        row("XP/hr", fmtXp((int)(total / h)), WHITE);
                }
            }
        }

        // ── Weapon charges ────────────────────────────────────
        if (config.showCharges())
        {
            WeaponCharges wc = plugin.getWeaponCharges();
            if (wc.hasAny()) {
                gap();
                if (wc.blowpipeCharges  > 0) row("Blowpipe",  wc.blowpipeCharges  + " darts",   wc.blowpipeLow  ? RED : GREEN);
                if (wc.tridentCharges   > 0) row("Trident",   wc.tridentCharges   + " charges", wc.tridentLow   ? RED : GREEN);
                if (wc.sangStaffCharges > 0) row("Sang staff",wc.sangStaffCharges + " charges", wc.sangStaffLow ? RED : GREEN);
                if (wc.tumekensCharges  > 0) row("Tumeken's", wc.tumekensCharges  + " charges", wc.tumekensLow  ? RED : GREEN);
            }
        }

        // ── Slayer ────────────────────────────────────────────
        if (config.showSlayer())
        {
            SlayerState slayer = plugin.getSlayerState();
            if (slayer.hasTask()) {
                gap();
                String name = slayer.taskName != null ? slayer.taskName : "Task #" + slayer.taskId;
                row("Slayer", trunc(name, 16), GOLD);
                row("  Remaining", slayer.remaining + " kills", WHITE);
                if (slayer.points > 0) row("  Points", String.valueOf(slayer.points), AMBER);
            }
        }

        // ── Death risk ────────────────────────────────────────
        if (config.showDeathRisk())
        {
            EquipmentState eq = plugin.getEquipmentState();
            if (eq.atRiskValue > 0) {
                gap();
                Color riskColor = eq.atRiskValue > config.deathRiskThreshold() ? RED : AMBER;
                row("At risk", fmtGp(eq.atRiskValue), riskColor);
                if (eq.atRiskValue > config.deathRiskThreshold())
                    row("  ⚠ HIGH RISK", "protect item!", RED);
            }
        }

        // ── Active GE slots ───────────────────────────────────
        if (config.showGeSlots())
        {
            List<TradeRecord> active = plugin.getActiveOffers();
            if (!active.isEmpty()) {
                gap();
                panelComponent.getChildren().add(LineComponent.builder()
                    .left("── GE Slots ──").leftColor(MUTED).build());
                for (TradeRecord rec : active) {
                    int pct  = rec.quantityOffered > 0 ? rec.quantityTraded * 100 / rec.quantityOffered : 0;
                    String   arrow = rec.isBuy ? "▼" : "▲";
                    Color    col   = rec.isBuy ? GREEN : GOLD;
                    row(arrow + " " + trunc(rec.itemName, 13),
                        pct + "% @" + fmtGp(rec.pricePerUnit), col);
                }
            }

            // Buy limit timers
            Map<Integer, Long> limits = plugin.getBuyLimitResetAt();
            long now = System.currentTimeMillis();
            boolean shownLimitHdr = false;
            for (Map.Entry<Integer, Long> e : limits.entrySet()) {
                long ms = e.getValue() - now;
                if (ms <= 0) continue;
                if (!shownLimitHdr) {
                    gap();
                    panelComponent.getChildren().add(LineComponent.builder()
                        .left("── Buy Limits ──").leftColor(MUTED).build());
                    shownLimitHdr = true;
                }
                long h = ms / 3600000, m = (ms % 3600000) / 60000;
                row("Limit ↺", (h > 0 ? h + "h " : "") + m + "m", AMBER);
            }
        }

        // ── GE search signal ──────────────────────────────────
        FlipSignal searched = plugin.getSearchedItemSignal();
        if (searched != null) {
            gap();
            panelComponent.getChildren().add(LineComponent.builder()
                .left("── " + trunc(searched.itemName, 18) + " ──").leftColor(GOLD).build());
            row("Buy @",  fmtGp(searched.buyPrice),      WHITE);
            row("Sell @", fmtGp(searched.sellPrice - 1), GREEN);
            row("Profit", "+" + fmtGp(searched.netMargin) + " (" + String.format("%.1f%%", searched.roi) + ")", GREEN);
            row(searched.signal, gradeColor(searched.grade) == PURPLE ? "Grade S" : "Grade " + searched.grade, signalColor(searched.signal));
        }
        // ── Top flip ──────────────────────────────────────────
        else if (config.showTopFlip())
        {
            List<FlipSignal> flips = plugin.getCachedFlips();
            if (!flips.isEmpty()) {
                FlipSignal f = flips.get(0);
                gap();
                panelComponent.getChildren().add(LineComponent.builder()
                    .left("── Top Flip ──").leftColor(MUTED).build());
                row(trunc(f.itemName, 16), "Grade " + f.grade + " [" + f.confidence + "%]", gradeColor(f.grade));
                row("Buy @",  fmtGp(f.buyPrice),      WHITE);
                row("Sell @", fmtGp(f.sellPrice - 1), GREEN);
                row("Profit", "+" + fmtGp(f.netMargin) + " (" + String.format("%.1f%%", f.roi) + ")", GREEN);
                row(f.signal, fmtGp(f.score) + "/hr realistic", signalColor(f.signal));
            }
        }

        // ── Intel summary ─────────────────────────────────────
        MarketIntelligence intel = plugin.getMarketIntelligence();
        if (intel != null && intel.timeContext != null) {
            gap();
            row("Market", intel.timeContext.session, MUTED);
        }

        return super.render(graphics);
    }

    // ── Helpers ───────────────────────────────────────────────

    private void row(String left, String right, Color rightColor)
    {
        panelComponent.getChildren().add(LineComponent.builder()
            .left(left).leftColor(MUTED)
            .right(right).rightColor(rightColor)
            .build());
    }

    private void gap()
    {
        panelComponent.getChildren().add(LineComponent.builder().left("").build());
    }

    private String fmtGp(long gp)
    {
        if (gp >= 1_000_000_000) return String.format("%.1fB", gp / 1_000_000_000.0);
        if (gp >= 1_000_000)     return String.format("%.1fM", gp / 1_000_000.0);
        if (gp >= 1_000)         return String.format("%.0fk", gp / 1_000.0);
        return String.valueOf(gp);
    }

    private String fmtSigned(long gp)
    {
        return (gp >= 0 ? "+" : "") + fmtGp(Math.abs(gp));
    }

    private String fmtXp(int xp)
    {
        if (xp >= 1_000_000) return String.format("%.1fM", xp / 1_000_000.0);
        if (xp >= 1_000)     return String.format("%.0fk", xp / 1_000.0);
        return String.valueOf(xp);
    }

    private String trunc(String s, int max)
    {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private Color signalColor(String s)
    {
        if ("ENTER".equals(s)) return GREEN;
        if ("EXIT".equals(s))  return RED;
        return AMBER;
    }

    private Color gradeColor(String g)
    {
        switch (g) { case "S": return PURPLE; case "A": return GOLD; case "B": return GREEN; default: return MUTED; }
    }
}
