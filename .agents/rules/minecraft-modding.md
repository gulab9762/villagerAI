# Minecraft Modding & Brain AI Guidelines

## 🛑 Strict Grounding & Anti-Hallucination Policy
1. **Never fabricate or assume non-existent Minecraft/modloader versions**:
   - For example: `1.21.11` is the absolute final release of the `1.21.x` series. Versions `1.21.12`, `1.21.13`, etc. DO NOT exist. The release cycle jumped directly from `1.21.11` to `26.1`.
2. **Always verify version history, API signatures, and descriptors**:
   - Query project manifests, official release tags, or run `node scripts/rag.js class/method` before stating compatibility.
   - Never extrapolate semantic version numbers without verification.

## Target Environment
- **Minecraft**: 26.3
- **Java**: 25+
- **Architectures**: Fabric Loader & NeoForge Multiloader

## AI & Brain Systems
1. **Behaviors**:
   - Prefer extending `Behavior<Villager>` or `BehaviorControl<Villager>`.
   - In `checkExtraStartConditions(ServerLevel, Villager)`, ensure all required memories are present in `villager.getBrain()`.
   - Never block or sleep inside `tick()` or `start()`. Keep behavior steps non-blocking and tick-safe.
2. **Workstations & POIs**:
   - `PoiTypes` are registry-backed. Workstations correspond to `Holder<PoiType>`.
   - When searching for POIs, use `ServerLevel.getPoiManager().find(...)` or `findAllClosestFirstWithType(...)`.
   - Validate that a POI is not claimed before setting it in `MemoryModuleType.JOB_SITE` or `POTENTIAL_JOB_SITE`.
3. **No-Roam Discipline**:
   - Aimless wandering in vanilla is driven by `StrollToPoi` / `StrollAroundPoi` / `RandomStroll`.
   - In `VillagerGoalPackagesMixin`, ensure roaming behaviors are conditionally removed or bypassed when `VillagerAIConfig.getInstance().isDisableRoaming()` is true.
   - Do NOT disable walking to work (`JOB_SITE`), bed (`HOME`), or meeting bell (`MEETING_POINT`). Only aimless wander strolls should be restricted.

## Mixin Safety
- Avoid hard `@Overwrite` when `@Inject`, `@WrapOperation`, or targeted injection works.
- Always provide descriptive comments on injection points.
- If modifying method parameters or return values, check exact descriptors with `node scripts/rag.js method <MethodName>`.

