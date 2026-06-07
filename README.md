# Veil RuneLite Plugin

**The RuneLite plugin for the [Veil OSRS Android app](https://github.com/MisTAiM/veil-osrs).**

Tracks every GE offer, quest, drop, slayer task, XP gain, and weapon charge — and serves it all to your phone over Wi-Fi via a local HTTP API. Zero cloud. Zero accounts. Your data stays on your machine.

## What it tracks

| Feature | Event / API | Detail |
|---|---|---|
| GE offers | `GrandExchangeOfferChanged` | Every slot, fill %, post-tax profit |
| Buy limits | Offer timestamps | Per-item 4hr reset countdown |
| GE search signal | `GrandExchangeSearched` | Flip grade overlay on item search |
| Loot | `ItemContainerChanged` (INV=93) | Inventory diff, auto-valued |
| Equipment | `ItemContainerChanged` (WORN=94) | Death risk, at-risk GP |
| Slayer | `VarPlayerID.SLAYER_TARGET/COUNT` | Task, KC, points, streak |
| XP | `StatChanged` | Per-skill, real-time, true XP/hr |
| Quests | `Quest.getState(client)` | All quests, live state |
| Weapon charges | `VarbitID.CHARGES_*` | Blowpipe, Trident, Sang, Tumekens |
| Boss drop %  | KC × drop rate math | Probability per notable drop |

## API

The plugin serves a minimal HTTP server (default port 7337):

```
GET /veil/ping   → {"ok":true,"version":"3.0.0"}
GET /veil/state  → full SyncPayload JSON (see below)
```

All responses include CORS headers. No external calls — data never leaves your machine.

## Installation

### Plugin Hub (when live)
Search **"Veil Flipper"** in RuneLite → Plugin Hub → Install.

### Manual (dev)
```bash
# Clone alongside runelite
git clone https://github.com/MisTAiM/veil-rl-plugin
# Open in IntelliJ with runelite project open
# Run via external plugin loader
```

## Connect to Veil app

1. Plugin running in RuneLite ✓
2. Open Veil app → GE tab → RuneLite tab
3. Find PC IP: Windows → `ipconfig` → IPv4 under Wi-Fi
4. Enter IP + port 7337, tap Connect
5. Data flows automatically every 5 seconds

Phone and PC must be on the same Wi-Fi network.

## Config options

| Setting | Default | Description |
|---|---|---|
| Server Port | 7337 | HTTP sync server port |
| Enable Sync | true | Toggle local server |
| GE Overlay | true | Show overlay when GE open |
| Track Loot | true | Inventory diff loot tracking |
| Track XP | true | StatChanged XP tracking |
| Alert on Fill | true | Desktop notify on offer complete |
| Min GP Alert | 50,000 | Minimum profit/loot to notify |

## SyncPayload structure

```json
{
  "rsn": "Morpheus",
  "loggedIn": true,
  "activeOffers": [...],
  "sessionTrades": [...],
  "sessionProfitGp": 340000,
  "sessionLoot": [...],
  "sessionLootGp": 1200000,
  "slayer": {"taskId": 1, "remaining": 84, "points": 1340, "streak": 47},
  "xpGained": {"SLAYER": 48200, "ATTACK": 12400},
  "questState": {"finished": 142, "inProgress": 3, "questPoints": 268},
  "equipment": {"totalValue": 4200000, "atRiskValue": 1100000},
  "weaponCharges": {"blowpipeCharges": 847, "blowpipeLow": false},
  "dropProgress": {"Cerberus": [{"itemName": "Primordial crystal", "pctStr": "71.2%", "isDry": false}]},
  "buyLimitResetAt": {"4151": 1717682400000},
  "pluginVersion": "3.0.0"
}
```

## License
BSD 2-Clause
