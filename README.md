# 🧠 Villager AI

<p align="center">
  <img src="icon.png" alt="Villager AI Icon" width="180" height="180" style="border-radius: 20px;" />
</p>

<p align="center">
  <strong>Intelligent behaviors, no-roam discipline, smart workstation claims, and live in-game stats for Minecraft villagers.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-26.3-brightgreen.svg" alt="Minecraft 26.3" />
  <img src="https://img.shields.io/badge/Modloader-Fabric%20%7C%20NeoForge%20%7C%20Forge-blue.svg" alt="Fabric | NeoForge" />
  <img src="https://img.shields.io/badge/License-MIT-orange.svg" alt="MIT License" />
  <img src="https://img.shields.io/badge/Environment-Client%20%26%20Server-purple.svg" alt="Client and Server" />
</p>

---

## 🌟 Overview

**Villager AI** upgrades Minecraft villagers with intelligent routines, predictable pathfinding, instant workstation claims, and a vanilla-friendly inspection GUI. Tired of villagers wandering off into caves, running into zombie hordes, or stealing job sites from across the village? **Villager AI** fixes that while preserving the core vanilla feel.

---

## ✨ Features

### 🛑 No-Roam AI (Stay Put)
- Villagers keep their daily work, meeting, and sleeping routines without aimlessly wandering around town.
- Prevents villagers from jumping into ravines, straying into zombie pits, or getting lost far from their trading stations.
- Smoothly toggleable in-game at any time with instant live updates on active villagers (no restart required).

### ⚒️ Nearest Workstation Claiming
- When a workstation is placed (by a player) or generated (in village structures), the **nearest unassigned villager** immediately claims the job.
- Fixes the frustrating vanilla behavior where a distant villager through three walls claims a workstation right in front of an unemployed local villager.

### 🔍 Quick Inspection Overlay (Keybind `V`)
Press **`V`** (customizable in *Controls -> Key Binds*) at any time to open a clean, vanilla-styled tooltip overlay:
- **Looking at a Villager**:
  - Displays Profession, Level, XP, and Baby/Adult status.
  - Displays assigned Workstation, Bed, and Meeting Bell coordinates.
  - Live Brain activity (`WORK`, `MEET`, `REST`, `IDLE`).
  - Current walk target coordinates and distance.
  - AI mode (`No-Roam Active` vs `Vanilla Roaming Active`).
- **Looking Away / Into the Open**:
  - Displays the **Village Daily Schedule & Calendar**.
  - Shows world clock time, day count, and highlights the current active phase (`▶ [NOW]`).
  - Displays current mod settings.

### 📜 Daily Village Timetable Reference

| Time | Ticks | Phase | Activity |
| :--- | :--- | :--- | :--- |
| **06:00 – 08:00** | `10 – 2000t` | `IDLE` | Wake up & morning leisure |
| **08:00 – 15:00** | `2000 – 9000t` | `WORK` | Working & restocking at workstation |
| **15:00 – 17:00** | `9000 – 11000t` | `MEET` | Gathering & gossiping at village bell |
| **17:00 – 18:00** | `11000 – 12000t` | `IDLE` | Evening leisure & wandering |
| **18:00 – 06:00** | `12000 – 24000t` | `REST` | Sleeping in bed |

---

## 💻 Commands

All commands can be executed via `/villagerstats` or `/villagerStats`:

- `/villagerstats` — Smart command: inspects targeted villager, or shows daily calendar if no villager is targeted.
- `/villagerstats status` — Shows targeted villager info in chat.
- `/villagerstats calendar` — Shows village daily schedule and mod settings.
- `/villagerstats toggle-roam` *(Requires OP / Permission Level 2)* — Toggles No-Roam AI on/off and refreshes all loaded villagers instantly.

---

## ⚙️ Configuration

Configuration is located at `config/villagerai.json`:

```json
{
  "disableRoaming": true,
  "assignNearestVillager": true
}
```

- **`disableRoaming`** *(default: `true`)*: When `true`, disables aimless wandering while preserving essential work, sleep, and meeting behaviors.
- **`assignNearestVillager`** *(default: `true`)*: When `true`, prioritizes the physically nearest unemployed villager whenever a workstation is placed or generated.

---

## 🌐 Server & Multiplayer Compatibility

- **Dedicated Server Support**: Works seamlessly on dedicated servers. Server admins can install it server-side to enforce smart AI for all players.
- **Client & Server Integration**: The `V` keybind overlay works in singleplayer and LAN worlds, while the server handles all AI decisions authoritatively.

---

## 📦 Compatibility & Requirements

- **Minecraft**: `26.3`
- **Supported Mod Loaders**:
  - **Fabric** (Requires [Fabric Loader](https://fabricmc.net/) and [Fabric API](https://modrinth.com/mod/fabric-api))
  - **NeoForge**
  - **Minecraft Forge**
- **Java**: 25+
