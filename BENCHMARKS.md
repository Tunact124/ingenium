# Ingenium Performance Benchmarks

## Test Environment

**To be filled during Week 3 testing:**

- CPU: [Your CPU model]
- RAM: [Your RAM amount]
- GPU: [Your GPU model]
- Java Version: 21.0.x
- Minecraft Version: 1.20.1
- Fabric Loader: 0.15.11
- Ingenium Version: 0.3.0
- Test Date: [YYYY-MM-DD]

---

## How to Run Tests

### 1. Baseline Collection

```bash
# In Minecraft server console

# Start data collection
/Ingenium-perf collect start

# Record vanilla baseline (no Ingenium)
/Ingenium-perf collect baseline vanilla 60

# Record Ingenium balanced config
/Ingenium-perf collect baseline Ingenium_balanced 60

# Record Ingenium aggressive config
/Ingenium-perf collect baseline Ingenium_aggressive 60

# Stop collection
/Ingenium-perf collect stop

# Compare results
/Ingenium-perf collect compare vanilla Ingenium_balanced
```

### 2. Entity Stress Test

```bash
# Run full stress scenario (0→1000 entities)
/Ingenium-perf stress scenario

# Or manual control:
/Ingenium-perf stress start 100    # Spawn 100 entities
/Ingenium-perf stress start 500    # Spawn 500 entities
/Ingenium-perf stress report       # View results
/Ingenium-perf stress clear        # Remove all entities
```

### 3. Adaptive Systems Test

```bash
# Enable adaptive features
# Edit config/Ingenium.json:
{
  "enableAdaptiveOptimization": true,
  "enableTickGovernor": true,
  "enableEntityLOD": true
}

# Start tick profiler
/Ingenium-perf tick start

# Add load gradually
/Ingenium-perf stress start 100
/Ingenium-perf tick report

/Ingenium-perf stress start 300
/Ingenium-perf tick report

/Ingenium-perf stress start 500
/Ingenium-perf tick report

# Observe mode transitions in logs
```

### 4. Threshold Tuning

```bash
# After collecting baselines, analyze and tune
/Ingenium-perf tune analyze    # Generate recommendations
/Ingenium-perf tune report     # View tuning report
/Ingenium-perf tune apply      # Apply recommendations
```

---

## Expected Results Template

Fill in your actual results below:

### Vanilla Baseline

| Metric | Value |
|--------|-------|
| TPS | ? |
| MSPT (avg) | ? ms |
| MSPT (min) | ? ms |
| MSPT (max) | ? ms |
| MSPT (P95) | ? ms |
| Memory Usage | ? MB |
| Entity Count | ? |

### Ingenium (Balanced Config)

| Metric | Value |
|--------|-------|
| TPS | ? |
| MSPT (avg) | ? ms |
| MSPT (min) | ? ms |
| MSPT (max) | ? ms |
| MSPT (P95) | ? ms |
| Memory Usage | ? MB |
| Entity Count | ? |

### Performance Comparison

| Metric | Vanilla | Ingenium | Improvement |
|--------|---------|----------|-------------|
| TPS | ? | ? | ?% |
| MSPT (avg) | ? | ? | ?% |
| MSPT (P95) | ? | ? | ?% |
| Memory | ? | ? | ?% |

### Entity Stress Test Results

| Phase | Entities | TPS | MSPT | Lag% | Scaling |
|-------|----------|-----|------|------|---------|
| Baseline | 0 | ? | ? | ?% | - |
| Light | 100 | ? | ? | ?% | ? |
| Medium | 300 | ? | ? | ?% | ? |
| Heavy | 500 | ? | ? | ?% | ? |
| Extreme | 1000 | ? | ? | ?% | ? |

**Scaling Efficiency**: ? ms per entity (target: <0.1ms)

---

## Success Criteria

### Week 3 Validation Checklist

- [ ] Vanilla baseline measured (5+ minutes)
- [ ] Ingenium baseline measured (all configs)
- [ ] Entity stress test completed
- [ ] Memory usage data collected
- [ ] Comparison table filled with REAL numbers
- [ ] TickGovernor activates under load
- [ ] EntityLOD reduces tick time
- [ ] AdaptiveOptimizer transitions correctly
- [ ] No gameplay breakage detected
- [ ] MSPT improvement: 15-30% verified
- [ ] Entity stress: 30-50% improvement verified
- [ ] Memory reduction: 10-20% verified
- [ ] Thresholds tuned based on data
- [ ] BENCHMARKS.md updated with real data

---

## Interpreting Results

### TPS (Ticks Per Second)
- **20.0**: Perfect (target)
- **19.5+**: Excellent
- **18.0-19.5**: Good
- **15.0-18.0**: Fair (lag noticeable)
- **<15.0**: Poor (unplayable)

### MSPT (Milliseconds Per Tick)
- **<35ms**: Excellent (plenty of headroom)
- **35-45ms**: Good (comfortable)
- **45-50ms**: Warning (approaching limit)
- **50-55ms**: Critical (TPS will drop)
- **>55ms**: Emergency (severe lag)

### Memory Usage
- **<2GB**: Excellent
- **2-3GB**: Good
- **3-4GB**: Fair
- **>4GB**: High (may cause GC pauses)

### Scaling Efficiency
- **<0.1ms/entity**: Excellent (O(k) achieved)
- **0.1-0.5ms/entity**: Good
- **0.5-1.0ms/entity**: Fair
- **>1.0ms/entity**: Poor (linear scaling failed)

---

## Troubleshooting

### "No vanilla baseline found"
Run: `/Ingenium-perf collect baseline vanilla 60`

### "Stress test entities not spawning"
- Ensure you're in a loaded chunk
- Check server console for errors
- Try: `/Ingenium-perf stress clear` then retry

### "TPS not improving"
- Verify Ingenium is loaded: check logs for "[Ingenium] Initialization complete"
- Check config: `config/Ingenium.json`
- Ensure features are enabled

### "Profiler shows no data"
- Start profiler: `/Ingenium-perf tick start`
- Wait at least 5 seconds
- Check report: `/Ingenium-perf tick report`

---

## Data Export

Export collected data to CSV for external analysis:

```bash
/Ingenium-perf collect export
# Creates: Ingenium/data_export.csv
```

CSV columns:
- `timestamp`: Sample time
- `tps`: Ticks per second
- `mspt`: Milliseconds per tick
- `memory_mb`: Memory usage
- `entity_count`: Number of entities
- `chunk_count`: Number of loaded chunks

---

## Next Steps

After completing Week 3 testing:

1. **Fill in this file** with your actual results
2. **Update thresholds** based on your data
3. **Share results** with the community
4. **Report issues** on GitHub

---

*Generated for Ingenium Optimization Mod v0.3.0*
