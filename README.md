# Ingenium

Ingenium is an **Adaptive Performance Intelligence System** for **Minecraft 1.20.1 (Fabric)**, designed to keep servers stable at **20 TPS (50ms MSPT)** by dynamically adjusting optimization strategies based on real-time load.

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

## 🖥️ What Ingenium does

Ingenium focuses on reducing tick-time spikes and smoothing worst-case behavior under load.

### Core systems
- **Adaptive Governor**
  - Tracks MSPT and switches optimization profiles (Aggressive / Balanced / Reactive / Emergency).
  - Budgets expensive subsystems to prevent runaway tick cost.

- **Timing Wheel Scheduled Ticks**
  - Replaces vanilla’s scheduled tick scanning with a timing wheel to reduce overhead in heavy worlds.
  - Designed to reduce worst-case spikes during large scheduled-tick workloads.

- **Block Entity Throttling**
  - Dynamically reduces tick frequency for block entities that are far from players or low-priority.

- **Off-heap Block Entity Metadata**
  - Stores select BE metadata off-heap to reduce long-lived heap pressure and GC churn.

- **SIMD / Vectorized scans (optional)**
  - Uses the Java Vector API when available for specific scan-style workloads where it actually helps.

- **Live diagnostics**
  - Tracks tick health and internal subsystem cost so you can understand *why* MSPT is rising.

* * *

## 🖥️ Installation

1. Install **Fabric Loader** for Minecraft **1.20.1**
2. Install **Fabric API**
3. Drop `Ingenium-*.jar` into your `mods` folder
4. Launch the game and configure via **Mod Menu**

* * *

## ⚙️ Configuration

Ingenium integrates with **Mod Menu** (YACL-based config screen).
Most options include a tooltip describing what they change and an **impact indicator** (Low/Medium/High).

General guidance:
- If you’re running a modpack with multiple optimization mods, enable changes gradually.
- The timing wheel and aggressive throttles can be high-impact in heavily-modded environments.

* * *

## 🔁 Compatibility

Ingenium attempts to be a good citizen around other performance mods and will avoid stepping on systems it detects as incompatible or redundant.

That said:
- Mixins in tick-critical paths are inherently sensitive.
- If you run into issues, please report them (see below) with logs and a mod list.

* * *

## 📬 Reporting issues

If you’d like to report a bug, crash, or compatibility problem:
- Open an issue on GitHub: https://github.com/Tunact124/ingenium/issues

Helpful info to include:
- Minecraft version (1.20.1)
- Fabric Loader + Fabric API versions
- Ingenium version
- Full mod list
- Latest log (`logs/latest.log`)
- Reproduction steps (if possible)

* * *

## 🧭 Roadmap / ideas (subject to change)

I’m actively experimenting with additional optimizations and diagnostics.

Planned ideas may include:
- More robust benchmark & report tooling for before/after comparisons
- Additional governor-aware budgeting hooks across more subsystems
- Expanded compatibility layers for popular performance mod stacks
- Safer runtime toggles / fallbacks for invasive systems

**Note:** Plans can change or be scrapped entirely based on test results, compatibility concerns, or new Minecraft versions — so treat this section as “direction,” not a promise.

* * *

## 🛠️ Building from source

Ingenium uses **Gradle**.

Typical workflow:
- `./gradlew build`

Build artifacts will be in:
- `build/libs` (or your configured output directory)

> If your project requires specific Java/Gradle versions, list them here (recommended).

* * *
