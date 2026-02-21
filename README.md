# Ingenium

Ingenium is an **Adaptive Performance Intelligence System** for **Minecraft 1.20.1 (Fabric)**, designed to keep servers stable at **20 TPS (50ms MSPT)** by dynamically adjusting optimization strategies based on real-time load.

It provides a robust, high-performance optimization layer that complements existing mods like Sodium and Lithium by focusing on areas they don't cover, using modern Java techniques and hardware-aware logic.

**This mod is the result of a lot of iteration, profiling, and in-game testing — and it’s made possible thanks to players like you.**
If you’d like to support development, you can contribute here:
[Ko-fi: https://ko-fi.com/ingeniummod](https://ko-fi.com/ingeniummod)

* * *

## 📥 Downloads

### Stable builds
- Modrinth: <MODRINTH LINK>
- CurseForge: <CURSEFORGE LINK>

### Source / development builds
- GitHub: https://github.com/Tunact124/ingenium

> If you’re testing dev builds, expect rough edges and limited support.

* * *

## 🖥️ Key Systems

### 1. Intelligence & Governance
- **Adaptive Performance Governor**
  - Monitors MSPT in real-time and transitions between four profiles: `AGGRESSIVE`, `BALANCED`, `REACTIVE`, and `EMERGENCY`.
  - Manages strict nanosecond budgets for subsystems to ensure tick stability.
- **Hardware Intelligence**
  - Features a deterministic benchmarking kernel that categorizes your hardware into `LOW`, `MID`, or `HIGH` tiers.
  - Automatically scales optimization intensity based on your PC's actual performance capabilities.
- **Fail-Safe Runtime**
  - Robust exception handling at all Mixin boundaries to report failures without crashing the game.

### 2. Visual Pipeline (Client-Only)
- **Entity Backface Culling**
  - Zero-allocation polygon-level culling using face normals and dot products. Significant FPS boost in entity-heavy areas.
- **Sodium-Native Item Rendering**
  - Bypasses vanilla list rendering for item models, using Sodium's `VertexBufferWriter` for high-speed bulk vertex emission.
- **Optimized Decals**
  - Visible-face pruning for block-breaking overlays with Sodium-specific bulk pushes.

### 3. Worldgen & Math
- **SIMD Vectorization**
  - Uses the **Java 21 Vector API** to vectorize noise interpolation kernels, significantly accelerating world generation.
- **FMA-based Math**
  - Leverages Fused Multiply-Add (FMA) instructions across both scalar and SIMD paths for high-precision, high-performance calculations.

### 4. High-Performance Infrastructure
- **Timing Wheel Scheduler**
  - Replaces vanilla's O(log N) priority-queue scheduler with an O(1) timing wheel for block and fluid ticks.
- **Off-heap Metadata Storage**
  - Stores block entity metadata in native memory (`DirectByteBuffer`) to reduce JVM heap pressure and GC scanning times.
- **Primitive Collections**
  - Custom open-addressing hash maps (`LongIntHashMap`, `LongObjHashMap`) designed to avoid boxing and iterator allocations in hot paths.

### 5. Specialist Optimizations
- **Intelligent Throttling**
  - **Hoppers**: Budget-aware cooldowns when pushing into full inventories.
  - **Experience Orbs**: Count-aware merging logic to reduce entity counts without losing XP.
  - **POI Queries**: Per-tick spatial hashing index to collapse repeated villager/POI lookups.

* * *

## 🔁 Compatibility & "Buddy Logic"

Ingenium is designed to be a "good citizen" in your modpack. It includes built-in **Buddy Logic** to detect and yield to major optimization mods:
- **Sodium / Iris**: Integrates with Sodium's config menu and leverages Sodium's rendering API.
- **Lithium**: Automatically yields domain ownership (like the Timing Wheel) if Lithium is present to avoid conflicts.
- **Krypton**: Disables network-level pooling when Krypton's superior networking is detected.
- **C2ME**: Thread-local scratch buffers ensure safety with C2ME's parallel worldgen.

* * *

## 📊 A/B Benchmark Service

Includes a built-in benchmark command (`/ingenium benchmark`) that allows you to validate the mod's impact in your specific environment by running Phase A (Baseline) and Phase B (Ingenium) tests side-by-side.

* * *

## 🖥️ Installation

1. Install **Fabric Loader** for Minecraft **1.20.1**
2. Install **Fabric API**
3. (Optional but Recommended) Install **Sodium** and **YetAnotherConfigLib (YACL) v3**
4. Drop `Ingenium-*.jar` into your `mods` folder
5. Launch the game and configure via **Mod Menu**

* * *

## ⚙️ Configuration

Ingenium integrates with **Mod Menu** (YACL-based config screen).
Most options include a tooltip describing what they change and an **impact indicator** (Low/Medium/High).

* * *

## 📬 Reporting issues

If you’d like to report a bug, crash, or compatibility problem:
- Open an issue on GitHub: https://github.com/Tunact124/ingenium/issues

Helpful info to include:
- Minecraft version (1.20.1)
- Fabric Loader + Fabric API versions
- Full mod list
- Latest log (`logs/latest.log`)
- Reproduction steps (if possible)

* * *

## 🛠️ Building from source

Ingenium requires **Java 21** (for the Vector API) and uses **Gradle**.

Typical workflow:
- `./gradlew build`

Build artifacts will be in:
- `build/libs`

* * *
