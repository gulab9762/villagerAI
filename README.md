# Block Place Animations

A client-side Minecraft mod (for **Minecraft 26.3** Fabric & NeoForge) that adds fluid, satisfying visual animations when placing blocks in first-person view.

Inspired by [A Good Place](https://modrinth.com/mod/a-good-place), this mod gives tactile weight and visual feedback to building: blocks visibly travel from the player's held hand to the target placement position, dynamically scaling and rotating into place.

---

## ✨ Features

- **Hand-to-Target Flight**:
  - Dynamically calculates the 3D world coordinates of the player's held hand in first-person perspective, accounting for camera eye position, look direction, yaw, pitch, and player arm orientation (main hand / off hand, right-handed / left-handed).
- **Dynamic Size Scaling**:
  - Starts miniature at hand scale (~`0.32x`) and smoothly expands to full block size (`1.0x`) as it approaches the target block location.
  - Includes a subtle arrival settle bounce for a satisfying snap.
- **Flight Trajectory & Rotation**:
  - Smooth cubic ease-out deceleration curve with a parabolic travel arc.
  - Rotates from the natural tilt of the player's hand to align with the world block grid.
- **Chunk & Block Entity Occlusion**:
  - While the block is in transit, it is temporarily hidden from static chunk baking (`SectionCompiler`) and dynamic block entity extraction (`BlockEntityRenderDispatcher`).
  - Upon arrival, the block is seamlessly committed to the chunk mesh with optional subtle placement particles.
- **Modern Rendering Pipeline**:
  - Built natively for Minecraft 26.3 utilizing the modern `SubmitNodeCollector` and `MovingBlockRenderState` pipeline.
- **Multi-Loader Architecture**:
  - Clean separation between platform-independent animation logic (`common`), Fabric client implementation (`fabric`), and NeoForge client implementation (`neoforge`).

---

## 📁 Project Structure

```
block-place/
├── build-logic/         # Shared multi-loader Gradle convention plugins
├── common/              # Platform-agnostic client logic, trajectories & mixins
│   └── src/main/java/com/blockplace/
│       ├── BlockPlaceCommon.java
│       ├── Constants.java
│       ├── client/
│       │   ├── animation/
│       │   │   ├── BlockFlyAnimation.java
│       │   │   ├── BlockPlacementManager.java
│       │   │   └── FirstPersonHandHelper.java
│       │   └── config/
│       │       └── BlockPlaceConfig.java
│       └── mixin/
│           ├── BlockEntityRenderDispatcherMixin.java
│           ├── BlockItemMixin.java
│           ├── LevelRendererMixin.java
│           └── SectionCompilerMixin.java
├── fabric/              # Fabric loader module
│   └── src/main/java/com/blockplace/fabric/
│       ├── BlockPlaceFabric.java
│       └── client/BlockPlaceFabricClient.java
└── neoforge/            # NeoForge loader module
    └── src/main/java/com/blockplace/neoforge/
        └── BlockPlaceNeoForge.java
```

---

## 🛠️ Building & Running

### Prerequisites
- **JDK 25** (installed and configured in `gradle.properties`)

### Gradle Commands

- **Build Fabric Mod JAR**:
  ```powershell
  .\gradlew.bat buildFabric
  ```
  *Output:* `fabric/build/libs/blockplace-fabric-26.3-1.0.0.jar`

- **Run Fabric Client**:
  ```powershell
  .\gradlew.bat runFabricClient
  ```

- **Build NeoForge Mod JAR**:
  ```powershell
  .\gradlew.bat buildNeoForge
  ```
  *Output:* `neoforge/build/libs/blockplace-neoforge-26.3-1.0.0.jar`

- **Build All Loader JARs**:
  ```powershell
  .\gradlew.bat buildAllMods
  ```

---

## ⚙️ Configuration

Animation parameters can be adjusted via [BlockPlaceConfig.java](file:///c:/Users/Gulab/IdeaProjects/block-place/common/src/main/java/com/blockplace/client/config/BlockPlaceConfig.java):

| Option | Default | Description |
|---|---|---|
| `durationTicks` | `5` (~250 ms) | Duration of the flight animation in game ticks |
| `startScale` | `0.32f` | Initial scale when leaving the player hand |
| `arcHeight` | `0.18f` | Parabolic peak height along trajectory |
| `initialTiltDegrees` | `25.0f` | Initial rotational tilt from hand angle |
| `enableSettleBounce` | `true` | Subtle spring bounce upon landing |
| `enableArrivalParticles`| `true` | Subtle block landing puff |
| `firstPersonOnly` | `true` | Enable exclusively in first-person camera |
