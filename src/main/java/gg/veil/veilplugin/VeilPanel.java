package gg.veil.veilplugin;

import net.runelite.api.Skill;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.GridLayout;
import java.awt.event.*;
import java.util.*;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Veil 6.0 Side Panel — 7 tabs
 * Dashboard | Flips | Trades | Portfolio | Intel | Skills | Tracker
 * Created by RSN: MorpheusXP | Discord: Morpheus7239
 */
public class VeilPanel extends PluginPanel
{
    // ── Colors ────────────────────────────────────────────────
    static final Color BG      = new Color(0x0C, 0x0E, 0x13);
    static final Color SURFACE = new Color(0x13, 0x16, 0x1E);
    static final Color SURFACE2= new Color(0x1A, 0x1E, 0x2A);
    static final Color BORDER  = new Color(0x1E, 0x23, 0x30);
    static final Color GOLD    = new Color(0xC9, 0xA8, 0x4C);
    static final Color GREEN   = new Color(0x3D, 0xAA, 0x6E);
    static final Color RED     = new Color(0xC2, 0x54, 0x54);
    static final Color AMBER   = new Color(0xBA, 0x75, 0x17);
    static final Color PURPLE  = new Color(0xAA, 0x00, 0xDD);
    static final Color BLUE    = new Color(0x5B, 0x7F, 0xD4);
    static final Color TEXT    = new Color(0xE8, 0xE6, 0xDE);
    static final Color MUTED   = new Color(0x6B, 0x72, 0x80);

    private final VeilPlugin  plugin;
    private final VeilConfig  config;

    private DashboardTab  dashTab;
    private FlipsTab      flipsTab;
    private TradesTab     tradesTab;
    private PortfolioTab  portfolioTab;
    private IntelTab      intelTab;
    private SkillsTab     skillsTab;
    private TrackerTab       trackerTab;
    private AlchScannerTab   alchTab;
    private SlotOptimizerTab slotTab;
    private AlertsGoalsTab   alertsTab;
    private GuideTab         guideTab;
    private GrandmaPanel     grandmaPanel;
    private ToolsTab         toolsTab;
    private PersonalityTab   personalityTab;
    private HistoryTab       historyTab;
    private BossGpHrTab      bossTab;
    private StatsTab         statsTab;
    private WatchlistTab     watchlistTab;

    private JTabbedPane tabs;
    private JLabel      coinLabel;

    @Inject
    public VeilPanel(VeilPlugin plugin, VeilConfig config)
    {
        this.plugin = plugin;
        this.config = config;
        setBackground(BG);
        setLayout(new BorderLayout());
        build();
    }

    private void build()
    {
        add(buildHeader(), BorderLayout.NORTH);

        tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setBackground(BG);
        tabs.setForeground(MUTED);
        tabs.setFont(FontManager.getRunescapeSmallFont().deriveFont(10f));

        dashTab      = new DashboardTab(plugin, config, this);
        flipsTab     = new FlipsTab(plugin, config, this);
        tradesTab    = new TradesTab(plugin, this);
        portfolioTab = new PortfolioTab(plugin, this);
        intelTab     = new IntelTab(plugin, this);
        skillsTab    = new SkillsTab(plugin, this);
        trackerTab   = new TrackerTab(plugin, this);
        alchTab      = new AlchScannerTab(plugin);
        slotTab      = new SlotOptimizerTab(plugin);
        alertsTab    = new AlertsGoalsTab(plugin);
        guideTab       = new GuideTab(plugin);
        grandmaPanel   = new GrandmaPanel(plugin);
        historyTab     = new HistoryTab(plugin);
        statsTab       = new StatsTab(plugin);
        watchlistTab   = new WatchlistTab(plugin);
        bossTab        = new BossGpHrTab(plugin);
        toolsTab       = new ToolsTab(plugin);
        personalityTab = new PersonalityTab(plugin);

        tabs.addTab("Home",     scroll(dashTab));
        tabs.addTab("Flips",    scroll(flipsTab));
        tabs.addTab("Trades",   scroll(tradesTab));
        tabs.addTab("Wallet",   scroll(portfolioTab));
        tabs.addTab("Intel",    scroll(intelTab));
        tabs.addTab("XP",       scroll(skillsTab));
        tabs.addTab("Tracker",  scroll(trackerTab));
        tabs.addTab("Alch",     scroll(alchTab));
        tabs.addTab("Slots",    scroll(slotTab));
        tabs.addTab("Now!",     scroll(grandmaPanel));
        tabs.addTab("History",  scroll(historyTab));
        tabs.addTab("Stats",    scroll(statsTab));
        tabs.addTab("Positions",scroll(watchlistTab));
        tabs.addTab("Bosses",   scroll(bossTab));
        tabs.addTab("Guide",    scroll(guideTab));
        tabs.addTab("Tools",    scroll(toolsTab));
        tabs.addTab("Style",    scroll(personalityTab));
        tabs.addTab("Alerts",   scroll(alertsTab));

        // Clear search bar when switching away from Flips tab
        tabs.addChangeListener(e -> {
            if (flipsTab != null && tabs.getSelectedIndex() != 1) {
                // Don't clear — user may want to come back to same search
                // But DO refresh flips data if returning
            }
        });

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildHeader()
    {
        JPanel h = new JPanel(new BorderLayout(0, 2));
        h.setBackground(SURFACE);
        h.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(8, 10, 8, 10)));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(SURFACE);

        JLabel title = new JLabel("VEIL 10.0");
        title.setForeground(GOLD);
        title.setFont(FontManager.getRunescapeBoldFont().deriveFont(13f));
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel credits = new JLabel("MorpheusXP  ·  Morpheus7239");
        credits.setForeground(MUTED);
        credits.setFont(FontManager.getRunescapeSmallFont());
        credits.setAlignmentX(LEFT_ALIGNMENT);

        left.add(title);
        left.add(credits);

        coinLabel = new JLabel("—");
        coinLabel.setForeground(GREEN);
        coinLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));

        h.add(left, BorderLayout.WEST);
        h.add(coinLabel, BorderLayout.EAST);
        return h;
    }

    private JScrollPane scroll(JPanel p)
    {
        // Hard-constrain to 209px (225px panel - 16px padding)
        p.setMaximumSize(new Dimension(209, Integer.MAX_VALUE));
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.setMaximumSize(new Dimension(225, Integer.MAX_VALUE));
        wrapper.add(p, BorderLayout.NORTH); // NORTH = natural height, no stretch
        JScrollPane sp = new JScrollPane(wrapper);
        sp.setBackground(BG);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        return sp;
    }

    // ── Public refresh methods ─────────────────────────────────

    public void refreshAll()
    {
        SwingUtilities.invokeLater(() -> {
            updateCoin();
            dashTab.refresh();
            flipsTab.refresh();
            portfolioTab.refresh();
            skillsTab.refresh();
            trackerTab.refresh();
        });
    }

    public void refreshFlips()
    {
        SwingUtilities.invokeLater(() -> {
            updateCoin();
            flipsTab.refresh();
            portfolioTab.refresh();
            intelTab.refresh();
            dashTab.refresh();
        });
    }

    public void updateSession()
    {
        SwingUtilities.invokeLater(() -> {
            updateCoin();
            dashTab.refresh();
            tradesTab.refresh();
            skillsTab.refresh();
            if (guideTab    != null) guideTab.refresh();
            if (historyTab    != null) historyTab.refresh();
            if (statsTab      != null) statsTab.refresh();
            if (watchlistTab  != null) watchlistTab.refresh();
        });
    }

    private void updateCoin()
    {
        long coins = plugin.getCoinStack();
        coinLabel.setText(coins > 0 ? "💰 " + fmtGp(coins) : "Open inv");
    }

    // ── Shared static helpers ──────────────────────────────────

    static String fmtGp(long gp)
    {
        if (gp >= 1_000_000_000) return String.format("%.1fB", gp / 1_000_000_000.0);
        if (gp >= 1_000_000)     return String.format("%.1fM", gp / 1_000_000.0);
        if (gp >= 1_000)         return String.format("%.0fk", gp / 1_000.0);
        return String.valueOf(gp);
    }

    static String fmtSigned(long gp)
    {
        return (gp >= 0 ? "+" : "") + fmtGp(Math.abs(gp));
    }

    static String fmtXp(int xp)
    {
        if (xp >= 1_000_000) return String.format("%.1fM", xp / 1_000_000.0);
        if (xp >= 1_000)     return String.format("%.0fk", xp / 1_000.0);
        return String.valueOf(xp);
    }

    static JPanel card(String title)
    {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(SURFACE);
        p.setBorder(new CompoundBorder(
            new MatteBorder(1, 1, 1, 1, BORDER),
            new EmptyBorder(8, 10, 8, 10)));
        if (title != null && !title.isEmpty()) {
            JLabel l = new JLabel(title);
            l.setForeground(MUTED);
            l.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
            l.setAlignmentX(LEFT_ALIGNMENT);
            p.add(l);
            p.add(Box.createVerticalStrut(5));
        }
        return p;
    }

    static JPanel row(String left, String right, Color rightColor)
    {
        JPanel r = new JPanel(new BorderLayout(4, 0));
        r.setBackground(SURFACE);
        r.setAlignmentX(LEFT_ALIGNMENT);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        // Truncate both sides to prevent overflow in 225px panel
        JLabel l  = new JLabel(clip(left,  22)); l.setForeground(MUTED); l.setFont(FontManager.getRunescapeSmallFont());
        JLabel rv = new JLabel(clip(right, 22)); rv.setForeground(rightColor); rv.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        r.add(l, BorderLayout.WEST);
        r.add(rv, BorderLayout.EAST);
        return r;
    }

    /** Clip string to max chars, appending … if truncated */
    static String clip(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    static JPanel bigRow(String left, String right, Color rightColor)
    {
        JPanel r = new JPanel(new BorderLayout(4, 0));
        r.setBackground(SURFACE);
        r.setAlignmentX(LEFT_ALIGNMENT);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel l  = new JLabel(clip(left,  20)); l.setForeground(TEXT); l.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        JLabel rv = new JLabel(clip(right, 20)); rv.setForeground(rightColor); rv.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        r.add(l, BorderLayout.WEST);
        r.add(rv, BorderLayout.EAST);
        return r;
    }

    static JButton btn(String text, Color bg, Color fg)
    {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        b.setBorder(new EmptyBorder(5, 10, 5, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFocusPainted(false);
        b.setAlignmentX(LEFT_ALIGNMENT);
        return b;
    }

    static JTextField field(String placeholder)
    {
        JTextField f = new JTextField() {
            @Override public void paint(java.awt.Graphics g) {
                super.paint(g);
                if (getText().isEmpty()) {
                    java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
                    g2.setColor(MUTED);
                    g2.setFont(FontManager.getRunescapeSmallFont());
                    java.awt.Insets ins = getInsets();
                    g2.drawString(placeholder, ins.left + 2, getHeight() - ins.bottom - 3);
                }
            }
        };
        f.setBackground(BG);
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setBorder(new CompoundBorder(
            new MatteBorder(1, 1, 1, 1, BORDER),
            new EmptyBorder(4, 6, 4, 6)));
        f.setFont(FontManager.getRunescapeSmallFont());
        return f;
    }

    static JLabel muted(String text)
    {
        JLabel l = new JLabel("<html><div style='width:195'>" + text + "</div></html>");
        l.setForeground(MUTED);
        l.setFont(FontManager.getRunescapeSmallFont());
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    static JLabel bold(String text, Color color)
    {
        JLabel l = new JLabel("<html><div style='width:195'>" + text + "</div></html>");
        l.setForeground(color);
        l.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    static JPanel pad(JPanel inner)
    {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG);
        outer.setBorder(new EmptyBorder(0, 8, 0, 8));
        outer.add(inner, BorderLayout.CENTER);
        return outer;
    }

    static Color gradeColor(String g)
    {
        switch (g) { case "S": return PURPLE; case "A": return GOLD; case "B": return GREEN; default: return MUTED; }
    }

    static Color signalColor(String s)
    {
        if ("ENTER".equals(s)) return GREEN;
        if ("EXIT".equals(s))  return RED;
        return AMBER;
    }
}


// ════════════════════════════════════════════════════════════
// TAB 1: DASHBOARD
// ════════════════════════════════════════════════════════════
class DashboardTab extends JPanel
{
    private final VeilPlugin plugin;
    private final VeilConfig config;
    private final VeilPanel  veil;
    private JPanel slotsPanel, watchlistPanel, summaryPanel;
    private final List<WatchItem> watchlist = new ArrayList<>();

    static class WatchItem { String name; int itemId; int alertPrice; boolean alertBelow;
        WatchItem(String n, int id, int price, boolean below) { name=n; itemId=id; alertPrice=price; alertBelow=below; } }

    DashboardTab(VeilPlugin plugin, VeilConfig config, VeilPanel veil)
    {
        this.plugin = plugin; this.config = config; this.veil = veil;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        build();
    }

    private void build()
    {
        // ── Summary card ──────────────────────────────────────
        summaryPanel = VeilPanel.card(null);
        summaryPanel.setAlignmentX(LEFT_ALIGNMENT);
        summaryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        add(summaryPanel);
        add(Box.createVerticalStrut(6));

        // ── Active GE slots ───────────────────────────────────
        add(VeilPanel.bold("ACTIVE GE SLOTS", VeilPanel.GOLD));
        add(Box.createVerticalStrut(4));
        slotsPanel = new JPanel();
        slotsPanel.setLayout(new BoxLayout(slotsPanel, BoxLayout.Y_AXIS));
        slotsPanel.setBackground(VeilPanel.BG);
        add(slotsPanel);
        add(Box.createVerticalStrut(8));

        // ── Watchlist ─────────────────────────────────────────
        add(VeilPanel.bold("WATCHLIST", VeilPanel.GOLD));
        add(Box.createVerticalStrut(4));

        // Add to watchlist row
        JPanel addRow = new JPanel(new BorderLayout(4, 0));
        addRow.setBackground(VeilPanel.BG);
        addRow.setAlignmentX(LEFT_ALIGNMENT);
        addRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JTextField wNameField = VeilPanel.field("Item name or ID...");
        JButton wAddBtn = VeilPanel.btn("Watch", VeilPanel.GOLD, VeilPanel.BG);
        wAddBtn.setBorder(new EmptyBorder(3, 8, 3, 8));
        wAddBtn.addActionListener(e -> {
            String t = wNameField.getText().trim();
            if (t.isEmpty()) return;
            // Try to match to a known flip
            List<FlipSignal> flips = plugin.getCachedFlips();
            FlipSignal match = flips.stream()
                .filter(f -> f.itemName.toLowerCase().contains(t.toLowerCase()))
                .findFirst().orElse(null);
            if (match != null) {
                watchlist.add(new WatchItem(match.itemName, match.itemId, 0, false));
                wNameField.setText("");
                refresh();
            }
        });
        addRow.add(wNameField, BorderLayout.CENTER);
        addRow.add(wAddBtn, BorderLayout.EAST);
        add(addRow);
        add(Box.createVerticalStrut(4));

        watchlistPanel = new JPanel();
        watchlistPanel.setLayout(new BoxLayout(watchlistPanel, BoxLayout.Y_AXIS));
        watchlistPanel.setBackground(VeilPanel.BG);
        add(watchlistPanel);
    }

    void refresh()
    {
        // Summary
        summaryPanel.removeAll();
        long coins = plugin.getCoinStack();
        VeilPlugin.SessionStats s = plugin.getSessionStats();
        int loot = plugin.getSessionLootGp();
        long total = s.sessionProfitGp + loot;

        JLabel coinLbl = new JLabel(coins > 0 ? VeilPanel.fmtGp(coins) + " GP" : "Open inventory to read coins");
        coinLbl.setForeground(coins > 0 ? VeilPanel.GOLD : VeilPanel.MUTED);
        coinLbl.setFont(FontManager.getRunescapeBoldFont().deriveFont(16f));
        coinLbl.setAlignmentX(LEFT_ALIGNMENT);
        summaryPanel.add(coinLbl);
        summaryPanel.add(VeilPanel.muted("Your coin stack (live)"));
        summaryPanel.add(Box.createVerticalStrut(6));
        summaryPanel.add(VeilPanel.row("GE profit", VeilPanel.fmtSigned(s.sessionProfitGp), s.sessionProfitGp >= 0 ? VeilPanel.GREEN : VeilPanel.RED));
        summaryPanel.add(VeilPanel.row("Loot value", "+" + VeilPanel.fmtGp(loot), VeilPanel.GREEN));
        summaryPanel.add(VeilPanel.row("Total today", VeilPanel.fmtSigned(total), total >= 0 ? VeilPanel.GREEN : VeilPanel.RED));

        // Prayer + spec if available
        int prayer = plugin.getPrayerPoints();
        int spec   = plugin.getSpecPercent();
        if (prayer > 0 || spec > 0) {
            summaryPanel.add(Box.createVerticalStrut(4));
            if (prayer > 0) summaryPanel.add(VeilPanel.row("Prayer", prayer + " pts", prayer < 20 ? VeilPanel.RED : VeilPanel.GREEN));
            if (spec > 0)   summaryPanel.add(VeilPanel.row("Special", spec + "%", spec >= 100 ? VeilPanel.GREEN : VeilPanel.AMBER));
        }

        // GE slots
        slotsPanel.removeAll();
        List<TradeRecord> active = plugin.getActiveOffers();
        List<FlipSignal> flips = plugin.getCachedFlips();
        Map<Integer, FlipSignal> flipMap = new HashMap<>();
        for (FlipSignal f : flips) flipMap.put(f.itemId, f);

        if (active.isEmpty()) {
            slotsPanel.add(VeilPanel.muted("No active GE offers"));
        }
        for (TradeRecord rec : active) {
            slotsPanel.add(buildSlotCard(rec, flipMap.get(rec.itemId)));
            slotsPanel.add(Box.createVerticalStrut(4));
        }

        // Weapon charges
        WeaponCharges wc = plugin.getWeaponCharges();
        if (wc.hasAny()) {
            slotsPanel.add(Box.createVerticalStrut(4));
            slotsPanel.add(VeilPanel.bold("WEAPON CHARGES", VeilPanel.GOLD));
            slotsPanel.add(Box.createVerticalStrut(2));
            if (wc.blowpipeCharges  > 0) slotsPanel.add(buildChargeRow("Blowpipe",  wc.blowpipeCharges  + " darts",   wc.blowpipeLow));
            if (wc.tridentCharges   > 0) slotsPanel.add(buildChargeRow("Trident",   wc.tridentCharges   + " charges", wc.tridentLow));
            if (wc.sangStaffCharges > 0) slotsPanel.add(buildChargeRow("Sang Staff",wc.sangStaffCharges + " charges", wc.sangStaffLow));
            if (wc.tumekensCharges  > 0) slotsPanel.add(buildChargeRow("Tumeken's", wc.tumekensCharges  + " charges", wc.tumekensLow));
        }

        // Slayer
        SlayerState sl = plugin.getSlayerState();
        if (sl.hasTask()) {
            slotsPanel.add(Box.createVerticalStrut(4));
            slotsPanel.add(VeilPanel.bold("SLAYER", VeilPanel.GOLD));
            String taskName = sl.taskName != null ? sl.taskName : "Task #" + sl.taskId;
            slotsPanel.add(VeilPanel.row(taskName, sl.remaining + " remaining", VeilPanel.TEXT));
            if (sl.points > 0) slotsPanel.add(VeilPanel.row("Points", String.valueOf(sl.points), VeilPanel.AMBER));
        }

        // Watchlist
        watchlistPanel.removeAll();
        for (int i = 0; i < watchlist.size(); i++) {
            WatchItem w = watchlist.get(i);
            FlipSignal sig = flipMap.get(w.itemId);
            watchlistPanel.add(buildWatchCard(w, sig, i));
            watchlistPanel.add(Box.createVerticalStrut(3));
        }

        summaryPanel.revalidate(); summaryPanel.repaint();
        slotsPanel.revalidate();   slotsPanel.repaint();
        watchlistPanel.revalidate(); watchlistPanel.repaint();
    }

    private JPanel buildSlotCard(TradeRecord rec, FlipSignal signal)
    {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(VeilPanel.SURFACE);
        card.setBorder(new CompoundBorder(
            new MatteBorder(0, rec.isBuy ? 2 : 0, 0, rec.isBuy ? 0 : 2,
                rec.isBuy ? VeilPanel.GREEN : VeilPanel.GOLD),
            new EmptyBorder(7, 10, 7, 10)));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        // Name + type
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(VeilPanel.SURFACE);
        top.setAlignmentX(LEFT_ALIGNMENT);
        top.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        JLabel name = new JLabel(VeilPanel.clip(rec.itemName, 18));
        name.setForeground(VeilPanel.TEXT);
        name.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        JLabel type = new JLabel(rec.isBuy ? " BUYING " : " SELLING ");
        type.setForeground(rec.isBuy ? VeilPanel.GREEN : VeilPanel.GOLD);
        type.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        top.add(name, BorderLayout.WEST);
        top.add(type, BorderLayout.EAST);
        card.add(top);
        card.add(Box.createVerticalStrut(4));

        // Fill bar
        int pct = rec.quantityOffered > 0 ? rec.quantityTraded * 100 / rec.quantityOffered : 0;
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(pct);
        bar.setStringPainted(true);
        bar.setString(pct + "% (" + rec.quantityTraded + "/" + rec.quantityOffered + ")");
        bar.setBackground(VeilPanel.BG);
        bar.setForeground(pct == 100 ? VeilPanel.GREEN : rec.isBuy ? VeilPanel.BLUE : VeilPanel.GOLD);
        bar.setBorderPainted(false);
        bar.setFont(FontManager.getRunescapeSmallFont());
        bar.setAlignmentX(LEFT_ALIGNMENT);
        bar.setMaximumSize(new Dimension(180, 16));
        card.add(bar);
        card.add(Box.createVerticalStrut(4));

        card.add(VeilPanel.row("You offered:", VeilPanel.fmtGp(rec.pricePerUnit) + " gp ea", VeilPanel.MUTED));

        if (rec.isBuy && signal != null) {
            int sellAt  = signal.sellPrice - 1;
            int tax     = Math.min(5_000_000, Math.max(1, (int)(sellAt * 0.01)));
            int profEa  = sellAt - rec.pricePerUnit - tax;
            int profTot = profEa * Math.max(rec.quantityTraded, 1);
            card.add(Box.createVerticalStrut(3));

            // Show sell confidence
            Color riskCol = "LOW RISK".equals(signal.sellRisk) ? VeilPanel.GREEN
                : "MEDIUM RISK".equals(signal.sellRisk) ? VeilPanel.AMBER : VeilPanel.RED;
            String riskStr = signal.sellRisk != null ? " [" + signal.sellRisk + "]" : "";
            card.add(VeilPanel.bigRow("→ SELL AT:", VeilPanel.fmtGp(sellAt) + "ea" + riskStr, riskCol));

            if (signal.sellConfidence > 0) {
                card.add(VeilPanel.row("  Confidence:", signal.sellConfidence + "/100 — " +
                    (signal.sellConfidence >= 75 ? "price is reliable" :
                     signal.sellConfidence >= 50 ? "moderate risk — verify price" :
                     "high risk — check wiki before posting"),
                    riskCol));
            }
            if (signal.sellRiskReason != null && !"LOW RISK".equals(signal.sellRisk)) {
                card.add(VeilPanel.row("  ⚠", signal.sellRiskReason, VeilPanel.AMBER));
            }

            card.add(VeilPanel.row("  Profit ea:", VeilPanel.fmtSigned(profEa) + " (after 1% tax)", profEa > 0 ? VeilPanel.GREEN : VeilPanel.RED));
            if (rec.quantityTraded > 0)
                card.add(VeilPanel.bigRow("  Total profit:", VeilPanel.fmtSigned(profTot), profTot > 0 ? VeilPanel.GREEN : VeilPanel.RED));

            // Price prediction for active hold
            if (signal.pred2hrLow > 0) {
                card.add(Box.createVerticalStrut(2));
                String trend = "UP".equals(signal.predTrend) ? "↑ trending up" :
                               "DOWN".equals(signal.predTrend) ? "↓ trending down" : "→ flat";
                card.add(VeilPanel.row("  2hr price range:",
                    VeilPanel.fmtGp(signal.pred2hrLow) + "—" + VeilPanel.fmtGp(signal.pred2hrHigh),
                    VeilPanel.MUTED));
                card.add(VeilPanel.row("  Trend:", trend,
                    "UP".equals(signal.predTrend) ? VeilPanel.GREEN :
                    "DOWN".equals(signal.predTrend) ? VeilPanel.RED : VeilPanel.MUTED));
            }
        } else if (!rec.isBuy && signal != null) {
            int tax    = Math.min(5_000_000, Math.max(1, (int)(rec.pricePerUnit * 0.01)));
            int netGp  = (rec.pricePerUnit - tax) * Math.max(rec.quantityOffered, 1);
            card.add(VeilPanel.row("After tax:", VeilPanel.fmtGp(netGp), VeilPanel.GREEN));
        }
        return card;
    }

    private JPanel buildChargeRow(String name, String charges, boolean low)
    {
        JPanel r = VeilPanel.row(name, charges, low ? VeilPanel.RED : VeilPanel.GREEN);
        if (low) {
            r.setBackground(VeilPanel.RED.darker().darker());
        }
        return r;
    }

    private JPanel buildWatchCard(WatchItem w, FlipSignal sig, int idx)
    {
        JPanel card = VeilPanel.card(null);
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        card.add(VeilPanel.bigRow(VeilPanel.clip(w.name, 20), "", VeilPanel.TEXT));
        if (sig != null) {
            card.add(VeilPanel.row("Buy @",  VeilPanel.fmtGp(sig.buyPrice),  VeilPanel.MUTED));
            card.add(VeilPanel.row("Sell @", VeilPanel.fmtGp(sig.sellPrice - 1), VeilPanel.GREEN));
            card.add(VeilPanel.row("Margin", "+" + VeilPanel.fmtGp(sig.netMargin) + " (" + String.format("%.1f%%", sig.roi) + ")", VeilPanel.GREEN));
            card.add(VeilPanel.row("Signal", sig.signal, VeilPanel.signalColor(sig.signal)));
        } else {
            card.add(VeilPanel.muted("No market data"));
        }
        JButton rm = VeilPanel.btn("Remove", VeilPanel.SURFACE2, VeilPanel.RED);
        rm.addActionListener(e -> { watchlist.remove(idx); refresh(); });
        card.add(Box.createVerticalStrut(3));
        card.add(rm);
        return card;
    }
}


// ════════════════════════════════════════════════════════════
// TAB 2: FLIPS — config-driven filters, every item
// ════════════════════════════════════════════════════════════
class FlipsTab extends JPanel
{
    private final VeilPlugin plugin;
    private final VeilConfig config;
    private final VeilPanel  veil;
    private JTextField searchField;
    private JComboBox<String> sortBox, signalBox;
    private JCheckBox membersChk, f2pOnlyChk;
    private JLabel countLabel;
    private JPanel listPanel;
    private List<FlipSignal> allFlips = new ArrayList<>();

    FlipsTab(VeilPlugin plugin, VeilConfig config, VeilPanel veil)
    {
        this.plugin = plugin; this.config = config; this.veil = veil;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        build();
    }

    private void build()
    {
        // Search
        searchField = VeilPanel.field("Search any of 4,035 GE items...");
        searchField.setAlignmentX(LEFT_ALIGNMENT);
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { filter(); }
            public void removeUpdate(DocumentEvent e)  { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });
        add(searchField);
        add(Box.createVerticalStrut(6));

        // Filter row
        // Sort row
        JPanel sortRow = new JPanel(new BorderLayout(4, 0));
        sortRow.setBackground(VeilPanel.BG);
        sortRow.setAlignmentX(LEFT_ALIGNMENT);
        sortRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        JLabel sortLbl = new JLabel("Sort: ");
        sortLbl.setForeground(VeilPanel.MUTED);
        sortLbl.setFont(FontManager.getRunescapeSmallFont());
        sortBox = new JComboBox<>(new String[]{"GP/hr","Margin","ROI","Fill Speed"});
        sortBox.setBackground(VeilPanel.SURFACE); sortBox.setForeground(VeilPanel.TEXT);
        sortBox.setFont(FontManager.getRunescapeSmallFont());
        sortBox.addActionListener(e -> filter());
        sortRow.add(sortLbl, BorderLayout.WEST);
        sortRow.add(sortBox, BorderLayout.CENTER);
        add(sortRow);
        add(Box.createVerticalStrut(3));

        // Signal filter row
        JPanel sigRow = new JPanel(new BorderLayout(4, 0));
        sigRow.setBackground(VeilPanel.BG);
        sigRow.setAlignmentX(LEFT_ALIGNMENT);
        sigRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        JLabel sigLbl = new JLabel("Signal: ");
        sigLbl.setForeground(VeilPanel.MUTED);
        sigLbl.setFont(FontManager.getRunescapeSmallFont());
        signalBox = new JComboBox<>(new String[]{"All Signals","ENTER only","HOLD only","EXIT only"});
        signalBox.setBackground(VeilPanel.SURFACE); signalBox.setForeground(VeilPanel.TEXT);
        signalBox.setFont(FontManager.getRunescapeSmallFont());
        signalBox.addActionListener(e -> filter());
        sigRow.add(sigLbl, BorderLayout.WEST);
        sigRow.add(signalBox, BorderLayout.CENTER);
        add(sigRow);
        add(Box.createVerticalStrut(3));
        add(Box.createVerticalStrut(4));

        JPanel chkRow = new JPanel(new GridLayout(1, 2, 4, 0));
        chkRow.setBackground(VeilPanel.BG);
        chkRow.setAlignmentX(LEFT_ALIGNMENT);
        chkRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        f2pOnlyChk = new JCheckBox("F2P only");
        f2pOnlyChk.setBackground(VeilPanel.BG); f2pOnlyChk.setForeground(VeilPanel.MUTED);
        f2pOnlyChk.setFont(FontManager.getRunescapeSmallFont());
        f2pOnlyChk.addActionListener(e -> filter());

        membersChk = new JCheckBox("Members only");
        membersChk.setBackground(VeilPanel.BG); membersChk.setForeground(VeilPanel.MUTED);
        membersChk.setFont(FontManager.getRunescapeSmallFont());
        membersChk.addActionListener(e -> filter());

        chkRow.add(f2pOnlyChk);
        chkRow.add(membersChk);
        add(chkRow);
        add(Box.createVerticalStrut(4));

        countLabel = new JLabel("Loading 4,035 items...");
        countLabel.setForeground(VeilPanel.MUTED);
        countLabel.setFont(FontManager.getRunescapeSmallFont());
        countLabel.setAlignmentX(LEFT_ALIGNMENT);
        add(countLabel);
        add(Box.createVerticalStrut(4));

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(VeilPanel.BG);
        add(listPanel);
    }

    void refresh() { allFlips = new ArrayList<>(plugin.getCachedFlips()); filter(); }

    private void filter()
    {
        String q = searchField.getText().trim().toLowerCase();
        String sig = (String) signalBox.getSelectedItem();
        boolean f2pOnly = f2pOnlyChk.isSelected();
        boolean memOnly = membersChk.isSelected();

        List<FlipSignal> filtered = allFlips.stream()
            .filter(f -> q.isEmpty() || f.itemName.toLowerCase().contains(q))
            .filter(f -> !f2pOnly || !f.members)
            .filter(f -> !memOnly || f.members)
            .filter(f -> sig == null || sig.equals("All Signals") || f.signal.equals(sig.replace(" only","")))
            .filter(f -> !config.hideDGrade() || !f.grade.equals("D"))
            .filter(f -> !config.enterOnly() || f.signal.equals("ENTER"))
            .filter(f -> f.netMargin >= config.minNetMargin())
            .filter(f -> f.roi >= config.minRoi())
            .filter(f -> f.fillMins <= config.maxFillMins())
            .collect(Collectors.toList());

        // Sort
        String sort = (String) sortBox.getSelectedItem();
        switch (sort != null ? sort : "GP/hr") {
            case "Margin":     filtered.sort((a, b) -> Integer.compare(b.netMargin, a.netMargin)); break;
            case "ROI":        filtered.sort((a, b) -> Double.compare(b.roi, a.roi)); break;
            case "Fill Speed": filtered.sort((a, b) -> Integer.compare(a.fillMins, b.fillMins)); break;
            default:           filtered.sort((a, b) -> Integer.compare(b.score, a.score));
        }

        final List<FlipSignal> show = filtered;
        final long coins = plugin.getCoinStack();

        SwingUtilities.invokeLater(() -> {
            listPanel.removeAll();

            // Show loading state if data isn't ready yet
            if (allFlips.isEmpty()) {
                JPanel loadCard = new JPanel();
                loadCard.setLayout(new BoxLayout(loadCard, BoxLayout.Y_AXIS));
                loadCard.setBackground(VeilPanel.SURFACE);
                loadCard.setBorder(new EmptyBorder(20, 20, 20, 20));
                loadCard.setAlignmentX(LEFT_ALIGNMENT);
                loadCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
                JLabel ll = new JLabel("Loading flip data...");
                ll.setForeground(VeilPanel.GOLD);
                ll.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
                ll.setAlignmentX(CENTER_ALIGNMENT);
                JLabel ll2 = new JLabel("Scanning 4,035 GE items");
                ll2.setForeground(VeilPanel.MUTED);
                ll2.setFont(FontManager.getRunescapeSmallFont());
                ll2.setAlignmentX(CENTER_ALIGNMENT);
                JLabel ll3 = new JLabel("Updated every 60s");
                ll3.setForeground(VeilPanel.MUTED);
                ll3.setFont(FontManager.getRunescapeSmallFont());
                ll3.setAlignmentX(CENTER_ALIGNMENT);
                loadCard.add(Box.createVerticalGlue());
                loadCard.add(ll); loadCard.add(Box.createVerticalStrut(4));
                loadCard.add(ll2); loadCard.add(Box.createVerticalStrut(2));
                loadCard.add(ll3);
                loadCard.add(Box.createVerticalGlue());
                listPanel.add(loadCard);
                listPanel.revalidate(); listPanel.repaint();
                return;
            }

            countLabel.setText(show.size() + " items" + (q.isEmpty() ? "" : " matching \"" + q + "\"")
                + "  |  Scanning all 4,035 GE tradeable items");

            if (show.isEmpty()) listPanel.add(VeilPanel.muted("No items match your filters"));

            // Show items in tiers with headers
            String currentGrade = "";
            int rank = 0;
            for (int i = 0; i < Math.min(50, show.size()); i++) {
                FlipSignal flip = show.get(i);
                rank++;
                // Add grade tier header when grade changes
                if (!flip.grade.equals(currentGrade)) {
                    currentGrade = flip.grade;
                    String tierLabel;
                    Color tierColor;
                    switch (currentGrade) {
                        case "S": tierLabel = "── S GRADE: ELITE ─────"; tierColor = VeilPanel.PURPLE; break;
                        case "A": tierLabel = "── A GRADE: GREAT ─────"; tierColor = VeilPanel.GOLD; break;
                        case "B": tierLabel = "── B GRADE: SOLID ─────"; tierColor = VeilPanel.GREEN; break;
                        case "C": tierLabel = "── C GRADE: DECENT ────"; tierColor = VeilPanel.MUTED; break;
                        default:  tierLabel = "── D GRADE: LOW PRIORITY ─"; tierColor = VeilPanel.MUTED; break;
                    }
                    JPanel tierHdr = new JPanel(new BorderLayout());
                    tierHdr.setBackground(VeilPanel.BG);
                    tierHdr.setAlignmentX(LEFT_ALIGNMENT);
                    tierHdr.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
                    JLabel tierLbl = new JLabel(tierLabel);
                    tierLbl.setForeground(tierColor);
                    tierLbl.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
                    tierHdr.add(tierLbl, BorderLayout.WEST);
                    listPanel.add(Box.createVerticalStrut(8));
                    listPanel.add(tierHdr);
                    listPanel.add(Box.createVerticalStrut(4));
                }
                listPanel.add(buildCard(flip, rank, coins));
                listPanel.add(Box.createVerticalStrut(4));
            }
            if (show.size() > 50) {
                listPanel.add(VeilPanel.muted("Showing top 50 — use search or filters to find more"));
            }
            listPanel.revalidate(); listPanel.repaint();
        });
    }

    private JLabel stepLabel(String text) {
        // Wrap in HTML so long strings wrap rather than overflow
        JLabel l = new JLabel("<html><div style='width:195'>" + text.replace("<","&lt;") + "</div></html>");
        l.setForeground(VeilPanel.MUTED);
        l.setFont(FontManager.getRunescapeSmallFont());
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return l;
    }

    private JPanel buildCard(FlipSignal f, int rank, long coins)
    {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(VeilPanel.SURFACE);
        card.setBorder(new CompoundBorder(
            new MatteBorder(0, 3, 0, 0, VeilPanel.gradeColor(f.grade)),
            new EmptyBorder(7, 10, 7, 10)));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 800));

        // Header: rank + name + badges
        JPanel hdr = new JPanel(new BorderLayout(4, 0));
        hdr.setBackground(VeilPanel.SURFACE);
        hdr.setAlignmentX(LEFT_ALIGNMENT);
        hdr.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JLabel nameL = new JLabel("#" + rank + "  " + f.itemName);
        nameL.setForeground(VeilPanel.TEXT);
        nameL.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));

        JPanel badges = new JPanel();
        badges.setLayout(new BoxLayout(badges, BoxLayout.X_AXIS));
        badges.setBackground(VeilPanel.SURFACE);
        badges.add(badge(f.grade, VeilPanel.gradeColor(f.grade)));
        badges.add(badge(f.signal, VeilPanel.signalColor(f.signal)));
        if (!f.members) badges.add(badge("F2P", VeilPanel.BLUE));

        hdr.add(nameL, BorderLayout.WEST);
        hdr.add(badges, BorderLayout.EAST);
        card.add(hdr);
        card.add(Box.createVerticalStrut(5));

        // THE CORE FLIP INFO
        // ── 3 BUY PRICE SCENARIOS ──────────────────────────
        card.add(VeilPanel.bold("BUY OPTIONS:", VeilPanel.TEXT));
        card.add(VeilPanel.row("  INSTANT (+1gp):", String.format("%,d", f.buyInstant) + " gp  → fills in ~" + f.fillFast + " min", VeilPanel.GREEN));
        card.add(VeilPanel.bigRow("  STD (instabuy−1):", String.format("%,d", f.buyStd) + " gp  → fills in ~" + f.fillMins + " min", VeilPanel.GOLD));
        if (f.buyPatientSavings > 0) {
            card.add(VeilPanel.row("  PATIENT:", String.format("%,d", f.buyPatient) + " gp (~" + f.fillPatient + "m)", VeilPanel.AMBER));
            card.add(VeilPanel.row("    Patient saves:", "+" + VeilPanel.fmtGp(f.buyPatientSavings) + " ea = +" + VeilPanel.fmtGp(f.buyPatientSavingsTotal) + "/cycle", VeilPanel.AMBER));
        }
        card.add(Box.createVerticalStrut(2));

        // SELL PRICE WITH CONFIDENCE
        Color sellRiskColor = "LOW RISK".equals(f.sellRisk) ? VeilPanel.GREEN
            : "MEDIUM RISK".equals(f.sellRisk) ? VeilPanel.AMBER : VeilPanel.RED;
        String sellRiskLabel = f.sellRisk != null ? " [" + f.sellRisk + "]" : "";
        card.add(VeilPanel.bigRow("SELL at:", VeilPanel.fmtGp(f.sellPrice - 1) + " gp" + sellRiskLabel, sellRiskColor));

        // Sell confidence bar
        if (f.sellConfidence > 0) {
            JPanel scRow = new JPanel(new BorderLayout(4, 0));
            scRow.setBackground(VeilPanel.SURFACE);
            scRow.setAlignmentX(LEFT_ALIGNMENT);
            scRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
            JLabel scLbl = new JLabel("  Sell confidence:");
            scLbl.setForeground(VeilPanel.MUTED);
            scLbl.setFont(FontManager.getRunescapeSmallFont());
            JProgressBar scBar = new JProgressBar(0, 100);
            scBar.setValue(f.sellConfidence);
            scBar.setStringPainted(true);
            scBar.setString(f.sellConfidence + "/100");
            scBar.setBackground(VeilPanel.BG);
            scBar.setForeground(f.sellConfidence >= 75 ? VeilPanel.GREEN
                : f.sellConfidence >= 50 ? VeilPanel.AMBER : VeilPanel.RED);
            scBar.setBorderPainted(false);
            scBar.setFont(FontManager.getRunescapeSmallFont());
            scRow.add(scLbl, BorderLayout.WEST);
            scRow.add(scBar, BorderLayout.CENTER);
            card.add(scRow);
        }

        // Show risk reason if not low risk
        if (f.sellRiskReason != null && !f.sellRiskReason.isEmpty() && !"LOW RISK".equals(f.sellRisk)) {
            JLabel riskLbl = new JLabel("<html><div style=\'width:190\'>" + f.sellRiskReason + "</div></html>");
            riskLbl.setForeground(sellRiskColor);
            riskLbl.setFont(FontManager.getRunescapeSmallFont());
            riskLbl.setAlignmentX(LEFT_ALIGNMENT);
            card.add(riskLbl);
        }

        // Safe alternative price if high risk
        if (f.safeSellPrice > 0 && !"LOW RISK".equals(f.sellRisk)) {
            card.add(VeilPanel.bigRow("  SAFER SELL at:", VeilPanel.fmtGp(f.safeSellPrice) + " gp (lower risk)", VeilPanel.AMBER));
        }

        card.add(Box.createVerticalStrut(3));
        card.add(VeilPanel.row("Net profit ea:", "+" + VeilPanel.fmtGp(f.netMargin) + " gp  (" + String.format("%.1f%%", f.roi) + " ROI)", VeilPanel.GREEN));
        card.add(Box.createVerticalStrut(3));

        // PRICE PREDICTION
        if (f.pred2hrLow > 0 && f.pred2hrHigh > 0) {
            card.add(Box.createVerticalStrut(2));
            String trendIcon = "UP".equals(f.predTrend) ? "↑" : "DOWN".equals(f.predTrend) ? "↓" : "→";
            Color trendColor = "UP".equals(f.predTrend) ? VeilPanel.GREEN
                : "DOWN".equals(f.predTrend) ? VeilPanel.RED : VeilPanel.MUTED;
            card.add(VeilPanel.bold("PRICE PREDICTION " + trendIcon, trendColor));
            card.add(VeilPanel.row("  1hr range:",
                VeilPanel.fmtGp(f.pred1hrLow) + "—" + VeilPanel.fmtGp(f.pred1hrHigh),
                VeilPanel.MUTED));
            card.add(VeilPanel.row("  2hr range:",
                VeilPanel.fmtGp(f.pred2hrLow) + "—" + VeilPanel.fmtGp(f.pred2hrHigh),
                VeilPanel.MUTED));
            if (f.pred4hrLow > 0)
                card.add(VeilPanel.row("  4hr range:",
                    VeilPanel.fmtGp(f.pred4hrLow) + "—" + VeilPanel.fmtGp(f.pred4hrHigh),
                    VeilPanel.MUTED));
            if (f.predVolatilityPct > 0)
                card.add(VeilPanel.row("  Volatility:", String.format("±%.1f%%/hr", f.predVolatilityPct),
                    f.predVolatilityPct < 1.0 ? VeilPanel.GREEN : VeilPanel.AMBER));
            if (f.bestTimeToSell != null && !f.bestTimeToSell.isEmpty())
                card.add(VeilPanel.row("  Best sell window:", f.bestTimeToSell, VeilPanel.GOLD));
            if (f.timeOfDayBias != 0)
                card.add(VeilPanel.row("  This hour's bias:",
                    String.format("%+.2f%%", f.timeOfDayBias) + (f.timeOfDayBias > 0 ? " (historically positive)" : " (historically negative)"),
                    f.timeOfDayBias > 0 ? VeilPanel.GREEN : VeilPanel.RED));
        }
        card.add(Box.createVerticalStrut(3));

        // ── HOW MANY TO BUY ─────────────────────────────────
        card.add(Box.createVerticalStrut(2));
        if (coins > 0) {
            long canAfford   = Math.min(coins / Math.max(f.buyPrice, 1), f.buyLimit);
            long totalCost   = canAfford * f.buyPrice;
            long totalProfit = canAfford * f.netMargin;
            long leftover    = coins - totalCost;

            card.add(VeilPanel.bigRow("► BUY exactly:", canAfford + "× at " + VeilPanel.fmtGp(f.buyPrice) + " ea", VeilPanel.BLUE));
            card.add(VeilPanel.row("  Total cost:", VeilPanel.fmtGp(totalCost) + " (" + VeilPanel.fmtGp(leftover) + " left)", VeilPanel.MUTED));
            card.add(VeilPanel.bigRow("► SELL at:", VeilPanel.fmtGp(f.sellPrice - 1) + " gp ea", VeilPanel.GREEN));
            card.add(VeilPanel.bigRow("  Total profit:", VeilPanel.fmtSigned(totalProfit) + " gp on this trade", totalProfit > 0 ? VeilPanel.GREEN : VeilPanel.RED));
            card.add(Box.createVerticalStrut(3));

            // Step by step instructions
            card.add(stepLabel("HOW TO DO THIS FLIP:"));
            card.add(stepLabel("  1. GE → Buy → '" + f.itemName + "'"));
            card.add(stepLabel("  2. Qty: " + canAfford + "×"));
            card.add(stepLabel("     INSTANT: " + String.format("%,d", f.buyInstant) + " gp (+1) → ~" + f.fillFast + "min"));
            card.add(stepLabel("     STANDARD: " + String.format("%,d", f.buyStd) + " gp → ~" + f.fillMins + "min"));
            if (f.buyPatientSavings > 0)
                card.add(stepLabel("     PATIENT: " + String.format("%,d", f.buyPatient) + " gp → ~" + f.fillPatient + "min, save +" + VeilPanel.fmtGp(f.buyPatientSavingsTotal)));
            card.add(stepLabel("  3. Wait ~" + f.fillMins + " min to fill"));
            card.add(stepLabel("  4. Sell " + canAfford + "× at " + String.format("%,d", f.sellPrice - 1) + " gp"));
            card.add(stepLabel("  5. Profit: +" + VeilPanel.fmtGp(totalProfit) + " gp — repeat!"));
        } else {
            card.add(stepLabel("Open inv to see how many you can afford"));
            card.add(VeilPanel.row("  Full limit cost:", VeilPanel.fmtGp((long)f.buyPrice * f.buyLimit) + " gp", VeilPanel.MUTED));
            card.add(VeilPanel.row("  Full limit profit:", "+" + VeilPanel.fmtGp((long)f.netMargin * f.buyLimit), VeilPanel.GREEN));
        }
        card.add(Box.createVerticalStrut(3));

        // ── CONFIDENCE BAR ────────────────────────────────────
        JPanel confRow = new JPanel(new BorderLayout(6, 0));
        confRow.setBackground(VeilPanel.SURFACE);
        confRow.setAlignmentX(LEFT_ALIGNMENT);
        confRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        JLabel confLbl = new JLabel("Confidence:");
        confLbl.setForeground(VeilPanel.MUTED);
        confLbl.setFont(FontManager.getRunescapeSmallFont());
        JProgressBar confBar = new JProgressBar(0, 100);
        confBar.setValue(f.confidence);
        confBar.setStringPainted(true);
        confBar.setString(f.confidence + "/100  " + (f.spreadFlag != null ? "[" + f.spreadFlag + "]" : ""));
        confBar.setBackground(VeilPanel.BG);
        confBar.setForeground(f.confidence >= 70 ? VeilPanel.GREEN
            : f.confidence >= 50 ? VeilPanel.AMBER : VeilPanel.RED);
        confBar.setBorderPainted(false);
        confBar.setFont(FontManager.getRunescapeSmallFont());
        confBar.setMaximumSize(new Dimension(120, 16));
        confRow.add(confLbl, BorderLayout.WEST);
        confRow.add(confBar, BorderLayout.CENTER);
        card.add(confRow);
        card.add(Box.createVerticalStrut(3));

        // ── GP/HR: REALISTIC vs THEORETICAL ──────────────────
        card.add(VeilPanel.bigRow("GP/hr realistic:", VeilPanel.fmtGp(f.score) + "/hr", VeilPanel.GOLD));
        if (f.gpHrTheoretical > f.score)
            card.add(VeilPanel.row("  theoretical max:", VeilPanel.fmtGp(f.gpHrTheoretical) + "/hr", VeilPanel.MUTED));
        card.add(Box.createVerticalStrut(2));

        // ── THREE FILL TIME SCENARIOS ─────────────────────────
        card.add(VeilPanel.bold("Fill time by strategy:", VeilPanel.TEXT));
        card.add(VeilPanel.row("  FAST (at instabuy):", f.fillFast + " min — fills quickest, less margin", VeilPanel.GREEN));
        card.add(VeilPanel.row("  STD (instabuy−1):", f.fillMins + " min — balanced", VeilPanel.GOLD));
        card.add(VeilPanel.row("  SLOW (patient):", f.fillPatient + " min — max margin, slowest", VeilPanel.AMBER));
        card.add(Box.createVerticalStrut(2));

        card.add(VeilPanel.row("Buy limit:", f.buyLimit + "×/4hr", VeilPanel.MUTED));
        card.add(VeilPanel.row("Vol/hr (24h avg):", f.hourVol + "/hr",
            f.hourVol > f.buyLimit * 4 ? VeilPanel.GREEN : VeilPanel.AMBER));
        card.add(VeilPanel.row("Market pressure:", String.format("%.1f×", f.pressure) + "  momentum: " + String.format("%+.1f%%", f.momentum),
            f.pressure >= 1.2 ? VeilPanel.GREEN : f.pressure < 0.8 ? VeilPanel.RED : VeilPanel.MUTED));
        card.add(VeilPanel.row("Price freshness:", f.freshness + "%  " + (f.freshness >= 80 ? "fresh" : f.freshness >= 50 ? "getting old" : "stale — verify"),
            f.freshness >= 80 ? VeilPanel.GREEN : f.freshness >= 50 ? VeilPanel.AMBER : VeilPanel.RED));

        // Plain English advice
        // Signal explanation
        String advice = "ENTER".equals(f.signal)
            ? "More buyers than sellers — good time to start this flip"
            : "EXIT".equals(f.signal)
            ? "More sellers than buyers — wait or avoid for now"
            : "Balanced market — standard conditions";
        card.add(VeilPanel.row("Advice:", advice, VeilPanel.signalColor(f.signal)));

        // Margin context — tells you WHY the margin is what it is
        if (f.marginContext != null && !f.marginContext.isEmpty()) {
            card.add(VeilPanel.row("Context:", f.marginContext, VeilPanel.MUTED));
        }

        // Show if alch is better than selling on GE
        if (f.alchProfit > f.netMargin && f.alchProfit > 500) {
            card.add(VeilPanel.row("⚗ Alch instead?", "+" + VeilPanel.fmtGp(f.alchProfit) + " vs +" + VeilPanel.fmtGp(f.netMargin) + " GE", VeilPanel.GOLD));
        }

        // Wiki link
        card.add(Box.createVerticalStrut(4));
        JLabel wiki = new JLabel("<html><u>Price chart on wiki ↗</u></html>");
        wiki.setForeground(VeilPanel.BLUE);
        wiki.setFont(FontManager.getRunescapeSmallFont());
        wiki.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        wiki.setAlignmentX(LEFT_ALIGNMENT);
        final int iid = f.itemId;
        wiki.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { LinkBrowser.browse("https://prices.runescape.wiki/osrs/item/" + iid); }
        });
        card.add(wiki);
        return card;
    }

    private JLabel badge(String text, Color color)
    {
        JLabel l = new JLabel(" " + text + " ");
        l.setForeground(color);
        l.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        l.setBackground(color.darker().darker());
        l.setOpaque(true);
        return l;
    }
}


// ════════════════════════════════════════════════════════════
// TAB 3: TRADES — auto-tracked + manual entry
// ════════════════════════════════════════════════════════════
class TradesTab extends JPanel
{
    private final VeilPlugin plugin;
    private final VeilPanel  veil;
    private JPanel autoPanel, manualPanel;
    private JTextField itemField, buyField, qtyField;
    private final List<ManualTrade> manual = new ArrayList<>();

    static class ManualTrade { String name; int buy, qty;
        ManualTrade(String n, int b, int q) { name=n; buy=b; qty=q; } }

    TradesTab(VeilPlugin plugin, VeilPanel veil) {
        this.plugin = plugin; this.veil = veil;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        build();
    }

    private void build()
    {
        add(VeilPanel.bold("AUTO-TRACKED GE", VeilPanel.GOLD));
        add(Box.createVerticalStrut(4));
        autoPanel = new JPanel();
        autoPanel.setLayout(new BoxLayout(autoPanel, BoxLayout.Y_AXIS));
        autoPanel.setBackground(VeilPanel.BG);
        add(autoPanel);
        add(Box.createVerticalStrut(10));

        // Manual entry
        JPanel addCard = VeilPanel.card("ADD MANUAL TRADE");
        addCard.setAlignmentX(LEFT_ALIGNMENT);
        addCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        itemField = VeilPanel.field("Item name");
        itemField.setAlignmentX(LEFT_ALIGNMENT);
        itemField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        buyField  = VeilPanel.field("Buy price (e.g. 1.1m or 1100000)");
        buyField.setAlignmentX(LEFT_ALIGNMENT);
        buyField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        qtyField  = VeilPanel.field("Quantity bought");
        qtyField.setAlignmentX(LEFT_ALIGNMENT);
        qtyField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JButton addBtn = VeilPanel.btn("+ Add Trade", VeilPanel.GOLD, VeilPanel.BG);
        addBtn.addActionListener(e -> addManual());

        addCard.add(itemField);
        addCard.add(Box.createVerticalStrut(4));
        addCard.add(buyField);
        addCard.add(Box.createVerticalStrut(4));
        addCard.add(qtyField);
        addCard.add(Box.createVerticalStrut(6));
        addCard.add(addBtn);
        add(addCard);
        add(Box.createVerticalStrut(8));

        add(VeilPanel.bold("MANUAL TRADES", VeilPanel.GOLD));
        add(Box.createVerticalStrut(4));
        manualPanel = new JPanel();
        manualPanel.setLayout(new BoxLayout(manualPanel, BoxLayout.Y_AXIS));
        manualPanel.setBackground(VeilPanel.BG);
        add(manualPanel);
    }

    private void addManual()
    {
        String name = itemField.getText().trim();
        if (name.isEmpty()) return;
        int buy = parseGp(buyField.getText().trim());
        int qty = parseQty(qtyField.getText().trim());
        if (buy <= 0 || qty <= 0) return;
        manual.add(0, new ManualTrade(name, buy, qty));
        itemField.setText(""); buyField.setText(""); qtyField.setText("");
        refresh();
    }

    private static int parseGp(String s) {
        try { s=s.toLowerCase().replaceAll(",","");
            if(s.endsWith("m")) return (int)(Double.parseDouble(s.replace("m",""))*1_000_000);
            if(s.endsWith("k")) return (int)(Double.parseDouble(s.replace("k",""))*1_000);
            return Integer.parseInt(s); } catch(Exception e){ return 0; }
    }
    private static int parseQty(String s) { try { return Integer.parseInt(s.trim()); } catch(Exception e){ return 1; } }

    void refresh()
    {
        // Auto
        autoPanel.removeAll();
        List<TradeRecord> active = plugin.getActiveOffers();
        List<FlipSignal> flips = plugin.getCachedFlips();
        Map<Integer,FlipSignal> fm = new HashMap<>();
        for (FlipSignal f : flips) fm.put(f.itemId, f);

        if (active.isEmpty()) autoPanel.add(VeilPanel.muted("No active GE offers — go flip something!"));
        for (TradeRecord rec : active) {
            autoPanel.add(buildAutoCard(rec, fm.get(rec.itemId)));
            autoPanel.add(Box.createVerticalStrut(4));
        }

        // Completed
        List<TradeRecord> done = plugin.getSessionTrades();
        if (!done.isEmpty()) {
            autoPanel.add(Box.createVerticalStrut(4));
            autoPanel.add(VeilPanel.bold("COMPLETED THIS SESSION", VeilPanel.GOLD));
            autoPanel.add(Box.createVerticalStrut(2));
            int totalProfit = 0;
            for (TradeRecord r : done) {
                totalProfit += r.profitGp;
                JPanel row = new JPanel(new BorderLayout(4,0));
                row.setBackground(VeilPanel.SURFACE);
                row.setBorder(new EmptyBorder(4, 8, 4, 8));
                row.setAlignmentX(LEFT_ALIGNMENT);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
                JLabel l = new JLabel((r.isBuy?"[B] ":"[S] ") + VeilPanel.clip(r.itemName, 16) + " ×" + r.quantityTraded);
                l.setForeground(r.isBuy ? VeilPanel.GREEN : VeilPanel.GOLD);
                l.setFont(FontManager.getRunescapeSmallFont());
                JLabel rv = new JLabel(r.profitGp == 0 ? "@"+VeilPanel.fmtGp(r.pricePerUnit) : VeilPanel.fmtSigned(r.profitGp));
                rv.setForeground(r.profitGp > 0 ? VeilPanel.GREEN : r.profitGp < 0 ? VeilPanel.RED : VeilPanel.MUTED);
                rv.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
                row.add(l, BorderLayout.WEST);
                row.add(rv, BorderLayout.EAST);
                autoPanel.add(row);
            }
            autoPanel.add(Box.createVerticalStrut(4));
            autoPanel.add(VeilPanel.bigRow("Session total:", VeilPanel.fmtSigned(totalProfit) + " gp", totalProfit >= 0 ? VeilPanel.GREEN : VeilPanel.RED));
        }

        // Manual
        manualPanel.removeAll();
        Map<String,FlipSignal> fn = new HashMap<>();
        for (FlipSignal f : flips) fn.put(f.itemName.toLowerCase(), f);
        for (int i = 0; i < manual.size(); i++) {
            manualPanel.add(buildManualCard(manual.get(i), fn.get(manual.get(i).name.toLowerCase()), i));
            manualPanel.add(Box.createVerticalStrut(4));
        }

        autoPanel.revalidate(); autoPanel.repaint();
        manualPanel.revalidate(); manualPanel.repaint();
    }

    private JPanel buildAutoCard(TradeRecord rec, FlipSignal sig)
    {
        JPanel card = VeilPanel.card(null);
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        card.add(VeilPanel.bigRow(rec.itemName, rec.isBuy ? "BUYING" : "SELLING", rec.isBuy ? VeilPanel.GREEN : VeilPanel.GOLD));
        card.add(VeilPanel.row("Your price:", VeilPanel.fmtGp(rec.pricePerUnit) + " gp ea", VeilPanel.MUTED));
        int pct = rec.quantityOffered > 0 ? rec.quantityTraded * 100 / rec.quantityOffered : 0;
        card.add(VeilPanel.row("Filled:", rec.quantityTraded + "/" + rec.quantityOffered + " (" + pct + "%)", VeilPanel.TEXT));
        if (rec.isBuy && sig != null) {
            int sa = sig.sellPrice - 1;
            int tx = Math.min(5_000_000, Math.max(1,(int)(sa*0.01)));
            int pr = (sa - rec.pricePerUnit - tx) * Math.max(rec.quantityTraded, 1);
            card.add(Box.createVerticalStrut(3));
            card.add(VeilPanel.bigRow("→ SELL AT:", VeilPanel.fmtGp(sa) + " gp ea", VeilPanel.GREEN));
            card.add(VeilPanel.bigRow("  Total profit:", VeilPanel.fmtSigned(pr) + " gp", pr >= 0 ? VeilPanel.GREEN : VeilPanel.RED));
        }
        return card;
    }

    private JPanel buildManualCard(ManualTrade t, FlipSignal sig, int idx)
    {
        JPanel card = VeilPanel.card(null);
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        card.add(VeilPanel.bigRow(t.name, t.qty + "× bought", VeilPanel.TEXT));
        card.add(VeilPanel.row("Bought at:", VeilPanel.fmtGp(t.buy) + " gp ea", VeilPanel.MUTED));
        card.add(VeilPanel.row("Total cost:", VeilPanel.fmtGp((long)t.buy * t.qty) + " gp", VeilPanel.MUTED));
        if (sig != null) {
            int sa = sig.sellPrice - 1;
            int tx = Math.min(5_000_000, Math.max(1,(int)(sa*0.01)));
            int pea = sa - t.buy - tx;
            int tot = pea * t.qty;
            card.add(Box.createVerticalStrut(3));
            card.add(VeilPanel.bigRow("→ SELL AT:", VeilPanel.fmtGp(sa) + " gp ea", VeilPanel.GREEN));
            card.add(VeilPanel.row("Profit ea:", VeilPanel.fmtSigned(pea) + " gp", pea >= 0 ? VeilPanel.GREEN : VeilPanel.RED));
            card.add(VeilPanel.bigRow("Total profit:", VeilPanel.fmtSigned(tot) + " gp", tot >= 0 ? VeilPanel.GREEN : VeilPanel.RED));
        } else {
            card.add(VeilPanel.muted("Not in flip database — search for it in Flips tab"));
        }
        JButton rm = VeilPanel.btn("Remove", VeilPanel.SURFACE2, VeilPanel.RED);
        rm.addActionListener(e -> { manual.remove(idx); refresh(); });
        card.add(Box.createVerticalStrut(3));
        card.add(rm);
        return card;
    }
}


// ════════════════════════════════════════════════════════════
// TAB 4: PORTFOLIO
// ════════════════════════════════════════════════════════════
class PortfolioTab extends JPanel
{
    private final VeilPlugin plugin;
    private final VeilPanel  veil;
    private JPanel content;

    PortfolioTab(VeilPlugin plugin, VeilPanel veil) {
        this.plugin=plugin; this.veil=veil;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8,8,8,8));
        content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(VeilPanel.BG);
        add(content);
    }

    void refresh()
    {
        long coins = plugin.getCoinStack();
        List<FlipSignal> flips = plugin.getCachedFlips();
        content.removeAll();

        // Coin card
        JPanel coinCard = VeilPanel.card("YOUR BANKROLL");
        coinCard.setAlignmentX(LEFT_ALIGNMENT);
        coinCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        JLabel coinLbl = new JLabel(coins > 0 ? VeilPanel.fmtGp(coins) + " GP" : "Open inventory to see coin stack");
        coinLbl.setForeground(coins > 0 ? VeilPanel.GOLD : VeilPanel.MUTED);
        coinLbl.setFont(FontManager.getRunescapeBoldFont().deriveFont(18f));
        coinLbl.setAlignmentX(LEFT_ALIGNMENT);
        coinCard.add(coinLbl);
        if (coins > 0) coinCard.add(VeilPanel.muted("Live from your inventory"));
        content.add(coinCard);
        content.add(Box.createVerticalStrut(8));

        if (coins > 0 && !flips.isEmpty()) {
            JPanel afford = VeilPanel.card("WHAT TO FLIP NOW");
            afford.setAlignmentX(LEFT_ALIGNMENT);
            afford.setMaximumSize(new Dimension(Integer.MAX_VALUE, 500));

            long totalExpected = 0;
            int shown = 0;
            for (FlipSignal f : flips) {
                if (f.buyPrice > coins || shown >= 5) continue;
                long qty    = Math.min(coins / Math.max(f.buyPrice, 1), f.buyLimit);
                long cost   = qty * f.buyPrice;
                long profit = qty * f.netMargin;
                totalExpected += profit;
                afford.add(VeilPanel.bigRow(VeilPanel.clip(f.itemName, 16), f.grade + "·" + f.signal, VeilPanel.gradeColor(f.grade)));
                afford.add(VeilPanel.row("  Buy " + qty + "× @", VeilPanel.fmtGp(f.buyPrice) + " = " + VeilPanel.fmtGp(cost), VeilPanel.MUTED));
                afford.add(VeilPanel.row("  Sell @", "→ +" + VeilPanel.fmtGp(profit) + " profit", VeilPanel.GREEN));
                afford.add(Box.createVerticalStrut(5));
                shown++;
            }
            if (shown > 0) {
                afford.add(VeilPanel.bigRow("Expected total:", "+" + VeilPanel.fmtGp(totalExpected) + " gp", VeilPanel.GREEN));
                afford.add(VeilPanel.row("ROI:", String.format("%.2f%%", totalExpected * 100.0 / coins), VeilPanel.GOLD));
            } else {
                afford.add(VeilPanel.muted("No affordable flips found"));
            }
            content.add(afford);
            content.add(Box.createVerticalStrut(8));

            if (totalExpected > 0 && shown > 0) {
                JPanel comp = VeilPanel.card("COMPOUNDING");
                comp.setAlignmentX(LEFT_ALIGNMENT);
                comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
                double roi = totalExpected / (double) coins;
                double daily = Math.pow(1 + roi, 3) - 1;
                comp.add(VeilPanel.row("After 7 days:",  VeilPanel.fmtGp((long)(coins * Math.pow(1+daily, 7))), VeilPanel.GREEN));
                comp.add(VeilPanel.row("After 30 days:", VeilPanel.fmtGp((long)(coins * Math.pow(1+daily, 30))), VeilPanel.GOLD));
                comp.add(VeilPanel.row("After 90 days:", VeilPanel.fmtGp((long)(coins * Math.pow(1+daily, 90))), VeilPanel.PURPLE));
                comp.add(Box.createVerticalStrut(4));
                comp.add(VeilPanel.muted("* Assumes consistent S/A grade flips"));
                content.add(comp);
            }
        }
        content.revalidate(); content.repaint();
    }
}


// ════════════════════════════════════════════════════════════
// TAB 5: INTEL
// ════════════════════════════════════════════════════════════
class IntelTab extends JPanel
{
    private final VeilPlugin plugin;
    private final VeilPanel  veil;
    private JPanel content;

    IntelTab(VeilPlugin plugin, VeilPanel veil) {
        this.plugin=plugin; this.veil=veil;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8,8,8,8));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(VeilPanel.BG);
        topRow.setAlignmentX(LEFT_ALIGNMENT);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel hdr = new JLabel("MARKET INTELLIGENCE");
        hdr.setForeground(VeilPanel.GOLD);
        hdr.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        JButton refreshBtn = VeilPanel.btn("↺ Refresh", VeilPanel.SURFACE2, VeilPanel.GOLD);
        refreshBtn.addActionListener(e -> refresh());
        topRow.add(hdr, BorderLayout.WEST);
        topRow.add(refreshBtn, BorderLayout.EAST);
        add(topRow);
        add(Box.createVerticalStrut(6));

        content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(VeilPanel.BG);
        add(content);
    }

    void refresh()
    {
        MarketIntelligence intel = plugin.getMarketIntelligence();
        SwingUtilities.invokeLater(() -> {
            content.removeAll();
            if (intel == null) { content.add(VeilPanel.muted("Loading... updates every 60 seconds")); content.revalidate(); return; }
            buildContent(intel);
            content.revalidate(); content.repaint();
        });
    }

    private void buildContent(MarketIntelligence intel)
    {
        // Session plan
        if (intel.sessionPlan != null) {
            JPanel plan = VeilPanel.card("YOUR PLAN");
            plan.setAlignmentX(LEFT_ALIGNMENT);
            plan.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
            for (String line : intel.sessionPlan.split("\n")) {
                JLabel l = new JLabel("<html><div style='width:185'>" + line.replace("★","⚡").replace("🔥","★") + "</div></html>");
                l.setForeground(line.startsWith("★") ? VeilPanel.GOLD : line.startsWith("⚡") ? VeilPanel.RED : line.startsWith("▸") ? VeilPanel.TEXT : VeilPanel.MUTED);
                l.setFont(line.length() < 25 ? FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD) : FontManager.getRunescapeSmallFont());
                l.setAlignmentX(LEFT_ALIGNMENT);
                plan.add(l);
                if (line.isEmpty()) plan.add(Box.createVerticalStrut(3));
            }
            content.add(plan);
            content.add(Box.createVerticalStrut(6));
        }

        // Time context
        if (intel.timeContext != null) {
            JPanel time = VeilPanel.card(null);
            time.setAlignmentX(LEFT_ALIGNMENT);
            time.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
            Color tc = "PEAK HOURS".equals(intel.timeContext.session) ? VeilPanel.GREEN
                : "OFF-PEAK".equals(intel.timeContext.session) ? VeilPanel.AMBER : VeilPanel.GOLD;
            time.add(VeilPanel.bold(intel.timeContext.session + " (UTC " + intel.timeContext.utcHour + ":xx)", tc));
            if (intel.timeContext.minutesUntilPeak > 0) {
                int h = intel.timeContext.minutesUntilPeak/60, m = intel.timeContext.minutesUntilPeak%60;
                time.add(VeilPanel.muted("Peak in " + (h>0?h+"h ":"") + m + "m"));
            }
            time.add(VeilPanel.row("Flip now:", VeilPanel.clip(intel.timeContext.bestCategories, 20), VeilPanel.GREEN));
            if (intel.timeContext.avoidCategories != null && !intel.timeContext.avoidCategories.isEmpty())
                time.add(VeilPanel.row("Avoid:", VeilPanel.clip(intel.timeContext.avoidCategories, 20), VeilPanel.RED));
            content.add(time);
            content.add(Box.createVerticalStrut(6));
        }

        // Heat map
        JPanel heat = VeilPanel.card("MARKET HEAT MAP");
        heat.setAlignmentX(LEFT_ALIGNMENT);
        heat.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
        for (MarketIntelligence.CategoryHeat h : intel.categoryHeat) {
            Color cc = h.isHot ? VeilPanel.GREEN : h.avgPressure > 1.0 ? VeilPanel.GOLD : VeilPanel.MUTED;
            heat.add(VeilPanel.row((h.isHot?"🔥 ":"   ") + h.category,
                String.format("%.1f× %+.1f%%", h.avgPressure, h.avgMomentum), cc));
        }
        content.add(heat);
        content.add(Box.createVerticalStrut(6));

        // Supply shocks
        if (!intel.supplyShocks.isEmpty()) {
            JPanel shock = VeilPanel.card("ACCUMULATION ALERTS");
            shock.setAlignmentX(LEFT_ALIGNMENT);
            shock.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
            for (MarketIntelligence.SupplyShock s : intel.supplyShocks.subList(0, Math.min(5, intel.supplyShocks.size()))) {
                shock.add(VeilPanel.bigRow(s.itemName, String.format("%.0f×", s.pressure) + " pressure", VeilPanel.RED));
                shock.add(VeilPanel.muted("  " + s.alert));
                shock.add(VeilPanel.row("  Buy @", VeilPanel.fmtGp(s.buyPrice) + "  Margin +" + VeilPanel.fmtGp(s.netMargin), VeilPanel.GREEN));
                shock.add(Box.createVerticalStrut(3));
            }
            content.add(shock);
            content.add(Box.createVerticalStrut(6));
        }

        // Thin market gems
        if (!intel.thinMarketGems.isEmpty()) {
            JPanel gems = VeilPanel.card("THIN MARKET GEMS");
            gems.setAlignmentX(LEFT_ALIGNMENT);
            gems.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
            gems.add(VeilPanel.muted("Buy limit ≤10. Bots ignore these. You capture 90%+ of spread."));
            gems.add(Box.createVerticalStrut(5));
            for (MarketIntelligence.ThinMarketGem g : intel.thinMarketGems.subList(0, Math.min(8, intel.thinMarketGems.size()))) {
                gems.add(VeilPanel.bigRow(g.itemName, "Limit: " + g.buyLimit, VeilPanel.PURPLE));
                gems.add(VeilPanel.row("  Buy @", VeilPanel.fmtGp(g.buyPrice), VeilPanel.MUTED));
                gems.add(VeilPanel.row("  Sell @", VeilPanel.fmtGp(g.sellPrice - 1), VeilPanel.GREEN));
                gems.add(VeilPanel.row("  4hr cycle:", "+" + VeilPanel.fmtGp((long)g.netMargin * g.buyLimit), VeilPanel.GREEN));
                gems.add(VeilPanel.muted("  No bots — " + g.hourVol + " trades/hr, not worth automating"));
                final int iid = g.itemId;
                JLabel wl = new JLabel("<html><u>Chart ↗</u></html>");
                wl.setForeground(VeilPanel.BLUE); wl.setFont(FontManager.getRunescapeSmallFont());
                wl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                wl.setAlignmentX(LEFT_ALIGNMENT);
                wl.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { LinkBrowser.browse("https://prices.runescape.wiki/osrs/item/"+iid); }});
                gems.add(wl);
                gems.add(Box.createVerticalStrut(6));
            }
            content.add(gems);
        }

        // Price spikes
        if (!intel.priceSpikes.isEmpty()) {
            JPanel spikes = VeilPanel.card("PRICE SPIKES (30min)");
            spikes.setAlignmentX(LEFT_ALIGNMENT);
            spikes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
            for (MarketIntelligence.PriceSpike s : intel.priceSpikes) {
                spikes.add(VeilPanel.bigRow(s.itemName, String.format("%+.1f%%", s.changePct), s.changePct > 0 ? VeilPanel.GREEN : VeilPanel.RED));
                spikes.add(VeilPanel.muted("  " + s.possibleCause));
            }
            content.add(spikes);
        }
    }
}


// ════════════════════════════════════════════════════════════
// TAB 6: SKILLS — all skills, real-time XP from StatChanged
// ════════════════════════════════════════════════════════════
class SkillsTab extends JPanel
{
    private final VeilPlugin plugin;
    private final VeilPanel  veil;
    private JPanel content;

    private static final Skill[] SKILL_ORDER = {
        Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.PRAYER,
        Skill.MAGIC, Skill.RUNECRAFT, Skill.CONSTRUCTION,
        Skill.HITPOINTS, Skill.AGILITY, Skill.HERBLORE, Skill.THIEVING,
        Skill.CRAFTING, Skill.FLETCHING, Skill.SLAYER, Skill.HUNTER,
        Skill.MINING, Skill.SMITHING, Skill.FISHING, Skill.COOKING,
        Skill.FIREMAKING, Skill.WOODCUTTING, Skill.FARMING, Skill.OVERALL
    };

    SkillsTab(VeilPlugin plugin, VeilPanel veil) {
        this.plugin=plugin; this.veil=veil;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8,8,8,8));
        content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(VeilPanel.BG);
        add(content);
    }

    void refresh()
    {
        Map<Skill, Integer> gained = plugin.getXpGained();
        long ms  = System.currentTimeMillis() - plugin.getSessionStartMs();
        double h = ms / 3_600_000.0;

        content.removeAll();

        // Session totals
        int total = gained.values().stream().mapToInt(Integer::intValue).sum();
        if (total > 0) {
            JPanel sumCard = VeilPanel.card("SESSION XP");
            sumCard.setAlignmentX(LEFT_ALIGNMENT);
            sumCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
            sumCard.add(VeilPanel.bigRow("Total XP gained:", VeilPanel.fmtXp(total), VeilPanel.GOLD));
            if (h > 0.02)
                sumCard.add(VeilPanel.row("XP/hr:", VeilPanel.fmtXp((int)(total/h)), VeilPanel.GREEN));
            sumCard.add(VeilPanel.muted("Real-time · no API polling"));
            content.add(sumCard);
            content.add(Box.createVerticalStrut(6));
        }

        if (gained.isEmpty()) {
            content.add(VeilPanel.muted("No XP gained this session. Train a skill!"));
            return;
        }

        // Per-skill breakdown
        JPanel skillCard = VeilPanel.card("XP BY SKILL (active only)");
        skillCard.setAlignmentX(LEFT_ALIGNMENT);
        skillCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));

        for (Skill skill : SKILL_ORDER) {
            if (skill == Skill.OVERALL) continue;
            Integer g = gained.get(skill);
            if (g == null || g <= 0) continue;
            int rate = h > 0.02 ? (int)(g / h) : 0;
            skillCard.add(VeilPanel.row(VeilPanel.clip(skill.getName(), 14), "+" + VeilPanel.fmtXp(g) + (rate > 0 ? " (" + VeilPanel.fmtXp(rate) + "/hr)" : ""), VeilPanel.GREEN));
        }

        content.add(skillCard);
        content.revalidate(); content.repaint();
    }
}


// ════════════════════════════════════════════════════════════
// TAB 7: TRACKER — boss KC, drop progress, loot log, death risk
// ════════════════════════════════════════════════════════════
class TrackerTab extends JPanel
{
    private final VeilPlugin plugin;
    private final VeilPanel  veil;
    private JPanel content;

    TrackerTab(VeilPlugin plugin, VeilPanel veil) {
        this.plugin=plugin; this.veil=veil;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8,8,8,8));
        content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(VeilPanel.BG);
        add(content);
    }

    void refresh()
    {
        content.removeAll();

        // Death risk
        EquipmentState eq = plugin.getEquipmentState();
        if (eq == null || eq.totalValue == 0) {
            JPanel ph = VeilPanel.card("DEATH RISK");
            ph.setAlignmentX(LEFT_ALIGNMENT);
            ph.add(VeilPanel.muted("Equip gear and open inventory to track death risk."));
            content.add(ph);
            content.add(Box.createVerticalStrut(6));
        } else {
            JPanel dr = VeilPanel.card("DEATH RISK");
            dr.setAlignmentX(LEFT_ALIGNMENT);
            dr.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
            dr.add(VeilPanel.bigRow("Total gear value:", VeilPanel.fmtGp(eq.totalValue), VeilPanel.GOLD));
            dr.add(VeilPanel.row("Protected (top 3):", VeilPanel.fmtGp(eq.protectedValue), VeilPanel.GREEN));
            dr.add(VeilPanel.bigRow("AT RISK on death:", VeilPanel.fmtGp(eq.atRiskValue), eq.atRiskValue > 5_000_000 ? VeilPanel.RED : VeilPanel.AMBER));
            if (!eq.slots.isEmpty()) {
                dr.add(Box.createVerticalStrut(4));
                List<EquipmentState.EquipSlot> sorted = new ArrayList<>(eq.slots);
                sorted.sort((a,b) -> Integer.compare(b.geValue, a.geValue));
                for (EquipmentState.EquipSlot s : sorted.subList(0, Math.min(5, sorted.size()))) {
                    dr.add(VeilPanel.row("  " + s.itemName, VeilPanel.fmtGp(s.geValue), VeilPanel.MUTED));
                }
            }
            content.add(dr);
            content.add(Box.createVerticalStrut(6));
        }

        // Drop progress
        BossDropProgress drops = plugin.getDropProgress();
        if (drops != null && !drops.bosses.isEmpty()) {
            JPanel dp = VeilPanel.card("DROP PROBABILITY");
            dp.setAlignmentX(LEFT_ALIGNMENT);
            dp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
            dp.add(VeilPanel.muted("Chance you've seen each drop by now, based on KC"));
            dp.add(Box.createVerticalStrut(4));
            for (Map.Entry<String, List<BossDropProgress.DropEntry>> e : drops.bosses.entrySet()) {
                dp.add(VeilPanel.bold(e.getKey(), VeilPanel.GOLD));
                for (BossDropProgress.DropEntry d : e.getValue()) {
                    Color c = d.isDry ? VeilPanel.RED : d.probability > 0.7 ? VeilPanel.AMBER : VeilPanel.GREEN;
                    dp.add(VeilPanel.row("  " + d.itemName + " (1/" + d.dropRate + ")",
                        d.pctStr + " @ " + d.kc + " KC" + (d.isDry ? " ⚠ DRY" : ""), c));
                }
                dp.add(Box.createVerticalStrut(4));
            }
            content.add(dp);
            content.add(Box.createVerticalStrut(6));
        }

        // Loot log
        List<LootRecord> loot = plugin.getSessionLoot();
        if (!loot.isEmpty()) {
            JPanel ll = VeilPanel.card("LOOT LOG (auto-tracked)");
            ll.setAlignmentX(LEFT_ALIGNMENT);
            ll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
            ll.add(VeilPanel.bigRow("Session loot total:", "+" + VeilPanel.fmtGp(plugin.getSessionLootGp()), VeilPanel.GREEN));
            ll.add(Box.createVerticalStrut(4));
            for (LootRecord r : loot.subList(0, Math.min(15, loot.size()))) {
                ll.add(VeilPanel.row("Drop", "+" + VeilPanel.fmtGp(r.totalGp) + " (" + r.items.size() + " items)", VeilPanel.GREEN));
            }
            content.add(ll);
        } else {
            content.add(VeilPanel.muted("Kill something — loot auto-tracks via inventory diff"));
        }

        // Slayer advisor
        SlayerState sl = plugin.getSlayerState();
        if (sl.hasTask()) {
            content.add(Box.createVerticalStrut(6));
            JPanel sa = VeilPanel.card("SLAYER TASK");
            sa.setAlignmentX(LEFT_ALIGNMENT);
            sa.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
            String name = sl.taskName != null ? sl.taskName : "Task #" + sl.taskId;
            sa.add(VeilPanel.bigRow(name, sl.remaining + " remaining", VeilPanel.GOLD));
            sa.add(VeilPanel.row("Points", String.valueOf(sl.points), VeilPanel.AMBER));
            sa.add(VeilPanel.row("Streak", String.valueOf(sl.streak), VeilPanel.MUTED));
            content.add(sa);
        }

        content.revalidate(); content.repaint();
    }
}


// ════════════════════════════════════════════════════════════
// HIGH ALCH SCANNER TAB — 97 profitable alch items right now
// ════════════════════════════════════════════════════════════
class AlchScannerTab extends JPanel
{
    private final VeilPlugin plugin;
    private JPanel listPanel;
    private JLabel countLabel;

    AlchScannerTab(VeilPlugin plugin) {
        this.plugin = plugin;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8,8,8,8));

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(VeilPanel.BG);
        hdr.setAlignmentX(LEFT_ALIGNMENT);
        hdr.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel title = new JLabel("HIGH ALCH SCANNER");
        title.setForeground(VeilPanel.GOLD);
        title.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));

        countLabel = new JLabel("Loading...");
        countLabel.setForeground(VeilPanel.MUTED);
        countLabel.setFont(FontManager.getRunescapeSmallFont());

        hdr.add(title, BorderLayout.WEST);
        hdr.add(countLabel, BorderLayout.EAST);
        add(hdr);
        add(VeilPanel.muted("High alch profit > 0. Includes nat rune cost."));
        add(Box.createVerticalStrut(6));

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(VeilPanel.BG);
        add(listPanel);
    }

    void refresh()
    {
        List<FlipSignal> allFlips = plugin.getCachedFlips();
        List<FlipSignal> flips = allFlips;
        List<FlipSignal> alchable = flips.stream()
            .filter(f -> f.alchProfit > 0)
            .sorted((a, b) -> Integer.compare(b.alchProfit * Math.min(b.buyLimit, b.hourVol),
                                               a.alchProfit * Math.min(a.buyLimit, a.hourVol)))
            .collect(Collectors.toList());

        SwingUtilities.invokeLater(() -> {
            listPanel.removeAll();
            countLabel.setText(alchable.size() + " profitable alch items");

            if (alchable.isEmpty()) {
                if (allFlips.isEmpty() || plugin.getCachedFlips().isEmpty()) {
                    listPanel.add(VeilPanel.muted("Loading... flip data updates every 60s."));
                    listPanel.add(VeilPanel.muted("Come back in a moment."));
                } else {
                    listPanel.add(VeilPanel.muted("No profitable alch items right now."));
                    listPanel.add(VeilPanel.muted("Nature rune cost may exceed alch value."));
                }
            }

            for (FlipSignal f : alchable.subList(0, Math.min(20, alchable.size()))) {
                JPanel card = VeilPanel.card(null);
                card.setAlignmentX(LEFT_ALIGNMENT);
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
                card.add(VeilPanel.bigRow(VeilPanel.clip(f.itemName, 16), "+"+VeilPanel.fmtGp(f.alchProfit)+"ea", VeilPanel.GREEN));
                card.add(VeilPanel.row("Buy at GE:", VeilPanel.fmtGp(f.buyPrice) + " gp", VeilPanel.MUTED));
                card.add(VeilPanel.row("High alch:", VeilPanel.fmtGp(f.highalch) + " gp", VeilPanel.GOLD));
                card.add(VeilPanel.row("Alch profit:", "+"+VeilPanel.fmtGp(f.alchProfit)+" gp (after nat rune)", VeilPanel.GREEN));
                card.add(VeilPanel.row("Vol/hr:", VeilPanel.fmtGp(f.hourVol) + "  Limit: "+f.buyLimit, VeilPanel.MUTED));
                long maxPerCycle = f.alchProfit * (long) Math.min(f.buyLimit, f.hourVol * 4);
                card.add(VeilPanel.row("Max per 4hr:", "+"+VeilPanel.fmtGp(maxPerCycle), VeilPanel.GOLD));
                listPanel.add(card);
                listPanel.add(Box.createVerticalStrut(4));
            }
            listPanel.revalidate(); listPanel.repaint();
        });
    }
}


// ════════════════════════════════════════════════════════════
// 8-SLOT OPTIMIZER
// ════════════════════════════════════════════════════════════
class SlotOptimizerTab extends JPanel
{
    private final VeilPlugin plugin;
    private JPanel listPanel;
    private JLabel summaryLabel;

    SlotOptimizerTab(VeilPlugin plugin) {
        this.plugin = plugin;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8,8,8,8));

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(VeilPanel.BG);
        hdr.setAlignmentX(LEFT_ALIGNMENT);
        hdr.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        JLabel title = new JLabel("8-SLOT OPTIMIZER");
        title.setForeground(VeilPanel.GOLD);
        title.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        JButton ref = VeilPanel.btn("↺", VeilPanel.SURFACE2, VeilPanel.GOLD);
        ref.addActionListener(e -> refresh());
        hdr.add(title, BorderLayout.WEST);
        hdr.add(ref, BorderLayout.EAST);
        add(hdr);
        add(VeilPanel.muted("Optimal GE slot allocation."));
        add(VeilPanel.muted("Staggers fills — always collecting."));
        add(Box.createVerticalStrut(4));
        summaryLabel = new JLabel("Loading...");
        summaryLabel.setForeground(VeilPanel.GREEN);
        summaryLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        summaryLabel.setAlignmentX(LEFT_ALIGNMENT);
        add(summaryLabel);
        add(Box.createVerticalStrut(6));
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(VeilPanel.BG);
        add(listPanel);
    }

    void refresh()
    {
        List<FlipSignal> flips = plugin.getCachedFlips();
        long coins = plugin.getCoinStack();

        List<FlipSignal> fast   = flips.stream().filter(f -> f.fillMins < 10).sorted((a,b)->Integer.compare(b.score,a.score)).limit(3).collect(Collectors.toList());
        List<FlipSignal> medium = flips.stream().filter(f -> f.fillMins >= 10 && f.fillMins < 45).sorted((a,b)->Integer.compare(b.score,a.score)).limit(3).collect(Collectors.toList());
        List<FlipSignal> slow   = flips.stream().filter(f -> f.fillMins >= 45 && f.fillMins < 120).sorted((a,b)->Integer.compare(b.score,a.score)).limit(2).collect(Collectors.toList());

        List<FlipSignal> portfolio = new ArrayList<>();
        portfolio.addAll(fast.subList(0, Math.min(2, fast.size())));
        portfolio.addAll(medium.subList(0, Math.min(3, medium.size())));
        portfolio.addAll(slow.subList(0, Math.min(3, slow.size())));
        while (portfolio.size() < 8 && portfolio.size() < flips.size())
            portfolio.add(flips.get(portfolio.size()));

        long totalGpHr = portfolio.stream().mapToLong(f -> f.score).sum();

        SwingUtilities.invokeLater(() -> {
            listPanel.removeAll();
            summaryLabel.setText("~" + VeilPanel.fmtGp(totalGpHr) + "/hr · " + portfolio.size() + " slots");

            String[] tiers = {"FAST","FAST","MED","MED","MED","SLOW","SLOW","SLOW"};
            Color[]  tc    = {VeilPanel.GREEN, VeilPanel.GREEN, VeilPanel.GOLD, VeilPanel.GOLD, VeilPanel.GOLD, VeilPanel.AMBER, VeilPanel.AMBER, VeilPanel.AMBER};

            for (int i = 0; i < portfolio.size(); i++) {
                FlipSignal f = portfolio.get(i);
                JPanel card = VeilPanel.card(null);
                card.setAlignmentX(LEFT_ALIGNMENT);
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

                JPanel top = new JPanel(new BorderLayout());
                top.setBackground(VeilPanel.SURFACE);
                top.setAlignmentX(LEFT_ALIGNMENT);
                top.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
                JLabel slotLbl = new JLabel("SLOT " + (i+1) + " [" + (i < tiers.length ? tiers[i] : "?") + "]");
                slotLbl.setForeground(i < tc.length ? tc[i] : VeilPanel.MUTED);
                slotLbl.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
                JLabel gpLbl = new JLabel(VeilPanel.fmtGp(f.score) + "/hr");
                gpLbl.setForeground(VeilPanel.GOLD);
                gpLbl.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
                top.add(slotLbl, BorderLayout.WEST);
                top.add(gpLbl, BorderLayout.EAST);
                card.add(top);
                card.add(Box.createVerticalStrut(3));

                card.add(VeilPanel.bigRow(VeilPanel.clip(f.itemName, 16), f.grade + "·" + f.signal, VeilPanel.gradeColor(f.grade)));
                card.add(VeilPanel.row("Buy @",  VeilPanel.fmtGp(f.buyPrice) + " gp ea", VeilPanel.GREEN));
                card.add(VeilPanel.row("Sell @", VeilPanel.fmtGp(f.sellPrice - 1) + " gp ea", VeilPanel.GOLD));
                card.add(VeilPanel.row("Profit", "+"+VeilPanel.fmtGp(f.netMargin)+" ea  ("+String.format("%.1f%%",f.roi)+" ROI)", VeilPanel.GREEN));
                card.add(VeilPanel.row("Fill",   f.fillMins + " min  |  Limit: "+f.buyLimit, VeilPanel.MUTED));

                if (coins > 0) {
                    long qty = Math.min(coins / Math.max(f.buyPrice, 1), f.buyLimit);
                    card.add(VeilPanel.row("You can buy:", qty + "×  profit: +"+VeilPanel.fmtGp(qty * f.netMargin), VeilPanel.BLUE));
                }
                listPanel.add(card);
                listPanel.add(Box.createVerticalStrut(4));
            }
            listPanel.revalidate(); listPanel.repaint();
        });
    }
}


// ════════════════════════════════════════════════════════════
// PRICE ALERTS + GOAL TRACKER + TAX CALCULATOR
// ════════════════════════════════════════════════════════════
class AlertsGoalsTab extends JPanel
{
    private final VeilPlugin plugin;
    private JPanel alertsPanel;

    AlertsGoalsTab(VeilPlugin plugin) {
        this.plugin = plugin;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8,8,8,8));
        build();
    }

    private void build()
    {
        // ── GOAL TRACKER ──────────────────────────────────────
        add(VeilPanel.bold("GP GOAL TRACKER", VeilPanel.GOLD));
        add(Box.createVerticalStrut(4));
        JPanel goalCard = VeilPanel.card(null);
        goalCard.setAlignmentX(LEFT_ALIGNMENT);
        goalCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        JTextField goalNameField = VeilPanel.field("Goal name (e.g. Twisted bow)");
        goalNameField.setAlignmentX(LEFT_ALIGNMENT);
        goalNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JTextField goalGpField = VeilPanel.field("Target GP (e.g. 900m)");
        goalGpField.setAlignmentX(LEFT_ALIGNMENT);
        goalGpField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JButton goalBtn = VeilPanel.btn("Set Goal", VeilPanel.GOLD, VeilPanel.BG);
        goalBtn.addActionListener(e -> {
            String name = goalNameField.getText().trim();
            String gps  = goalGpField.getText().trim().toLowerCase().replaceAll(",","");
            long gp;
            try {
                if (gps.endsWith("m")) gp = (long)(Double.parseDouble(gps.replace("m","")) * 1_000_000);
                else if (gps.endsWith("b")) gp = (long)(Double.parseDouble(gps.replace("b","")) * 1_000_000_000);
                else gp = Long.parseLong(gps);
            } catch (Exception ex) { return; }
            plugin.setGoal(gp, name);
            goalNameField.setText(""); goalGpField.setText("");
            refresh();
        });
        goalCard.add(goalNameField);
        goalCard.add(Box.createVerticalStrut(4));
        goalCard.add(goalGpField);
        goalCard.add(Box.createVerticalStrut(4));
        goalCard.add(goalBtn);
        add(goalCard);
        add(Box.createVerticalStrut(8));

        // ── TAX CALCULATOR ────────────────────────────────────
        add(VeilPanel.bold("TAX CALCULATOR", VeilPanel.GOLD));
        add(VeilPanel.muted("Work backwards from desired profit"));
        add(Box.createVerticalStrut(4));

        JPanel taxCard = VeilPanel.card(null);
        taxCard.setAlignmentX(LEFT_ALIGNMENT);
        taxCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        JTextField buyPriceCalc = VeilPanel.field("Buy price (e.g. 23.5m)");
        buyPriceCalc.setAlignmentX(LEFT_ALIGNMENT);
        buyPriceCalc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JTextField targetProfit = VeilPanel.field("Target profit (e.g. 5m)");
        targetProfit.setAlignmentX(LEFT_ALIGNMENT);
        targetProfit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JTextField qtyCalc = VeilPanel.field("Quantity");
        qtyCalc.setAlignmentX(LEFT_ALIGNMENT);
        qtyCalc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel calcResult = new JLabel("Enter values above");
        calcResult.setForeground(VeilPanel.MUTED);
        calcResult.setFont(FontManager.getRunescapeSmallFont());
        calcResult.setAlignmentX(LEFT_ALIGNMENT);

        JButton calcBtn = VeilPanel.btn("Calculate", VeilPanel.GOLD, VeilPanel.BG);
        calcBtn.addActionListener(e -> {
            try {
                long buy = parseGp(buyPriceCalc.getText());
                long wantProfit = parseGp(targetProfit.getText());
                int qty = Integer.parseInt(qtyCalc.getText().trim());
                if (buy <= 0 || wantProfit <= 0 || qty <= 0) return;
                long profitEa = wantProfit / qty;
                // sell_price - buy - (sell_price * 0.01) = profitEa
                // sell_price * 0.99 = profitEa + buy
                long sellAt = (long)((profitEa + buy) / 0.99) + 1;
                long tax = Math.min(5_000_000, Math.max(1, (long)(sellAt * 0.01)));
                long actualProfit = (sellAt - buy - tax) * qty;
                calcResult.setForeground(VeilPanel.GREEN);
                calcResult.setText("<html><b>SELL AT: " + VeilPanel.fmtGp(sellAt) + " gp ea</b><br>" +
                    "Tax: " + VeilPanel.fmtGp(tax) + " gp ea  |  " +
                    "Actual profit: +" + VeilPanel.fmtGp(actualProfit) + " total</html>");
            } catch (Exception ex) {
                calcResult.setForeground(VeilPanel.RED);
                calcResult.setText("Invalid input");
            }
        });

        taxCard.add(buyPriceCalc);
        taxCard.add(Box.createVerticalStrut(4));
        taxCard.add(targetProfit);
        taxCard.add(Box.createVerticalStrut(4));
        taxCard.add(qtyCalc);
        taxCard.add(Box.createVerticalStrut(4));
        taxCard.add(calcBtn);
        taxCard.add(Box.createVerticalStrut(4));
        taxCard.add(calcResult);
        add(taxCard);
        add(Box.createVerticalStrut(8));

        // ── PRICE ALERTS ──────────────────────────────────────
        add(VeilPanel.bold("PRICE ALERTS", VeilPanel.GOLD));
        add(VeilPanel.muted("Get notified when any item hits your target price"));
        add(Box.createVerticalStrut(4));

        JPanel addAlert = VeilPanel.card(null);
        addAlert.setAlignmentX(LEFT_ALIGNMENT);
        addAlert.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));

        JTextField alertItem  = VeilPanel.field("Item name");
        alertItem.setAlignmentX(LEFT_ALIGNMENT);
        alertItem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JTextField alertPrice = VeilPanel.field("Target price (e.g. 900m or 1500000)");
        alertPrice.setAlignmentX(LEFT_ALIGNMENT);
        alertPrice.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JPanel dirRow = new JPanel();
        dirRow.setLayout(new BoxLayout(dirRow, BoxLayout.Y_AXIS));
        dirRow.setBackground(VeilPanel.SURFACE);
        dirRow.setAlignmentX(LEFT_ALIGNMENT);
        JRadioButton belowBtn = new JRadioButton("Price drops BELOW target");
        JRadioButton aboveBtn = new JRadioButton("Price rises ABOVE target");
        belowBtn.setBackground(VeilPanel.SURFACE); belowBtn.setForeground(VeilPanel.MUTED);
        belowBtn.setFont(FontManager.getRunescapeSmallFont()); belowBtn.setSelected(true);
        belowBtn.setAlignmentX(LEFT_ALIGNMENT);
        aboveBtn.setBackground(VeilPanel.SURFACE); aboveBtn.setForeground(VeilPanel.MUTED);
        aboveBtn.setFont(FontManager.getRunescapeSmallFont());
        aboveBtn.setAlignmentX(LEFT_ALIGNMENT);
        ButtonGroup bg = new ButtonGroup(); bg.add(belowBtn); bg.add(aboveBtn);
        dirRow.add(belowBtn); dirRow.add(aboveBtn);

        JButton addAlertBtn = VeilPanel.btn("+ Add Alert", VeilPanel.GOLD, VeilPanel.BG);
        addAlertBtn.addActionListener(e -> {
            String name = alertItem.getText().trim();
            if (name.isEmpty()) return;
            long price = parseGp(alertPrice.getText());
            if (price <= 0) return;
            // Match to flip list
            List<FlipSignal> flips = plugin.getCachedFlips();
            FlipSignal match = flips.stream()
                .filter(f -> f.itemName.toLowerCase().contains(name.toLowerCase()))
                .findFirst().orElse(null);
            int itemId = match != null ? match.itemId : 0;
            plugin.addPriceAlert(itemId, name, (int)Math.min(price, Integer.MAX_VALUE), belowBtn.isSelected());
            alertItem.setText(""); alertPrice.setText("");
            refresh();
        });

        addAlert.add(alertItem);
        addAlert.add(Box.createVerticalStrut(4));
        addAlert.add(alertPrice);
        addAlert.add(Box.createVerticalStrut(4));
        addAlert.add(dirRow);
        addAlert.add(Box.createVerticalStrut(4));
        addAlert.add(addAlertBtn);
        add(addAlert);
        add(Box.createVerticalStrut(4));

        alertsPanel = new JPanel();
        alertsPanel.setLayout(new BoxLayout(alertsPanel, BoxLayout.Y_AXIS));
        alertsPanel.setBackground(VeilPanel.BG);
        add(alertsPanel);
    }

    void refresh()
    {
        // Goal progress
        long goal = plugin.getGpGoal();
        String goalName = plugin.getGpGoalName();
        if (goal > 0) {
            long coins = plugin.getCoinStack();
            long sessionProfit = plugin.getSessionStats().sessionProfitGp + plugin.getSessionLootGp();
            double pct = coins > 0 ? Math.min(100, coins * 100.0 / goal) : 0;
            // Update goal display - simplified here
        }

        // Active alerts
        alertsPanel.removeAll();
        List<VeilPlugin.PriceAlert> alerts = plugin.getPriceAlerts();
        if (alerts.isEmpty()) {
            alertsPanel.add(VeilPanel.muted("No price alerts set"));
        }
        for (VeilPlugin.PriceAlert a : alerts) {
            JPanel alertCard = VeilPanel.card(null);
            alertCard.setAlignmentX(LEFT_ALIGNMENT);
            alertCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
            String status = a.triggered ? "✓ TRIGGERED" : "⏳ Watching";
            Color statusColor = a.triggered ? VeilPanel.GREEN : VeilPanel.AMBER;
            alertCard.add(VeilPanel.bigRow(a.itemName, status, statusColor));
            String dir = a.alertBelow ? "Alert if drops below: " : "Alert if rises above: ";
            alertCard.add(VeilPanel.row(dir, VeilPanel.fmtGp(a.targetPrice) + " gp", VeilPanel.GOLD));
            alertsPanel.add(alertCard);
            alertsPanel.add(Box.createVerticalStrut(3));
        }
        alertsPanel.revalidate(); alertsPanel.repaint();
    }

    private static long parseGp(String s)
    {
        try {
            s = s.trim().toLowerCase().replaceAll(",","");
            if (s.endsWith("m")) return (long)(Double.parseDouble(s.replace("m","")) * 1_000_000);
            if (s.endsWith("b")) return (long)(Double.parseDouble(s.replace("b","")) * 1_000_000_000);
            if (s.endsWith("k")) return (long)(Double.parseDouble(s.replace("k","")) * 1_000);
            return Long.parseLong(s);
        } catch (Exception e) { return 0; }
    }
}


// ════════════════════════════════════════════════════════════
// GUIDE TAB — slayer advisor, boss guides, daily routine,
//              progression stage, equipment upgrades
//              The "hold your hand" tab
// ════════════════════════════════════════════════════════════
class GuideTab extends JPanel
{
    private final VeilPlugin plugin;
    private JPanel content;

    GuideTab(VeilPlugin plugin) {
        this.plugin = plugin;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(VeilPanel.BG);
        add(content);
        refresh();
    }

    void refresh()
    {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();
            long coins = plugin.getCoinStack();
            SlayerState sl = plugin.getSlayerState();

            // ── PROGRESSION STAGE ──────────────────────────────
            String stage = VeilKnowledge.getStage(coins);
            Color stageColor = "ENDGAME".equals(stage) ? VeilPanel.PURPLE
                : "ADVANCED".equals(stage) ? VeilPanel.GOLD
                : "MID".equals(stage) ? VeilPanel.GREEN
                : "EARLY".equals(stage) ? VeilPanel.AMBER
                : VeilPanel.MUTED;

            JPanel stageCard = VeilPanel.card(null);
            stageCard.setAlignmentX(LEFT_ALIGNMENT);
            stageCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));

            JLabel stageLbl = new JLabel("STAGE: " + stage);
            stageLbl.setForeground(stageColor);
            stageLbl.setFont(FontManager.getRunescapeBoldFont().deriveFont(13f));
            stageLbl.setAlignmentX(LEFT_ALIGNMENT);
            stageCard.add(stageLbl);

            if (coins > 0) {
                JLabel nextLbl = new JLabel("Next: " + VeilKnowledge.getNextMilestone(coins));
                nextLbl.setForeground(VeilPanel.MUTED);
                nextLbl.setFont(FontManager.getRunescapeSmallFont());
                nextLbl.setAlignmentX(LEFT_ALIGNMENT);
                stageCard.add(nextLbl);
                stageCard.add(Box.createVerticalStrut(4));

                // Wrap advice text
                JLabel adviceLbl = new JLabel("<html><div style='width:195'>" +
                    VeilKnowledge.getStageAdvice(coins) + "</div></html>");
                adviceLbl.setForeground(VeilPanel.TEXT);
                adviceLbl.setFont(FontManager.getRunescapeSmallFont());
                adviceLbl.setAlignmentX(LEFT_ALIGNMENT);
                stageCard.add(adviceLbl);
            } else {
                stageCard.add(VeilPanel.muted("Open inventory to detect stage"));
            }
            content.add(stageCard);
            content.add(Box.createVerticalStrut(6));

            // ── SLAYER TASK GUIDE ──────────────────────────────
            if (sl.hasTask()) {
                String taskName = sl.taskName != null ? sl.taskName : "";
                VeilKnowledge.SlayerTask task = VeilKnowledge.findTask(taskName);

                JPanel slayCard = VeilPanel.card("SLAYER: " +
                    (sl.taskName != null ? sl.taskName.toUpperCase() : "TASK #" + sl.taskId));
                slayCard.setAlignmentX(LEFT_ALIGNMENT);
                slayCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));

                slayCard.add(VeilPanel.row("Remaining:", sl.remaining + " kills left", VeilPanel.GOLD));
                slayCard.add(VeilPanel.row("Points:", String.valueOf(sl.points), VeilPanel.AMBER));
                slayCard.add(Box.createVerticalStrut(4));

                if (task != null) {
                    // Location
                    slayCard.add(VeilPanel.bold("WHERE TO GO:", VeilPanel.TEXT));
                    JLabel locLbl = new JLabel("<html><div style='width:195'>" + task.location + "</div></html>");
                    locLbl.setForeground(VeilPanel.GREEN);
                    locLbl.setFont(FontManager.getRunescapeSmallFont());
                    locLbl.setAlignmentX(LEFT_ALIGNMENT);
                    slayCard.add(locLbl);
                    slayCard.add(Box.createVerticalStrut(4));

                    // Method
                    slayCard.add(VeilPanel.bold("METHOD:", VeilPanel.TEXT));
                    JLabel mLbl = new JLabel("<html><div style='width:195'>" + task.method + "</div></html>");
                    mLbl.setForeground(VeilPanel.MUTED);
                    mLbl.setFont(FontManager.getRunescapeSmallFont());
                    mLbl.setAlignmentX(LEFT_ALIGNMENT);
                    slayCard.add(mLbl);
                    slayCard.add(Box.createVerticalStrut(4));

                    // Gear
                    if (!task.gear.isEmpty()) {
                        slayCard.add(VeilPanel.bold("BRING:", VeilPanel.TEXT));
                        JLabel gLbl = new JLabel("<html><div style='width:195'>" + task.gear + "</div></html>");
                        gLbl.setForeground(VeilPanel.MUTED);
                        gLbl.setFont(FontManager.getRunescapeSmallFont());
                        gLbl.setAlignmentX(LEFT_ALIGNMENT);
                        slayCard.add(gLbl);
                        slayCard.add(Box.createVerticalStrut(4));
                    }

                    // GP/hr
                    slayCard.add(VeilPanel.row("GP/hr:", task.gpHr, VeilPanel.GOLD));

                    // Extend/Block recommendation
                    slayCard.add(Box.createVerticalStrut(4));
                    if (task.extend) {
                        slayCard.add(VeilPanel.bold("EXTEND THIS TASK ✓", VeilPanel.GREEN));
                        JLabel extLbl = new JLabel("<html><div style='width:195'>" + task.extendReason + "</div></html>");
                        extLbl.setForeground(VeilPanel.MUTED);
                        extLbl.setFont(FontManager.getRunescapeSmallFont());
                        extLbl.setAlignmentX(LEFT_ALIGNMENT);
                        slayCard.add(extLbl);
                    } else if (task.block) {
                        slayCard.add(VeilPanel.bold("BLOCK THIS TASK ✗", VeilPanel.RED));
                        JLabel blkLbl = new JLabel("<html><div style='width:195'>" + task.blockReason + "</div></html>");
                        blkLbl.setForeground(VeilPanel.MUTED);
                        blkLbl.setFont(FontManager.getRunescapeSmallFont());
                        blkLbl.setAlignmentX(LEFT_ALIGNMENT);
                        slayCard.add(blkLbl);
                    } else {
                        slayCard.add(VeilPanel.bold("SKIP IF BAD ROLLS", VeilPanel.AMBER));
                    }

                    // Critical tips
                    if (!task.tips.isEmpty()) {
                        slayCard.add(Box.createVerticalStrut(4));
                        slayCard.add(VeilPanel.bold("IMPORTANT:", VeilPanel.RED));
                        JLabel tipLbl = new JLabel("<html><div style='width:195'>" + task.tips + "</div></html>");
                        tipLbl.setForeground(VeilPanel.RED);
                        tipLbl.setFont(FontManager.getRunescapeSmallFont());
                        tipLbl.setAlignmentX(LEFT_ALIGNMENT);
                        slayCard.add(tipLbl);
                    }

                    // Requirements
                    if (!task.requirements.isEmpty()) {
                        slayCard.add(Box.createVerticalStrut(4));
                        slayCard.add(VeilPanel.row("Requirements:", task.requirements, VeilPanel.MUTED));
                    }
                } else {
                    slayCard.add(VeilPanel.muted("Task guide not in database yet."));
                    slayCard.add(VeilPanel.muted("Check OSRS wiki for: " + taskName));
                }
                content.add(slayCard);
                content.add(Box.createVerticalStrut(6));
            }

            // ── DAILY ROUTINE ──────────────────────────────────
            JPanel routineCard = VeilPanel.card("DAILY ROUTINE");
            routineCard.setAlignmentX(LEFT_ALIGNMENT);
            routineCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
            String routine = VeilKnowledge.getDailyRoutine(
                coins, sl.hasTask(), sl.remaining);
            for (String line : routine.split("\n")) {
                Color lc = line.startsWith("MORNING") || line.startsWith("MAIN") ||
                           line.startsWith("EVENING") || line.startsWith("EXPECTED")
                    ? VeilPanel.GOLD : line.startsWith("  ") ? VeilPanel.TEXT : VeilPanel.MUTED;
                JLabel lbl = new JLabel("<html><div style='width:185'>" + line.replace("  ", "&nbsp;&nbsp;") + "</div></html>");
                lbl.setForeground(lc);
                lbl.setFont(line.startsWith("MORNING") || line.startsWith("MAIN") ||
                            line.startsWith("EVENING") || line.startsWith("EXPECTED")
                    ? FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD)
                    : FontManager.getRunescapeSmallFont());
                lbl.setAlignmentX(LEFT_ALIGNMENT);
                routineCard.add(lbl);
                if (line.isEmpty()) routineCard.add(Box.createVerticalStrut(3));
            }
            content.add(routineCard);
            content.add(Box.createVerticalStrut(6));

            // ── EQUIPMENT UPGRADE PATH ─────────────────────────
            JPanel upgradeCard = VeilPanel.card("NEXT UPGRADES");
            upgradeCard.setAlignmentX(LEFT_ALIGNMENT);
            upgradeCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            JLabel upgLbl = new JLabel("<html><div style='width:195'>" +
                VeilKnowledge.getUpgradeAdvice(coins) + "</div></html>");
            upgLbl.setForeground(VeilPanel.TEXT);
            upgLbl.setFont(FontManager.getRunescapeSmallFont());
            upgLbl.setAlignmentX(LEFT_ALIGNMENT);
            upgradeCard.add(upgLbl);
            content.add(upgradeCard);
            content.add(Box.createVerticalStrut(6));

            // ── BOSS GUIDES ────────────────────────────────────
            JPanel bossCard = VeilPanel.card("BOSS UNLOCK GUIDES");
            bossCard.setAlignmentX(LEFT_ALIGNMENT);
            bossCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
            bossCard.add(VeilPanel.muted("What you need to access each boss:"));
            bossCard.add(Box.createVerticalStrut(4));

            for (Map.Entry<String, VeilKnowledge.BossInfo> e :
                 VeilKnowledge.BOSS_DB.entrySet()) {
                VeilKnowledge.BossInfo b = e.getValue();
                bossCard.add(VeilPanel.bold(b.name, VeilPanel.GOLD));
                bossCard.add(VeilPanel.row("GP/hr:", b.gpHr, VeilPanel.GREEN));

                JLabel qLbl = new JLabel("<html><div style='width:185'><b>Unlock:</b> " + b.questChain + "</div></html>");
                qLbl.setForeground(VeilPanel.MUTED);
                qLbl.setFont(FontManager.getRunescapeSmallFont());
                qLbl.setAlignmentX(LEFT_ALIGNMENT);
                bossCard.add(qLbl);

                JLabel bgLbl = new JLabel("<html><div style='width:185'><b>Beginner:</b> " + b.beginnerGuide + "</div></html>");
                bgLbl.setForeground(VeilPanel.BLUE);
                bgLbl.setFont(FontManager.getRunescapeSmallFont());
                bgLbl.setAlignmentX(LEFT_ALIGNMENT);
                bossCard.add(bgLbl);

                JLabel dropsLbl = new JLabel("<html><div style='width:185'><b>Top drops:</b> " + b.topDrops + "</div></html>");
                dropsLbl.setForeground(VeilPanel.PURPLE);
                dropsLbl.setFont(FontManager.getRunescapeSmallFont());
                dropsLbl.setAlignmentX(LEFT_ALIGNMENT);
                bossCard.add(dropsLbl);
                bossCard.add(Box.createVerticalStrut(8));
            }
            content.add(bossCard);

            content.revalidate();
            content.repaint();
        });
    }
}


// ════════════════════════════════════════════════════════════
// GRANDMA MODE — ONE BUTTON, ONE INSTRUCTION
// "What do I do right now?"
// ════════════════════════════════════════════════════════════
class GrandmaPanel extends JPanel
{
    private final VeilPlugin plugin;
    private JLabel  instructionLabel;
    private JPanel  detailPanel;

    GrandmaPanel(VeilPlugin plugin) {
        this.plugin = plugin;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8,8,8,8));
        build();
    }

    private javax.swing.Timer autoRefreshTimer;
    private long lastRefreshMs = 0;
    private JLabel updatedLabel;

    private void build()
    {
        JLabel hdr = new JLabel("WHAT DO I DO RIGHT NOW?");
        hdr.setForeground(VeilPanel.GOLD);
        hdr.setFont(FontManager.getRunescapeBoldFont().deriveFont(13f));
        hdr.setAlignmentX(LEFT_ALIGNMENT);
        add(hdr);

        JPanel subRow = new JPanel(new BorderLayout());
        subRow.setBackground(VeilPanel.BG);
        subRow.setAlignmentX(LEFT_ALIGNMENT);
        subRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        JLabel sub = new JLabel("Auto-updates every 60s");
        sub.setForeground(VeilPanel.MUTED);
        sub.setFont(FontManager.getRunescapeSmallFont());
        updatedLabel = new JLabel("—");
        updatedLabel.setForeground(VeilPanel.MUTED);
        updatedLabel.setFont(FontManager.getRunescapeSmallFont());
        subRow.add(sub, BorderLayout.WEST);
        subRow.add(updatedLabel, BorderLayout.EAST);
        add(subRow);
        add(Box.createVerticalStrut(8));

        JButton btn = VeilPanel.btn("▶  WHAT DO I DO?", VeilPanel.GOLD, VeilPanel.BG);
        btn.setFont(FontManager.getRunescapeBoldFont().deriveFont(12f));
        btn.addActionListener(e -> refresh());
        add(btn);
        add(Box.createVerticalStrut(10));

        // Auto-refresh every 60 seconds
        autoRefreshTimer = new javax.swing.Timer(60_000, e -> refresh());
        autoRefreshTimer.setInitialDelay(5_000); // first auto-refresh after 5s
        autoRefreshTimer.start();

        instructionLabel = new JLabel("<html><div style='width:210'>Press the button above.</div></html>");
        instructionLabel.setForeground(VeilPanel.TEXT);
        instructionLabel.setFont(FontManager.getRunescapeBoldFont().deriveFont(12f));
        instructionLabel.setAlignmentX(LEFT_ALIGNMENT);
        add(instructionLabel);
        add(Box.createVerticalStrut(8));

        detailPanel = new JPanel();
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
        detailPanel.setBackground(VeilPanel.BG);
        add(detailPanel);
    }

    void refresh()
    {
        long coins = plugin.getCoinStack();
        List<FlipSignal> flips = plugin.getCachedFlips();
        SlayerState sl = plugin.getSlayerState();
        List<TradeRecord> active = plugin.getActiveOffers();
        long nowMs = System.currentTimeMillis();

        detailPanel.removeAll();
        String instruction = "";
        String detail = "";
        Color color = VeilPanel.GREEN;

        // ── DECISION TREE ────────────────────────────────────
        // Priority 1: Collect filled offers
        for (TradeRecord rec : active) {
            if (rec.quantityTraded >= rec.quantityOffered * 0.99 && !rec.complete) {
                instruction = "Collect your " + rec.itemName + " from the GE!";
                detail = "Your buy order is filled. Go to GE → collect " +
                         rec.quantityTraded + "× " + rec.itemName + ".";
                color = VeilPanel.GREEN;
                break;
            }
        }

        // Priority 1.5: Farming patches ready
        long farmingMs = plugin.getFarmingPatchReadyAt();
        if (farmingMs > 0 && System.currentTimeMillis() >= farmingMs) {
            instruction = "Collect your herbs! Farming patches are ready.";
            detail = "Go to your herb patches and harvest.\n" +
                     "Then check the Herb Patch Optimizer in Tools tab\n" +
                     "to see what to plant next for maximum GP.\n\n" +
                     "After replanting, Veil will track the next ready time.";
            color = VeilPanel.GREEN;
        }

        // Priority 2: Slot is free and you have GP — flip something
        if (instruction.isEmpty() && active.size() < 8 && coins > 10_000) {
            FlipSignal best = null;
            for (FlipSignal f : flips) {
                if (!"ENTER".equals(f.signal) && !"WATCH".equals(f.signal)) continue;
                if (f.buyPrice > coins) continue;
                if (f.confidence < 55) continue;
                if (f.fillMins > 30) continue;
                best = f;
                break;
            }
            if (best != null) {
                long qty = Math.min(coins / best.buyPrice, best.buyLimit);
                long cost = qty * best.buyPrice;
                long profit = qty * best.netMargin;
                instruction = "Buy " + qty + "× " + best.itemName;
                detail = "1. Open GE → Buy\n" +
                         "2. Search: " + best.itemName + "\n" +
                         "3. Quantity: " + qty + "\n" +
                         "4. Price: " + String.format("%,d", best.buyPrice) + " gp\n" +
                         "5. Confirm. Come back in ~" + best.fillFast + " min.\n" +
                         "6. Sell at " + String.format("%,d", best.sellPrice - 1) + " gp\n" +
                         "7. Profit: +" + VeilPanel.fmtGp(profit) + " gp";
                color = VeilPanel.GREEN;
            }
        }

        // Priority 3: Do slayer task
        if (instruction.isEmpty() && sl.hasTask() && sl.remaining > 0) {
            String task = sl.taskName != null ? sl.taskName : "slayer task";
            VeilKnowledge.SlayerTask guide = VeilKnowledge.findTask(task);
            instruction = "Slayer: " + task + " (" + sl.remaining + " left)";
            detail = guide != null
                ? "Go to: " + guide.location + "\n" +
                  "Method: " + guide.method + "\n" +
                  "GP/hr: " + guide.gpHr + "\n" +
                  "Remaining: " + sl.remaining + " kills"
                : "Go kill " + sl.remaining + " " + task + ". Check wiki for location.";
            color = VeilPanel.GOLD;
        }

        // Priority 4: Get a slayer task
        if (instruction.isEmpty()) {
            instruction = "Get a slayer task from Duradel";
            detail = "Slayer = XP + GP at the same time.\n" +
                     "Teleport to Shilo Village (fairy ring CKR).\n" +
                     "Talk to Duradel. Get a task. Check Guide tab for method.";
            color = VeilPanel.AMBER;
        }

        // Display
        final String finalInstruction = instruction;
        final String finalDetail = detail;
        final Color  finalColor   = color;

        lastRefreshMs = System.currentTimeMillis();

        SwingUtilities.invokeLater(() -> {
            instructionLabel.setText("<html><div style='width:210'>" + finalInstruction + "</div></html>");
            instructionLabel.setForeground(finalColor);
            if (updatedLabel != null) updatedLabel.setText("now");
            // Start a ticker to show "Xs ago"
            new javax.swing.Timer(10_000, ev -> {
                long secsAgo = (System.currentTimeMillis() - lastRefreshMs) / 1000;
                if (updatedLabel != null && secsAgo < 300)
                    updatedLabel.setText(secsAgo + "s ago");
            }) {{ setRepeats(true); start(); }};

            detailPanel.removeAll();
            JPanel stepCard = VeilPanel.card("HOW TO DO IT:");
            stepCard.setAlignmentX(LEFT_ALIGNMENT);
            stepCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
            for (String line : finalDetail.split("\n")) {
                JLabel l = new JLabel("<html><div style='width:185'>" + line + "</div></html>");
                l.setForeground(line.startsWith("[") ? VeilPanel.GOLD : VeilPanel.TEXT);
                l.setFont(FontManager.getRunescapeSmallFont());
                l.setAlignmentX(LEFT_ALIGNMENT);
                stepCard.add(l);
                stepCard.add(Box.createVerticalStrut(2));
            }
            detailPanel.add(stepCard);

            // Stage tip
            long coins2 = plugin.getCoinStack();
            if (coins2 > 0) {
                detailPanel.add(Box.createVerticalStrut(6));
                JPanel tipCard = VeilPanel.card("WHERE YOU'RE AT:");
                tipCard.setAlignmentX(LEFT_ALIGNMENT);
                tipCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
                String stage = VeilKnowledge.getStage(coins2);
                tipCard.add(VeilPanel.bigRow("Stage: " + stage, VeilPanel.fmtGp(coins2) + " GP", VeilPanel.GOLD));
                JLabel next = new JLabel("<html><div style='width:195'>" + VeilKnowledge.getNextMilestone(coins2) + "</div></html>");
                next.setForeground(VeilPanel.MUTED);
                next.setFont(FontManager.getRunescapeSmallFont());
                next.setAlignmentX(LEFT_ALIGNMENT);
                tipCard.add(next);
                detailPanel.add(tipCard);
            }

            detailPanel.revalidate(); detailPanel.repaint();
        });
    }
}


// ════════════════════════════════════════════════════════════
// TOOLS TAB — Superheat, Herb Patch, Market Making, Pairs,
//              Session Replay, Gear Tracker, Discord Webhook
// ════════════════════════════════════════════════════════════
class ToolsTab extends JPanel
{
    private final VeilPlugin plugin;
    private JPanel content;

    ToolsTab(VeilPlugin plugin) {
        this.plugin = plugin;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8,8,8,8));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(VeilPanel.BG);
        topRow.setAlignmentX(LEFT_ALIGNMENT);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel hdr = new JLabel("ADVANCED TOOLS");
        hdr.setForeground(VeilPanel.GOLD);
        hdr.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        JButton ref = VeilPanel.btn("↺", VeilPanel.SURFACE2, VeilPanel.GOLD);
        ref.addActionListener(e -> refresh());
        topRow.add(hdr, BorderLayout.WEST);
        topRow.add(ref, BorderLayout.EAST);
        add(topRow);
        add(Box.createVerticalStrut(6));

        content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(VeilPanel.BG);
        add(content);
        refresh();
    }

    void refresh()
    {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();

            // ── CRAFTING ARBITRAGE ─────────────────────────────
            List<WikiFlipFetcher.CraftResult> crafts = plugin.getCraftingResults();
            if (!crafts.isEmpty()) {
                List<WikiFlipFetcher.CraftResult> profitable = crafts.stream()
                    .filter(cr -> cr.profitPerCraft > 0)
                    .collect(Collectors.toList());
                if (!profitable.isEmpty()) {
                    JPanel craftCard = VeilPanel.card("CRAFTING ARBITRAGE");
                    craftCard.setAlignmentX(LEFT_ALIGNMENT);
                    craftCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
                    JLabel cDesc = new JLabel("<html><div style='width:195'>Buy raw materials, craft, sell. Live prices. Updates every 60s.</div></html>");
                    cDesc.setForeground(VeilPanel.MUTED);
                    cDesc.setFont(FontManager.getRunescapeSmallFont());
                    cDesc.setAlignmentX(LEFT_ALIGNMENT);
                    craftCard.add(cDesc);
                    craftCard.add(Box.createVerticalStrut(6));
                    for (WikiFlipFetcher.CraftResult cr : profitable.subList(0, Math.min(8, profitable.size()))) {
                        Color c = cr.profitPerCraft > 5000 ? VeilPanel.GREEN : VeilPanel.AMBER;
                        craftCard.add(VeilPanel.bigRow(cr.name + " [" + cr.category + "]",
                            "+" + VeilPanel.fmtGp(cr.profitPerCraft) + " ea", c));
                        craftCard.add(VeilPanel.row("  Cost → Sell:", VeilPanel.fmtGp(cr.totalCost) + " → " + VeilPanel.fmtGp(cr.sellPrice), VeilPanel.MUTED));
                        craftCard.add(VeilPanel.row("  GP/hr (~" + cr.craftsPerHour + "/hr):", "+" + VeilPanel.fmtGp(cr.profitPerHour) + " + XP", VeilPanel.GREEN));
                        craftCard.add(VeilPanel.row("  Req level:", cr.levelRequired + "  XP: " + cr.xpPerCraft + " (" + String.format("%.1f", cr.gpPerXp) + " gp/xp)", VeilPanel.MUTED));
                        craftCard.add(Box.createVerticalStrut(4));
                    }
                    content.add(craftCard);
                    content.add(Box.createVerticalStrut(6));
                }
            }

            // ── MARKET MAKING ──────────────────────────────────
            List<WikiFlipFetcher.MarketMakeOpp> mmOpps = plugin.getMarketMakeOpps();
            if (!mmOpps.isEmpty()) {
                JPanel mmCard = VeilPanel.card("MARKET MAKING");
                mmCard.setAlignmentX(LEFT_ALIGNMENT);
                mmCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
                JLabel mmDesc = new JLabel("<html><div style='width:195'>" +
                    "Post a buy AND sell order simultaneously. Collect the spread from whoever crosses your price. " +
                    "Only works when price momentum is flat.</div></html>");
                mmDesc.setForeground(VeilPanel.MUTED);
                mmDesc.setFont(FontManager.getRunescapeSmallFont());
                mmDesc.setAlignmentX(LEFT_ALIGNMENT);
                mmCard.add(mmDesc);
                mmCard.add(Box.createVerticalStrut(6));

                for (WikiFlipFetcher.MarketMakeOpp mm : mmOpps.subList(0, Math.min(5, mmOpps.size()))) {
                    mmCard.add(VeilPanel.bigRow(mm.itemName, "~" + VeilPanel.fmtGp(mm.estimatedGpPerHour) + "/hr", VeilPanel.PURPLE));
                    mmCard.add(VeilPanel.row("  Buy at:",  String.format("%,d", mm.buyAt)  + " gp  (+1 above instabuy)", VeilPanel.GREEN));
                    mmCard.add(VeilPanel.row("  Sell at:", String.format("%,d", mm.sellAt) + " gp  (-1 below instasell)", VeilPanel.GOLD));
                    mmCard.add(VeilPanel.row("  Net/trade:", String.format("%,d", mm.netPerTrade) + " gp after tax", VeilPanel.GREEN));
                    mmCard.add(VeilPanel.row("  Vol/hr:", VeilPanel.fmtGp(mm.volume) + "/hr", VeilPanel.MUTED));
                    mmCard.add(Box.createVerticalStrut(4));
                }
                content.add(mmCard);
                content.add(Box.createVerticalStrut(6));
            }

            // ── CORRELATION PAIRS ──────────────────────────────
            List<WikiFlipFetcher.CorrelationPlay> pairs = plugin.getCorrelationPlays();
            if (!pairs.isEmpty()) {
                JPanel pairCard = VeilPanel.card("CORRELATION PAIRS");
                pairCard.setAlignmentX(LEFT_ALIGNMENT);
                pairCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
                JLabel pDesc = new JLabel("<html><div style='width:195'>" +
                    "Pairs that move together. Buy the cheap one — zero directional risk.</div></html>");
                pDesc.setForeground(VeilPanel.MUTED);
                pDesc.setFont(FontManager.getRunescapeSmallFont());
                pDesc.setAlignmentX(LEFT_ALIGNMENT);
                pairCard.add(pDesc);
                pairCard.add(Box.createVerticalStrut(6));

                for (WikiFlipFetcher.CorrelationPlay cp : pairs) {
                    Color devColor = Math.abs(cp.deviationPct) > 30 ? VeilPanel.RED
                        : Math.abs(cp.deviationPct) > 15 ? VeilPanel.AMBER : VeilPanel.GOLD;
                    pairCard.add(VeilPanel.bigRow(VeilPanel.clip(cp.action, 16), String.format("%+.1f%%", cp.deviationPct), devColor));
                    pairCard.add(VeilPanel.row("  " + cp.itemAName + ":", VeilPanel.fmtGp(cp.itemAPrice), VeilPanel.MUTED));
                    pairCard.add(VeilPanel.row("  " + cp.itemBName + ":", VeilPanel.fmtGp(cp.itemBPrice), VeilPanel.MUTED));
                    pairCard.add(VeilPanel.row("  Ratio:", String.format("%.3f (expected %.3f)", cp.currentRatio, cp.expectedRatio), VeilPanel.MUTED));
                    JLabel advL = new JLabel("<html><div style='width:195'>" + cp.advice + "</div></html>");
                    advL.setForeground(VeilPanel.TEXT);
                    advL.setFont(FontManager.getRunescapeSmallFont());
                    advL.setAlignmentX(LEFT_ALIGNMENT);
                    pairCard.add(advL);
                    pairCard.add(Box.createVerticalStrut(6));
                }
                content.add(pairCard);
                content.add(Box.createVerticalStrut(6));
            }

            // ── SUPERHEAT ARBITRAGE ────────────────────────────
            List<WikiFlipFetcher.SuperheatResult> supers = plugin.getSuperheatResults();
            if (!supers.isEmpty()) {
                JPanel shCard = VeilPanel.card("SUPERHEAT ARBITRAGE");
                shCard.setAlignmentX(LEFT_ALIGNMENT);
                shCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
                JLabel shDesc = new JLabel("<html><div style='width:195'>" +
                    "Buy ore on GE, cast High Level Alchemy or Superheat Item, sell bar. " +
                    "Passive income while training Magic. Requires 43+ Magic.</div></html>");
                shDesc.setForeground(VeilPanel.MUTED);
                shDesc.setFont(FontManager.getRunescapeSmallFont());
                shDesc.setAlignmentX(LEFT_ALIGNMENT);
                shCard.add(shDesc);
                shCard.add(Box.createVerticalStrut(6));

                for (WikiFlipFetcher.SuperheatResult sr : supers) {
                    Color profColor = sr.profitPerCast > 0 ? VeilPanel.GREEN : VeilPanel.RED;
                    shCard.add(VeilPanel.bigRow(sr.oreName + " → " + sr.barName,
                        (sr.profitPerCast > 0 ? "+" : "") + VeilPanel.fmtGp(sr.profitPerCast) + " gp/cast",
                        profColor));
                    if (sr.profitPerCast > 0) {
                        shCard.add(VeilPanel.row("  Ore buy:", VeilPanel.fmtGp(sr.oreBuy), VeilPanel.MUTED));
                        shCard.add(VeilPanel.row("  Bar sell:", VeilPanel.fmtGp(sr.barSell), VeilPanel.MUTED));
                        shCard.add(VeilPanel.row("  Spell cost:", VeilPanel.fmtGp(sr.superheatCost), VeilPanel.MUTED));
                        shCard.add(VeilPanel.row("  Est. GP/hr:", "~" + VeilPanel.fmtGp(sr.profitPerHour) + "/hr", VeilPanel.GREEN));
                    }
                    shCard.add(Box.createVerticalStrut(4));
                }
                content.add(shCard);
                content.add(Box.createVerticalStrut(6));
            }

            // ── HERB PATCH OPTIMIZER ───────────────────────────
            List<WikiFlipFetcher.HerbPatchResult> herbs = plugin.getHerbPatchResults();
            if (!herbs.isEmpty()) {
                JPanel herbCard = VeilPanel.card("HERB PATCH OPTIMIZER");
                herbCard.setAlignmentX(LEFT_ALIGNMENT);
                herbCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
                JLabel herbDesc = new JLabel("<html><div style='width:195'>" +
                    "Best herb to plant right now. Based on live prices. " +
                    "Assumes 9 herbs per patch with magic secateurs + 65+ Farming.</div></html>");
                herbDesc.setForeground(VeilPanel.MUTED);
                herbDesc.setFont(FontManager.getRunescapeSmallFont());
                herbDesc.setAlignmentX(LEFT_ALIGNMENT);
                herbCard.add(herbDesc);
                herbCard.add(Box.createVerticalStrut(6));

                for (WikiFlipFetcher.HerbPatchResult hr : herbs.subList(0, Math.min(6, herbs.size()))) {
                    Color c = hr.profitPerPatch > 0 ? VeilPanel.GREEN : VeilPanel.RED;
                    herbCard.add(VeilPanel.bigRow(hr.herbName,
                        VeilPanel.fmtGp(hr.profitPerPatch) + " per patch", c));
                    herbCard.add(VeilPanel.row("  Seed:", VeilPanel.fmtGp(hr.seedBuy), VeilPanel.MUTED));
                    herbCard.add(VeilPanel.row("  Herb×" + hr.avgYield + ":", VeilPanel.fmtGp(hr.herbSell), VeilPanel.MUTED));
                    herbCard.add(Box.createVerticalStrut(3));
                }
                content.add(herbCard);
                content.add(Box.createVerticalStrut(6));
            }

            // ── SESSION REPLAY ─────────────────────────────────
            buildSessionReplay();

            // ── GEAR UPGRADE TRACKER ───────────────────────────
            buildGearTracker();

            content.revalidate(); content.repaint();
        });
    }

    private void buildSessionReplay()
    {
        List<TradeRecord> trades = plugin.getSessionTrades();
        if (trades.isEmpty()) return;

        JPanel replayCard = VeilPanel.card("SESSION REPLAY");
        replayCard.setAlignmentX(LEFT_ALIGNMENT);
        replayCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        VeilPlugin.SessionStats stats = plugin.getSessionStats();
        int loot = plugin.getSessionLootGp();
        long total = stats.sessionProfitGp + loot;
        long sessionMs = System.currentTimeMillis() - plugin.getSessionStartMs();
        double sessionHrs = sessionMs / 3_600_000.0;
        long gpPerHr = sessionHrs > 0.05 ? (long)(total / sessionHrs) : 0;

        replayCard.add(VeilPanel.bigRow("Session earnings:", VeilPanel.fmtSigned(total), total >= 0 ? VeilPanel.GREEN : VeilPanel.RED));
        if (gpPerHr > 0)
            replayCard.add(VeilPanel.row("Your GP/hr:", VeilPanel.fmtGp(gpPerHr) + "/hr  (" + String.format("%.1f", sessionHrs) + "hrs played)", VeilPanel.GOLD));
        replayCard.add(VeilPanel.row("GE profit:", VeilPanel.fmtSigned(stats.sessionProfitGp), VeilPanel.GREEN));
        replayCard.add(VeilPanel.row("Loot:", "+" + VeilPanel.fmtGp(loot), VeilPanel.GREEN));
        replayCard.add(VeilPanel.row("Trades done:", String.valueOf(stats.tradeCount), VeilPanel.MUTED));

        // Best trade this session
        TradeRecord best = trades.stream().max(java.util.Comparator.comparingInt(r -> r.profitGp)).orElse(null);
        if (best != null && best.profitGp > 0) {
            replayCard.add(Box.createVerticalStrut(4));
            replayCard.add(VeilPanel.bigRow("Best flip:", best.itemName + " +" + VeilPanel.fmtGp(best.profitGp), VeilPanel.PURPLE));
        }

        // Coaching tip
        replayCard.add(Box.createVerticalStrut(4));
        String coaching;
        if (gpPerHr > 5_000_000)       coaching = "Excellent session! You're at top-tier efficiency.";
        else if (gpPerHr > 2_000_000)  coaching = "Strong session. Try market making on flat items for more.";
        else if (gpPerHr > 500_000)    coaching = "Solid. Upgrade to S/A-grade flips to push higher.";
        else if (stats.tradeCount > 0) coaching = "Getting started. Check confidence scores — skip items under 60.";
        else                           coaching = "Start a flip! Open Flips tab → pick top ENTER signal.";
        JLabel coachLbl = new JLabel("<html><div style='width:195'>" + coaching + "</div></html>");
        coachLbl.setForeground(VeilPanel.BLUE);
        coachLbl.setFont(FontManager.getRunescapeSmallFont());
        coachLbl.setAlignmentX(LEFT_ALIGNMENT);
        replayCard.add(coachLbl);

        content.add(replayCard);
        content.add(Box.createVerticalStrut(6));
    }

    private void buildGearTracker()
    {
        EquipmentState eq = plugin.getEquipmentState();
        long coins = plugin.getCoinStack();
        if (eq == null || eq.totalValue == 0) return;

        JPanel gearCard = VeilPanel.card("GEAR UPGRADE TRACKER");
        gearCard.setAlignmentX(LEFT_ALIGNMENT);
        gearCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        gearCard.add(VeilPanel.bigRow("Gear value:", VeilPanel.fmtGp(eq.totalValue), VeilPanel.GOLD));

        // Next upgrade recommendation
        String nextUpgrade = VeilKnowledge.getUpgradeAdvice(coins + eq.totalValue);
        JLabel upgLbl = new JLabel("<html><div style='width:195'><b>Next upgrade:</b> " + nextUpgrade + "</div></html>");
        upgLbl.setForeground(VeilPanel.TEXT);
        upgLbl.setFont(FontManager.getRunescapeSmallFont());
        upgLbl.setAlignmentX(LEFT_ALIGNMENT);
        gearCard.add(Box.createVerticalStrut(4));
        gearCard.add(upgLbl);

        // Days until next upgrade
        VeilPlugin.SessionStats stats = plugin.getSessionStats();
        long sessionMs = System.currentTimeMillis() - plugin.getSessionStartMs();
        double sessionHrs = sessionMs / 3_600_000.0;
        long gpPerHr = sessionHrs > 0.1 ? (long)((stats.sessionProfitGp + plugin.getSessionLootGp()) / sessionHrs) : 0;
        if (gpPerHr > 0) {
            gearCard.add(Box.createVerticalStrut(4));
            gearCard.add(VeilPanel.row("GP/hr this session:", VeilPanel.fmtGp(gpPerHr) + "/hr", VeilPanel.MUTED));
            gearCard.add(VeilPanel.muted("Keep flipping — every session gets you closer."));
        }

        content.add(gearCard);
        content.add(Box.createVerticalStrut(6));
    }
}


// ════════════════════════════════════════════════════════════
// PERSONALITY TAB — one-time setup, auto-adjusts everything
// ════════════════════════════════════════════════════════════
class PersonalityTab extends JPanel
{
    private final VeilPlugin plugin;
    private JPanel content;
    private String selectedPersonality = "BALANCED";

    PersonalityTab(VeilPlugin plugin) {
        this.plugin = plugin;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8,8,8,8));
        build();
    }

    private void build()
    {
        add(VeilPanel.bold("HOW DO YOU PLAY?", VeilPanel.GOLD));
        add(VeilPanel.muted("Pick your style. Veil auto-adjusts to it."));
        add(Box.createVerticalStrut(10));

        String[][] personalities = {
            {"ACTIVE",    "I'm at my computer, checking every few minutes",       "Fast flips only. Fill < 15min. ENTER signals. Market making eligible."},
            {"BALANCED",  "I check in a few times per session",                    "Mix of fast and medium flips. Fill < 45min. All signals."},
            {"PASSIVE",   "I set it and walk away",                                "Patient flips and thin market gems. Fill up to 2hrs. High margin priority."},
            {"BEGINNER",  "I'm new and want simple, safe recommendations",         "Only B+ grade items. Confidence > 70. Step-by-step instructions always shown."},
            {"WHALE",     "I have 100M+ and want maximum GP/hr",                   "S/A grade only. Market making. Correlation pairs. Highest GP/hr items."},
        };

        for (String[] p : personalities) {
            String key = p[0], title = p[1], desc = p[2];

            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            boolean selected = key.equals(selectedPersonality);
            card.setBackground(selected ? VeilPanel.SURFACE2 : VeilPanel.SURFACE);
            card.setBorder(new CompoundBorder(
                new MatteBorder(0, selected ? 3 : 1, 0, 0, selected ? VeilPanel.GOLD : VeilPanel.BORDER),
                new EmptyBorder(8, 10, 8, 10)));
            card.setAlignmentX(LEFT_ALIGNMENT);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

            JLabel titleLbl = new JLabel(key);
            titleLbl.setForeground(selected ? VeilPanel.GOLD : VeilPanel.TEXT);
            titleLbl.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
            titleLbl.setAlignmentX(LEFT_ALIGNMENT);

            JLabel subLbl = new JLabel("<html><div style='width:185'>" + title + "</div></html>");
            subLbl.setForeground(VeilPanel.MUTED);
            subLbl.setFont(FontManager.getRunescapeSmallFont());
            subLbl.setAlignmentX(LEFT_ALIGNMENT);

            JLabel descLbl = new JLabel("<html><div style='width:185'>" + desc + "</div></html>");
            descLbl.setForeground(selected ? VeilPanel.TEXT : VeilPanel.MUTED);
            descLbl.setFont(FontManager.getRunescapeSmallFont());
            descLbl.setAlignmentX(LEFT_ALIGNMENT);

            card.add(titleLbl);
            card.add(Box.createVerticalStrut(2));
            card.add(subLbl);
            card.add(Box.createVerticalStrut(2));
            card.add(descLbl);

            if (!selected) {
                JButton selectBtn = VeilPanel.btn("Select " + key, VeilPanel.SURFACE2, VeilPanel.GOLD);
                selectBtn.setAlignmentX(LEFT_ALIGNMENT);
                selectBtn.addActionListener(e -> {
                    selectedPersonality = key;
                    removeAll();
                    build();
                    revalidate(); repaint();
                });
                card.add(Box.createVerticalStrut(4));
                card.add(selectBtn);
            } else {
                JLabel selLbl = new JLabel("✓ SELECTED");
                selLbl.setForeground(VeilPanel.GOLD);
                selLbl.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
                selLbl.setAlignmentX(LEFT_ALIGNMENT);
                card.add(Box.createVerticalStrut(4));
                card.add(selLbl);
            }

            add(card);
            add(Box.createVerticalStrut(4));
        }

        // Discord webhook setup
        add(Box.createVerticalStrut(8));
        add(VeilPanel.bold("DISCORD ALERTS", VeilPanel.PURPLE));
        add(VeilPanel.muted("Get price alerts sent to your Discord."));
        add(Box.createVerticalStrut(4));

        JTextField webhookField = VeilPanel.field("Paste Discord webhook URL here...");
        webhookField.setAlignmentX(LEFT_ALIGNMENT);
        webhookField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JButton testBtn = VeilPanel.btn("Send Test Message", VeilPanel.PURPLE, VeilPanel.TEXT);
        testBtn.addActionListener(e -> {
            String url = webhookField.getText().trim();
            if (url.startsWith("https://discord.com/api/webhooks/")) {
                sendDiscordMessage(url, "🎮 Veil connected! Price alerts will appear here.");
            }
        });

        JLabel webhookHint = new JLabel("<html><div style='width:195'>In Discord: right-click channel → Edit Channel → Integrations → Webhooks → New Webhook → Copy URL</div></html>");
        webhookHint.setForeground(VeilPanel.MUTED);
        webhookHint.setFont(FontManager.getRunescapeSmallFont());
        webhookHint.setAlignmentX(LEFT_ALIGNMENT);

        add(webhookField);
        add(Box.createVerticalStrut(4));
        add(testBtn);
        add(Box.createVerticalStrut(4));
        add(webhookHint);
    }

    private void sendDiscordMessage(String webhookUrl, String message)
    {
        new Thread(() -> {
            try {
                String payload = "{\"content\":\"" + message.replace("\"","\\\"") + "\"}";
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    new java.net.URL(webhookUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.getOutputStream().write(payload.getBytes("UTF-8"));
                int code = conn.getResponseCode();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null,
                        code == 204 ? "✓ Discord message sent!" : "Error: HTTP " + code,
                        "Veil Discord", JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(null,
                        "Failed: " + ex.getMessage(), "Veil Discord", JOptionPane.ERROR_MESSAGE));
            }
        }, "veil-discord").start();
    }

    public String getPersonality() { return selectedPersonality; }
}


// ════════════════════════════════════════════════════════════
// HISTORY TAB — Persistent flip history from disk
// Shows last 30 days, per-item stats, best flips, daily totals
// ════════════════════════════════════════════════════════════
class HistoryTab extends JPanel
{
    private final VeilPlugin plugin;
    private JPanel content;

    HistoryTab(VeilPlugin plugin) {
        this.plugin = plugin;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(VeilPanel.BG);
        topRow.setAlignmentX(LEFT_ALIGNMENT);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel hdr = new JLabel("FLIP HISTORY");
        hdr.setForeground(VeilPanel.GOLD);
        hdr.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnRow.setBackground(VeilPanel.BG);
        JButton ref = VeilPanel.btn("↺", VeilPanel.SURFACE2, VeilPanel.GOLD);
        ref.addActionListener(e -> refresh());
        JButton csvBtn = VeilPanel.btn("⬇ CSV", VeilPanel.SURFACE2, VeilPanel.GREEN);
        csvBtn.addActionListener(e -> exportCsv());
        btnRow.add(ref); btnRow.add(csvBtn);
        topRow.add(hdr, BorderLayout.WEST);
        topRow.add(btnRow, BorderLayout.EAST);
        add(topRow);
        add(VeilPanel.muted("Loaded from disk. Persists across sessions."));
        add(Box.createVerticalStrut(6));

        content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(VeilPanel.BG);
        add(content);
        refresh();
    }

    private void exportCsv()
    {
        try {
            java.io.File f = new java.io.File(
                net.runelite.client.RuneLite.RUNELITE_DIR, "veil/trades_export.csv");
            f.getParentFile().mkdirs();
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(f))) {
                pw.println("Date,Item,Type,Quantity,Price,Profit,RSN");
                List<TradeRecord> history = plugin.getPersistentHistory();
                List<TradeRecord> session = plugin.getSessionTrades();
                Map<String, TradeRecord> all = new java.util.LinkedHashMap<>();
                for (TradeRecord r : history) all.put(r.slot + "_" + r.openedAt, r);
                for (TradeRecord r : session) all.put(r.slot + "_" + r.openedAt, r);
                for (TradeRecord r : all.values()) {
                    if (!r.complete) continue;
                    String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(
                        new java.util.Date(r.closedAt));
                    pw.printf("%s,%s,%s,%d,%d,%d,%s%n",
                        date, r.itemName.replace(",",""),
                        r.isBuy ? "BUY" : "SELL",
                        r.quantityTraded, r.pricePerUnit,
                        r.profitGp, r.rsn != null ? r.rsn : "");
                }
            }
            SwingUtilities.invokeLater(() ->
                javax.swing.JOptionPane.showMessageDialog(null,
                    "Exported to:\n" + f.getAbsolutePath(),
                    "Veil Export", javax.swing.JOptionPane.INFORMATION_MESSAGE));
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() ->
                javax.swing.JOptionPane.showMessageDialog(null,
                    "Export failed: " + ex.getMessage(),
                    "Veil Export", javax.swing.JOptionPane.ERROR_MESSAGE));
        }
    }

    void refresh()
    {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();

            List<TradeRecord> history = plugin.getPersistentHistory();
            List<TradeRecord> session = plugin.getSessionTrades();

            // Combine: persistent + session (dedup by slot+openedAt)
            Map<String, TradeRecord> combined = new LinkedHashMap<>();
            for (TradeRecord r : history) combined.put(r.slot + "_" + r.openedAt, r);
            for (TradeRecord r : session) combined.put(r.slot + "_" + r.openedAt, r);
            List<TradeRecord> all = new ArrayList<>(combined.values());
            all.sort((a, b) -> Long.compare(b.closedAt, a.closedAt));

            List<TradeRecord> completed = all.stream()
                .filter(r -> r.complete)
                .collect(Collectors.toList());
            // Show count of ALL completed trades, profit from sell records only
            long totalProfit = completed.stream()
                .mapToLong(r -> (long) r.profitGp).sum();

            if (completed.isEmpty()) {
                content.add(VeilPanel.muted("No completed flips recorded yet."));
                content.add(VeilPanel.muted("Complete a GE trade to start tracking."));
                content.revalidate(); content.repaint();
                return;
            }

            // ── ALL-TIME STATS ────────────────────────────────

            int totalTrades  = completed.size();
            long bestProfit  = completed.stream().mapToLong(r -> r.profitGp).max().orElse(0);
            TradeRecord bestTrade = completed.stream()
                .max(Comparator.comparingInt(r -> r.profitGp)).orElse(null);
            double avgProfit = totalTrades > 0 ? (double) totalProfit / totalTrades : 0;

            JPanel statsCard = VeilPanel.card("ALL-TIME STATS");
            statsCard.setAlignmentX(LEFT_ALIGNMENT);
            statsCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
            statsCard.add(VeilPanel.bigRow("Total profit:", VeilPanel.fmtGp(totalProfit) + " gp", totalProfit >= 0 ? VeilPanel.GREEN : VeilPanel.RED));
            statsCard.add(VeilPanel.row("Total trades:", String.valueOf(totalTrades), VeilPanel.MUTED));
            statsCard.add(VeilPanel.row("Avg/trade:", VeilPanel.fmtGp((long) avgProfit) + " gp", VeilPanel.MUTED));
            if (bestTrade != null)
                statsCard.add(VeilPanel.row("Best flip:", VeilPanel.clip(bestTrade.itemName, 16) + " +" + VeilPanel.fmtGp(bestProfit), VeilPanel.PURPLE));
            content.add(statsCard);
            content.add(Box.createVerticalStrut(6));

            // ── PER-ITEM BREAKDOWN ────────────────────────────
            Map<String, long[]> itemStats = new LinkedHashMap<>(); // name → [totalProfit, count]
            for (TradeRecord r : completed) {
                itemStats.computeIfAbsent(r.itemName, k -> new long[2]);
                itemStats.get(r.itemName)[0] += r.profitGp;
                itemStats.get(r.itemName)[1]++;
            }

            List<Map.Entry<String, long[]>> sorted = itemStats.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .collect(Collectors.toList());

            JPanel itemCard = VeilPanel.card("BY ITEM (by total profit)");
            itemCard.setAlignmentX(LEFT_ALIGNMENT);
            itemCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
            for (Map.Entry<String, long[]> e : sorted.subList(0, Math.min(15, sorted.size()))) {
                long profit = e.getValue()[0];
                long count  = e.getValue()[1];
                long avg    = count > 0 ? profit / count : 0;
                Color c = profit > 0 ? VeilPanel.GREEN : VeilPanel.RED;
                itemCard.add(VeilPanel.bigRow(e.getKey(),
                    VeilPanel.fmtGp(profit) + " total", c));
                itemCard.add(VeilPanel.row("  " + count + " trades:", "avg " + VeilPanel.fmtGp(avg), VeilPanel.MUTED));
                itemCard.add(Box.createVerticalStrut(3));
            }
            content.add(itemCard);
            content.add(Box.createVerticalStrut(6));

            // ── BUY LIMIT COUNTDOWNS ──────────────────────────
            Map<Integer, Long> resets   = plugin.getBuyLimitResetAt();
            Map<Integer, Long> openedAt = plugin.getBuyLimitOpenedAt();
            Map<Integer, String> names  = plugin.getBuyLimitItemName();
            if (!resets.isEmpty()) {
                JPanel limitCard = VeilPanel.card("BUY LIMIT RESETS");
                limitCard.setAlignmentX(LEFT_ALIGNMENT);
                limitCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
                limitCard.add(VeilPanel.muted("4hr reset starts when offer is PLACED, not when it fills."));
                limitCard.add(Box.createVerticalStrut(4));

                long now = System.currentTimeMillis();
                for (Map.Entry<Integer, Long> e : resets.entrySet()) {
                    int itemId = e.getKey();
                    long resetMs = e.getValue();
                    long msLeft  = resetMs - now;
                    String name  = names.getOrDefault(itemId, "Item #" + itemId);

                    JPanel row = new JPanel(new BorderLayout(4,0));
                    row.setBackground(VeilPanel.SURFACE);
                    row.setBorder(new EmptyBorder(5, 8, 5, 8));
                    row.setAlignmentX(LEFT_ALIGNMENT);
                    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

                    if (msLeft <= 0) {
                        // Limit has reset
                        JLabel l = new JLabel("✓ " + name);
                        l.setForeground(VeilPanel.GREEN);
                        l.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
                        JLabel r = new JLabel("LIMIT RESET — buy now!");
                        r.setForeground(VeilPanel.GREEN);
                        r.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
                        row.add(l, BorderLayout.WEST);
                        row.add(r, BorderLayout.EAST);
                    } else {
                        long h = msLeft / 3_600_000, m = (msLeft % 3_600_000) / 60_000;
                        String timeStr = (h > 0 ? h + "h " : "") + m + "m";
                        // Optimal re-post time = when limit resets
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        cal.setTimeInMillis(resetMs);
                        String resetTime = String.format("%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE));

                        JPanel inner = new JPanel();
                        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
                        inner.setBackground(VeilPanel.SURFACE);
                        inner.setAlignmentX(LEFT_ALIGNMENT);
                        JLabel l = new JLabel(name);
                        l.setForeground(VeilPanel.TEXT);
                        l.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
                        JLabel t = new JLabel("Resets in " + timeStr + " (at " + resetTime + ")");
                        t.setForeground(VeilPanel.AMBER);
                        t.setFont(FontManager.getRunescapeSmallFont());
                        JLabel tip = new JLabel("→ Re-post buy at " + resetTime + " for max cycles/day");
                        tip.setForeground(VeilPanel.MUTED);
                        tip.setFont(FontManager.getRunescapeSmallFont());
                        inner.add(l); inner.add(t); inner.add(tip);
                        row.add(inner, BorderLayout.CENTER);
                    }
                    limitCard.add(row);
                    limitCard.add(Box.createVerticalStrut(3));
                }
                content.add(limitCard);
                content.add(Box.createVerticalStrut(6));
            }

            // ── RECENT TRADES ─────────────────────────────────
            JPanel recentCard = VeilPanel.card("RECENT TRADES");
            recentCard.setAlignmentX(LEFT_ALIGNMENT);
            recentCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
            for (TradeRecord r : completed.subList(0, Math.min(20, completed.size()))) {
                JPanel row = new JPanel(new BorderLayout(4, 0));
                row.setBackground(VeilPanel.SURFACE);
                row.setAlignmentX(LEFT_ALIGNMENT);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
                String type = r.isBuy ? "[B]" : "[S]";
                Color  col  = r.profitGp > 0 ? VeilPanel.GREEN : r.profitGp < 0 ? VeilPanel.RED : VeilPanel.MUTED;
                JLabel l = new JLabel(type + " " + r.itemName + " ×" + r.quantityTraded);
                l.setForeground(VeilPanel.MUTED);
                l.setFont(FontManager.getRunescapeSmallFont());
                JLabel rv = new JLabel(VeilPanel.fmtSigned(r.profitGp) + " gp");
                rv.setForeground(col);
                rv.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
                row.add(l, BorderLayout.WEST);
                row.add(rv, BorderLayout.EAST);
                recentCard.add(row);
            }
            content.add(recentCard);
            // ── PROFIT CHART ─────────────────────────────
            List<long[]> gpHistory = plugin.getProfitHistory();
            if (gpHistory != null && gpHistory.size() >= 3) {
                JPanel chartCard = VeilPanel.card("SESSION GP CHART");
                chartCard.setAlignmentX(LEFT_ALIGNMENT);
                chartCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
                chartCard.add(new ProfitChartPanel(gpHistory));
                content.add(chartCard);
                content.add(Box.createVerticalStrut(6));
            }

            content.revalidate(); content.repaint();
        });
    }
}


// ════════════════════════════════════════════════════════════
// PROFIT CHART PANEL
// ════════════════════════════════════════════════════════════
class ProfitChartPanel extends JPanel
{
    private final List<long[]> data;
    ProfitChartPanel(List<long[]> data) {
        this.data = data;
        setBackground(VeilPanel.SURFACE);
        setPreferredSize(new Dimension(200, 80));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
    }
    @Override protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);
        if (data.size() < 2) return;
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth() - 16, h = getHeight() - 20, ox = 8, oy = 6;
        long minGp = Long.MAX_VALUE, maxGp = Long.MIN_VALUE;
        long minTs = data.get(0)[0], maxTs = data.get(data.size()-1)[0];
        for (long[] pt : data) { minGp = Math.min(minGp, pt[1]); maxGp = Math.max(maxGp, pt[1]); }
        if (maxGp == minGp) maxGp = minGp + 1;
        long gpRange = maxGp - minGp, tsRange = Math.max(maxTs - minTs, 1);

        // Grid line at zero
        int zeroY = oy + (int)(h - (0 - minGp) * h / gpRange);
        g2.setColor(new java.awt.Color(80, 80, 100, 80));
        g2.drawLine(ox, zeroY, ox + w, zeroY);

        // Line chart
        g2.setStroke(new java.awt.BasicStroke(1.5f));
        for (int i = 1; i < data.size(); i++) {
            long[] a = data.get(i-1), b = data.get(i);
            int x1 = ox + (int)((a[0]-minTs) * w / tsRange);
            int y1 = oy + (int)(h - (a[1]-minGp) * h / gpRange);
            int x2 = ox + (int)((b[0]-minTs) * w / tsRange);
            int y2 = oy + (int)(h - (b[1]-minGp) * h / gpRange);
            g2.setColor(b[1] >= a[1] ? new java.awt.Color(0x57, 0xAB, 0x5A) : new java.awt.Color(0xE5, 0x53, 0x4B));
            g2.drawLine(x1, y1, x2, y2);
        }

        // Labels
        g2.setFont(FontManager.getRunescapeSmallFont());
        g2.setColor(VeilPanel.MUTED);
        g2.drawString(VeilPanel.fmtGp(maxGp), ox + 1, oy + 9);
        g2.drawString(VeilPanel.fmtGp(data.get(data.size()-1)[1]), ox + w - 32, oy + 9);
        g2.setColor(maxGp < 0 ? VeilPanel.RED : VeilPanel.GREEN);
        g2.drawString((maxGp >= 0 ? "+" : "") + VeilPanel.fmtGp(data.get(data.size()-1)[1]), ox, oy + h + 14);
    }
}


// ════════════════════════════════════════════════════════════
// BOSS GP/HR TAB — Live prices, not static estimates
// Updates every 60s. When drop prices change, GP/hr changes.
// ════════════════════════════════════════════════════════════
class BossGpHrTab extends JPanel
{
    private final VeilPlugin plugin;
    private JPanel content;
    private JLabel updatedLabel;

    // Verified item IDs from live data
    // {bossName: {{dropName, itemId, rate, avgQty}, ...}, killsPerHr}
    private static final Object[][][] BOSS_DROPS = {
        // Zulrah
        {{"Zulrah", 30}, {"Magic fang","12932",1/512.0,1}, {"Tanzanite fang","12931",1/512.0,1},
         {"Serpentine visage","13228",1/512.0,1}, {"Zulrah's scales","12934",1/6.5,251},
         {"Uncut onyx","6571",1/128.0,1}, {"Rune platelegs","1079",1/32.0,1}},
        // Vorkath
        {{"Vorkath", 32}, {"Skeletal visage","22006",1/5000.0,1},
         {"Dragonbone necklace","22111",1/1000.0,1}, {"Dragon dart tip","11232",50.0/1,1},
         {"Draconic visage","11286",1/1000.0,1}, {"Dragon bones","537",2.6/1,1}},
        // Cerberus
        {{"Cerberus", 40}, {"Primordial crystal","13231",1/512.0,1},
         {"Pegasian crystal","13229",1/512.0,1}, {"Eternal crystal","13227",1/512.0,1},
         {"Smouldering stone","13233",1/512.0,1}, {"Dragon bones","537",3.0/1,1}},
        // Abyssal Sire
        {{"Abyssal Sire", 20}, {"Abyssal whip","4151",1/512.0,1},
         {"Abyssal dagger","13265",1/256.0,1}, {"Death rune","560",55.0/1,1}},
        // Alchemical Hydra
        {{"Alch. Hydra", 25}, {"Hydra tail","22988",1/512.0,1},
         {"Hydra leather","22983",1/512.0,1}, {"Hydra's eye","22971",1/1000.0,1},
         {"Hydra's fang","22973",1/1000.0,1}, {"Hydra's heart","22975",1/1000.0,1}},
        // Kraken
        {{"Kraken", 60}, {"Trident of the seas","11940",1/512.0,1},
         {"Kraken tentacle","12004",1/512.0,1}, {"Death rune","560",80.0/1,1}},
        // Grotesque Guardians
        {{"Grotes. Guards.", 18}, {"Granite gloves","13576",1/128.0,1},
         {"Granite ring","13578",1/256.0,1}, {"Granite hammer","21742",1/256.0,1},
         {"Death rune","560",100.0/1,1}},
        // Dagannoth Kings
        {{"Dagannoth Rex", 50}, {"Berserker ring","6737",1/128.0,1},
         {"Warrior ring","6735",1/128.0,1}, {"Dragon bones","537",15.0/1,1}},
    };

    BossGpHrTab(VeilPlugin plugin) {
        this.plugin = plugin;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(VeilPanel.BG);
        topRow.setAlignmentX(LEFT_ALIGNMENT);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel hdr = new JLabel("LIVE BOSS GP/HR");
        hdr.setForeground(VeilPanel.GOLD);
        hdr.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        updatedLabel = new JLabel("updating...");
        updatedLabel.setForeground(VeilPanel.MUTED);
        updatedLabel.setFont(FontManager.getRunescapeSmallFont());
        topRow.add(hdr, BorderLayout.WEST);
        topRow.add(updatedLabel, BorderLayout.EAST);
        add(topRow);
        add(VeilPanel.muted("Live drop prices. Updates every 60s."));
        add(Box.createVerticalStrut(6));

        content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(VeilPanel.BG);
        add(content);
    }

    void refresh(Map<Integer, Integer> livePrices)
    {
        if (livePrices == null || livePrices.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                content.removeAll();
                updatedLabel.setText("waiting...");
                content.add(VeilPanel.muted("Loading boss data..."));
                content.add(VeilPanel.muted("Flip data must load first (60s)."));
                content.revalidate(); content.repaint();
            });
            return;
        }
        SwingUtilities.invokeLater(() -> {
            content.removeAll();
            updatedLabel.setText("Updated " + new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date()));

            // Score all bosses
            List<long[]> bossScores = new ArrayList<>(); // [gpHr, bossIdx]
            for (int bi = 0; bi < BOSS_DROPS.length; bi++) {
                Object[] meta = (Object[]) BOSS_DROPS[bi][0];
                int kph = (int) meta[1];
                double gpKill = 0;
                for (int di = 1; di < BOSS_DROPS[bi].length; di++) {
                    Object[] drop = (Object[]) BOSS_DROPS[bi][di];
                    int itemId = Integer.parseInt((String) drop[1]);
                    double rate = (double) drop[2];
                    int qty = (int)(drop[3] instanceof Double ? (double)drop[3] : (int)drop[3]);
                    int price = livePrices.getOrDefault(itemId, 0);
                    gpKill += rate * price * qty;
                }
                bossScores.add(new long[]{(long)(gpKill * kph), bi});
            }
            bossScores.sort((a, b) -> Long.compare(b[0], a[0]));

            for (long[] bs : bossScores) {
                int bi = (int) bs[1];
                Object[] meta = (Object[]) BOSS_DROPS[bi][0];
                String bossName = (String) meta[0];
                int kph = (int) meta[1];
                long gpHr = bs[0];

                String grade = gpHr > 3_000_000 ? "S" : gpHr > 1_500_000 ? "A"
                    : gpHr > 800_000 ? "B" : gpHr > 300_000 ? "C" : "D";
                Color gradeColor = VeilPanel.gradeColor(grade);

                JPanel card = VeilPanel.card(null);
                card.setAlignmentX(LEFT_ALIGNMENT);
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

                // Header
                JPanel hdrRow = new JPanel(new BorderLayout());
                hdrRow.setBackground(VeilPanel.SURFACE);
                hdrRow.setAlignmentX(LEFT_ALIGNMENT);
                hdrRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
                JLabel nameLbl = new JLabel(VeilPanel.clip(bossName, 20));
                nameLbl.setForeground(VeilPanel.TEXT);
                nameLbl.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
                JLabel gradeLbl = new JLabel(" " + grade + " ");
                gradeLbl.setForeground(gradeColor);
                gradeLbl.setBackground(gradeColor.darker().darker());
                gradeLbl.setOpaque(true);
                gradeLbl.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
                hdrRow.add(nameLbl, BorderLayout.WEST);
                hdrRow.add(gradeLbl, BorderLayout.EAST);
                card.add(hdrRow);
                card.add(Box.createVerticalStrut(4));

                card.add(VeilPanel.bigRow("GP/hr:", VeilPanel.fmtGp(gpHr) + "/hr", gradeColor));
                card.add(VeilPanel.row("Kills/hr:", kph + "/hr", VeilPanel.MUTED));
                card.add(Box.createVerticalStrut(3));

                // Top 3 drops by expected value
                List<double[]> drops = new ArrayList<>();
                for (int di = 1; di < BOSS_DROPS[bi].length; di++) {
                    Object[] drop = (Object[]) BOSS_DROPS[bi][di];
                    String dropName = (String) drop[0];
                    int itemId = Integer.parseInt((String) drop[1]);
                    double rate = (double) drop[2];
                    int qty = (int)(drop[3] instanceof Double ? (double)drop[3] : (int)drop[3]);
                    int price = livePrices.getOrDefault(itemId, 0);
                    double ev = rate * price * qty;
                    drops.add(new double[]{ev, di});
                }
                drops.sort((a, b) -> Double.compare(b[0], a[0]));

                card.add(VeilPanel.bold("Top drops (expected per kill):", VeilPanel.MUTED));
                for (double[] d : drops.subList(0, Math.min(3, drops.size()))) {
                    int di = (int) d[1];
                    Object[] drop = (Object[]) BOSS_DROPS[bi][di];
                    String dropName = (String) drop[0];
                    long ev = (long) d[0];
                    if (ev > 0)
                        card.add(VeilPanel.row("  " + dropName + ":", VeilPanel.fmtGp(ev) + " gp/kill avg", VeilPanel.MUTED));
                }

                content.add(card);
                content.add(Box.createVerticalStrut(4));
            }

            content.revalidate(); content.repaint();
        });
    }
}


// ════════════════════════════════════════════════════════════
// STATS TAB — Real hiscores data + personalised advice
//              + Watchlist P&L tracker
//              + Bot-warned flip list
// ════════════════════════════════════════════════════════════
class StatsTab extends JPanel
{
    private final VeilPlugin plugin;
    private JPanel content;

    StatsTab(VeilPlugin plugin) {
        this.plugin = plugin;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(VeilPanel.BG);
        topRow.setAlignmentX(LEFT_ALIGNMENT);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel hdr = new JLabel("YOUR STATS");
        hdr.setForeground(VeilPanel.GOLD);
        hdr.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        JButton ref = VeilPanel.btn("↺ Refresh", VeilPanel.SURFACE2, VeilPanel.GOLD);
        ref.addActionListener(e -> {
            String rsn = plugin.getClient() != null && plugin.getClient().getLocalPlayer() != null
                ? plugin.getClient().getLocalPlayer().getName() : null;
            if (rsn != null) HiscoresService.fetchAsync(rsn);
            SwingUtilities.invokeLater(() -> { try { Thread.sleep(2000); } catch(Exception ex){} refresh(); });
        });
        topRow.add(hdr, BorderLayout.WEST);
        topRow.add(ref, BorderLayout.EAST);
        add(topRow);
        add(VeilPanel.muted("Live from OSRS Hiscores. Logs in at start of session."));
        add(Box.createVerticalStrut(6));

        content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(VeilPanel.BG);
        add(content);
        refresh();
    }

    void refresh()
    {
        // Trigger hiscores fetch if not loaded yet
        HiscoresService.PlayerStats existingStats = plugin.getHiscoreStats();
        if (existingStats == null || !existingStats.loaded) {
            // Try to get player name from plugin client
            try {
                net.runelite.api.Client cl = plugin.getClient();
                if (cl != null && cl.getLocalPlayer() != null) {
                    String rsn = cl.getLocalPlayer().getName();
                    if (rsn != null) HiscoresService.fetchAsync(rsn);
                }
            } catch (Exception ignored) {}
        }
        SwingUtilities.invokeLater(() -> {
            content.removeAll();

            HiscoresService.PlayerStats stats = plugin.getHiscoreStats();

            if (stats == null || !stats.loaded) {
                content.add(VeilPanel.muted("Loading from hiscores..."));
                content.add(VeilPanel.muted("Will appear once you log in."));
                content.revalidate(); return;
            }

            // ── HEADER ────────────────────────────────────────
            JPanel header = VeilPanel.card(null);
            header.setAlignmentX(LEFT_ALIGNMENT);
            header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            header.add(VeilPanel.bigRow(stats.rsn, "Total level: " + stats.getTotalLevel(), VeilPanel.GOLD));
            header.add(VeilPanel.muted("Live from OSRS Hiscores"));
            content.add(header);
            content.add(Box.createVerticalStrut(6));

            // ── PERSONALISED ADVICE ───────────────────────────
            String advice = HiscoresService.getPersonalisedAdvice(stats);
            if (!advice.isEmpty()) {
                JPanel advCard = VeilPanel.card("PERSONALISED ADVICE");
                advCard.setAlignmentX(LEFT_ALIGNMENT);
                advCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
                for (String line : advice.split("\n")) {
                    Color lc = line.contains(":") && !line.startsWith("  ")
                        ? VeilPanel.GOLD : line.startsWith("  ") ? VeilPanel.TEXT : VeilPanel.MUTED;
                    JLabel l = new JLabel("<html><div style='width:195'>" +
                        line.replace("  ", "&nbsp;&nbsp;") + "</div></html>");
                    l.setForeground(lc);
                    l.setFont(line.contains(":") && !line.startsWith("  ")
                        ? FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD)
                        : FontManager.getRunescapeSmallFont());
                    l.setAlignmentX(LEFT_ALIGNMENT);
                    advCard.add(l);
                }
                content.add(advCard);
                content.add(Box.createVerticalStrut(6));
            }

            // ── COMBAT SKILLS ─────────────────────────────────
            String[] combatSkills = {"Attack","Strength","Defence","Hitpoints","Ranged","Prayer","Magic","Slayer"};
            JPanel combatCard = VeilPanel.card("COMBAT SKILLS");
            combatCard.setAlignmentX(LEFT_ALIGNMENT);
            combatCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
            for (String s : combatSkills) {
                int lvl = stats.getLevel(s);
                if (lvl > 1) {
                    Color c = lvl >= 99 ? VeilPanel.PURPLE : lvl >= 80 ? VeilPanel.GOLD
                        : lvl >= 60 ? VeilPanel.GREEN : VeilPanel.MUTED;
                    combatCard.add(VeilPanel.row(s + ":", lvl + (lvl >= 99 ? " (MAX)" : ""), c));
                }
            }
            content.add(combatCard);
            content.add(Box.createVerticalStrut(6));

            // ── SKILLING ──────────────────────────────────────
            String[] skillSkills = {"Herblore","Farming","Crafting","Smithing","Fletching",
                                    "Runecraft","Agility","Thieving","Hunter","Construction"};
            JPanel skillCard = VeilPanel.card("SKILLING");
            skillCard.setAlignmentX(LEFT_ALIGNMENT);
            skillCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
            for (String s : skillSkills) {
                int lvl = stats.getLevel(s);
                if (lvl > 1) {
                    Color c = lvl >= 99 ? VeilPanel.PURPLE : lvl >= 70 ? VeilPanel.GOLD : VeilPanel.MUTED;
                    skillCard.add(VeilPanel.row(s + ":", String.valueOf(lvl), c));
                }
            }
            content.add(skillCard);
            content.add(Box.createVerticalStrut(6));

            // ── BOSS KC ───────────────────────────────────────
            if (!stats.bossKc.isEmpty()) {
                JPanel bossCard = VeilPanel.card("BOSS KC");
                bossCard.setAlignmentX(LEFT_ALIGNMENT);
                bossCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
                stats.bossKc.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .forEach(e -> bossCard.add(VeilPanel.row(e.getKey() + ":", e.getValue() + " KC", VeilPanel.GOLD)));
                content.add(bossCard);
                content.add(Box.createVerticalStrut(6));
            }

            // ── WHAT CAN I DO? (level-gated content) ─────────
            JPanel gateCard = VeilPanel.card("CONTENT UNLOCKED");
            gateCard.setAlignmentX(LEFT_ALIGNMENT);
            gateCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
            int slayer = stats.getLevel("Slayer");
            int magic  = stats.getLevel("Magic");
            int herb   = stats.getLevel("Herblore");
            int farm   = stats.getLevel("Farming");
            int craft  = stats.getLevel("Crafting");
            int agility= stats.getLevel("Agility");

            checkGate(gateCard, "Kraken (slayer boss)",   slayer >= 87, "87 Slayer", slayer, 87);
            checkGate(gateCard, "Abyssal demons",         slayer >= 85, "85 Slayer", slayer, 85);
            checkGate(gateCard, "Cave krakens",           slayer >= 87, "87 Slayer", slayer, 87);
            checkGate(gateCard, "Gargoyles",              slayer >= 75, "75 Slayer", slayer, 75);
            checkGate(gateCard, "Cerberus",               slayer >= 91, "91 Slayer", slayer, 91);
            checkGate(gateCard, "Hydra",                  slayer >= 95, "95 Slayer", slayer, 95);
            checkGate(gateCard, "Superheat (magic)",      magic  >= 43, "43 Magic",  magic,  43);
            checkGate(gateCard, "Trident of the seas",    magic  >= 75, "75 Magic",  magic,  75);
            checkGate(gateCard, "Prayer potions",         herb   >= 38, "38 Herb",   herb,   38);
            checkGate(gateCard, "Super restore",          herb   >= 63, "63 Herb",   herb,   63);
            checkGate(gateCard, "Bastion potions",        herb   >= 80, "80 Herb",   herb,   80);
            checkGate(gateCard, "Herb farming",           farm   >= 9,  "9 Farming", farm,   9);
            checkGate(gateCard, "Snapdragon farming",     farm   >= 62, "62 Farm",   farm,   62);
            checkGate(gateCard, "Zenyte jewellery",       craft  >= 98, "98 Craft",  craft,  98);
            checkGate(gateCard, "Graceful (agility)",     agility>= 60, "60+ Agil",  agility, 60);
            content.add(gateCard);

            content.revalidate(); content.repaint();
        });
    }

    private void checkGate(JPanel card, String content, boolean unlocked, String req, int current, int needed)
    {
        int left = needed - current;
        Color c = unlocked ? VeilPanel.GREEN : left <= 5 ? VeilPanel.AMBER : VeilPanel.RED;
        String status = unlocked ? "✓ UNLOCKED" : "✗ Need " + left + " more levels";
        card.add(VeilPanel.row(VeilPanel.clip(content, 18) + ":", status, c));
    }
}


// ════════════════════════════════════════════════════════════
// WATCHLIST P&L TAB — Unrealized profit on every position
// ════════════════════════════════════════════════════════════
class WatchlistTab extends JPanel
{
    private final VeilPlugin plugin;
    private JPanel listPanel;
    private JTextField itemField, buyPriceField, qtyField;

    static class Position {
        String name; int itemId; long buyPrice; int qty; long addedAt;
        Position(String n, int id, long bp, int q) {
            name=n; itemId=id; buyPrice=bp; qty=q; addedAt=System.currentTimeMillis();
        }
    }

    private final List<Position> positions = new ArrayList<>();

    WatchlistTab(VeilPlugin plugin) {
        this.plugin = plugin;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        build();
    }

    private void build()
    {
        add(VeilPanel.bold("PORTFOLIO TRACKER", VeilPanel.GOLD));
        add(VeilPanel.muted("Track P&L on items you're holding."));
        add(Box.createVerticalStrut(6));

        // Add position form
        JPanel form = VeilPanel.card("ADD POSITION");
        form.setAlignmentX(LEFT_ALIGNMENT);
        form.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));

        itemField     = VeilPanel.field("Item name...");
        itemField.setAlignmentX(LEFT_ALIGNMENT);
        itemField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        buyPriceField = VeilPanel.field("Buy price (e.g. 23.2m)");
        buyPriceField.setAlignmentX(LEFT_ALIGNMENT);
        buyPriceField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        qtyField      = VeilPanel.field("Quantity");
        qtyField.setAlignmentX(LEFT_ALIGNMENT);
        qtyField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JButton addBtn = VeilPanel.btn("+ Track Position", VeilPanel.GOLD, VeilPanel.BG);
        addBtn.addActionListener(e -> addPosition());

        form.add(itemField);
        form.add(Box.createVerticalStrut(4));
        form.add(buyPriceField);
        form.add(Box.createVerticalStrut(4));
        form.add(qtyField);
        form.add(Box.createVerticalStrut(6));
        form.add(addBtn);
        add(form);
        add(Box.createVerticalStrut(8));

        add(VeilPanel.bold("YOUR POSITIONS", VeilPanel.GOLD));
        add(Box.createVerticalStrut(4));

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(VeilPanel.BG);
        add(listPanel);
    }

    private void addPosition()
    {
        String name = itemField.getText().trim();
        if (name.isEmpty()) return;
        long bp  = parseGp(buyPriceField.getText());
        int qty   = parseQty(qtyField.getText());
        if (bp <= 0 || qty <= 0) return;

        // Try to match item ID from flip cache
        int itemId = 0;
        for (FlipSignal f : plugin.getCachedFlips()) {
            if (f.itemName.toLowerCase().contains(name.toLowerCase())) {
                itemId = f.itemId; name = f.itemName; break;
            }
        }
        positions.add(0, new Position(name, itemId, bp, qty));
        itemField.setText(""); buyPriceField.setText(""); qtyField.setText("");
        refresh();
    }

    void refresh()
    {
        // Build live price map from flip cache
        Map<Integer, FlipSignal> flipMap = new HashMap<>();
        for (FlipSignal f : plugin.getCachedFlips()) flipMap.put(f.itemId, f);

        listPanel.removeAll();
        if (positions.isEmpty()) {
            listPanel.add(VeilPanel.muted("No positions tracked. Add items you've bought above."));
        }

        long totalUnrealizedPnl = 0;

        for (int i = 0; i < positions.size(); i++) {
            Position p = positions.get(i);
            FlipSignal sig = flipMap.get(p.itemId);

            JPanel card = VeilPanel.card(null);
            card.setAlignmentX(LEFT_ALIGNMENT);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));

            // Current price
            long currentMid = 0;
            int  currentSell = 0;
            if (sig != null) {
                currentMid  = (sig.buyPrice + sig.sellPrice) / 2L;
                currentSell = sig.sellPrice - 1;
            }

            long costBasis = p.buyPrice * p.qty;
            long tax = currentSell > 0 ? Math.min(5_000_000, Math.max(1, (int)(currentSell * 0.01))) * p.qty : 0;
            long proceeds = (long) currentSell * p.qty - tax;
            long unrealizedPnl = currentSell > 0 ? proceeds - costBasis : 0;
            double pct = p.buyPrice > 0 && currentSell > 0 ? (currentSell - p.buyPrice) * 100.0 / p.buyPrice : 0;
            totalUnrealizedPnl += unrealizedPnl;

            // Header
            Color pnlColor = unrealizedPnl > 0 ? VeilPanel.GREEN
                : unrealizedPnl < 0 ? VeilPanel.RED : VeilPanel.MUTED;
            String arrow = unrealizedPnl > 0 ? "▲" : unrealizedPnl < 0 ? "▼" : "→";
            card.add(VeilPanel.bigRow(arrow + " " + p.name,
                unrealizedPnl != 0 ? VeilPanel.fmtSigned(unrealizedPnl) + " gp" : "no data", pnlColor));

            card.add(VeilPanel.row("Your cost:", VeilPanel.fmtGp(p.buyPrice) + "×" + p.qty + "=" + VeilPanel.fmtGp(costBasis), VeilPanel.MUTED));

            if (sig != null) {
                card.add(VeilPanel.row("Current sell:", VeilPanel.fmtGp(currentSell) + " gp ea", VeilPanel.TEXT));
                card.add(VeilPanel.row("Change:", String.format("%+.2f%%", pct), pnlColor));
                card.add(VeilPanel.row("After tax sell:", VeilPanel.fmtGp(proceeds) + " total", VeilPanel.TEXT));
                card.add(VeilPanel.bigRow("Unrealized P&L:", VeilPanel.fmtSigned(unrealizedPnl) + " gp", pnlColor));

                // Sell signal
                card.add(Box.createVerticalStrut(3));
                String sellAdvice;
                Color  sellColor;
                if ("LOW RISK".equals(sig.sellRisk) && unrealizedPnl > 0) {
                    sellAdvice = "SELL NOW — low risk, profit";
                    sellColor  = VeilPanel.GREEN;
                } else if ("HIGH RISK".equals(sig.sellRisk)) {
                    sellAdvice = "HOLD — HIGH RISK sell price";
                    sellColor  = VeilPanel.RED;
                } else if ("DOWN".equals(sig.predTrend)) {
                    sellAdvice = "SELL SOON — trending down";
                    sellColor  = VeilPanel.AMBER;
                } else if (unrealizedPnl < 0) {
                    sellAdvice = "HOLD — still at a loss";
                    sellColor  = VeilPanel.AMBER;
                } else {
                    sellAdvice = "HOLD — stable";
                    sellColor  = VeilPanel.MUTED;
                }
                card.add(VeilPanel.bigRow("Verdict:", sellAdvice, sellColor));

                // Sell confidence
                if (sig.sellConfidence > 0)
                    card.add(VeilPanel.row("Sell confidence:", sig.sellConfidence + "/100 [" + sig.sellRisk + "]", pnlColor));
            } else {
                card.add(VeilPanel.muted("Price data loading — check Flips tab"));
            }

            // Remove button
            card.add(Box.createVerticalStrut(3));
            JButton rm = VeilPanel.btn("Remove", VeilPanel.SURFACE2, VeilPanel.RED);
            final int idx = i;
            rm.addActionListener(e -> { positions.remove(idx); refresh(); });
            card.add(rm);

            listPanel.add(card);
            listPanel.add(Box.createVerticalStrut(4));
        }

        // Portfolio total
        if (positions.size() > 1) {
            JPanel total = VeilPanel.card("PORTFOLIO TOTAL");
            total.setAlignmentX(LEFT_ALIGNMENT);
            total.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            total.add(VeilPanel.bigRow("Total unrealized P&L:", VeilPanel.fmtSigned(totalUnrealizedPnl) + " gp",
                totalUnrealizedPnl >= 0 ? VeilPanel.GREEN : VeilPanel.RED));
            listPanel.add(total);
        }

        listPanel.revalidate(); listPanel.repaint();
    }

    private static long parseGp(String s) {
        try { s=s.trim().toLowerCase().replaceAll(",","");
            if(s.endsWith("b")) return (long)(Double.parseDouble(s.replace("b",""))*1_000_000_000);
            if(s.endsWith("m")) return (long)(Double.parseDouble(s.replace("m",""))*1_000_000);
            if(s.endsWith("k")) return (long)(Double.parseDouble(s.replace("k",""))*1_000);
            return Long.parseLong(s); } catch(Exception e){ return 0; }
    }
    private static int parseQty(String s) { try{return Integer.parseInt(s.trim());}catch(Exception e){return 1;} }
}
