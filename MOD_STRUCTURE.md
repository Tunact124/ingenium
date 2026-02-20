# Ingenium Optimization - Mod Structure & Developer Reference

This document provides a comprehensive overview of the Ingenium Optimization Mod's architecture and codebase. It is intended for both human developers and AI agents to understand how the mod is structured and how its components interact.

## 🧠 Design Philosophy: Adaptive Intelligence
Ingenium is not a static set of optimizations. It is an **Adaptive Performance Intelligence System** for Minecraft 1.20.1.
- **Goal**: Maintain 20 TPS (50ms MSPT) by dynamically adjusting optimization levels.
- **Method**: Real-time performance monitoring, performance budgets, and adaptive profiling.
- **Safety**: Every optimization must have a safe fallback to vanilla behavior.

---

## 📁 Package Structure (com.ingenium)

| Package | Description | Key Classes |
|---------|-------------|-------------|
| `com.ingenium` | Entry point and mod initialization. | `IngeniumMod` |
| `com.ingenium.core` | The core "brain" and engine of the mod. | `IngeniumGovernor`, `IngeniumExecutors`, `GCAdaptiveScheduler` |
| `com.ingenium.tick` | High-performance tick scheduling replacements. | `WheelBackedWorldTickScheduler`, `WheelStore` |
| `com.ingenium.be` | Block Entity optimization policies. | `BlockEntityThrottlePolicy` |
| `com.ingenium.benchmark` | Benchmarking and real-time diagnostics. | `IngeniumBenchmarkService`, `IngeniumDiagnostics` |
| `com.ingenium.mixin` | Hooks into Minecraft/Fabric internals. | `ScheduledTickWheelMixin`, `BlockEntityTickThrottleMixin` |
| `com.ingenium.config` | Configuration handling (YACL integration). | `IngeniumConfig`, `IngeniumYaclScreen` |
| `com.ingenium.compat` | Compatibility layers for other mods. | `CompatibilityRegistry`, `IrisCompatibilityLayer` |
| `com.ingenium.ds` | Custom high-performance data structures. | `LongObjHashMap` |
| `com.ingenium.offheap` | Memory management and off-heap storage. | `OffHeapBlockEntityStore` |
| `com.ingenium.simd` | SIMD-based performance optimizations. | `SIMDPaletteOptimizer` |

---

## 🛠 Key Components & Code Examples

### 1. `IngeniumGovernor` - The Performance Brain
The Governor manages four optimization profiles based on current MSPT and heap pressure.

```java
public enum OptimizationProfile {
    AGGRESSIVE(1, 8.0f, 10.0f, 6.0f, 5.0f, 0.85f, true),
    BALANCED(2, 6.0f, 7.0f, 4.0f, 4.0f, 0.75f, true),
    REACTIVE(3, 4.0f, 5.0f, 2.0f, 3.0f, 0.65f, true),
    EMERGENCY(6, 2.0f, 2.5f, 0.5f, 1.0f, 0.55f, false);
    
    // beDiv: divisor for block entity ticking
    // budgets: time (in ns) allowed for different subsystems per tick
}

// Logic for consuming budget:
public boolean consumeBudget(Subsystem subsystem, long costNs) {
    AtomicLong budget = getBudgetFor(subsystem);
    if (budget == null) return true;
    // ... atomic budget subtraction logic ...
}
```

### 2. `WheelBackedWorldTickScheduler` - Timing Wheel
Replaces vanilla's `WorldTickScheduler` with a zero-allocation, O(1) timing wheel for scheduled ticks (blocks and fluids).

```java
public final class WheelBackedWorldTickScheduler<T> {
    private final ObjectArrayList<OrderedTick<T>>[] buckets; // Slot-based storage
    private final LongObjMap<ObjectArrayList<OrderedTick<T>>> overflowByCycle; // Long-delay storage

    public int drainDue(long worldTimeNow, int limit, TickConsumer<T> consumer) {
        // Zero-allocation drain: iterates only through current time slot's bucket
        // Advance cycle logic handles overflow re-insertion.
    }
}
```

### 3. `BlockEntityThrottlePolicy` - Adaptive Throttling
Dynamically decides if a Block Entity should tick based on its proximity to players and the current Governor profile.

```java
public static boolean shouldTick(ServerWorld world, BlockEntity be, long worldTime) {
    // 1. Critical Radius Check (always tick if near player)
    // 2. Governor Divisor (tick every Nth tick based on profile)
    // 3. Budget Check (consume Governor budget)
    // 4. Emergency Sampling (deterministic skipping in emergency)
}
```

---

## 🤖 Guide for AI Agents & Contributors

### ⚠️ SAFETY FIRST: Do not mess up what we have!
1. **Never use `Thread.sleep()`** or blocking operations on the server thread.
2. **Minimize allocations** in the hot path (ticking code). Use `fastutil` or custom data structures like `LongObjHashMap`.
3. **Always use `IngeniumConfig.get().featureEnabled`** checks before applying any logic.
4. **Preserve Mixin Chainability**: Use `@WrapOperation` from `MixinExtras` instead of `@Redirect` or `@Overwrite` whenever possible.
5. **Fallback**: Ensure that if a component fails or is disabled, the game reverts to vanilla behavior without crashing.

### How to add a new optimization:
1. Define a toggle in `IngeniumConfig`.
2. Register your optimization's budget/subsystem in `IngeniumGovernor` if it's a per-tick task.
3. Use a Mixin to hook into the relevant Minecraft class.
4. Add benchmarks in `com.ingenium.benchmark` to prove the performance gain.

---

## 📋 Build & Compatibility
- **Minecraft**: 1.20.1 (Fabric)
- **Java**: 17+
- **Key Dependencies**: FastUtil, MixinExtras, YACL v3, ModMenu, Spark (Soft Depend).
- **Incompatible with**: Optifine (and Optifabric).
- **Complements**: Sodium, Lithium, FerriteCore, Starlight.
