package gg.veil.veilplugin;

import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Veil 4.0 Side Panel
 * 5 tabs: Dashboard | Flip Finder | My Trades | Portfolio | History
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

    private final VeilPlugin plugin;

    // Sub-panels
    private DashboardPanel   dashPanel;
    private FlipFinderPanel  flipPanel;
    private MyTradesPanel    tradesPanel;
    private PortfolioPanel   portfolioPanel;
    private HistoryPanel     historyPanel;
    private IntelligencePanel intelPanel;

    private JTabbedPane tabs;

    @Inject
    public VeilPanel(VeilPlugin plugin)
    {
        this.plugin = plugin;
        setBackground(BG);
        setLayout(new BorderLayout());
        build();
    }

    private void build()
    {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(SURFACE);
        header.setBorder(new CompoundBorder(
            new MatteBorder(0,0,1,0,BORDER),
            new EmptyBorder(8,10,8,10)));

        JPanel titleCol = new JPanel();
        titleCol.setLayout(new BoxLayout(titleCol, BoxLayout.Y_AXIS));
        titleCol.setBackground(SURFACE);

        JLabel title = new JLabel("VEIL  5.0");
        title.setForeground(GOLD);
        title.setFont(FontManager.getRunescapeBoldFont().deriveFont(13f));
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel credits = new JLabel("RSN: MorpheusXP  |  Discord: Morpheus7239");
        credits.setForeground(MUTED);
        credits.setFont(FontManager.getRunescapeSmallFont().deriveFont(9f));
        credits.setAlignmentX(LEFT_ALIGNMENT);

        titleCol.add(title);
        titleCol.add(credits);

        JLabel coin = new JLabel("loading...");
        coin.setForeground(GREEN);
        coin.setFont(FontManager.getRunescapeSmallFont());
        coin.setName("coinLabel");

        header.add(titleCol, BorderLayout.WEST);
        header.add(coin, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Tabs
        tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setBackground(BG);
        tabs.setForeground(MUTED);
        tabs.setFont(FontManager.getRunescapeSmallFont());

        dashPanel      = new DashboardPanel(plugin, this);
        flipPanel      = new FlipFinderPanel(plugin, this);
        tradesPanel    = new MyTradesPanel(plugin, this);
        portfolioPanel = new PortfolioPanel(plugin, this);
        historyPanel   = new HistoryPanel(plugin, this);
        intelPanel     = new IntelligencePanel(plugin, this);

        tabs.addTab("Dashboard", wrap(dashPanel));
        tabs.addTab("Flips",     wrap(flipPanel));
        tabs.addTab("Trades",    wrap(tradesPanel));
        tabs.addTab("Portfolio", wrap(portfolioPanel));
        tabs.addTab("History",   wrap(historyPanel));

        IntelligencePanel intelPanel = new IntelligencePanel(plugin, this);
        tabs.addTab("Intel", wrap(intelPanel));

        // Style tabs
        tabs.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            protected void installDefaults() {
                super.installDefaults();
                highlight = BG;
                lightHighlight = BORDER;
                shadow = BG;
                darkShadow = BG;
                focus = GOLD;
            }
        });

        add(tabs, BorderLayout.CENTER);
    }

    private JScrollPane wrap(JPanel p)
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
            updateCoinLabel();
            dashPanel.refresh();
            flipPanel.refresh();
            tradesPanel.refresh();
            portfolioPanel.refresh();
        });
    }

    public void refreshFlips()
    {
        SwingUtilities.invokeLater(() -> {
            flipPanel.refresh();
            portfolioPanel.refresh();
            dashPanel.refresh();
            if (intelPanel != null) intelPanel.refresh();
        });
    }

    public void updateSession()
    {
        SwingUtilities.invokeLater(() -> {
            updateCoinLabel();
            dashPanel.refresh();
            tradesPanel.refresh();
        });
    }

    private void updateCoinLabel()
    {
        Component[] comps = ((JPanel)getComponent(0)).getComponents();
        for (Component c : comps) {
            if ("coinLabel".equals(c.getName()) && c instanceof JLabel) {
                long coins = plugin.getCoinStack();
                ((JLabel)c).setText("💰 " + fmtGp(coins));
            }
        }
    }

    // ── Shared helpers ─────────────────────────────────────────

    static String fmtGp(long gp)
    {
        if (gp >= 1_000_000_000) return String.format("%.1fB", gp / 1_000_000_000.0);
        if (gp >= 1_000_000)     return String.format("%.1fM", gp / 1_000_000.0);
        if (gp >= 1_000)         return String.format("%.0fk", gp / 1_000.0);
        return String.valueOf(gp);
    }

    static String fmtGpSigned(long gp)
    {
        return (gp >= 0 ? "+" : "") + fmtGp(gp);
    }

    static JPanel card(String title)
    {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(SURFACE);
        p.setBorder(new CompoundBorder(
            new MatteBorder(1,1,1,1,BORDER),
            new EmptyBorder(8,10,8,10)));
        if (title != null) {
            JLabel lbl = new JLabel(title);
            lbl.setForeground(MUTED);
            lbl.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
            lbl.setAlignmentX(LEFT_ALIGNMENT);
            p.add(lbl);
            p.add(Box.createVerticalStrut(6));
        }
        return p;
    }

    static JPanel row(String left, String right, Color rightColor)
    {
        JPanel r = new JPanel(new BorderLayout(4,0));
        r.setBackground(SURFACE);
        r.setAlignmentX(LEFT_ALIGNMENT);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        JLabel l = new JLabel(left); l.setForeground(MUTED); l.setFont(FontManager.getRunescapeSmallFont());
        JLabel rv = new JLabel(right); rv.setForeground(rightColor); rv.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        r.add(l, BorderLayout.WEST);
        r.add(rv, BorderLayout.EAST);
        return r;
    }

    static JPanel bigRow(String left, String right, Color rightColor)
    {
        JPanel r = new JPanel(new BorderLayout(4,0));
        r.setBackground(SURFACE);
        r.setAlignmentX(LEFT_ALIGNMENT);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel l = new JLabel(left); l.setForeground(TEXT); l.setFont(FontManager.getRunescapeBoldFont().deriveFont(12f));
        JLabel rv = new JLabel(right); rv.setForeground(rightColor); rv.setFont(FontManager.getRunescapeBoldFont().deriveFont(12f));
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
        b.setBorder(new EmptyBorder(5,10,5,10));
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
        f.setBorder(new CompoundBorder(new MatteBorder(1,1,1,1,BORDER), new EmptyBorder(4,6,4,6)));
        f.setFont(FontManager.getRunescapeSmallFont());
        f.putClientProperty("placeholder", placeholder);
        return f;
    }

    static Color gradeColor(String g)
    {
        switch(g) { case "S": return PURPLE; case "A": return GOLD; case "B": return GREEN; default: return MUTED; }
    }

    static Color signalColor(String s)
    {
        if ("ENTER".equals(s)) return GREEN;
        if ("EXIT".equals(s))  return RED;
        return AMBER;
    }
}


// ══════════════════════════════════════════════════════════════
// DASHBOARD TAB
// ══════════════════════════════════════════════════════════════
class DashboardPanel extends JPanel
{
    private final VeilPlugin plugin;
    private final VeilPanel  veil;
    private JPanel slotsPanel;
    private JLabel coinLabel, plLabel, statusLabel;

    DashboardPanel(VeilPlugin plugin, VeilPanel veil)
    {
        this.plugin = plugin; this.veil = veil;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8,8,8,8));
        build();
    }

    private void build()
    {
        // Coin stack — big and prominent
        JPanel coinCard = VeilPanel.card(null);
        coinCard.setAlignmentX(LEFT_ALIGNMENT);
        coinCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        coinLabel = new JLabel("Loading coin stack...");
        coinLabel.setForeground(VeilPanel.GOLD);
        coinLabel.setFont(FontManager.getRunescapeBoldFont().deriveFont(16f));
        coinLabel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel coinSub = new JLabel("Your coin stack (live)");
        coinSub.setForeground(VeilPanel.MUTED);
        coinSub.setFont(FontManager.getRunescapeSmallFont());
        coinSub.setAlignmentX(LEFT_ALIGNMENT);

        coinCard.add(coinLabel);
        coinCard.add(coinSub);
        add(coinCard);
        add(Box.createVerticalStrut(6));

        // Session P&L
        JPanel plCard = VeilPanel.card("Session");
        plCard.setAlignmentX(LEFT_ALIGNMENT);
        plCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        plLabel = new JLabel("+0 gp");
        plLabel.setForeground(VeilPanel.GREEN);
        plLabel.setFont(FontManager.getRunescapeBoldFont().deriveFont(14f));
        plLabel.setAlignmentX(LEFT_ALIGNMENT);
        plCard.add(plLabel);
        add(plCard);
        add(Box.createVerticalStrut(6));

        // Active GE slots header
        JLabel slotHeader = new JLabel("ACTIVE GE SLOTS");
        slotHeader.setForeground(VeilPanel.GOLD);
        slotHeader.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        slotHeader.setAlignmentX(LEFT_ALIGNMENT);
        add(slotHeader);
        add(Box.createVerticalStrut(4));

        slotsPanel = new JPanel();
        slotsPanel.setLayout(new BoxLayout(slotsPanel, BoxLayout.Y_AXIS));
        slotsPanel.setBackground(VeilPanel.BG);
        slotsPanel.setAlignmentX(LEFT_ALIGNMENT);
        add(slotsPanel);

        add(Box.createVerticalStrut(8));
        statusLabel = new JLabel("Flip data loading...");
        statusLabel.setForeground(VeilPanel.MUTED);
        statusLabel.setFont(FontManager.getRunescapeSmallFont());
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);
        add(statusLabel);
    }

    void refresh()
    {
        // Coin stack
        long coins = plugin.getCoinStack();
        coinLabel.setText(VeilPanel.fmtGp(coins) + " GP");

        // Session
        VeilPlugin.SessionStats s = plugin.getSessionStats();
        int loot = plugin.getSessionLootGp();
        long total = s.sessionProfitGp + loot;
        plLabel.setText(VeilPanel.fmtGpSigned(total) + " GP   (GE: " +
            VeilPanel.fmtGpSigned(s.sessionProfitGp) + "  Loot: +" + VeilPanel.fmtGp(loot) + ")");
        plLabel.setForeground(total >= 0 ? VeilPanel.GREEN : VeilPanel.RED);

        // GE slots
        slotsPanel.removeAll();
        List<TradeRecord> active = plugin.getActiveOffers();
        List<FlipSignal> flips = plugin.getCachedFlips();

        // Build flip lookup by itemId
        Map<Integer,FlipSignal> flipMap = new HashMap<>();
        for (FlipSignal f : flips) flipMap.put(f.itemId, f);

        if (active.isEmpty()) {
            JLabel none = new JLabel("No active GE offers");
            none.setForeground(VeilPanel.MUTED);
            none.setFont(FontManager.getRunescapeSmallFont());
            slotsPanel.add(none);
        }

        for (TradeRecord rec : active)
        {
            FlipSignal signal = flipMap.get(rec.itemId);
            slotsPanel.add(buildSlotCard(rec, signal));
            slotsPanel.add(Box.createVerticalStrut(4));
        }

        // Status
        int flipCount = flips.size();
        // Get total scored vs total tradeable
        int totalScored = plugin.getCachedFlips().size();
        statusLabel.setText(totalScored > 0
            ? "Scanning all 4,035 GE tradeable items  ·  " + totalScored + " profitable  ·  " + (coins > 0 ? "Coins: " + VeilPanel.fmtGp(coins) : "Open inv for coins")
            : "Loading flip data... scanning all GE items");

        slotsPanel.revalidate();
        slotsPanel.repaint();
    }

    private JPanel buildSlotCard(TradeRecord rec, FlipSignal signal)
    {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(VeilPanel.SURFACE);
        card.setBorder(new CompoundBorder(
            new MatteBorder(0, rec.isBuy ? 2 : 0, 0, rec.isBuy ? 0 : 2,
                rec.isBuy ? VeilPanel.GREEN : VeilPanel.GOLD),
            new EmptyBorder(7,10,7,10)));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        // Item name + type badge
        JPanel top = new JPanel(new BorderLayout(4,0));
        top.setBackground(VeilPanel.SURFACE);
        top.setAlignmentX(LEFT_ALIGNMENT);
        top.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JLabel name = new JLabel(rec.itemName);
        name.setForeground(VeilPanel.TEXT);
        name.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));

        JLabel type = new JLabel(rec.isBuy ? " BUYING " : " SELLING ");
        type.setForeground(rec.isBuy ? VeilPanel.GREEN : VeilPanel.GOLD);
        type.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        type.setBackground(rec.isBuy ? VeilPanel.GREEN.darker().darker() : VeilPanel.GOLD.darker().darker());
        type.setOpaque(true);

        top.add(name, BorderLayout.WEST);
        top.add(type, BorderLayout.EAST);
        card.add(top);
        card.add(Box.createVerticalStrut(4));

        // Fill progress
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

        // THE KEY FEATURE: BUY @ and SELL @ prices
        card.add(VeilPanel.row("You bought @", VeilPanel.fmtGp(rec.pricePerUnit) + " gp ea", VeilPanel.MUTED));

        if (rec.isBuy && signal != null)
        {
            // Item is being bought — tell them exactly what to sell it for
            int sellFast = signal.sellPrice - 1;
            int sellMax  = signal.sellPrice + 1;
            int tax      = Math.min(5_000_000, Math.max(1, (int)(sellFast * 0.01)));
            int profitFast = sellFast - rec.pricePerUnit - tax;
            int profitMax  = sellMax  - rec.pricePerUnit - Math.min(5_000_000, Math.max(1,(int)(sellMax*0.01)));

            card.add(Box.createVerticalStrut(2));
            card.add(VeilPanel.bigRow("→ SELL at (fast fill):",
                VeilPanel.fmtGp(sellFast) + " gp", VeilPanel.GREEN));
            card.add(VeilPanel.row("  Profit per item:",
                VeilPanel.fmtGpSigned(profitFast) + " gp (after tax)",
                profitFast > 0 ? VeilPanel.GREEN : VeilPanel.RED));
            card.add(Box.createVerticalStrut(2));
            card.add(VeilPanel.bigRow("→ SELL at (max profit):",
                VeilPanel.fmtGp(sellMax) + " gp", VeilPanel.AMBER));
            card.add(VeilPanel.row("  Profit per item:",
                VeilPanel.fmtGpSigned(profitMax) + " gp (after tax)",
                profitMax > 0 ? VeilPanel.AMBER : VeilPanel.RED));

            if (rec.quantityTraded > 0) {
                int totalProfit = profitFast * rec.quantityTraded;
                card.add(Box.createVerticalStrut(2));
                card.add(VeilPanel.bigRow("Total profit if sold:",
                    VeilPanel.fmtGpSigned(totalProfit) + " gp",
                    totalProfit > 0 ? VeilPanel.GREEN : VeilPanel.RED));
            }
        }
        else if (!rec.isBuy)
        {
            // Selling — show what they're selling at and expected GP
            card.add(VeilPanel.row("Selling @",
                VeilPanel.fmtGp(rec.pricePerUnit) + " gp ea", VeilPanel.GOLD));
            if (signal != null) {
                int tax = Math.min(5_000_000, Math.max(1,(int)(rec.pricePerUnit * 0.01)));
                int netGp = (rec.pricePerUnit - tax) * rec.quantityOffered;
                card.add(VeilPanel.row("Est. GP after tax:",
                    VeilPanel.fmtGp(netGp) + " gp total", VeilPanel.GREEN));
            }
        }
        else
        {
            // No signal data yet
            card.add(VeilPanel.row("Sell price:", "Loading market data...", VeilPanel.MUTED));
        }

        return card;
    }
}


// ══════════════════════════════════════════════════════════════
// FLIP FINDER TAB — search every item
// ══════════════════════════════════════════════════════════════
class FlipFinderPanel extends JPanel
{
    private final VeilPlugin plugin;
    private final VeilPanel  veil;
    private JTextField searchField;
    private JComboBox<String> sortBox;
    private JSlider maxBuySlider;
    private JLabel sliderLabel, resultCount;
    private JPanel listPanel;
    private List<FlipSignal> allFlips = new ArrayList<>();

    FlipFinderPanel(VeilPlugin plugin, VeilPanel veil)
    {
        this.plugin = plugin; this.veil = veil;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8,8,8,8));
        build();
    }

    private void build()
    {
        // Search bar
        JPanel searchRow = new JPanel(new BorderLayout(4,0));
        searchRow.setBackground(VeilPanel.BG);
        searchRow.setAlignmentX(LEFT_ALIGNMENT);
        searchRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        searchField = VeilPanel.field("Search any item...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { filter(); }
            public void removeUpdate(DocumentEvent e)  { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });
        searchRow.add(searchField, BorderLayout.CENTER);
        add(searchRow);
        add(Box.createVerticalStrut(6));

        // Filters row
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filterRow.setBackground(VeilPanel.BG);
        filterRow.setAlignmentX(LEFT_ALIGNMENT);

        sortBox = new JComboBox<>(new String[]{"GP/hr","Margin","ROI","Fill Speed"});
        sortBox.setBackground(VeilPanel.SURFACE);
        sortBox.setForeground(VeilPanel.TEXT);
        sortBox.setFont(FontManager.getRunescapeSmallFont());
        sortBox.addActionListener(e -> filter());
        filterRow.add(new JLabel("Sort: ") {{setForeground(VeilPanel.MUTED);setFont(FontManager.getRunescapeSmallFont());}});
        filterRow.add(sortBox);
        add(filterRow);
        add(Box.createVerticalStrut(4));

        // Max buy price slider (based on coin stack)
        JPanel sliderRow = new JPanel(new BorderLayout(4,0));
        sliderRow.setBackground(VeilPanel.BG);
        sliderRow.setAlignmentX(LEFT_ALIGNMENT);
        sliderRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel sliderLbl = new JLabel("Max buy: ");
        sliderLbl.setForeground(VeilPanel.MUTED);
        sliderLbl.setFont(FontManager.getRunescapeSmallFont());
        maxBuySlider = new JSlider(0, 500, 500);
        maxBuySlider.setBackground(VeilPanel.BG);
        maxBuySlider.addChangeListener(e -> { updateSliderLabel(); filter(); });
        sliderLabel = new JLabel("Any");
        sliderLabel.setForeground(VeilPanel.GOLD);
        sliderLabel.setFont(FontManager.getRunescapeSmallFont());
        sliderRow.add(sliderLbl, BorderLayout.WEST);
        sliderRow.add(maxBuySlider, BorderLayout.CENTER);
        sliderRow.add(sliderLabel, BorderLayout.EAST);
        add(sliderRow);
        add(Box.createVerticalStrut(4));

        // Result count
        resultCount = new JLabel("Loading...");
        resultCount.setForeground(VeilPanel.MUTED);
        resultCount.setFont(FontManager.getRunescapeSmallFont());
        resultCount.setAlignmentX(LEFT_ALIGNMENT);
        add(resultCount);
        add(Box.createVerticalStrut(4));

        // Results
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(VeilPanel.BG);
        add(listPanel);
    }

    private void updateSliderLabel()
    {
        int val = maxBuySlider.getValue();
        if (val >= 500) { sliderLabel.setText("Any"); return; }
        long coins = plugin.getCoinStack();
        long maxBuy = coins > 0 ? (long)(coins * val / 100.0) : val * 1_000_000L;
        sliderLabel.setText(VeilPanel.fmtGp(maxBuy));
    }

    private long getMaxBuyPrice()
    {
        if (maxBuySlider.getValue() >= 500) return Long.MAX_VALUE;
        long coins = plugin.getCoinStack();
        if (coins > 0) return (long)(coins * maxBuySlider.getValue() / 100.0);
        return maxBuySlider.getValue() * 1_000_000L;
    }

    void refresh()
    {
        allFlips = new ArrayList<>(plugin.getCachedFlips());
        filter();
    }

    private void filter()
    {
        String q = searchField.getText().trim().toLowerCase();
        long maxBuy = getMaxBuyPrice();
        String sort = (String) sortBox.getSelectedItem();

        List<FlipSignal> filtered = allFlips.stream()
            .filter(f -> q.isEmpty() || f.itemName.toLowerCase().contains(q))
            .filter(f -> f.buyPrice <= maxBuy)
            .collect(Collectors.toList());

        // Sort
        switch (sort != null ? sort : "GP/hr") {
            case "Margin":     filtered.sort((a,b) -> Integer.compare(b.netMargin, a.netMargin)); break;
            case "ROI":        filtered.sort((a,b) -> Double.compare(b.roi, a.roi)); break;
            case "Fill Speed": filtered.sort((a,b) -> Integer.compare(a.fillMins, b.fillMins)); break;
            default:           filtered.sort((a,b) -> Integer.compare(b.score, a.score)); break;
        }

        final List<FlipSignal> toShow = filtered;
        SwingUtilities.invokeLater(() -> {
            listPanel.removeAll();
            resultCount.setText(toShow.size() + " items" + (q.isEmpty() ? "" : " matching \"" + q + "\""));

            if (toShow.isEmpty()) {
                JLabel none = new JLabel("No items match");
                none.setForeground(VeilPanel.MUTED);
                none.setFont(FontManager.getRunescapeSmallFont());
                listPanel.add(none);
            }

            long coins = plugin.getCoinStack();
            for (int i = 0; i < Math.min(50, toShow.size()); i++) {
                listPanel.add(buildFlipCard(toShow.get(i), i+1, coins));
                listPanel.add(Box.createVerticalStrut(4));
            }

            if (toShow.size() > 50) {
                JLabel more = new JLabel("Showing top 50 — search to narrow down");
                more.setForeground(VeilPanel.MUTED);
                more.setFont(FontManager.getRunescapeSmallFont());
                listPanel.add(more);
            }

            listPanel.revalidate();
            listPanel.repaint();
        });
    }

    private JPanel buildFlipCard(FlipSignal f, int rank, long coins)
    {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(VeilPanel.SURFACE);
        card.setBorder(new CompoundBorder(
            new MatteBorder(0, 3, 0, 0, VeilPanel.gradeColor(f.grade)),
            new EmptyBorder(7,10,7,10)));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 500));

        // Header: rank + name + grade + signal
        JPanel hdr = new JPanel(new BorderLayout(4,0));
        hdr.setBackground(VeilPanel.SURFACE);
        hdr.setAlignmentX(LEFT_ALIGNMENT);
        hdr.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JLabel rankLbl = new JLabel("#" + rank + " ");
        rankLbl.setForeground(VeilPanel.MUTED);
        rankLbl.setFont(FontManager.getRunescapeSmallFont());

        JLabel nameLbl = new JLabel(f.itemName);
        nameLbl.setForeground(VeilPanel.TEXT);
        nameLbl.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        badges.setBackground(VeilPanel.SURFACE);

        JLabel gradeLbl = new JLabel(" " + f.grade + " ");
        gradeLbl.setForeground(VeilPanel.gradeColor(f.grade));
        gradeLbl.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        gradeLbl.setBackground(VeilPanel.gradeColor(f.grade).darker().darker());
        gradeLbl.setOpaque(true);

        JLabel sigLbl = new JLabel(" " + f.signal + " ");
        sigLbl.setForeground(VeilPanel.signalColor(f.signal));
        sigLbl.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        sigLbl.setBackground(VeilPanel.signalColor(f.signal).darker().darker());
        sigLbl.setOpaque(true);

        badges.add(gradeLbl);
        badges.add(sigLbl);

        // F2P badge — helpful to know if you can flip on F2P
        if (!f.members) {
            JLabel f2pLbl = new JLabel(" F2P ");
            f2pLbl.setForeground(VeilPanel.BLUE);
            f2pLbl.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
            f2pLbl.setBackground(VeilPanel.BLUE.darker().darker());
            f2pLbl.setOpaque(true);
            badges.add(f2pLbl);
        }

        JPanel leftHdr = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        leftHdr.setBackground(VeilPanel.SURFACE);
        leftHdr.add(rankLbl);
        leftHdr.add(nameLbl);

        hdr.add(leftHdr, BorderLayout.WEST);
        hdr.add(badges, BorderLayout.EAST);
        card.add(hdr);
        card.add(Box.createVerticalStrut(5));

        // THE MOST IMPORTANT LINES:
        card.add(VeilPanel.bigRow("BUY at:",  VeilPanel.fmtGp(f.buyPrice)  + " gp ea", VeilPanel.GREEN));
        card.add(VeilPanel.bigRow("SELL at:", VeilPanel.fmtGp(f.sellPrice - 1) + " gp ea (fast)", VeilPanel.GOLD));
        card.add(VeilPanel.bigRow("SELL at:", VeilPanel.fmtGp(f.sellPrice + 1) + " gp ea (patient)", VeilPanel.AMBER));
        card.add(Box.createVerticalStrut(3));
        card.add(VeilPanel.row("Profit/item (post-tax):", "+" + VeilPanel.fmtGp(f.netMargin) + " gp", VeilPanel.GREEN));
        card.add(VeilPanel.row("ROI:", String.format("%.1f%%", f.roi), VeilPanel.GREEN));
        card.add(Box.createVerticalStrut(3));

        // How many can YOU afford
        if (coins > 0) {
            long canAfford = Math.min(coins / Math.max(f.buyPrice, 1), f.buyLimit);
            int expectedProfit = (int)(canAfford * f.netMargin);
            card.add(VeilPanel.bigRow("You can afford:", canAfford + "× = " + VeilPanel.fmtGp(coins / Math.max(f.buyPrice,1) * f.buyPrice) + " gp", VeilPanel.BLUE));
            card.add(VeilPanel.row("Expected profit:", VeilPanel.fmtGpSigned(expectedProfit) + " gp", expectedProfit > 0 ? VeilPanel.GREEN : VeilPanel.RED));
            card.add(Box.createVerticalStrut(3));
        }

        card.add(VeilPanel.row("GP/hr estimate:", VeilPanel.fmtGp(f.score) + "/hr", VeilPanel.GOLD));
        card.add(VeilPanel.row("Fill time:", f.fillMins + " min to fill limit order", f.fillMins < 30 ? VeilPanel.GREEN : VeilPanel.AMBER));
        card.add(VeilPanel.row("Buy limit:", f.buyLimit + " per 4 hours", VeilPanel.MUTED));
        card.add(VeilPanel.row("Market pressure:", String.format("%.1f× ", f.pressure) + (f.pressure >= 1.2 ? "more buyers" : f.pressure < 0.8 ? "more sellers ⚠" : "balanced"), f.pressure >= 1.2 ? VeilPanel.GREEN : f.pressure < 0.8 ? VeilPanel.RED : VeilPanel.MUTED));

        // Signal explanation
        String sigExplain = "ENTER".equals(f.signal) ? "Good time — buyers dominate, price stable"
            : "EXIT".equals(f.signal) ? "Caution — sellers increasing or price dropping"
            : "Watch first before committing GP";
        card.add(VeilPanel.row("Advice:", sigExplain, VeilPanel.signalColor(f.signal)));

        // Wiki link
        card.add(Box.createVerticalStrut(4));
        JLabel wiki = new JLabel("<html><u>Price chart ↗</u></html>");
        wiki.setForeground(VeilPanel.BLUE);
        wiki.setFont(FontManager.getRunescapeSmallFont());
        wiki.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        wiki.setAlignmentX(LEFT_ALIGNMENT);
        wiki.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                LinkBrowser.browse("https://prices.runescape.wiki/osrs/item/" + f.itemId);
            }
        });
        card.add(wiki);

        return card;
    }
}


// ══════════════════════════════════════════════════════════════
// MY TRADES TAB — create, view, edit trades
// ══════════════════════════════════════════════════════════════
class MyTradesPanel extends JPanel
{
    private final VeilPlugin plugin;
    private final VeilPanel  veil;
    private JPanel activePanel, manualPanel;

    // Manual trade entry fields
    private JTextField itemNameField, buyPriceField, qtyField;

    // Stored manual trades
    private final List<ManualTrade> manualTrades = new ArrayList<>();

    static class ManualTrade {
        String itemName; int buyPrice; int qty; long timestamp;
        int currentSellPrice; // from flip cache
        ManualTrade(String n, int b, int q) { itemName=n; buyPrice=b; qty=q; timestamp=Instant.now().toEpochMilli(); }
    }

    MyTradesPanel(VeilPlugin plugin, VeilPanel veil)
    {
        this.plugin = plugin; this.veil = veil;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8,8,8,8));
        build();
    }

    private void build()
    {
        // Auto-tracked slots from plugin
        JLabel autoHdr = new JLabel("AUTO-TRACKED (from GE)");
        autoHdr.setForeground(VeilPanel.GOLD);
        autoHdr.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        autoHdr.setAlignmentX(LEFT_ALIGNMENT);
        add(autoHdr);
        add(Box.createVerticalStrut(4));

        activePanel = new JPanel();
        activePanel.setLayout(new BoxLayout(activePanel, BoxLayout.Y_AXIS));
        activePanel.setBackground(VeilPanel.BG);
        add(activePanel);
        add(Box.createVerticalStrut(10));

        // Manual entry section
        JPanel addCard = VeilPanel.card("ADD MANUAL TRADE");
        addCard.setAlignmentX(LEFT_ALIGNMENT);
        addCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        itemNameField = VeilPanel.field("Item name");
        itemNameField.setAlignmentX(LEFT_ALIGNMENT);
        itemNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        buyPriceField = VeilPanel.field("Buy price (e.g. 1.1m)");
        buyPriceField.setAlignmentX(LEFT_ALIGNMENT);
        buyPriceField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        qtyField = VeilPanel.field("Quantity");
        qtyField.setAlignmentX(LEFT_ALIGNMENT);
        qtyField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JButton addBtn = VeilPanel.btn("+ Add Trade", VeilPanel.GOLD, VeilPanel.BG);
        addBtn.setAlignmentX(LEFT_ALIGNMENT);
        addBtn.addActionListener(e -> addManualTrade());

        addCard.add(itemNameField);
        addCard.add(Box.createVerticalStrut(4));
        addCard.add(buyPriceField);
        addCard.add(Box.createVerticalStrut(4));
        addCard.add(qtyField);
        addCard.add(Box.createVerticalStrut(6));
        addCard.add(addBtn);
        add(addCard);
        add(Box.createVerticalStrut(8));

        manualPanel = new JPanel();
        manualPanel.setLayout(new BoxLayout(manualPanel, BoxLayout.Y_AXIS));
        manualPanel.setBackground(VeilPanel.BG);
        add(manualPanel);
    }

    private void addManualTrade()
    {
        String name = itemNameField.getText().trim();
        if (name.isEmpty()) return;
        int price = parseGp(buyPriceField.getText().trim());
        int qty   = parseQty(qtyField.getText().trim());
        if (price <= 0 || qty <= 0) return;

        manualTrades.add(0, new ManualTrade(name, price, qty));
        itemNameField.setText(""); buyPriceField.setText(""); qtyField.setText("");
        refresh();
    }

    private int parseGp(String s)
    {
        try {
            s = s.toLowerCase().replaceAll(",","");
            if (s.endsWith("m")) return (int)(Double.parseDouble(s.replace("m","")) * 1_000_000);
            if (s.endsWith("k")) return (int)(Double.parseDouble(s.replace("k","")) * 1_000);
            return Integer.parseInt(s);
        } catch (Exception e) { return 0; }
    }

    private int parseQty(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; } }

    void refresh()
    {
        // Auto-tracked
        activePanel.removeAll();
        List<TradeRecord> active = plugin.getActiveOffers();
        List<FlipSignal> flips = plugin.getCachedFlips();
        Map<Integer,FlipSignal> flipMap = new HashMap<>();
        for (FlipSignal f : flips) flipMap.put(f.itemId, f);

        if (active.isEmpty()) {
            JLabel none = new JLabel("No active GE offers");
            none.setForeground(VeilPanel.MUTED);
            none.setFont(FontManager.getRunescapeSmallFont());
            activePanel.add(none);
        }
        for (TradeRecord rec : active) {
            activePanel.add(buildAutoCard(rec, flipMap.get(rec.itemId)));
            activePanel.add(Box.createVerticalStrut(4));
        }

        // Manual trades
        manualPanel.removeAll();
        if (!manualTrades.isEmpty()) {
            JLabel manHdr = new JLabel("MANUAL TRADES");
            manHdr.setForeground(VeilPanel.GOLD);
            manHdr.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
            manHdr.setAlignmentX(LEFT_ALIGNMENT);
            manualPanel.add(manHdr);
            manualPanel.add(Box.createVerticalStrut(4));

            // Match manual trades to flip signals by name
            Map<String,FlipSignal> flipByName = new HashMap<>();
            for (FlipSignal f : flips) flipByName.put(f.itemName.toLowerCase(), f);

            for (int i = 0; i < manualTrades.size(); i++) {
                final int idx = i;
                ManualTrade t = manualTrades.get(i);
                FlipSignal sig = flipByName.get(t.itemName.toLowerCase());
                manualPanel.add(buildManualCard(t, sig, idx));
                manualPanel.add(Box.createVerticalStrut(4));
            }
        }

        activePanel.revalidate(); activePanel.repaint();
        manualPanel.revalidate(); manualPanel.repaint();
    }

    private JPanel buildAutoCard(TradeRecord rec, FlipSignal signal)
    {
        JPanel card = VeilPanel.card(null);
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        card.add(VeilPanel.bigRow(rec.itemName, rec.isBuy ? "BUYING" : "SELLING",
            rec.isBuy ? VeilPanel.GREEN : VeilPanel.GOLD));
        card.add(Box.createVerticalStrut(3));
        card.add(VeilPanel.row("Your price:", VeilPanel.fmtGp(rec.pricePerUnit) + " gp ea", VeilPanel.MUTED));
        card.add(VeilPanel.row("Filled:", rec.quantityTraded + " / " + rec.quantityOffered, VeilPanel.TEXT));

        if (rec.isBuy && signal != null) {
            int sellAt = signal.sellPrice - 1;
            int tax    = Math.min(5_000_000, Math.max(1, (int)(sellAt * 0.01)));
            int profit = (sellAt - rec.pricePerUnit - tax) * rec.quantityTraded;
            card.add(Box.createVerticalStrut(3));
            card.add(VeilPanel.bigRow("→ SELL AT:", VeilPanel.fmtGp(sellAt) + " gp", VeilPanel.GREEN));
            card.add(VeilPanel.row("Profit so far:", VeilPanel.fmtGpSigned(profit), profit >= 0 ? VeilPanel.GREEN : VeilPanel.RED));
        }
        return card;
    }

    private JPanel buildManualCard(ManualTrade t, FlipSignal signal, int idx)
    {
        JPanel card = VeilPanel.card(null);
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        card.add(VeilPanel.bigRow(t.itemName, t.qty + "× bought", VeilPanel.TEXT));
        card.add(VeilPanel.row("Bought at:", VeilPanel.fmtGp(t.buyPrice) + " gp ea", VeilPanel.MUTED));
        card.add(VeilPanel.row("Total cost:", VeilPanel.fmtGp((long)t.buyPrice * t.qty), VeilPanel.MUTED));

        if (signal != null) {
            int sellAt = signal.sellPrice - 1;
            int tax    = Math.min(5_000_000, Math.max(1, (int)(sellAt * 0.01)));
            int profitEa = sellAt - t.buyPrice - tax;
            int totalProfit = profitEa * t.qty;
            card.add(Box.createVerticalStrut(3));
            card.add(VeilPanel.bigRow("→ SELL AT:", VeilPanel.fmtGp(sellAt) + " gp", VeilPanel.GREEN));
            card.add(VeilPanel.row("Profit per item:", VeilPanel.fmtGpSigned(profitEa), profitEa >= 0 ? VeilPanel.GREEN : VeilPanel.RED));
            card.add(VeilPanel.bigRow("Total profit:", VeilPanel.fmtGpSigned(totalProfit), totalProfit >= 0 ? VeilPanel.GREEN : VeilPanel.RED));
            card.add(VeilPanel.row("Current market:", "Buy " + VeilPanel.fmtGp(signal.buyPrice) + " / Sell " + VeilPanel.fmtGp(signal.sellPrice), VeilPanel.MUTED));
        } else {
            card.add(VeilPanel.row("Market data:", "Not in flip database", VeilPanel.MUTED));
        }

        JButton del = VeilPanel.btn("Remove", VeilPanel.SURFACE2, VeilPanel.RED);
        del.setAlignmentX(LEFT_ALIGNMENT);
        del.addActionListener(e -> { manualTrades.remove(idx); refresh(); });
        card.add(Box.createVerticalStrut(4));
        card.add(del);
        return card;
    }
}


// ══════════════════════════════════════════════════════════════
// PORTFOLIO TAB
// ══════════════════════════════════════════════════════════════
class PortfolioPanel extends JPanel
{
    private final VeilPlugin plugin;
    private final VeilPanel  veil;
    private JPanel contentPanel;

    PortfolioPanel(VeilPlugin plugin, VeilPanel veil)
    {
        this.plugin = plugin; this.veil = veil;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8,8,8,8));
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(VeilPanel.BG);
        add(contentPanel);
    }

    void refresh()
    {
        long coins = plugin.getCoinStack();
        List<FlipSignal> flips = plugin.getCachedFlips();

        contentPanel.removeAll();

        // Coin stack
        JPanel coinCard = VeilPanel.card("YOUR BANKROLL");
        coinCard.setAlignmentX(LEFT_ALIGNMENT);
        coinCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        JLabel coinLbl = new JLabel(coins > 0 ? VeilPanel.fmtGp(coins) + " GP" : "Open inventory to read coins");
        coinLbl.setForeground(coins > 0 ? VeilPanel.GOLD : VeilPanel.MUTED);
        coinLbl.setFont(FontManager.getRunescapeBoldFont().deriveFont(18f));
        coinLbl.setAlignmentX(LEFT_ALIGNMENT);
        coinCard.add(coinLbl);
        if (coins > 0) {
            JLabel hint = new JLabel("Live from your inventory");
            hint.setForeground(VeilPanel.MUTED);
            hint.setFont(FontManager.getRunescapeSmallFont());
            hint.setAlignmentX(LEFT_ALIGNMENT);
            coinCard.add(hint);
        }
        contentPanel.add(coinCard);
        contentPanel.add(Box.createVerticalStrut(8));

        if (coins > 0 && !flips.isEmpty())
        {
            // What you can afford
            JPanel affordCard = VeilPanel.card("WHAT YOU CAN FLIP RIGHT NOW");
            affordCard.setAlignmentX(LEFT_ALIGNMENT);
            affordCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 500));

            long totalExpected = 0;
            int  shown = 0;
            for (FlipSignal f : flips) {
                if (f.buyPrice > coins) continue;
                if (shown >= 5) break;
                long qty    = Math.min(coins / f.buyPrice, f.buyLimit);
                long cost   = qty * f.buyPrice;
                long profit = qty * f.netMargin;
                totalExpected += profit;
                affordCard.add(VeilPanel.bigRow(f.itemName, "", VeilPanel.TEXT));
                affordCard.add(VeilPanel.row("  Buy " + qty + "× @ " + VeilPanel.fmtGp(f.buyPrice), "= " + VeilPanel.fmtGp(cost) + " gp", VeilPanel.MUTED));
                affordCard.add(VeilPanel.row("  Sell @ " + VeilPanel.fmtGp(f.sellPrice-1), "profit: +" + VeilPanel.fmtGp(profit) + " gp", VeilPanel.GREEN));
                affordCard.add(Box.createVerticalStrut(6));
                shown++;
            }

            if (shown == 0) {
                JLabel none = new JLabel("No affordable flips found");
                none.setForeground(VeilPanel.MUTED);
                none.setFont(FontManager.getRunescapeSmallFont());
                affordCard.add(none);
            } else {
                affordCard.add(VeilPanel.bigRow("Total expected return:", "+" + VeilPanel.fmtGp(totalExpected), VeilPanel.GREEN));
                affordCard.add(VeilPanel.row("Return on bankroll:", String.format("%.2f%%", totalExpected * 100.0 / coins), VeilPanel.GOLD));
            }

            contentPanel.add(affordCard);
            contentPanel.add(Box.createVerticalStrut(8));

            // Compounding projector
            if (totalExpected > 0 && shown > 0) {
                JPanel compCard = VeilPanel.card("COMPOUNDING PROJECTOR (3 cycles/day)");
                compCard.setAlignmentX(LEFT_ALIGNMENT);
                compCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));

                double roi = totalExpected / (double) coins;
                double daily = Math.pow(1 + roi, 3) - 1;
                double week  = Math.pow(1 + daily, 7);
                double month = Math.pow(1 + daily, 30);
                double quarter = Math.pow(1 + daily, 90);

                compCard.add(VeilPanel.row("After 7 days:",  VeilPanel.fmtGp((long)(coins * week)), VeilPanel.GREEN));
                compCard.add(VeilPanel.row("After 30 days:", VeilPanel.fmtGp((long)(coins * month)), VeilPanel.GOLD));
                compCard.add(VeilPanel.row("After 90 days:", VeilPanel.fmtGp((long)(coins * quarter)), VeilPanel.PURPLE));
                compCard.add(Box.createVerticalStrut(4));
                compCard.add(VeilPanel.row("* assumes consistent",  "S/A grade flips daily", VeilPanel.MUTED));

                contentPanel.add(compCard);
            }
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }
}


// ══════════════════════════════════════════════════════════════
// HISTORY TAB
// ══════════════════════════════════════════════════════════════
class HistoryPanel extends JPanel
{
    private final VeilPlugin plugin;
    private final VeilPanel  veil;
    private JPanel listPanel;

    HistoryPanel(VeilPlugin plugin, VeilPanel veil)
    {
        this.plugin = plugin; this.veil = veil;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8,8,8,8));

        JLabel hdr = new JLabel("COMPLETED TRADES THIS SESSION");
        hdr.setForeground(VeilPanel.GOLD);
        hdr.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        hdr.setAlignmentX(LEFT_ALIGNMENT);
        add(hdr);
        add(Box.createVerticalStrut(6));

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(VeilPanel.BG);
        add(listPanel);
        refresh();
    }

    void refresh()
    {
        listPanel.removeAll();
        List<TradeRecord> trades = plugin.getSessionTrades();

        if (trades.isEmpty()) {
            JLabel none = new JLabel("No completed trades yet — go flip something!");
            none.setForeground(VeilPanel.MUTED);
            none.setFont(FontManager.getRunescapeSmallFont());
            listPanel.add(none);
        }

        int totalProfit = 0;
        for (TradeRecord rec : trades) {
            totalProfit += rec.profitGp;
            listPanel.add(buildTradeRow(rec));
            listPanel.add(Box.createVerticalStrut(3));
        }

        if (!trades.isEmpty()) {
            listPanel.add(Box.createVerticalStrut(8));
            JPanel summary = VeilPanel.card("SESSION SUMMARY");
            summary.setAlignmentX(LEFT_ALIGNMENT);
            summary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            summary.add(VeilPanel.bigRow("Total profit:", VeilPanel.fmtGpSigned(totalProfit) + " gp", totalProfit >= 0 ? VeilPanel.GREEN : VeilPanel.RED));
            summary.add(VeilPanel.row("Trades completed:", String.valueOf(trades.size()), VeilPanel.MUTED));
            if (trades.size() > 0)
                summary.add(VeilPanel.row("Avg per trade:", VeilPanel.fmtGp(totalProfit / trades.size()) + " gp", VeilPanel.GOLD));
            listPanel.add(summary);
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel buildTradeRow(TradeRecord rec)
    {
        JPanel r = new JPanel(new BorderLayout(6,0));
        r.setBackground(VeilPanel.SURFACE);
        r.setBorder(new EmptyBorder(5,8,5,8));
        r.setAlignmentX(LEFT_ALIGNMENT);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        String type = rec.isBuy ? "[B]" : "[S]";
        JLabel left = new JLabel(type + " " + rec.itemName + " ×" + rec.quantityTraded);
        left.setForeground(rec.isBuy ? VeilPanel.GREEN : VeilPanel.GOLD);
        left.setFont(FontManager.getRunescapeSmallFont());

        JLabel right = new JLabel(rec.profitGp == 0 ? "@" + VeilPanel.fmtGp(rec.pricePerUnit)
            : VeilPanel.fmtGpSigned(rec.profitGp) + " gp");
        right.setForeground(rec.profitGp > 0 ? VeilPanel.GREEN : rec.profitGp < 0 ? VeilPanel.RED : VeilPanel.MUTED);
        right.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));

        r.add(left, BorderLayout.WEST);
        r.add(right, BorderLayout.EAST);
        return r;
    }
}


// ══════════════════════════════════════════════════════════════
// INTELLIGENCE TAB — market heat, supply shocks, thin gems,
//                    time advisor, session plan, spike alerts
// ══════════════════════════════════════════════════════════════
class IntelligencePanel extends JPanel
{
    private final VeilPlugin plugin;
    private final VeilPanel  veil;
    private JPanel content;

    IntelligencePanel(VeilPlugin plugin, VeilPanel veil)
    {
        this.plugin = plugin; this.veil = veil;
        setBackground(VeilPanel.BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8,8,8,8));
        content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(VeilPanel.BG);
        add(content);
        refresh();
    }

    void refresh()
    {
        MarketIntelligence intel = plugin.getMarketIntelligence();
        SwingUtilities.invokeLater(() -> {
            content.removeAll();
            if (intel == null) {
                content.add(loadingLabel());
                content.revalidate(); content.repaint();
                return;
            }
            buildContent(intel);
            content.revalidate();
            content.repaint();
        });
    }

    private JLabel loadingLabel()
    {
        JLabel l = new JLabel("Loading market intelligence... (60s)");
        l.setForeground(VeilPanel.MUTED);
        l.setFont(FontManager.getRunescapeSmallFont());
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private void buildContent(MarketIntelligence intel)
    {
        // ── Session plan (the most important thing) ────────────
        if (intel.sessionPlan != null && !intel.sessionPlan.isEmpty())
        {
            JPanel planCard = VeilPanel.card("YOUR PLAN RIGHT NOW");
            planCard.setAlignmentX(LEFT_ALIGNMENT);
            planCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));

            for (String line : intel.sessionPlan.split("\n")) {
                JLabel lbl = new JLabel("<html>" + line.replace("→","▸")
                    .replace("🔥","★").replace("⚡","!").replace("⏰","~")
                    .replace("💎","◆") + "</html>");
                lbl.setForeground(
                    line.startsWith("★") ? VeilPanel.GOLD :
                    line.startsWith("!") ? VeilPanel.RED :
                    line.startsWith("▸") ? VeilPanel.TEXT :
                    line.startsWith("◆") ? VeilPanel.PURPLE :
                    VeilPanel.MUTED);
                lbl.setFont(line.length() < 25
                    ? FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD)
                    : FontManager.getRunescapeSmallFont());
                lbl.setAlignmentX(LEFT_ALIGNMENT);
                planCard.add(lbl);
                if (line.isEmpty()) planCard.add(Box.createVerticalStrut(4));
            }
            content.add(planCard);
            content.add(Box.createVerticalStrut(8));
        }

        // ── Time-of-day context ────────────────────────────────
        if (intel.timeContext != null)
        {
            JPanel timeCard = VeilPanel.card(null);
            timeCard.setAlignmentX(LEFT_ALIGNMENT);
            timeCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

            JLabel timeLbl = new JLabel(intel.timeContext.session + "  (UTC " + intel.timeContext.utcHour + ":xx)");
            timeLbl.setForeground(
                "PEAK HOURS".equals(intel.timeContext.session) ? VeilPanel.GREEN :
                "OFF-PEAK".equals(intel.timeContext.session) ? VeilPanel.AMBER : VeilPanel.GOLD);
            timeLbl.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
            timeLbl.setAlignmentX(LEFT_ALIGNMENT);
            timeCard.add(timeLbl);

            if (intel.timeContext.minutesUntilPeak > 0) {
                int hrs = intel.timeContext.minutesUntilPeak / 60;
                int min = intel.timeContext.minutesUntilPeak % 60;
                JLabel peak = new JLabel("Peak hours in " + (hrs > 0 ? hrs + "h " : "") + min + "m");
                peak.setForeground(VeilPanel.MUTED);
                peak.setFont(FontManager.getRunescapeSmallFont());
                peak.setAlignmentX(LEFT_ALIGNMENT);
                timeCard.add(peak);
            }

            JLabel bestLbl = new JLabel("<html><b>Flip now:</b> " + intel.timeContext.bestCategories + "</html>");
            bestLbl.setForeground(VeilPanel.GREEN);
            bestLbl.setFont(FontManager.getRunescapeSmallFont());
            bestLbl.setAlignmentX(LEFT_ALIGNMENT);
            timeCard.add(Box.createVerticalStrut(4));
            timeCard.add(bestLbl);

            if (intel.timeContext.avoidCategories != null && !intel.timeContext.avoidCategories.isEmpty()) {
                JLabel avoidLbl = new JLabel("<html><b>Avoid:</b> " + intel.timeContext.avoidCategories + "</html>");
                avoidLbl.setForeground(VeilPanel.RED);
                avoidLbl.setFont(FontManager.getRunescapeSmallFont());
                avoidLbl.setAlignmentX(LEFT_ALIGNMENT);
                timeCard.add(avoidLbl);
            }

            content.add(timeCard);
            content.add(Box.createVerticalStrut(8));
        }

        // ── Market heat map ────────────────────────────────────
        JPanel heatCard = VeilPanel.card("MARKET HEAT MAP");
        heatCard.setAlignmentX(LEFT_ALIGNMENT);
        heatCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));

        for (MarketIntelligence.CategoryHeat h : intel.categoryHeat)
        {
            JPanel row = new JPanel(new BorderLayout(4,0));
            row.setBackground(VeilPanel.SURFACE);
            row.setAlignmentX(LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));

            Color catColor = h.isHot ? VeilPanel.GREEN
                : h.avgPressure > 1.0 ? VeilPanel.GOLD : VeilPanel.MUTED;

            JLabel cat = new JLabel((h.isHot ? "★ " : "  ") + h.category);
            cat.setForeground(catColor);
            cat.setFont(FontManager.getRunescapeSmallFont().deriveFont(h.isHot ? Font.BOLD : Font.PLAIN));

            JLabel stats = new JLabel(h.avgPressure + "× " + String.format("%+.1f%%", h.avgMomentum));
            stats.setForeground(catColor);
            stats.setFont(FontManager.getRunescapeSmallFont());

            row.add(cat, BorderLayout.WEST);
            row.add(stats, BorderLayout.EAST);
            heatCard.add(row);
        }
        content.add(heatCard);
        content.add(Box.createVerticalStrut(8));

        // ── Supply shocks ──────────────────────────────────────
        if (!intel.supplyShocks.isEmpty())
        {
            JPanel shockCard = VeilPanel.card("⚡ ACCUMULATION ALERTS");
            shockCard.setAlignmentX(LEFT_ALIGNMENT);
            shockCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));

            for (MarketIntelligence.SupplyShock s : intel.supplyShocks.subList(0, Math.min(5, intel.supplyShocks.size())))
            {
                shockCard.add(VeilPanel.bigRow(s.itemName, s.pressure + "× pressure", VeilPanel.RED));
                shockCard.add(VeilPanel.row("  " + s.alert, "", VeilPanel.MUTED));
                shockCard.add(VeilPanel.row("  Buy @", VeilPanel.fmtGp(s.buyPrice) + "  Margin: +" + VeilPanel.fmtGp(s.netMargin), VeilPanel.GREEN));
                shockCard.add(Box.createVerticalStrut(4));
            }
            content.add(shockCard);
            content.add(Box.createVerticalStrut(8));
        }

        // ── Price spikes ───────────────────────────────────────
        if (!intel.priceSpikes.isEmpty())
        {
            JPanel spikeCard = VeilPanel.card("PRICE SPIKES (last 30min)");
            spikeCard.setAlignmentX(LEFT_ALIGNMENT);
            spikeCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

            for (MarketIntelligence.PriceSpike s : intel.priceSpikes)
            {
                spikeCard.add(VeilPanel.bigRow(
                    s.itemName,
                    String.format("%+.1f%%", s.changePct),
                    s.changePct > 0 ? VeilPanel.GREEN : VeilPanel.RED));
                spikeCard.add(VeilPanel.row("  " + s.possibleCause, "", VeilPanel.MUTED));
                spikeCard.add(Box.createVerticalStrut(2));
            }
            content.add(spikeCard);
            content.add(Box.createVerticalStrut(8));
        }

        // ── Thin market gems ───────────────────────────────────
        if (!intel.thinMarketGems.isEmpty())
        {
            JPanel gemCard = VeilPanel.card("THIN MARKET GEMS — ZERO BOT COMPETITION");
            gemCard.setAlignmentX(LEFT_ALIGNMENT);
            gemCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));

            JLabel desc = new JLabel("<html>These items have buy limit ≤ 10. Bots skip them.<br>You capture 90%+ of the spread. Pure patience play.</html>");
            desc.setForeground(VeilPanel.MUTED);
            desc.setFont(FontManager.getRunescapeSmallFont());
            desc.setAlignmentX(LEFT_ALIGNMENT);
            gemCard.add(desc);
            gemCard.add(Box.createVerticalStrut(6));

            for (MarketIntelligence.ThinMarketGem g : intel.thinMarketGems.subList(0, Math.min(8, intel.thinMarketGems.size())))
            {
                gemCard.add(VeilPanel.bigRow(g.itemName, "Limit: " + g.buyLimit, VeilPanel.PURPLE));
                gemCard.add(VeilPanel.row("  Buy @", VeilPanel.fmtGp(g.buyPrice), VeilPanel.MUTED));
                gemCard.add(VeilPanel.row("  Sell @", VeilPanel.fmtGp(g.sellPrice - 1), VeilPanel.GREEN));
                gemCard.add(VeilPanel.row("  Per cycle:", "+" + VeilPanel.fmtGp((long)g.netMargin * g.buyLimit) + " gp  (" + String.format("%.1f%%", g.roi) + " ROI)", VeilPanel.GREEN));
                gemCard.add(VeilPanel.row("  Bot risk:", g.botRisk, VeilPanel.PURPLE));

                JLabel wikiLnk = new JLabel("<html><u>Chart ↗</u></html>");
                wikiLnk.setForeground(VeilPanel.BLUE);
                wikiLnk.setFont(FontManager.getRunescapeSmallFont());
                wikiLnk.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                wikiLnk.setAlignmentX(LEFT_ALIGNMENT);
                final int iid = g.itemId;
                wikiLnk.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        LinkBrowser.browse("https://prices.runescape.wiki/osrs/item/" + iid);
                    }
                });
                gemCard.add(wikiLnk);
                gemCard.add(Box.createVerticalStrut(8));
            }
            content.add(gemCard);
        }
    }
}
