package com.ingenium.governor;

import com.ingenium.config.IngeniumConfig;
import com.ingenium.util.IngeniumLogger;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class IngeniumGovernor {
 
    // ── State ──────────────────────────────────────────────────────
    private static volatile OptimizationProfile ACTIVE  = OptimizationProfile.BALANCED;
    private static volatile boolean             FORCED  = false;
 
    // Hysteresis: track how many consecutive ticks the candidate has been seen
    private OptimizationProfile candidateProfile        = OptimizationProfile.BALANCED;
    private int                 candidateHoldTicks      = 0;
 
    private final MetricsCollector     metrics;
    private final ActivityClassifier   classifier;
    private final List<ProfileListener> listeners = new CopyOnWriteArrayList<>();
 
    // ── Per-domain tick budgets (ms allocated per subsystem per tick) ──
    // These are read by each subsystem to decide how much work to do per tick.
    private volatile int aiTickBudgetMs      = 4;
    private volatile int renderTickBudgetMs  = 3;
    private volatile int ioTickBudgetMs      = 2;
 
    // ── Main governor tick ─────────────────────────────────────────
    public void onTickStart(MinecraftServer server) {
        metrics.onTickStart();
    }
 
    public void onTickEnd(MinecraftServer server) {
        metrics.onTickEnd();
        classifier.tick();
 
        if (FORCED) return; // Skip resolution if profile is forced

        double mspt    = metrics.averageMspt();
        int    players = server.getCurrentPlayerCount();
        ActivityState  state = classifier.classify(mspt, players);
        OptimizationProfile candidate = resolveProfile(mspt, state);
 
        // ── Hysteresis gate ─────────────────────────────────────────
        int hysteresisTicks = IngeniumConfig.get().hysteresisTickWindow;
        if (candidate == candidateProfile) {
            candidateHoldTicks++;
        } else {
            candidateProfile   = candidate;
            candidateHoldTicks = 1;
        }
 
        if (candidateHoldTicks >= hysteresisTicks && candidate != ACTIVE) {
            setProfile(candidate);
            candidateHoldTicks = 0;
        }
    }

    private void setProfile(OptimizationProfile profile) {
        ACTIVE = profile;
        applyBudgets(profile); // update per-domain budgets
        listeners.forEach(l -> l.onProfileChange(profile));
        IngeniumLogger.info("Profile -> " + profile +
            " (forced=" + FORCED + ")");
    }

    public void forceProfile(OptimizationProfile profile) {
        FORCED = true;
        setProfile(profile);
    }

    public void releaseProfile() {
        FORCED = false;
        IngeniumLogger.info("Profile released from force.");
    }
 
    // ── Profile resolution ─────────────────────────────────────────
    private OptimizationProfile resolveProfile(double mspt, ActivityState s) {
        IngeniumConfig cfg = IngeniumConfig.get();
        if (mspt > cfg.msptEmergencyThreshold) return OptimizationProfile.EMERGENCY;
        if (mspt > cfg.msptBalancedThreshold)  return OptimizationProfile.REACTIVE;
        return switch (s) {
            case IDLE     -> OptimizationProfile.AGGRESSIVE;
            case COMBAT   -> OptimizationProfile.REACTIVE;
            case BUILDING -> OptimizationProfile.BALANCED;
            default       -> OptimizationProfile.BALANCED;
        };
    }
 
    // ── Per-domain budget application ──────────────────────────────
    // Budgets shrink under load to keep each subsystem from stealing tick time.
    private void applyBudgets(OptimizationProfile p) {
        switch (p) {
            case AGGRESSIVE -> { aiTickBudgetMs=8;  renderTickBudgetMs=5; ioTickBudgetMs=4; }
            case BALANCED   -> { aiTickBudgetMs=4;  renderTickBudgetMs=3; ioTickBudgetMs=2; }
            case REACTIVE   -> { aiTickBudgetMs=2;  renderTickBudgetMs=2; ioTickBudgetMs=1; }
            case EMERGENCY  -> { aiTickBudgetMs=1;  renderTickBudgetMs=1; ioTickBudgetMs=0; }
        }
    }
 
    // ── Public API ─────────────────────────────────────────────────
    public static OptimizationProfile current()  { return ACTIVE; }
    public int getAiBudgetMs()                   { return aiTickBudgetMs; }
    public int getRenderBudgetMs()               { return renderTickBudgetMs; }
    public int getIoBudgetMs()                   { return ioTickBudgetMs; }
    public void register(ProfileListener l)      { listeners.add(l); }
    public ActivityClassifier getClassifier()    { return classifier; }
 
    public IngeniumGovernor() {
        this.metrics    = new MetricsCollector();
        this.classifier = new ActivityClassifier();
    }
}
