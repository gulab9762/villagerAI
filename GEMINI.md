# Easy Vanilla Villagers (EVV) Project Guidelines & Architecture Map

## Overview
**Easy Vanilla Villagers (EVV)** is a Minecraft optimization mod that overhauls villager AI routines, pathfinding discipline, workstation claiming, and adds an in-game inspection overlay.
- **Mod ID**: `evv`
- **Mod Name**: `Easy Vanilla Villagers`
- **JAR Naming**: `easy-vanilla-villagers-${minecraft_version}-${version}.jar` (Universal) and `easy-vanilla-villagers-${platform}-${minecraft_version}-${version}.jar` (Platform-specific)
- **Minecraft Version**: `26.3` (Java 25+) on main, `1.21.11` (Java 21) on branch `1.21.11`
- **Supported Mod Loaders**: Fabric, Quilt, NeoForge, and Forge
- **Structure**: MultiLoader architecture (`common`, `fabric`, `quilt`, `neoforge`, `forge`) + Root Universal JAR builder

---

## ⚡ Fast RAG & Minecraft Knowledge Retrieval
This project has a built-in offline RAG tool for instant lookups without guessing or searching Gradle caches:
- **Search Minecraft classes & methods**: `node scripts/rag.js class <ClassName>` (e.g. `node scripts/rag.js class Villager`)
- **Search method descriptors & params**: `node scripts/rag.js method <MethodName>` (e.g. `node scripts/rag.js method setMemory`)
- **Search Fabric/NeoForge docs**: `node scripts/rag.js search "<query>"` (e.g. `node scripts/rag.js search "custom brain behavior"`)
- **Search project code**: `node scripts/rag.js project "<query>"` (e.g. `node scripts/rag.js project "AcquireNearestJobSite"`)

Always use `scripts/rag.js` to verify Minecraft method signatures, parameter names, and obfuscated mappings before writing Mixins or AI behaviors.

---

## 🛑 Strict Grounding & Anti-Hallucination Policy
1. **Never guess, assume, or hallucinate non-existent versions**:
   - `1.21.11` is the absolute final release of the `1.21.x` cycle. `1.21.12` and `1.21.13` do NOT exist. Mojang jumped directly from `1.21.11` to `26.1`.
   - Never extrapolate semantic version numbers without verification.
2. **Verify against facts**: Always verify release versions, mappings, and class names via the RAG database, gradle dependencies, or official metadata before presenting them.
3. **If uncertain**, explicitly look it up or ask the user instead of guessing.

---

## Architecture & Code Organization

### 1. `common/` (Platform-Agnostic Core Logic)
All gameplay, AI, configuration, commands, and client UI logic MUST be placed here:
- `com.villagerai.ai.AcquireNearestJobSite`: Custom `Behavior<Villager>` replacing vanilla `AcquirePoi` for `JOB_SITE`. Enforces physical proximity claiming.
- `com.villagerai.ai.VillagerAssignmentHelper`: Distance calculations, nearest unemployed villager search, POI reservation checks.
- `com.villagerai.config.VillagerAIConfig`: Configuration system (`config/evv.json` with fallback `config/villagerai.json`), controlling `disableRoaming` and `assignNearestVillager`.
- `com.villagerai.command.VillagerStatsCommand`: `/evv` (Easy Vanilla Villagers) and `/villagerstats` command logic (status, calendar, toggle-roam).
- `com.villagerai.client`: HUD overlay rendering (`VillagerStatsScreen`, `VillagerStatsClient`, `VillagerStatsData`) triggered via keybind `V`.
- `com.villagerai.mixin`:
  - `VillagerGoalPackagesMixin`: Overrides villager core, idle, rest, and meet packages. Strips aimless wandering when `disableRoaming` is active.
  - `ServerLevelMixin`: Hooks workstation block placement to trigger nearest villager assignment.

### 2. `fabric/` (Fabric Modloader Platform)
- Entrypoints: `VillagerAIFabric` (ModInitializer) and `VillagerAIFabricClient` (ClientModInitializer).
- Config / Manifest: `src/main/resources/fabric.mod.json`, `src/main/resources/quilt.mod.json`, `evv.mixins.json`.

### 3. `quilt/` (Quilt Modloader Platform)
- Entrypoints: `VillagerAIQuilt` (ModInitializer) and `VillagerAIQuiltClient` (ClientModInitializer).
- Config / Manifest: `src/main/resources/quilt.mod.json`, `src/main/resources/fabric.mod.json`, `evv.mixins.json`.

### 4. `neoforge/` (NeoForge Modloader Platform)
- Entrypoints: `VillagerAINeoForge` (`@Mod("evv")`) and `VillagerAINeoForgeClient`.
- Config / Manifest: `src/main/resources/META-INF/neoforge.mods.toml`, `evv.mixins.json`.

### 5. `forge/` (Forge Modloader Platform)
- Entrypoints: `VillagerAIForge` (`@Mod("evv")`) and `VillagerAIForgeClient`.
- Config / Manifest: `src/main/resources/META-INF/mods.toml`, `evv.mixins.json`.

---

## Development & Build Commands
- **Check compile (Common)**: `./gradlew :common:compileJava`
- **Build Fabric jar**: `./gradlew :fabric:build` (or `./gradlew buildFabric`)
- **Build Quilt jar**: `./gradlew :quilt:build` (or `./gradlew buildQuilt`)
- **Build NeoForge jar**: `./gradlew :neoforge:build` (or `./gradlew buildNeoForge`)
- **Build Forge jar**: `./gradlew :forge:build` (or `./gradlew buildForge`)
- **Build Universal Multi-Loader JAR**: `./gradlew universalJar` (or `./gradlew buildUniversalJar`)
  - Generates `build/libs/easy-vanilla-villagers-${minecraft_version}-1.0.0.jar` compatible across Fabric, Quilt, NeoForge, and Forge.
  - Automatically copies the universal jar to `D:\MultiMC\instances\${minecraft_version}\.minecraft\mods` and `~/Downloads`.
- **Copy Universal JAR explicitly**: `./gradlew copyUniversalJar`
- **Full project build (all loaders + universal jar)**: `./gradlew buildAllMods`

---

## Coding Rules & Constraints
1. **Multiloader Purity**: NEVER import Fabric, Quilt, NeoForge, or Forge classes inside `common/`. Keep `common/` pure vanilla Minecraft and Java.
2. **Brain & Behavior Principles**:
   - Villagers use Mojang's `Brain<Villager>` system with `Activity` phases (`CORE`, `IDLE`, `WORK`, `REST`, `MEET`).
   - Memories are stored via `MemoryModuleType` (e.g. `JOB_SITE`, `POTENTIAL_JOB_SITE`, `WALK_TARGET`, `HOME`, `MEETING_POINT`).
   - Always respect `VillagerAIConfig` flags when adjusting behavior packages.
3. **Mixin Discipline**:
   - Use `@Inject` with `cancellable = true` or targeted `@ModifyVariable` / `@Redirect` where safe.
   - Always verify target method signatures using `node scripts/rag.js method <MethodName>`.
4. **Git Commits**: Follow `.agents/rules/git-commits.md` (natural, casual, concise messages).
