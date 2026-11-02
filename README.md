# 🧠 Villager Tweaks

**Intelligent behaviors, no-roam discipline, better workstation claims, and live in-game stats for Minecraft villagers.**

![Minecraft 26.3](https://img.shields.io/badge/Minecraft-26.3-brightgreen.svg) ![Fabric | NeoForge](<https://img.shields.io/badge/Modloader-Fabric | NeoForge-blue.svg>) ![MIT License](https://img.shields.io/badge/License-MIT-orange.svg) ![Client and Server](<https://img.shields.io/badge/Environment-Client & Server-purple.svg>)

***

## 🌟 Overview

**Villagers Tweaks** is a lightweight, optimization-focused mod designed to completely fix and overhaul vanilla Minecraft's frustrating villager AI, pathfinding, and profession mechanics. If you are tired of villagers claiming workstations from miles away, stealing each other's jobs, or wandering out of their trading cells, this mod is the definitive quality-of-life solution for your world or modpack.

## 🛠️ Key Features

*   **Smart Nearest Workstation Claiming:** Overrides broken vanilla logic. Villagers will now strictly prioritize and claim only the **closest workstation** or **job site block** relative to their position. No more cross-linking through walls or stealing jobs from across your base!
*   **No-Roam Trading Discipline:** Implements strict pathfinding boundaries to stop villagers from wandering aimlessly. Keeps your traders exactly where you want them, making trading center layouts and automated villager farms perfectly predictable.
*   **Optimized AI Performance:** Reduces the constant background pathfinding checks that cause server lag in large trading hubs or automated villager breeders.

## 🚀 Why Download Villagers Tweaks?

Building a **Minecraft villager trading hall** should be fun, not an exercise in frustration. Vanilla pathfinding often allows roaming villagers to lock onto a **job block** they can't even reach, preventing other nearby villagers from taking the profession. **Villagers Tweaks** forces the AI to make logical local decisions, allowing you to cycle trades, assign professions, and lock down your trading setup effortlessly.

Perfect for vanilla-plus playstyles, large survival servers, and custom modpacks!

### 🔍 Quick Inspection Overlay (Keybind `V`)

Press **`V`** (customizable in _Controls -> Key Binds_) at any time to open a clean, vanilla-styled tooltip overlay:

*   **Looking at a Villager**:
    *   Displays Profession, Level, XP, and Baby/Adult status.
    *   Displays assigned Workstation, Bed, and Meeting Bell coordinates.
    *   Live Brain activity (`WORK`, `MEET`, `REST`, `IDLE`).
    *   Current walk target coordinates and distance.
    *   AI mode (`No-Roam Active` vs `Vanilla Roaming Active`).
*   **Looking Away / Into the Open**:
    *   Displays the **Village Daily Schedule & Calendar**.
    *   Shows world clock time, day count, and highlights the current active phase (`▶ [NOW]`).
    *   Displays current mod settings.

### 📜 Daily Village Timetable Reference

| Time          |Ticks          |Phase |Activity                              |
| ------------- |-------------- |----- |------------------------------------- |
| <strong>06:00 – 08:00</strong> |<code>10 – 2000t</code> |<code>IDLE</code> |Wake up &amp; morning leisure         |
| <strong>08:00 – 15:00</strong> |<code>2000 – 9000t</code> |<code>WORK</code> |Working &amp; restocking at workstation |
| <strong>15:00 – 17:00</strong> |<code>9000 – 11000t</code> |<code>MEET</code> |Gathering &amp; gossiping at village bell |
| <strong>17:00 – 18:00</strong> |<code>11000 – 12000t</code> |<code>IDLE</code> |Evening leisure &amp; wandering       |
| <strong>18:00 – 06:00</strong> |<code>12000 – 24000t</code> |<code>REST</code> |Sleeping in bed                       |

***

## 💻 Commands

All commands can be executed via `/villagerstats` or `/villagerStats`:

*   `/villagerstats` — Smart command: inspects targeted villager, or shows daily calendar if no villager is targeted.
*   `/villagerstats status` — Shows targeted villager info in chat.
*   `/villagerstats calendar` — Shows village daily schedule and mod settings.
*   `/villagerstats toggle-roam` _(Requires OP / Permission Level 2)_ — Toggles No-Roam AI on/off and refreshes all loaded villagers instantly.

***

## ⚙️ Configuration

Configuration is located at `config/villagerai.json`:

```
{
  "disableRoaming": true,
  "assignNearestVillager": true
}
```

*   **`disableRoaming`** _(default: `true`)_: When `true`, disables aimless wandering while preserving essential work, sleep, and meeting behaviors.
*   **`assignNearestVillager`** _(default: `true`)_: When `true`, prioritizes the physically nearest unemployed villager whenever a workstation is placed or generated.

***

## 🌐 Server & Multiplayer Compatibility

*   **Dedicated Server Support**: Works seamlessly on dedicated servers. Server admins can install it server-side to enforce smart AI for all players.
*   **Client & Server Integration**: The `V` keybind overlay works in singleplayer and LAN worlds, while the server handles all AI decisions authoritatively.

***

## 📦 Compatibility & Requirements

*   **Minecraft**: `26.3`
*   **Supported Mod Loaders**:
    *   **Fabric** (Requires [Fabric Loader](https://fabricmc.net/) and [Fabric API](https://modrinth.com/mod/fabric-api))
    *   **NeoForge**
*   **Java**: 25+