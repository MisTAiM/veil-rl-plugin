# VEIL 6.0 WORKSHEET — Execute Every Item

## KILL LIST (remove immediately)
- [x] Sync Server config section (Port, Enable Sync) — user uses RL only
- [x] serverEnabled / serverPort config items
- [x] VeilSyncServer references in startUp (keep class, just don't auto-start)

## CONFIG PANEL REBUILD (VeilConfig.java)
New sections:
- [x] OVERLAY section
  - Show Overlay toggle
  - Show Coins (toggle)
  - Show GP Today (toggle)
  - Show XP/hr (toggle)
  - Show Weapon Charges (toggle)
  - Show Slayer (toggle)
  - Show Death Risk (toggle)
  - Show GE Slots (toggle)
  - Show Top Flips (toggle)
  - Death Risk GP Threshold (slider 1M-50M)
  
- [x] FLIPS section
  - Min Net Margin (0 to 500k)
  - Max Fill Time (15/30/45/60/90/120 min)
  - Members Only toggle (hide F2P items)
  - Min ROI % (0 to 10%)
  - Show D-grade items toggle

- [x] ALERTS section
  - Alert on Fill (toggle)
  - Min GP for alert (0-10M)
  - Alert on Low Weapon Charges (toggle)
  - Alert on Supply Shock (toggle)
  - Alert on Price Spike (toggle)

- [x] TRACKING section
  - Track Loot (toggle)
  - Track XP (toggle)
  - Track Slayer (toggle)

## SIDE PANEL REBUILD (7 tabs)

### Tab 1: DASHBOARD
- [x] Coin stack — big gold number, live
- [x] Session summary: GE profit + loot + total
- [x] Watchlist: add any item, shows live price + your alert price
- [x] Active GE slots: item, buy@, sell@(recommended), fill%, profit if sold now
- [x] Weapon charges — low charge warnings
- [x] Slayer task remaining
- [x] Prayer + spec % (from varbits)

### Tab 2: FLIP FINDER
- [x] Search bar — any item name
- [x] Filters: members/F2P toggle, min margin, max fill, signal filter
- [x] Each card: BUY AT exact price, SELL AT exact price, profit, ROI, fill time, how many you can afford, signal with plain English reason
- [x] Sort: GP/hr, margin, ROI, fill speed

### Tab 3: MY TRADES
- [x] Auto-tracked from GE events (all 8 slots)
- [x] Manual add: item, buy price, qty → shows sell recommendation + profit
- [x] Each trade: sell at X for Y profit (exact numbers)
- [x] Edit / remove
- [x] Running P&L

### Tab 4: PORTFOLIO
- [x] Coin stack live
- [x] What you can afford right now (top flips within budget)
- [x] Exact: buy X of [item] at Y gp = Z cost → profit W gp
- [x] Compounding projector (7/30/90 day)
- [x] Capital utilization

### Tab 5: INTEL
- [x] Refresh button (manual trigger)
- [x] Session plan (auto-generated)
- [x] Market heat map (12 categories)
- [x] Supply shock alerts
- [x] Price spike detector
- [x] Thin market gems
- [x] Time-of-day advisor with countdown

### Tab 6: SKILLS
- [x] All skills with XP gained this session
- [x] XP/hr per skill (from StatChanged — real-time, no API)
- [x] Level + progress bar per skill
- [x] Total XP this session

### Tab 7: TRACKER
- [x] Boss KC from varbits (live, no hiscores needed)
- [x] Drop probability: X KC → Y% chance of seeing [drop]
- [x] Slayer task advisor: type task → method/GP/hr/block advice
- [x] Loot log: last 20 drops with values
- [x] Death risk: gear value vs protected value live

## OVERLAY REBUILD (VeilOverlay.java)
- [x] Respect config toggles per section
- [x] Fix coin stack overflow (use long)
- [x] Add prayer points (VarPlayer ID 709)
- [x] Add special attack % (VarPlayer 300)
- [x] Compact mode: one line per section
- [x] Credits: MorpheusXP | Morpheus7239

## EXECUTE ORDER
1. VeilConfig.java — kill sync, add all new options
2. VeilPlugin.java — add prayer/spec varbits, fix coin overflow
3. VeilOverlay.java — respect toggles, add prayer/spec
4. VeilPanel.java — full 7-tab rebuild
5. Push, test, done
