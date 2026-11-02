# 🛠️ Easy Vanilla Villagers (EVV) — Backporting Guide for AI Agents

> **Audience**: AI Agents & Mod Developers  
> **Purpose**: Step-by-step instructions for porting Easy Vanilla Villagers (EVV) from Minecraft `26.3` to older Minecraft versions (`1.21.x`, `1.20.x`, `1.19.x`).  
> *Note: Localization/translation is excluded as requested.*

---

## 🏗️ 1. Project Architecture Overview

EVV uses a clean MultiLoader architecture:
- `common/`: 100% platform-agnostic vanilla Java code (AI behaviors, commands, UI screen, config, Mixins).
- `fabric/`: Fabric-specific entrypoint & keybind registration.
- `quilt/`: Quilt-specific entrypoint & keybind registration.
- `neoforge/`: NeoForge `@Mod("evv")` entrypoint & client event subscribers.
- `forge/`: Forge `@Mod("evv")` entrypoint & event listeners.
- Root `build.gradle`: Assembles the merged Universal JAR (`universalJar`) and deploys it (`copyUniversalJar`).

When porting to an older version, **all core gameplay logic remains in `common/`**. Only version-dependent Minecraft APIs and loader plugins change.

---

## ☕ 2. Java & Gradle Toolchain Mapping

Adjust `gradle.properties` and Gradle plugins depending on the target Minecraft version:

| Target Minecraft Version | Java Toolchain | Loom Plugin | NeoForge Dev Plugin | ForgeGradle |
| :--- | :--- | :--- | :--- | :--- |
| **`26.3`** *(Current)* | Java 25 | `1.18.1` | `net.neoforged.moddev:2.0.147` | `7.0.40` |
| **`1.21.0 – 1.21.4`** | Java 21 | `1.7.0+` | `net.neoforged.moddev:2.0.75+` | Forge `51.x` |
| **`1.20.5 – 1.20.6`** | Java 21 | `1.6.0+` | `net.neoforged.moddev:2.0.14+` | Forge `50.x` |
| **`1.20.0 – 1.20.4`** | Java 17 | `1.4.0+` | `net.neoforged.gradle:7.0.x` | Forge `47.x` |
| **`1.19.0 – 1.19.4`** | Java 17 | `1.2.0+` | N/A (Forge only) | Forge `43.x` – `45.x` |

### Required Changes in `gradle.properties`:
```properties
minecraft_version=<TARGET_MC> # e.g. 1.21.1 or 1.20.1
java_version=<TARGET_JAVA>    # 21 for 1.20.5+, 17 for 1.18-1.20.4
fabric_loader_version=...
fabric_version=...
neoforge_version=...
forge_version=...
```

---

## 🖥️ 3. Client UI & Rendering (`VillagerStatsScreen.java`)

### A. Rendering Method Changes
Minecraft `26.3` replaced `Screen.render()` with `extractRenderState(GuiGraphicsExtractor, ...)`. In older versions, you must revert to the standard rendering methods:

#### For `1.20.0 – 1.21.4` (Using `GuiGraphics`):
```java
@Override
public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    // 1. Render background dimming
    this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

    // 2. Measure dimensions
    int maxWidth = 0;
    for (Component line : lines) {
        if (!line.getString().equals("---")) {
            maxWidth = Math.max(maxWidth, this.font.width(line));
        }
    }
    int cardWidth = Math.max(maxWidth + 24, 260);
    int cardHeight = (lines.size() * 12) + 16;
    int startX = (this.width - cardWidth) / 2;
    int startY = (this.height - cardHeight) / 2;

    // 3. Draw card panels and borders
    guiGraphics.fill(startX - 2, startY - 2, startX + cardWidth + 2, startY + cardHeight + 2, 0x60000000);
    guiGraphics.fill(startX - 1, startY - 1, startX + cardWidth + 1, startY + cardHeight + 1, 0xFF35354A);
    guiGraphics.fill(startX, startY, startX + cardWidth, startY + cardHeight, 0xF0101018);
    guiGraphics.fill(startX + 1, startY + 1, startX + cardWidth - 1, startY + 2, 0x80FFAA00);

    // 4. Render lines
    int textY = startY + 10;
    for (Component line : lines) {
        if (line.getString().equals("---")) {
            guiGraphics.fill(startX + 10, textY + 3, startX + cardWidth - 10, textY + 4, 0x2AFFFFFF);
            textY += 8;
        } else {
            guiGraphics.drawString(this.font, line, startX + 12, textY, 0xFFFFFFFF, true);
            textY += 12;
        }
    }

    super.render(guiGraphics, mouseX, mouseY, partialTick);
}
```

#### For `1.19.x` and Older (Using `PoseStack`):
Replace `GuiGraphics` with `PoseStack poseStack`:
- Background: `this.renderBackground(poseStack);`
- Fill: `GuiComponent.fill(poseStack, x1, y1, x2, y2, color);`
- Text: `this.font.drawShadow(poseStack, line, x, y, 0xFFFFFFFF);`

---

### B. Keyboard Input Handling
In `26.3`, Minecraft introduced `KeyEvent`. In all previous versions (`1.16` – `1.21.x`), use the standard `keyPressed(int keyCode, int scanCode, int modifiers)`:

```java
@Override
public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == InputConstants.KEY_V || keyCode == 256 /* ESCAPE */) {
        this.onClose();
        return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
}
```

---

## ⌨️ 4. Keybinding Registration

### A. Fabric & Quilt
- In `26.3`: Uses `KeyMappingHelper.registerKeyMapping(...)` (`net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper`).
- In `1.16` – `1.21.3`: Uses `KeyBindingHelper.registerKeyBinding(...)` (`net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper`).

#### Version Check in `VillagerAIFabricClient.java` & `VillagerAIQuiltClient.java`:
```java
// For MC <= 1.21.3:
KeyBindingHelper.registerKeyBinding(VillagerStatsClient.KEY_MAPPING);

// For MC 26.3+:
KeyMappingHelper.registerKeyMapping(VillagerStatsClient.KEY_MAPPING);
```

### B. NeoForge & Forge
- NeoForge (`1.20.4+`): `RegisterKeyMappingsEvent.register(VillagerStatsClient.KEY_MAPPING)` remains identical.
- Forge (`1.19.x` – `1.20.1`): `RegisterKeyMappingsEvent.register(VillagerStatsClient.KEY_MAPPING)` remains identical.

---

## 🧠 5. Brain, AI Behaviors & Goal Packages

### A. `VillagerGoalPackagesMixin.java`
Inspect the method signatures in `VillagerGoalPackages`:
- `getCorePackage(...)`
- `getIdlePackage(...)`
- `getRestPackage(...)`
- `getMeetPackage(...)`

In versions `1.19.x` – `1.21.x`:
1. Check return type: Some versions use `ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>` while earlier versions (`1.18.2` and older) use `ImmutableList<Pair<Integer, ? extends Behavior<? super Villager>>>`.
2. Wandering behaviors removed when `disableRoaming == true`:
   - `VillageBoundRandomStroll`
   - `StrollAroundPoi`
   - `StrollToPoi`
   - `StrollToPoiList`
   - `InteractWithDoor` (optional)

### B. `AcquireNearestJobSite.java`
- Extends `Behavior<Villager>`.
- In `checkExtraStartConditions`, verify `owner.getBrain().getMemory(MemoryModuleType.JOB_SITE).isEmpty()`.
- Proximity search queries `ServerLevel.getPoiManager().find(...)` or `getPoiManager().getInRange(...)` targeting `PoiTypes.ACQUIRABLE_JOB_SITE` or `PoiType::isJobSite`.
- In versions `1.19` – `1.20.4`, `Holder<PoiType>` is used. In `1.18.2` and earlier, raw `PoiType` is used.

### C. World Ticks & Clock Time
- In `26.3`: `level.getOverworldClockTime()`.
- In `1.16` – `1.21.x`: `level.getDayTime()`.

---

## 🎯 6. ServerLevelMixin (Block Placement Hook)

In `common/src/main/java/com/villagerai/mixin/ServerLevelMixin.java`:
Target is `ServerLevel.setBlock(BlockPos, BlockState, int, int)`:

```java
@Inject(
    method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
    at = @At("RETURN")
)
private void evv$onBlockPlaced(BlockPos pos, BlockState state, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
    if (cir.getReturnValueZ()) {
        VillagerAssignmentHelper.tryAssignNearestUnemployedVillager((ServerLevel) (Object) this, pos, state);
    }
}
```

> [!WARNING] Method Descriptor In Older Versions
> In Minecraft `1.19.2` and earlier, `setBlock` might not take `recursionLeft` (4 arguments) and instead take 3 arguments:
> `setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z`.
> Verify using: `node scripts/rag.js method setBlock` or check Parchment mappings for the target MC version.

---

## ⚡ 7. Commands & Brigadier (`VillagerStatsCommand.java`)

1. **Permission Check**:
   - `26.3`: `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)`
   - `1.16 – 1.20.4`: `source.hasPermission(2)`
2. **Raycasting Target**:
   - `ProjectileUtil.getHitResultOnViewVector(player, predicate, 12.0)` is available across `1.18` – `1.21.x`.

---

## 📦 8. Universal Multi-Loader JAR Assembly

In the root `build.gradle`, the `universalJar` task merges the jars of each subproject:
```groovy
tasks.register('universalJar', Jar) {
    archiveBaseName = "${jar_base_name}-${minecraft_version}"
    archiveVersion = "${version}"
    destinationDirectory = layout.buildDirectory.dir('libs')
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    dependsOn ':fabric:jar', ':quilt:jar', ':neoforge:jar', ':forge:jar'

    from {
        [
            project(':fabric').tasks.jar.archiveFile,
            project(':quilt').tasks.jar.archiveFile,
            project(':neoforge').tasks.jar.archiveFile,
            project(':forge').tasks.jar.archiveFile
        ].collect { zipTree(it) }
    }
}
```

For versions without NeoForge (e.g. `1.20.1` and older), omit `:neoforge` and keep `:fabric`, `:quilt`, and `:forge`.

---

## ✅ 9. Backporting Checklist for the Agent

1. [ ] Create or switch to target branch (e.g. `git checkout -b 1.21.1`).
2. [ ] Update `gradle.properties` (`minecraft_version`, `java_version`, loader versions).
3. [ ] If target is `<= 1.20.4`, drop Java toolchain to 17.
4. [ ] In `VillagerStatsScreen.java`: Replace `extractRenderState` with `render(GuiGraphics, ...)` and adjust `keyPressed`.
5. [ ] In Fabric/Quilt clients: Switch `KeyMappingHelper` to `KeyBindingHelper` if on `<= 1.21.3`.
6. [ ] In `ServerLevelMixin.java`: Verify method signature of `setBlock` in the target Minecraft version.
7. [ ] In `VillagerStatsCommand.java`: Verify permission check (`hasPermission(2)`).
8. [ ] Replace `getOverworldClockTime()` with `getDayTime()` if missing.
9. [ ] Run `./gradlew buildAllMods` to verify all platforms compile and build cleanly.
10. [ ] Run `./gradlew copyUniversalJar` to ensure the final universal JAR is produced and deployed.

