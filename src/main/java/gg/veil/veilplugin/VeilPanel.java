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
        tabs.setFont(FontManager.getRunescapeSmallFont());

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

        tabs.addTab("Home",     scroll(dashTab));
        tabs.addTab("Flips",    scroll(flipsTab));
        tabs.addTab("Trades",   scroll(tradesTab));
        tabs.addTab("Wallet",   scroll(portfolioTab));
        tabs.addTab("Intel",    scroll(intelTab));
        tabs.addTab("XP",       scroll(skillsTab));
        tabs.addTab("Tracker",  scroll(trackerTab));
        tabs.addTab("Alch",     scroll(alchTab));
        tabs.addTab("Slots",    scroll(slotTab));
        tabs.addTab("Alerts",   scroll(alertsTab));

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

        JLabel title = new JLabel("VEIL  6.0");
        title.setForeground(GOLD);
        title.setFont(FontManager.getRunescapeBoldFont().deriveFont(13f));
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel credits = new JLabel("RSN: MorpheusXP  ·  Discord: Morpheus7239");
        credits.setForeground(MUTED);
        credits.setFont(FontManager.getRunescapeSmallFont().deriveFont(9f));
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
        JScrollPane sp = new JScrollPane(p);
        sp.setBackground(BG);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
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
        JLabel l  = new JLabel(left);  l.setForeground(MUTED);       l.setFont(FontManager.getRunescapeSmallFont());
        JLabel rv = new JLabel(right); rv.setForeground(rightColor);  rv.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        r.add(l, BorderLayout.WEST);
        r.add(rv, BorderLayout.EAST);
        return r;
    }

    static JPanel bigRow(String left, String right, Color rightColor)
    {
        JPanel r = new JPanel(new BorderLayout(4, 0));
        r.setBackground(SURFACE);
        r.setAlignmentX(LEFT_ALIGNMENT);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel l  = new JLabel(left);  l.setForeground(TEXT);         l.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        JLabel rv = new JLabel(right); rv.setForeground(rightColor);  rv.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
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
        JTextField f = new JTextField();
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
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        l.setFont(FontManager.getRunescapeSmallFont());
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    static JLabel bold(String text, Color color)
    {
        JLabel l = new JLabel(text);
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
        JLabel name = new JLabel(rec.itemName);
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
        bar.setString(pct + "%  (" + rec.quantityTraded + " / " + rec.quantityOffered + ")");
        bar.setBackground(VeilPanel.BG);
        bar.setForeground(pct == 100 ? VeilPanel.GREEN : rec.isBuy ? VeilPanel.BLUE : VeilPanel.GOLD);
        bar.setBorderPainted(false);
        bar.setFont(FontManager.getRunescapeSmallFont());
        bar.setAlignmentX(LEFT_ALIGNMENT);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        card.add(bar);
        card.add(Box.createVerticalStrut(4));

        card.add(VeilPanel.row("You offered:", VeilPanel.fmtGp(rec.pricePerUnit) + " gp ea", VeilPanel.MUTED));

        if (rec.isBuy && signal != null) {
            int sellAt  = signal.sellPrice - 1;
            int tax     = Math.min(5_000_000, Math.max(1, (int)(sellAt * 0.01)));
            int profEa  = sellAt - rec.pricePerUnit - tax;
            int profTot = profEa * Math.max(rec.quantityTraded, 1);
            card.add(Box.createVerticalStrut(3));
            card.add(VeilPanel.bigRow("→ SELL AT:", VeilPanel.fmtGp(sellAt) + " gp ea", VeilPanel.GREEN));
            card.add(VeilPanel.row("  Profit ea:", VeilPanel.fmtSigned(profEa) + " (after 1% tax)", profEa > 0 ? VeilPanel.GREEN : VeilPanel.RED));
            if (rec.quantityTraded > 0)
                card.add(VeilPanel.bigRow("  Total profit:", VeilPanel.fmtSigned(profTot), profTot > 0 ? VeilPanel.GREEN : VeilPanel.RED));
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

        card.add(VeilPanel.bigRow(w.name, "", VeilPanel.TEXT));
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
                        case "S": tierLabel = "━━━ S TIER — ELITE (3M+ GP/hr) ━━━"; tierColor = VeilPanel.PURPLE; break;
                        case "A": tierLabel = "━━━ A TIER — GREAT (1M+ GP/hr) ━━━"; tierColor = VeilPanel.GOLD; break;
                        case "B": tierLabel = "━━━ B TIER — SOLID (300k+ GP/hr) ━━━"; tierColor = VeilPanel.GREEN; break;
                        case "C": tierLabel = "━━━ C TIER — DECENT (50k+ GP/hr) ━━━"; tierColor = VeilPanel.MUTED; break;
                        default:  tierLabel = "━━━ D TIER — LOW PRIORITY ━━━"; tierColor = VeilPanel.MUTED; break;
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
        JLabel l = new JLabel(text);
        l.setForeground(VeilPanel.MUTED);
        l.setFont(FontManager.getRunescapeSmallFont());
        l.setAlignmentX(LEFT_ALIGNMENT);
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
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 600));

        // Header: rank + name + badges
        JPanel hdr = new JPanel(new BorderLayout(4, 0));
        hdr.setBackground(VeilPanel.SURFACE);
        hdr.setAlignmentX(LEFT_ALIGNMENT);
        hdr.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JLabel nameL = new JLabel("#" + rank + "  " + f.itemName);
        nameL.setForeground(VeilPanel.TEXT);
        nameL.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        badges.setBackground(VeilPanel.SURFACE);
        badges.add(badge(f.grade, VeilPanel.gradeColor(f.grade)));
        badges.add(badge(f.signal, VeilPanel.signalColor(f.signal)));
        if (!f.members) badges.add(badge("F2P", VeilPanel.BLUE));

        hdr.add(nameL, BorderLayout.WEST);
        hdr.add(badges, BorderLayout.EAST);
        card.add(hdr);
        card.add(Box.createVerticalStrut(5));

        // THE CORE FLIP INFO
        card.add(VeilPanel.bigRow("BUY  at:", VeilPanel.fmtGp(f.buyPrice) + " gp ea", VeilPanel.GREEN));
        card.add(VeilPanel.bigRow("SELL at:", VeilPanel.fmtGp(f.sellPrice - 1) + " gp ea (fast fill)", VeilPanel.GOLD));
        card.add(VeilPanel.bigRow("SELL at:", VeilPanel.fmtGp(f.sellPrice + 1) + " gp ea (patient)", VeilPanel.AMBER));
        card.add(Box.createVerticalStrut(3));
        card.add(VeilPanel.row("Net profit ea:", "+" + VeilPanel.fmtGp(f.netMargin) + " gp  (" + String.format("%.1f%%", f.roi) + " ROI)", VeilPanel.GREEN));
        card.add(Box.createVerticalStrut(3));

        // ── HOW MANY TO BUY ─────────────────────────────────
        card.add(Box.createVerticalStrut(2));
        if (coins > 0) {
            long canAfford   = Math.min(coins / Math.max(f.buyPrice, 1), f.buyLimit);
            long totalCost   = canAfford * f.buyPrice;
            long totalProfit = canAfford * f.netMargin;
            long leftover    = coins - totalCost;

            card.add(VeilPanel.bigRow("► BUY exactly:", canAfford + "× at " + VeilPanel.fmtGp(f.buyPrice) + " ea", VeilPanel.BLUE));
            card.add(VeilPanel.row("  Total cost:", VeilPanel.fmtGp(totalCost) + " gp  (" + VeilPanel.fmtGp(leftover) + " left over)", VeilPanel.MUTED));
            card.add(VeilPanel.bigRow("► SELL at:", VeilPanel.fmtGp(f.sellPrice - 1) + " gp ea", VeilPanel.GREEN));
            card.add(VeilPanel.bigRow("  Total profit:", VeilPanel.fmtSigned(totalProfit) + " gp on this trade", totalProfit > 0 ? VeilPanel.GREEN : VeilPanel.RED));
            card.add(Box.createVerticalStrut(3));

            // Step by step instructions
            card.add(stepLabel("HOW TO DO THIS FLIP:"));
            card.add(stepLabel("  1. GE → Buy → '" + f.itemName + "'"));
            card.add(stepLabel("  2. Qty: " + canAfford + "×   Price: " + String.format("%,d", f.buyPrice) + " gp"));
            card.add(stepLabel("  3. Wait ~" + f.fillMins + " min to fill"));
            card.add(stepLabel("  4. Sell " + canAfford + "× at " + String.format("%,d", f.sellPrice - 1) + " gp"));
            card.add(stepLabel("  5. Profit: +" + VeilPanel.fmtGp(totalProfit) + " gp — repeat!"));
        } else {
            card.add(stepLabel("Open inventory to see exact quantities"));
            card.add(VeilPanel.row("  Full limit cost:", VeilPanel.fmtGp((long)f.buyPrice * f.buyLimit) + " gp", VeilPanel.MUTED));
            card.add(VeilPanel.row("  Full limit profit:", "+" + VeilPanel.fmtGp((long)f.netMargin * f.buyLimit) + " gp", VeilPanel.GREEN));
        }
        card.add(Box.createVerticalStrut(3));

        // Market stats
        card.add(VeilPanel.row("GP/hr:", VeilPanel.fmtGp(f.score) + "/hr", VeilPanel.GOLD));
        card.add(VeilPanel.row("Fill time:", f.fillMins + " min", f.fillMins < 30 ? VeilPanel.GREEN : VeilPanel.AMBER));
        card.add(VeilPanel.row("Buy limit:", f.buyLimit + " per 4hrs", VeilPanel.MUTED));
        card.add(VeilPanel.row("Vol/hr (24h avg):", VeilPanel.fmtGp(f.hourVol) + " trades/hr", f.hourVol > f.buyLimit * 4 ? VeilPanel.GREEN : VeilPanel.AMBER));
        card.add(VeilPanel.row("Market:", String.format("%.1f×", f.pressure) + " pressure  "
            + String.format("%+.1f%%", f.momentum) + " momentum",
            f.pressure >= 1.2 ? VeilPanel.GREEN : f.pressure < 0.8 ? VeilPanel.RED : VeilPanel.MUTED));

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
        add(VeilPanel.bold("AUTO-TRACKED (from GE)", VeilPanel.GOLD));
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
                JLabel l = new JLabel((r.isBuy?"[B] ":"[S] ") + r.itemName + " ×" + r.quantityTraded);
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
        card.add(VeilPanel.row("Filled:", rec.quantityTraded + " / " + rec.quantityOffered + "  (" + pct + "%)", VeilPanel.TEXT));
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
            JPanel afford = VeilPanel.card("WHAT TO FLIP RIGHT NOW");
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
                afford.add(VeilPanel.bigRow(f.itemName, f.grade + " · " + f.signal, VeilPanel.gradeColor(f.grade)));
                afford.add(VeilPanel.row("  Buy " + qty + "× @", VeilPanel.fmtGp(f.buyPrice) + " = " + VeilPanel.fmtGp(cost), VeilPanel.MUTED));
                afford.add(VeilPanel.row("  Sell @", VeilPanel.fmtGp(f.sellPrice - 1) + " → profit +" + VeilPanel.fmtGp(profit), VeilPanel.GREEN));
                afford.add(Box.createVerticalStrut(5));
                shown++;
            }
            if (shown > 0) {
                afford.add(VeilPanel.bigRow("Expected total:", "+" + VeilPanel.fmtGp(totalExpected) + " gp", VeilPanel.GREEN));
                afford.add(VeilPanel.row("ROI on bankroll:", String.format("%.2f%%", totalExpected * 100.0 / coins), VeilPanel.GOLD));
            } else {
                afford.add(VeilPanel.muted("No affordable flips found"));
            }
            content.add(afford);
            content.add(Box.createVerticalStrut(8));

            if (totalExpected > 0 && shown > 0) {
                JPanel comp = VeilPanel.card("COMPOUNDING (3 cycles/day)");
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
            JPanel plan = VeilPanel.card("YOUR PLAN RIGHT NOW");
            plan.setAlignmentX(LEFT_ALIGNMENT);
            plan.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
            for (String line : intel.sessionPlan.split("\n")) {
                JLabel l = new JLabel("<html>" + line.replace("★","⚡").replace("🔥","★") + "</html>");
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
            time.add(VeilPanel.row("Flip now:", intel.timeContext.bestCategories, VeilPanel.GREEN));
            if (intel.timeContext.avoidCategories != null && !intel.timeContext.avoidCategories.isEmpty())
                time.add(VeilPanel.row("Avoid:", intel.timeContext.avoidCategories, VeilPanel.RED));
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
                shock.add(VeilPanel.row("  " + s.alert, "", VeilPanel.MUTED));
                shock.add(VeilPanel.row("  Buy @", VeilPanel.fmtGp(s.buyPrice) + "  Margin +" + VeilPanel.fmtGp(s.netMargin), VeilPanel.GREEN));
                shock.add(Box.createVerticalStrut(3));
            }
            content.add(shock);
            content.add(Box.createVerticalStrut(6));
        }

        // Thin market gems
        if (!intel.thinMarketGems.isEmpty()) {
            JPanel gems = VeilPanel.card("THIN MARKET GEMS — ZERO BOT COMPETITION");
            gems.setAlignmentX(LEFT_ALIGNMENT);
            gems.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
            gems.add(VeilPanel.muted("Items with buy limit ≤ 10. Bots skip them. You capture 90%+ of spread."));
            gems.add(Box.createVerticalStrut(5));
            for (MarketIntelligence.ThinMarketGem g : intel.thinMarketGems.subList(0, Math.min(8, intel.thinMarketGems.size()))) {
                gems.add(VeilPanel.bigRow(g.itemName, "Limit: " + g.buyLimit, VeilPanel.PURPLE));
                gems.add(VeilPanel.row("  Buy @", VeilPanel.fmtGp(g.buyPrice), VeilPanel.MUTED));
                gems.add(VeilPanel.row("  Sell @", VeilPanel.fmtGp(g.sellPrice - 1), VeilPanel.GREEN));
                gems.add(VeilPanel.row("  Per 4hr cycle:", "+" + VeilPanel.fmtGp((long)g.netMargin * g.buyLimit) + "  (" + String.format("%.1f%%",g.roi) + " ROI)", VeilPanel.GREEN));
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
            JPanel spikes = VeilPanel.card("PRICE SPIKES (last 30min)");
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
            sumCard.add(VeilPanel.muted("Real-time via StatChanged — no API polling"));
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
            skillCard.add(VeilPanel.row(skill.getName(), "+" + VeilPanel.fmtXp(g) + (rate > 0 ? "  (" + VeilPanel.fmtXp(rate) + "/hr)" : ""), VeilPanel.GREEN));
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
        if (eq != null && eq.totalValue > 0) {
            JPanel dr = VeilPanel.card("DEATH RISK");
            dr.setAlignmentX(LEFT_ALIGNMENT);
            dr.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
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
        add(VeilPanel.muted("Items where high alch profit > 0 right now. Includes nature rune cost."));
        add(Box.createVerticalStrut(6));

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(VeilPanel.BG);
        add(listPanel);
    }

    void refresh()
    {
        List<FlipSignal> flips = plugin.getCachedFlips();
        List<FlipSignal> alchable = flips.stream()
            .filter(f -> f.alchProfit > 0)
            .sorted((a, b) -> Integer.compare(b.alchProfit * Math.min(b.buyLimit, b.hourVol),
                                               a.alchProfit * Math.min(a.buyLimit, a.hourVol)))
            .collect(Collectors.toList());

        SwingUtilities.invokeLater(() -> {
            listPanel.removeAll();
            countLabel.setText(alchable.size() + " profitable alch items");

            if (alchable.isEmpty()) {
                listPanel.add(VeilPanel.muted("No profitable alch items right now"));
            }

            for (FlipSignal f : alchable.subList(0, Math.min(20, alchable.size()))) {
                JPanel card = VeilPanel.card(null);
                card.setAlignmentX(LEFT_ALIGNMENT);
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
                card.add(VeilPanel.bigRow(f.itemName, "+"+VeilPanel.fmtGp(f.alchProfit)+" ea", VeilPanel.GREEN));
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
        add(VeilPanel.muted("Optimal allocation across all 8 GE slots."));
        add(VeilPanel.muted("Staggers fill times so you're always collecting."));
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
            summaryLabel.setText("Combined GP/hr: ~" + VeilPanel.fmtGp(totalGpHr) + "/hr  |  " + portfolio.size() + " slots assigned");

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
                JLabel slotLbl = new JLabel("SLOT " + (i+1) + "  [" + (i < tiers.length ? tiers[i] : "?") + "]");
                slotLbl.setForeground(i < tc.length ? tc[i] : VeilPanel.MUTED);
                slotLbl.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
                JLabel gpLbl = new JLabel(VeilPanel.fmtGp(f.score) + "/hr");
                gpLbl.setForeground(VeilPanel.GOLD);
                gpLbl.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
                top.add(slotLbl, BorderLayout.WEST);
                top.add(gpLbl, BorderLayout.EAST);
                card.add(top);
                card.add(Box.createVerticalStrut(3));

                card.add(VeilPanel.bigRow(f.itemName, f.grade + " · " + f.signal, VeilPanel.gradeColor(f.grade)));
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
        goalCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

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
        addAlert.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

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
        JRadioButton belowBtn = new JRadioButton("Alert when price drops BELOW target");
        JRadioButton aboveBtn = new JRadioButton("Alert when price rises ABOVE target");
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
