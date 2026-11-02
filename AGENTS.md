# VillagerAI Project Guidelines & Architecture Map

## Overview
**Villager Tweaks / VillagerAI** is a Minecraft optimization mod that overhauls villager AI routines, pathfinding discipline, workstation claiming, and adds an in-game inspection overlay.
- **Minecraft Version**: `26.3` (Java 25+)
- **Mod Loaders**: Fabric (`0.160.6+26.3` / Loader `0.19.5`) & NeoForge (`26.3.0.1-beta`)
- **Structure**: Architectury / Multiloader project (`common`, `fabric`, `neoforge`)

---

## ⚡ Fast RAG & Minecraft Knowledge Retrieval
This project has a built-in offline RAG tool for instant lookups without guessing or searching Gradle caches:
- **Search Minecraft classes & methods**: `node scripts/rag.js class <ClassName>` (e.g. `node scripts/rag.js class Villager`)
- **Search method descriptors & params**: `node scripts/rag.js method <MethodName>` (e.g. `node scripts/rag.js method setMemory`)
- **Search Fabric/NeoForge docs**: `node scripts/rag.js search "<query>"` (e.g. `node scripts/rag.js search "custom brain behavior"`)
- **Search project code**: `node scripts/rag.js project "<query>"` (e.g. `node scripts/rag.js project "AcquireNearestJobSite"`)

Always use `scripts/rag.js` to verify Minecraft method signatures, parameter names, and obfuscated mappings before writing Mixins or AI behaviors.

---

## Architecture & Code Organization

### 1. `common/` (Platform-Agnostic Core Logic)
All gameplay, AI, configuration, commands, and client UI logic MUST be placed here:
- `com.villagerai.ai.AcquireNearestJobSite`: Custom `Behavior<Villager>` replacing vanilla `AcquirePoi` for `JOB_SITE`. Enforces physical proximity claiming.
- `com.villagerai.ai.VillagerAssignmentHelper`: Distance calculations, nearest unemployed villager search, POI reservation checks.
- `com.villagerai.config.VillagerAIConfig`: Configuration system (`config/villagerai.json`), controlling `disableRoaming` and `assignNearestVillager`.
- `com.villagerai.command.VillagerStatsCommand`: `/villagerstats` command logic (status, calendar, toggle-roam).
- `com.villagerai.client`: HUD overlay rendering (`VillagerStatsScreen`, `VillagerStatsClient`, `VillagerStatsData`) triggered via keybind `V`.
- `com.villagerai.mixin`:
  - `VillagerGoalPackagesMixin`: Overrides villager core, idle, rest, and meet packages. Strips aimless wandering when `disableRoaming` is active.
  - `ServerLevelMixin`: Hooks workstation block placement to trigger nearest villager assignment.

### 2. `fabric/` (Fabric Modloader Platform)
- Entrypoints: `VillagerAIFabric` (ModInitializer) and `VillagerAIFabricClient` (ClientModInitializer).
- Config / Manifest: `src/main/resources/fabric.mod.json`, `villagerai.fabric.mixins.json`.

### 3. `neoforge/` (NeoForge Modloader Platform)
- Entrypoints: `VillagerAINeoForge` (`@Mod("villagerai")`) and `VillagerAINeoForgeClient`.
- Config / Manifest: `src/main/resources/META-INF/neoforge.mods.toml`, `villagerai.neoforge.mixins.json`.

---

## Development & Build Commands
- **Check compile (Common)**: `./gradlew :common:compileJava`
- **Build Fabric jar**: `./gradlew :fabric:build`
- **Build NeoForge jar**: `./gradlew :neoforge:build`
- **Full project build**: `./gradlew build`

---

## Coding Rules & Constraints
1. **Multiloader Purity**: NEVER import Fabric or NeoForge classes inside `common/`. Keep `common/` pure vanilla Minecraft and Java.
2. **Brain & Behavior Principles**:
   - Villagers use Mojang's `Brain<Villager>` system with `Activity` phases (`CORE`, `IDLE`, `WORK`, `REST`, `MEET`).
   - Memories are stored via `MemoryModuleType` (e.g. `JOB_SITE`, `POTENTIAL_JOB_SITE`, `WALK_TARGET`, `HOME`, `MEETING_POINT`).
   - Always respect `VillagerAIConfig` flags when adjusting behavior packages.
3. **Mixin Discipline**:
   - Use `@Inject` with `cancellable = true` or targeted `@ModifyVariable` / `@Redirect` where safe.
   - Always verify target method signatures using `node scripts/rag.js method <MethodName>`.
4. **Git Commits**: Follow `.agents/rules/git-commits.md` (natural, casual, concise messages).

