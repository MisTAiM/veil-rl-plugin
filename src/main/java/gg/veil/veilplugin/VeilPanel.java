package gg.veil.veilplugin;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;

/**
 * Veil side panel — full flip list with explanations,
 * bankroll entry, position sizing, signal glossary.
 */
public class VeilPanel extends PluginPanel
{
    // ── Veil colors ──────────────────────────────────────────
    private static final Color BG        = new Color(0x0C, 0x0E, 0x13);
    private static final Color SURFACE   = new Color(0x13, 0x16, 0x1E);
    private static final Color BORDER    = new Color(0x1E, 0x23, 0x30);
    private static final Color GOLD      = new Color(0xC9, 0xA8, 0x4C);
    private static final Color GREEN     = new Color(0x3D, 0xAA, 0x6E);
    private static final Color RED       = new Color(0xC2, 0x54, 0x54);
    private static final Color AMBER     = new Color(0xBA, 0x75, 0x17);
    private static final Color PURPLE    = new Color(0xAA, 0x00, 0xDD);
    private static final Color TEXT      = new Color(0xE8, 0xE6, 0xDE);
    private static final Color MUTED     = new Color(0x6B, 0x72, 0x80);

    private final VeilPlugin plugin;

    // UI refs
    private JPanel  flipListPanel;
    private JLabel  statusLabel;
    private JLabel  sessionLabel;
    private JTextField bankrollField;
    private JLabel  bankrollLabel;

    private int bankrollGp = 0;

    @Inject
    public VeilPanel(VeilPlugin plugin)
    {
        this.plugin = plugin;
        setBackground(BG);
        setLayout(new BorderLayout(0, 0));
        build();
    }

    private void build()
    {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(BG);

        // ── Header ────────────────────────────────────────────
        root.add(buildHeader());
        root.add(Box.createVerticalStrut(6));

        // ── Session summary ───────────────────────────────────
        root.add(buildSessionCard());
        root.add(Box.createVerticalStrut(6));

        // ── Bankroll input ────────────────────────────────────
        root.add(buildBankrollCard());
        root.add(Box.createVerticalStrut(6));

        // ── Flip list ─────────────────────────────────────────
        root.add(buildFlipHeader());
        flipListPanel = new JPanel();
        flipListPanel.setLayout(new BoxLayout(flipListPanel, BoxLayout.Y_AXIS));
        flipListPanel.setBackground(BG);
        root.add(flipListPanel);
        root.add(Box.createVerticalStrut(6));

        // ── Glossary ──────────────────────────────────────────
        root.add(buildGlossary());
        root.add(Box.createVerticalStrut(12));

        JScrollPane scroll = new JScrollPane(root);
        scroll.setBackground(BG);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);

        // Status bar
        statusLabel = new JLabel("Loading flip data...");
        statusLabel.setForeground(MUTED);
        statusLabel.setFont(FontManager.getRunescapeSmallFont());
        statusLabel.setBorder(new EmptyBorder(4, 8, 4, 8));
        add(statusLabel, BorderLayout.SOUTH);
    }

    // ── Header ────────────────────────────────────────────────
    private JPanel buildHeader()
    {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(SURFACE);
        p.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(10, 10, 10, 10)));

        JLabel title = new JLabel("VEIL FLIPPER");
        title.setForeground(GOLD);
        title.setFont(FontManager.getRunescapeBoldFont().deriveFont(14f));
        p.add(title, BorderLayout.WEST);

        JLabel sub = new JLabel("GE Intelligence");
        sub.setForeground(MUTED);
        sub.setFont(FontManager.getRunescapeSmallFont());
        p.add(sub, BorderLayout.EAST);

        return p;
    }

    // ── Session card ──────────────────────────────────────────
    private JPanel buildSessionCard()
    {
        JPanel card = card();
        card.setLayout(new GridLayout(2, 3, 6, 4));

        sessionLabel = new JLabel();
        sessionLabel.setText("Session: loading...");
        sessionLabel.setForeground(MUTED);
        sessionLabel.setFont(FontManager.getRunescapeSmallFont());
        card.add(sessionLabel);

        return wrapPadded(card);
    }

    // ── Bankroll input ────────────────────────────────────────
    private JPanel buildBankrollCard()
    {
        JPanel card = card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel lbl = label("Your Bankroll (GP)", MUTED);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        card.add(lbl);
        card.add(Box.createVerticalStrut(4));

        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBackground(SURFACE);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        bankrollField = new JTextField("e.g. 10m or 5000000");
        bankrollField.setBackground(BG);
        bankrollField.setForeground(MUTED);
        bankrollField.setCaretColor(TEXT);
        bankrollField.setBorder(new EmptyBorder(4, 6, 4, 6));
        bankrollField.setFont(FontManager.getRunescapeSmallFont());
        bankrollField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (bankrollField.getText().startsWith("e.g.")) {
                    bankrollField.setText("");
                    bankrollField.setForeground(TEXT);
                }
            }
        });

        JButton btn = new JButton("SET");
        btn.setBackground(GOLD);
        btn.setForeground(BG);
        btn.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        btn.setBorder(new EmptyBorder(4, 8, 4, 8));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> parseBankroll());

        row.add(bankrollField, BorderLayout.CENTER);
        row.add(btn, BorderLayout.EAST);
        card.add(row);
        card.add(Box.createVerticalStrut(4));

        bankrollLabel = new JLabel("Enter your GP to see position sizes");
        bankrollLabel.setForeground(MUTED);
        bankrollLabel.setFont(FontManager.getRunescapeSmallFont());
        bankrollLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(bankrollLabel);

        return wrapPadded(card);
    }

    private void parseBankroll()
    {
        String raw = bankrollField.getText().trim().toLowerCase()
            .replaceAll(",", "")
            .replaceAll("m$", "000000")
            .replaceAll("k$", "000");
        try {
            bankrollGp = Integer.parseInt(raw);
            bankrollLabel.setForeground(GREEN);
            bankrollLabel.setText("Bankroll: " + fmtGp(bankrollGp) + " — positions updated");
            refreshFlips();
        } catch (NumberFormatException ex) {
            bankrollLabel.setForeground(RED);
            bankrollLabel.setText("Invalid. Try: 10m or 5000000");
        }
    }

    // ── Flip list header ──────────────────────────────────────
    private JPanel buildFlipHeader()
    {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(0, 8, 4, 8));

        JLabel lbl = new JLabel("BEST FLIPS RIGHT NOW");
        lbl.setForeground(GOLD);
        lbl.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        p.add(lbl, BorderLayout.WEST);

        JLabel hint = new JLabel("updates every 60s");
        hint.setForeground(MUTED);
        hint.setFont(FontManager.getRunescapeSmallFont());
        p.add(hint, BorderLayout.EAST);

        return p;
    }

    // ── Build a flip card ─────────────────────────────────────
    private JPanel buildFlipCard(FlipSignal f, int rank)
    {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(SURFACE);
        card.setBorder(new CompoundBorder(
            new MatteBorder(0, gradeThickness(f.grade), 0, 0, gradeColor(f.grade)),
            new EmptyBorder(8, 10, 8, 10)));

        // Row 1: rank + name + grade badge
        JPanel row1 = new JPanel(new BorderLayout(6, 0));
        row1.setBackground(SURFACE);
        row1.setAlignmentX(LEFT_ALIGNMENT);
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JLabel rankLbl = new JLabel("#" + rank);
        rankLbl.setForeground(MUTED);
        rankLbl.setFont(FontManager.getRunescapeSmallFont());
        rankLbl.setPreferredSize(new Dimension(20, 16));
        row1.add(rankLbl, BorderLayout.WEST);

        JLabel nameLbl = new JLabel(f.itemName);
        nameLbl.setForeground(TEXT);
        nameLbl.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        row1.add(nameLbl, BorderLayout.CENTER);

        JLabel gradeLbl = new JLabel(" " + f.grade + " ");
        gradeLbl.setForeground(gradeColor(f.grade));
        gradeLbl.setBackground(gradeColor(f.grade).darker().darker());
        gradeLbl.setOpaque(true);
        gradeLbl.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        row1.add(gradeLbl, BorderLayout.EAST);
        card.add(row1);
        card.add(Box.createVerticalStrut(5));

        // Row 2: margin + ROI — THE MONEY LINE
        JPanel row2 = statRow(
            "Profit/item",  "+" + fmtGp(f.netMargin) + "  (" + String.format("%.1f%%", f.roi) + " ROI)",
            GREEN
        );
        card.add(row2);
        card.add(Box.createVerticalStrut(2));

        // Row 3: what to buy, how many, for how much
        int quantity = 0;
        int totalCost = 0;
        int expectedProfit = 0;
        if (bankrollGp > 0 && f.buyLimit > 0)
        {
            int maxByBankroll = (int)(bankrollGp * f.kelly);
            int maxByVolume   = Math.min(f.buyLimit, f.hourVol / 4);
            quantity      = Math.min(maxByBankroll / f.buyPrice, maxByVolume);
            totalCost     = quantity * f.buyPrice;
            expectedProfit = quantity * f.netMargin;
        }

        if (quantity > 0)
        {
            card.add(statRow("→ Buy exactly", quantity + "× @ " + fmtGp(f.buyPrice) + " ea", GOLD));
            card.add(Box.createVerticalStrut(2));
            card.add(statRow("→ Expected profit", "+" + fmtGp(expectedProfit), GREEN));
            card.add(statRow("  Capital needed", fmtGp(totalCost), MUTED));
            card.add(Box.createVerticalStrut(2));
        }
        else
        {
            card.add(statRow("Buy @", fmtGp(f.buyPrice), MUTED));
            card.add(statRow("Sell @", fmtGp(f.sellPrice), MUTED));
            card.add(Box.createVerticalStrut(2));
        }

        // Row: signal with plain English explanation
        String signalExplain = signalExplain(f);
        card.add(statRow("Signal", f.signal + " — " + signalExplain, signalColor(f.signal)));
        card.add(Box.createVerticalStrut(2));

        // Row: fill time — how long to wait
        String fillExplain = f.fillMins < 5   ? "fills in minutes (high volume)"
                           : f.fillMins < 30  ? "~" + f.fillMins + "min to fill"
                           : f.fillMins < 60  ? "~" + f.fillMins + "min — be patient"
                           : "slow fill — consider skipping";
        card.add(statRow("Fill time", fillExplain, f.fillMins < 30 ? GREEN : AMBER));
        card.add(Box.createVerticalStrut(2));

        // Row: pressure — plain English
        String pressExplain = f.pressure >= 2.0 ? "strong buyers — price likely rising"
                            : f.pressure >= 1.2 ? "more buyers than sellers — good sign"
                            : f.pressure >= 0.8 ? "balanced market"
                            : "more sellers than buyers — be careful";
        card.add(statRow("Market pressure", String.format("%.1f×  ", f.pressure) + pressExplain,
            f.pressure >= 1.2 ? GREEN : f.pressure < 0.8 ? RED : MUTED));
        card.add(Box.createVerticalStrut(2));

        // Row: momentum
        if (f.vwap5m > 0)
        {
            double mom = ((double)(f.vwap5m - f.vwap1h) / f.vwap1h) * 100;
            String momExplain = mom > 2  ? "price rising fast — buy now"
                              : mom > 0  ? "price trending up"
                              : mom > -2 ? "price flat or slightly falling"
                              :            "price dropping — wait";
            card.add(statRow("Momentum",
                String.format("%+.1f%%  ", mom) + momExplain,
                mom > 0 ? GREEN : RED));
            card.add(Box.createVerticalStrut(2));
        }

        // Row: 4hr max GP
        card.add(statRow("4hr ceiling", fmtGp(f.cycleGp) + " max GP this cycle", GOLD));
        card.add(Box.createVerticalStrut(2));

        // Row: vol sanity check
        card.add(statRow("Volume/hr", fmtGp(f.hourVol) + " traded",
            f.hourVol > f.buyLimit * 4 ? GREEN : AMBER));

        // Wiki link
        card.add(Box.createVerticalStrut(6));
        JLabel wikiLink = new JLabel("<html><u>View price chart on wiki ↗</u></html>");
        wikiLink.setForeground(new Color(0x5B, 0x7F, 0xD4));
        wikiLink.setFont(FontManager.getRunescapeSmallFont());
        wikiLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        wikiLink.setAlignmentX(LEFT_ALIGNMENT);
        wikiLink.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                LinkBrowser.browse("https://prices.runescape.wiki/osrs/item/" + f.itemId);
            }
        });
        card.add(wikiLink);

        return card;
    }

    // ── Glossary ──────────────────────────────────────────────
    private JPanel buildGlossary()
    {
        JPanel card = card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = label("WHAT DO THESE TERMS MEAN?", GOLD);
        title.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        card.add(title);
        card.add(Box.createVerticalStrut(8));

        String[][] terms = {
            {"Margin / Net margin",
                "How much GP you make per item AFTER the 1% GE tax. " +
                "If you buy at 100k and sell at 105k, raw margin = 5k. " +
                "Tax = 1050gp. Net margin = 3,950gp per item."},
            {"ROI % (Return on Investment)",
                "Net margin divided by buy price, as a percentage. " +
                "2% ROI means you make 2gp for every 100gp you invest. " +
                "Anything above 1% is generally worth doing."},
            {"Fill time",
                "How long it takes to fill one buy limit order at current volume. " +
                "Under 30 minutes = good. Over 1 hour = your GP is locked up a long time."},
            {"Buy limit",
                "The GE only lets you buy X of an item every 4 hours. " +
                "Abyssal whip = 70 per 4hrs. This caps how much you can make per cycle."},
            {"4hr cycle GP",
                "The most GP you can make in one 4-hour buy limit cycle. " +
                "Net margin × how many you can actually buy (limited by both " +
                "buy limit AND hourly volume). This is your real earnings ceiling."},
            {"Market pressure",
                "Buy volume divided by sell volume from the last hour. " +
                "2.0× = twice as many buyers as sellers = price likely going up. " +
                "0.5× = more sellers = price likely falling. " +
                "Aim for 1.2× or higher when entering a flip."},
            {"Momentum",
                "5-minute price trend vs the 1-hour average. " +
                "+3% means price jumped 3% in the last 5 minutes. " +
                "Positive = good time to buy. Negative = price falling, wait."},
            {"VWAP (Volume Weighted Avg Price)",
                "The true average price weighted by how much was traded. " +
                "Better than just looking at last trade price. " +
                "1h VWAP = fair value over past hour. " +
                "5m VWAP = what it's trading at RIGHT NOW."},
            {"Signal: ENTER",
                "Both pressure and momentum are positive right now. " +
                "Good conditions to start this flip."},
            {"Signal: HOLD",
                "Conditions are neutral. If you're already in this flip, stay. " +
                "If you're not in it yet, watch it first."},
            {"Signal: EXIT",
                "Pressure dropping below 0.7× or momentum falling past -3%. " +
                "Price is declining — close your position."},
            {"Grade S / A / B / C / D",
                "S = elite flip. A = great. B = solid. C = worth doing. D = skip it. " +
                "Based on composite score combining all signals. " +
                "S grade items are rare — focus on A and B daily."},
            {"Kelly % (position sizing)",
                "The mathematically optimal percentage of your bankroll to put " +
                "into one flip, based on win probability and expected return. " +
                "Prevents over-concentrating in one risky item. " +
                "Capped at 25% per item for safety."},
            {"Competition / Compression",
                "How tight the current spread is vs the 1-hour average spread. " +
                "Below 1.0 = spread is tighter than usual = lots of competition. " +
                "Above 1.5 = spread wider than normal = opportunity."},
        };

        for (String[] term : terms)
        {
            JLabel termLbl = label(term[0], GOLD);
            termLbl.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
            card.add(termLbl);

            JLabel defLbl = new JLabel("<html><div style='width:220px'>" + term[1] + "</div></html>");
            defLbl.setForeground(MUTED);
            defLbl.setFont(FontManager.getRunescapeSmallFont());
            defLbl.setAlignmentX(LEFT_ALIGNMENT);
            card.add(defLbl);
            card.add(Box.createVerticalStrut(8));
        }

        return wrapPadded(card);
    }

    // ── Refresh from plugin data ──────────────────────────────
    public void refreshFlips()
    {
        List<FlipSignal> flips = plugin.getCachedFlips();
        SwingUtilities.invokeLater(() -> {
            flipListPanel.removeAll();
            if (flips == null || flips.isEmpty()) {
                flipListPanel.add(label("Loading flip data...", MUTED));
            } else {
                int shown = Math.min(15, flips.size());
                for (int i = 0; i < shown; i++) {
                    flipListPanel.add(buildFlipCard(flips.get(i), i + 1));
                    flipListPanel.add(Box.createVerticalStrut(4));
                }
                statusLabel.setText("Updated just now  ·  " + flips.size() + " items scored");
            }
            flipListPanel.revalidate();
            flipListPanel.repaint();
        });
    }

    public void updateSession()
    {
        SwingUtilities.invokeLater(() -> {
            VeilPlugin.SessionStats s = plugin.getSessionStats();
            int loot = plugin.getSessionLootGp();
            int total = s.sessionProfitGp + loot;
            sessionLabel.setText(
                "<html>GE: <b style='color:#3DAA6E'>" + fmt(s.sessionProfitGp) + "</b>" +
                "  Loot: <b style='color:#3DAA6E'>+" + fmtGp(loot) + "</b>" +
                "  Total: <b style='color:#C9A84C'>" + fmt(total) + "</b></html>");
        });
    }

    // ── Helpers ───────────────────────────────────────────────

    private JPanel card()
    {
        JPanel p = new JPanel();
        p.setBackground(SURFACE);
        p.setBorder(new CompoundBorder(
            new MatteBorder(1, 1, 1, 1, BORDER),
            new EmptyBorder(8, 10, 8, 10)));
        return p;
    }

    private JPanel wrapPadded(JPanel inner)
    {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG);
        outer.setBorder(new EmptyBorder(0, 8, 0, 8));
        outer.add(inner, BorderLayout.CENTER);
        return outer;
    }

    private JPanel statRow(String left, String right, Color rightColor)
    {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBackground(SURFACE);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));

        JLabel l = new JLabel(left);
        l.setForeground(MUTED);
        l.setFont(FontManager.getRunescapeSmallFont());

        JLabel r = new JLabel(right);
        r.setForeground(rightColor);
        r.setFont(FontManager.getRunescapeSmallFont());

        row.add(l, BorderLayout.WEST);
        row.add(r, BorderLayout.EAST);
        return row;
    }

    private JLabel label(String text, Color color)
    {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(FontManager.getRunescapeSmallFont());
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private String signalExplain(FlipSignal f)
    {
        if ("ENTER".equals(f.signal)) return "good conditions, start this flip";
        if ("EXIT".equals(f.signal))  return "price falling, avoid or close";
        return "watch it first";
    }

    private Color signalColor(String s)
    {
        if ("ENTER".equals(s)) return GREEN;
        if ("EXIT".equals(s))  return RED;
        return AMBER;
    }

    private Color gradeColor(String g)
    {
        switch (g) {
            case "S": return PURPLE;
            case "A": return GOLD;
            case "B": return GREEN;
            default:  return MUTED;
        }
    }

    private int gradeThickness(String g)
    {
        switch (g) {
            case "S": return 3;
            case "A": return 2;
            default:  return 1;
        }
    }

    private String fmt(int gp)
    {
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
}
