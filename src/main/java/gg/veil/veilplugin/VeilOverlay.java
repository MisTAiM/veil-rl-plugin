package gg.veil.veilplugin;

import net.runelite.api.*;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.*;
import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.List;

/**
 * Full 8-slot live GE overlay + slayer task + loot counter + flip signal.
 * Only visible when the GE interface is open.
 */
public class VeilOverlay extends OverlayPanel
{
    private static final Color PROFIT  = new Color(0x3D, 0xAA, 0x6E);
    private static final Color LOSS    = new Color(0xC2, 0x54, 0x54);
    private static final Color GOLD    = new Color(0xC9, 0xA8, 0x4C);
    private static final Color MUTED   = new Color(0x6B, 0x72, 0x80);
    private static final Color HEADER  = new Color(0xE8, 0xE6, 0xDE);
    private static final Color AMBER   = new Color(0xBA, 0x75, 0x17);
    private static final Color PURPLE  = new Color(0xCC, 0x00, 0xFF);
    private static final Color BG      = new Color(0x0C, 0x0E, 0x13, 220);

    private final VeilPlugin plugin;

    @Inject
    public VeilOverlay(Client client, VeilPlugin plugin)
    {
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.LOW);
        panelComponent.setPreferredSize(new Dimension(250, 0));
        panelComponent.setBorder(new Rectangle(6, 6, 6, 6));
        panelComponent.setBackgroundColor(BG);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!plugin.isGeOpen()) return null;

        panelComponent.getChildren().clear();

        // ── Header ────────────────────────────────────────────
        panelComponent.getChildren().add(TitleComponent.builder()
            .text("VEIL  v2")
            .color(GOLD)
            .build());

        // ── Session summary ────────────────────────────────────
        VeilPlugin.SessionStats stats = plugin.getSessionStats();
        addLine("Session P&L", fmt(stats.sessionProfitGp),
            stats.sessionProfitGp >= 0 ? PROFIT : LOSS);

        int lootGp = plugin.getSessionLootGp();
        if (lootGp > 0) addLine("Loot value", fmt(lootGp), PROFIT);

        // ── XP gained ─────────────────────────────────────────
        Map<Skill, Integer> xp = plugin.getXpGained();
        if (!xp.isEmpty())
        {
            long ms = System.currentTimeMillis() - plugin.getSessionStartMs();
            double hrs = ms / 3_600_000.0;
            int total = xp.values().stream().mapToInt(Integer::intValue).sum();
            if (total > 0)
            {
                addLine("XP gained", fmtXp(total), HEADER);
                if (hrs > 0.02)
                    addLine("XP/hr", fmtXp((int)(total / hrs)), MUTED);
            }
        }

        sep();

        // ── Active GE slots ───────────────────────────────────
        List<TradeRecord> active = plugin.getActiveOffers();
        if (!active.isEmpty())
        {
            addLine("── GE Slots ──", "", MUTED);
            for (TradeRecord rec : active)
            {
                int pct = rec.quantityOffered > 0
                    ? (rec.quantityTraded * 100 / rec.quantityOffered) : 0;
                String arrow = rec.isBuy ? "▼" : "▲";
                Color  col   = rec.isBuy ? PROFIT : GOLD;
                addLine(arrow + " " + trunc(rec.itemName, 16),
                    pct + "% — " + fmtPx(rec.pricePerUnit), col);
            }
        }

        // ── Buy limit timers ──────────────────────────────────
        Map<Integer, Long> limits = plugin.getBuyLimitResetAt();
        long now = System.currentTimeMillis();
        boolean shownLimitHeader = false;
        for (Map.Entry<Integer, Long> e : limits.entrySet())
        {
            long resetAt = e.getValue();
            if (resetAt > now)
            {
                if (!shownLimitHeader) {
                    sep();
                    addLine("── Buy Limits ──", "", MUTED);
                    shownLimitHeader = true;
                }
                long secsLeft = (resetAt - now) / 1000;
                long h = secsLeft / 3600, m = (secsLeft % 3600) / 60;
                String timeStr = h > 0 ? h + "h " + m + "m" : m + "m";
                String name = trunc(resolveItemName(e.getKey()), 14);
                addLine(name, "↺ " + timeStr, AMBER);
            }
        }

        // ── Slayer ────────────────────────────────────────────
        SlayerState slayer = plugin.getSlayerState();
        if (slayer.hasTask())
        {
            sep();
            addLine("── Slayer ──", "", MUTED);
            String taskDisplay = slayer.taskName != null
                ? slayer.taskName : "Task #" + slayer.taskId;
            addLine(taskDisplay, slayer.remaining + " left", HEADER);
            if (slayer.points > 0)
                addLine("Points", String.valueOf(slayer.points), GOLD);
            if (slayer.streak > 0)
                addLine("Streak", String.valueOf(slayer.streak), MUTED);
        }

        // ── Searched item flip signal ─────────────────────────
        FlipSignal searched = plugin.getSearchedItemSignal();
        if (searched != null)
        {
            sep();
            addLine("── " + trunc(searched.itemName, 18) + " ──", "", GOLD);
            Color gradeColor = gradeColor(searched.grade);
            addLine("Grade " + searched.grade,
                "+" + fmtGp(searched.netMargin), gradeColor);
            addLine(searched.signal,
                String.format("%.1f%%", searched.roi), signalColor(searched.signal));
            addLine("Pressure", String.format("%.2f×", searched.pressure),
                searched.pressure >= 1.2 ? PROFIT : MUTED);
            if (searched.fillMins < 60)
                addLine("Fill ~" + searched.fillMins + "min", "", MUTED);
        }
        else
        {
            // Show top flip if nothing searched
            FlipSignal top = plugin.getTopFlip();
            if (top != null)
            {
                sep();
                addLine("── Top Flip ──", "", MUTED);
                addLine(trunc(top.itemName, 18), "+" + fmtGp(top.netMargin), PROFIT);
                addLine(top.signal, top.grade + " · " + String.format("%.1f%%", top.roi),
                    signalColor(top.signal));
            }
        }

        // ── Sync status ───────────────────────────────────────
        sep();
        addLine("Veil sync",
            plugin.isSyncServerRunning() ? "● " + plugin.getSessionStats().tradeCount + " trades" : "○ off",
            plugin.isSyncServerRunning() ? PROFIT : LOSS);

        return super.render(graphics);
    }

    // ── Helpers ───────────────────────────────────────────────

    private void addLine(String left, String right, Color rightColor)
    {
        panelComponent.getChildren().add(LineComponent.builder()
            .left(left).leftColor(MUTED)
            .right(right).rightColor(rightColor)
            .build());
    }

    private void sep()
    {
        panelComponent.getChildren().add(LineComponent.builder()
            .left("").build());
    }

    private String fmt(int gp)
    {
        if (gp == 0) return "0";
        String sign = gp < 0 ? "-" : "+";
        int abs = Math.abs(gp);
        if (abs >= 1_000_000) return sign + String.format("%.1fM", abs / 1_000_000.0);
        if (abs >= 1_000)     return sign + String.format("%.0fk", abs / 1_000.0);
        return sign + abs;
    }

    private String fmtGp(int gp)
    {
        if (gp >= 1_000_000) return String.format("%.1fM", gp / 1_000_000.0);
        if (gp >= 1_000)     return String.format("%.0fk", gp / 1_000.0);
        return String.valueOf(gp);
    }

    private String fmtPx(int gp) { return fmtGp(gp) + " gp"; }

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

    private String resolveItemName(int itemId) { return "#" + itemId; }

    private Color gradeColor(String grade)
    {
        switch (grade)
        {
            case "S": return PURPLE;
            case "A": return GOLD;
            case "B": return PROFIT;
            default:  return MUTED;
        }
    }

    private Color signalColor(String signal)
    {
        if ("ENTER".equals(signal)) return PROFIT;
        if ("EXIT".equals(signal))  return LOSS;
        return AMBER;
    }
}
