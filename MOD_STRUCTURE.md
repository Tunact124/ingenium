# Ingenium Optimization - Mod Structure & Code Overview

Ingenium is an **Adaptive Performance Intelligence System** for Minecraft 1.20.1 (Fabric). It aims to maintain 20 TPS (50ms MSPT) by dynamically adjusting its optimization strategies based on real-time server load.

---

### 📁 Project Structure (com.ingenium)

| Package | Role | Key Components |
|:---|:---|:---|
| `com.ingenium.core` | **The Brain** | `IngeniumGovernor` (budget/profile manager), `IngeniumExecutors` (thread pools), `GCAdaptiveScheduler`. |
| `com.ingenium.tick` | **Tick Engine** | `WheelBackedWorldTickScheduler` - Replaces vanilla's O(N) tick scheduler with an O(1) timing wheel. |
| `com.ingenium.be` | **BE Throttling** | `BlockEntityThrottleService` - Dynamically skips ticks for Block Entities far from players. |
| `com.ingenium.offheap`| **Memory** | `OffHeapBlockEntityStore` - Stores BE metadata outside the Java heap to reduce GC pressure. |
| `com.ingenium.simd` | **Vectorization** | `SIMDPaletteOptimizer` - Uses CPU SIMD instructions for block palette operations. |
| `com.ingenium.benchmark`| **Monitoring** | `IngeniumBenchmarkService`, `ChunkLatencyMonitor` - Real-time performance tracking and diagnostics. |
| `com.ingenium.compat` | **Compatibility** | `CompatibilityRegistry`, `IrisCompatibilityLayer` - Detects and yields to other optimization mods. |
| `com.ingenium.config` | **Settings** | `IngeniumConfig`, `IngeniumYaclScreen` - Config management via YetAnotherConfigLib. |
| `com.ingenium.ds` | **Structures** | `LongObjHashMap` - High-performance primitive-specialized collections to avoid boxing. |
| `com.ingenium.mixin` | **Integration** | Mixins for world ticking, chunk loading, and BE processing. |

---

### 💻 Code Examples & Logic

#### 1. Adaptive Performance Governor (`IngeniumGovernor`)
The Governor monitors MSPT and transitions between four profiles: `AGGRESSIVE`, `BALANCED`, `REACTIVE`, and `EMERGENCY`.

```java
// Transitions are based on MSPT thresholds (e.g., 40ms, 45ms, 50ms)
public void onTickEnd() {
    long mspt = getMspt(); // Simplified
    if (mspt > thresholds.emergency()) {
        transitionTo(OptimizationProfile.EMERGENCY);
    }
}

// Subsystems consume "time budget" from the Governor
if (governor.consumeBudget(SubsystemType.BLOCK_ENTITIES, estimatedCostNs)) {
    performTick();
}
```

#### 2. High-Performance Timing Wheel (`WheelBackedWorldTickScheduler`)
Replaces the vanilla `priority queue` with a `timing wheel` (buckets per tick). This makes scheduling and draining ticks nearly O(1).

```java
public int drainDue(long nowTick, int limit, TickConsumer<T> consumer) {
    int slot = slotForTick(nowTick);
    var bucket = buckets[slot];
    // Drain only what's due for the current slot
    for (Entry<T> entry : bucket) {
        if (entry.cycle <= currentCycle) {
            consumer.accept(entry.payload);
        }
    }
}
```

#### 3. Adaptive Block Entity Throttling (`BlockEntityThrottlePolicy`)
Decides whether to tick a Block Entity (BE) based on player proximity and the current Governor profile.

```java
public static Decision shouldTick(Inputs in) {
    // 1. Critical Radius: Always tick if a player is within X blocks
    if (in.distSq() <= in.criticalRadiusSq()) return Decision.tick("near_player");

    // 2. Starvation Safety: Force tick if it hasn't ticked for too long
    if (in.skipCount() >= in.maxSkip()) return Decision.tick("safety_net");

    // 3. Adaptive Throttling: Tick every Nth tick based on server load
    return (in.worldTime() % in.divisor() == 0) ? Decision.tick("ok") : Decision.skip("throttled");
}
```

#### 4. Off-Heap Metadata Storage (`OffHeapBlockEntityStore`)
To prevent millions of small objects from bloating the heap and triggering GC, BE metadata (like skip counts) is stored in native memory.

```java
// Accessing metadata without creating heap objects
long pointer = store.getPointer(blockPos.asLong());
int skipCount = UnsafeUtil.getInt(pointer + SKIP_COUNT_OFFSET);
```

---

### 🚀 Key Philosophies
1. **Zero-Allocation in Hot Paths**: Avoiding `new` during entity/block ticking.
2. **Safe Fallbacks**: If any optimization fails, it reverts to vanilla behavior rather than crashing.
3. **Complementary**: Automatically detects and yields to mods like Sodium, Lithium, and FerriteCore.
