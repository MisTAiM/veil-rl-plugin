package gg.veil.veilplugin;

import net.runelite.api.*;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.*;
import javax.inject.Inject;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Veil — Always-on RuneLite overlay.
 * Shows live GE flip signals, active offers, slayer, XP, loot, weapon charges.
 * Visible at all times, not just when GE is open.
 * Sections collapse when there's nothing to show.
 */
public class VeilOverlay extends OverlayPanel
{
    // ── Veil color palette ───────────────────────────────────
    private static final Color GOLD   = new Color(0xC9, 0xA8, 0x4C);
    private static final Color GREEN  = new Color(0x3D, 0xAA, 0x6E);
    private static final Color RED    = new Color(0xC2, 0x54, 0x54);
    private static final Color AMBER  = new Color(0xBA, 0x75, 0x17);
    private static final Color PURPLE = new Color(0xCC, 0x00, 0xFF);
    private static final Color MUTED  = new Color(0x6B, 0x72, 0x80);
    private static final Color WHITE  = new Color(0xE8, 0xE6, 0xDE);
    private static final Color BG     = new Color(0x0C, 0x0E, 0x13, 215);

    private final VeilPlugin plugin;

    @Inject
    public VeilOverlay(Client client, VeilPlugin plugin)
    {
        this.plugin = plugin;
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
        panelComponent.getChildren().clear();

        // ── HEADER ──────────────────────────────────────────
        panelComponent.getChildren().add(TitleComponent.builder()
            .text("VEIL")
            .color(GOLD)
            .build());

        // ── SESSION SUMMARY ──────────────────────────────────
        VeilPlugin.SessionStats stats = plugin.getSessionStats();
        int lootGp = plugin.getSessionLootGp();
        int totalGp = stats.sessionProfitGp + lootGp;

        // Coin stack
        long coins = plugin.getCoinStack();
        if (coins > 0) row("Coins", fmtGp((int)Math.min(coins, Integer.MAX_VALUE)), GOLD);

        row("GP today",
            fmt(totalGp),
            totalGp >= 0 ? GREEN : RED);

        if (stats.sessionProfitGp != 0)
            row("  GE profit", fmt(stats.sessionProfitGp),
                stats.sessionProfitGp >= 0 ? GREEN : RED);
        if (lootGp > 0)
            row("  Loot", "+" + fmtGp(lootGp), GREEN);

        // ── XP ───────────────────────────────────────────────
        Map<Skill, Integer> xp = plugin.getXpGained();
        if (!xp.isEmpty())
        {
            long ms  = System.currentTimeMillis() - plugin.getSessionStartMs();
            double h = ms / 3_600_000.0;
            int total = xp.values().stream().mapToInt(Integer::intValue).sum();
            if (total > 0)
            {
                gap();
                // Top skill by XP
                Skill topSkill = xp.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey).orElse(null);
                if (topSkill != null)
                    row(topSkill.getName(), "+" + fmtXp(xp.get(topSkill)), GOLD);
                row("XP/hr",
                    h > 0.02 ? fmtXp((int)(total / h)) : "...",
                    WHITE);
            }
        }

        // ── WEAPON CHARGES ────────────────────────────────────
        WeaponCharges wc = plugin.getWeaponCharges();
        if (wc.hasAny())
        {
            gap();
            if (wc.blowpipeCharges > 0)
                row("Blowpipe",
                    wc.blowpipeCharges + " darts",
                    wc.blowpipeLow ? RED : GREEN);
            if (wc.tridentCharges > 0)
                row("Trident",
                    wc.tridentCharges + " charges",
                    wc.tridentLow ? RED : GREEN);
            if (wc.sangStaffCharges > 0)
                row("Sang staff",
                    wc.sangStaffCharges + " charges",
                    wc.sangStaffLow ? RED : GREEN);
            if (wc.tumekensCharges > 0)
                row("Tumeken's",
                    wc.tumekensCharges + " charges",
                    wc.tumekensLow ? RED : GREEN);
        }

        // ── SLAYER ────────────────────────────────────────────
        SlayerState slayer = plugin.getSlayerState();
        if (slayer.hasTask())
        {
            gap();
            String taskName = slayer.taskName != null
                ? slayer.taskName : "Task #" + slayer.taskId;
            row("Slayer", trunc(taskName, 16), GOLD);
            row("  Remaining", slayer.remaining + " kills", WHITE);
            if (slayer.points > 0)
                row("  Points", String.valueOf(slayer.points), AMBER);
        }

        // ── DEATH RISK ────────────────────────────────────────
        EquipmentState eq = plugin.getEquipmentState();
        if (eq.atRiskValue > 0)
        {
            gap();
            row("At risk",
                fmtGp(eq.atRiskValue),
                eq.atRiskValue > 5_000_000 ? RED : AMBER);
        }

        // ── ACTIVE GE SLOTS ───────────────────────────────────
        List<TradeRecord> active = plugin.getActiveOffers();
        if (!active.isEmpty())
        {
            gap();
            header("GE Slots");
            for (TradeRecord rec : active)
            {
                int pct = rec.quantityOffered > 0
                    ? rec.quantityTraded * 100 / rec.quantityOffered : 0;
                String arrow = rec.isBuy ? "▼" : "▲";
                Color  col   = rec.isBuy ? GREEN : GOLD;
                row(arrow + " " + trunc(rec.itemName, 13),
                    pct + "% @ " + fmtGp(rec.pricePerUnit),
                    pct == 100 ? GOLD : col);
            }
        }

        // ── BUY LIMITS ────────────────────────────────────────
        Map<Integer, Long> limits = plugin.getBuyLimitResetAt();
        long now = System.currentTimeMillis();
        boolean shownLimitHdr = false;
        for (Map.Entry<Integer, Long> e : limits.entrySet())
        {
            long ms = e.getValue() - now;
            if (ms <= 0) continue;
            if (!shownLimitHdr) { gap(); header("Buy Limits"); shownLimitHdr = true; }
            long h = ms / 3600000, m = (ms % 3600000) / 60000;
            row("#" + e.getKey(),
                "↺ " + (h > 0 ? h + "h " + m + "m" : m + "m"),
                AMBER);
        }

        // ── GE SEARCH FLIP SIGNAL ─────────────────────────────
        FlipSignal searched = plugin.getSearchedItemSignal();
        if (searched != null)
        {
            gap();
            header(trunc(searched.itemName, 20));
            row("Grade " + searched.grade,
                "+" + fmtGp(searched.netMargin),
                gradeColor(searched.grade));
            row(searched.signal,
                String.format("%.1f%% ROI", searched.roi),
                signalColor(searched.signal));
            row("Pressure",
                String.format("%.2f×", searched.pressure),
                searched.pressure >= 1.2 ? GREEN : MUTED);
            row("Fill est",
                searched.fillMins + " min",
                MUTED);
        }
        // ── TOP FLIP (always visible, not just at GE) ─────────
        else
        {
            List<FlipSignal> flips = plugin.getCachedFlips();
            if (!flips.isEmpty())
            {
                gap();
                header("Top Flip Now");
                FlipSignal f = flips.get(0);
                row(trunc(f.itemName, 18),
                    "+" + fmtGp(f.netMargin),
                    GREEN);
                row(f.signal + " · " + f.grade,
                    String.format("%.1f%%", f.roi),
                    signalColor(f.signal));
                row("Pressure",
                    String.format("%.2f×", f.pressure),
                    f.pressure >= 1.2 ? GREEN : MUTED);

                // Show top 3 flips total
                if (flips.size() > 1)
                {
                    gap();
                    header("Next Best");
                    for (int i = 1; i < Math.min(4, flips.size()); i++)
                    {
                        FlipSignal fi = flips.get(i);
                        row(trunc(fi.itemName, 14),
                            fi.grade + " +" + fmtGp(fi.netMargin),
                            gradeColor(fi.grade));
                    }
                }
            }
        }

        // ── SESSION TRADES ────────────────────────────────────
        if (stats.tradeCount > 0)
        {
            gap();
            row("Trades done", String.valueOf(stats.tradeCount), MUTED);
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

    private void header(String text)
    {
        panelComponent.getChildren().add(LineComponent.builder()
            .left("── " + text + " ──").leftColor(new Color(0x4A, 0x50, 0x60))
            .build());
    }

    private void gap()
    {
        panelComponent.getChildren().add(LineComponent.builder().left("").build());
    }

    private String fmt(int gp)
    {
        if (gp == 0) return "0";
        String s = gp < 0 ? "-" : "+";
        int a = Math.abs(gp);
        if (a >= 1_000_000) return s + String.format("%.1fM", a / 1_000_000.0);
        if (a >= 1_000)     return s + String.format("%.0fk", a / 1_000.0);
        return s + a;
    }

    private String fmtGp(int gp)
    {
        if (gp >= 1_000_000) return String.format("%.1fM", gp / 1_000_000.0);
        if (gp >= 1_000)     return String.format("%.0fk", gp / 1_000.0);
        return String.valueOf(gp);
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

    private Color gradeColor(String g)
    {
        switch (g)
        {
            case "S": return PURPLE;
            case "A": return GOLD;
            case "B": return GREEN;
            default:  return MUTED;
        }
    }

    private Color signalColor(String s)
    {
        if ("ENTER".equals(s)) return GREEN;
        if ("EXIT".equals(s))  return RED;
        return AMBER;
    }
}
