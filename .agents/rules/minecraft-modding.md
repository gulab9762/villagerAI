# Minecraft Modding & Brain AI Guidelines

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

