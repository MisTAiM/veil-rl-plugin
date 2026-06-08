# ⚡ Veil — OSRS GE Intelligence Plugin

> The most advanced Grand Exchange intelligence plugin for RuneLite. Real-time flip scoring, sell confidence, price prediction, bot detection, and 18 feature tabs.

**Website + Wiki: [veil-gg.vercel.app](https://veil-gg.vercel.app)**

## Installation

### Option A — Plugin Hub (pending approval)
Search "Veil Flipper" in RuneLite Plugin Hub once the PR is approved.

### Option B — Manual Install
1. Download `veil-flipper.jar` from [Releases](https://github.com/MisTAiM/veil-rl-plugin/releases)
2. Place in `%USERPROFILE%\.runelite\plugins\` (Windows) or `~/.runelite/plugins/` (Mac/Linux)
3. Start RuneLite — ⚡ Veil icon appears in sidebar

### Option C — Build from Source
```
git clone https://github.com/MisTAiM/veil-rl-plugin.git
cd veil-rl-plugin
# Open in IntelliJ IDEA with JDK 11 (Eclipse Temurin)
# Run VeilPluginTest to test in the RuneLite client
```

## Features

| Tab | Feature |
|-----|---------|
| Now! | Grandma mode — one button, one instruction, auto-refreshes every 60s |
| Flips | Live flip signals with grade S/A/B/C/D, sell confidence 0-100, price prediction |
| Trades | Active GE offers with sell recommendations and 2hr price outlook |
| Wallet | 8-slot optimizer — best flips for your exact bank right now |
| Intel | Market intelligence — supply shocks, accumulation alerts, time-of-day advice |
| History | Persistent flip history, buy limit reset tracker, GP chart, CSV export |
| Bosses | Live boss GP/hr with real drop prices updated every 2 minutes |
| Tools | Market making, correlation pairs, superheat, herb patch, crafting arbitrage |
| Guide | Slayer task guides, boss unlock chains, daily routine generator |
| Stats | Live OSRS hiscores, personalised advice, content gates |
| Alerts | Price alerts, GP goal tracker, tax calculator |
| Style | 5 personality modes, Discord webhook alerts |

## What makes it different

- **Sell confidence (0-100)** — 5 checks verify the sell price will actually fill before you list
- **Price prediction** — linear regression with R² threshold (honest about uncertainty)
- **Bot detection** — 4 signals identify bot-infested markets you should avoid
- **Patient buy prices** — saves 350k–1.9M per flip cycle vs standard approach
- **Buy limit reset tracker** — tracks from when order was PLACED, not filled
- **Slayer integration** — 135 task name mappings, task guides, boss unlock chains

## API Usage

Veil fetches data from:
- **OSRS Wiki Prices API** — `prices.runescape.wiki` — public, no auth required
- **OSRS Hiscores** — `secure.runescape.com/m=hiscore_oldschool` — public, read-only

No game data is read beyond the official RuneLite API. No accounts, no passwords, no tracking.

## Plugin Hub

PR: [runelite/plugin-hub#12463](https://github.com/runelite/plugin-hub/pull/12463)

---
*Not affiliated with Jagex. OSRS is a trademark of Jagex Ltd.*
