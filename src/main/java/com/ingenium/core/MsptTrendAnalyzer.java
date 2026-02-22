package com.ingenium.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Predictive MSPT Trend Analyzer
 * 
 * Maintains a rolling window of MSPT samples and computes:
 * 1. Linear trend (slope) — is MSPT rising or falling?
 * 2. Variance — is MSPT stable or jittery?
 * 3. Prediction — where will MSPT be in N ticks?
 * 
 * The Governor uses this to preemptively tighten budgets
 * BEFORE a lag spike actually hits.
 */
public final class MsptTrendAnalyzer {
    
    private static final Logger LOG = LoggerFactory.getLogger("Ingenium/Trend");
    
    // Rolling window size — 40 ticks = 2 seconds of history
    private static final int WINDOW_SIZE = 40;
    
    // Prediction horizon — how many ticks ahead to forecast
    private static final int PREDICTION_HORIZON = 20; // 1 second ahead
    
    // Minimum slope (ms/tick) to consider a rising trend
    private static final double RISING_THRESHOLD = 0.3;
    
    // Minimum slope to trigger preemptive action
    private static final double PREEMPTIVE_THRESHOLD = 0.6;
    
    // Minimum R² to trust the trend (how well does linear fit the data)
    private static final double MIN_CONFIDENCE = 0.5;
    
    private final double[] samples;
    private int head = 0;
    private int count = 0;
    
    // Cached computation results — updated each sample
    private double slope = 0.0;
    private double intercept = 0.0;
    private double rSquared = 0.0;
    private double variance = 0.0;
    private double currentAverage = 0.0;
    
    public MsptTrendAnalyzer() {
        this.samples = new double[WINDOW_SIZE];
    }
    
    /**
     * Record a new MSPT sample. Called once per tick from the Governor.
     */
    public void recordSample(double mspt) {
        samples[head] = mspt;
        head = (head + 1) % WINDOW_SIZE;
        if (count < WINDOW_SIZE) count++;
        
        if (count >= 10) { // Need at least 10 samples for meaningful regression
            computeRegression();
        }
    }
    
    /**
     * Compute linear regression over the rolling window.
     * 
     * Standard least-squares: y = mx + b
     * Where x is the sample index (0..N-1) and y is the MSPT value.
     */
    private void computeRegression() {
        final int n = count;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;
        
        for (int i = 0; i < n; i++) {
            int idx = ((head - n + i) + WINDOW_SIZE) % WINDOW_SIZE;
            double x = i;
            double y = samples[idx];
            
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
            sumY2 += y * y;
        }
        
        double denom = (n * sumX2 - sumX * sumX);
        if (Math.abs(denom) < 1e-10) {
            slope = 0;
            intercept = sumY / n;
            rSquared = 0;
        } else {
            slope = (n * sumXY - sumX * sumY) / denom;
            intercept = (sumY - slope * sumX) / n;
            
            // R² — coefficient of determination
            double meanY = sumY / n;
            double ssTotal = sumY2 - n * meanY * meanY;
            double ssResidual = 0;
            for (int i = 0; i < n; i++) {
                int idx = ((head - n + i) + WINDOW_SIZE) % WINDOW_SIZE;
                double predicted = slope * i + intercept;
                double residual = samples[idx] - predicted;
                ssResidual += residual * residual;
            }
            rSquared = (ssTotal > 0) ? 1.0 - (ssResidual / ssTotal) : 0.0;
        }
        
        currentAverage = sumY / n;
        
        // Variance
        double meanY = sumY / n;
        double varSum = 0;
        for (int i = 0; i < n; i++) {
            int idx = ((head - n + i) + WINDOW_SIZE) % WINDOW_SIZE;
            double diff = samples[idx] - meanY;
            varSum += diff * diff;
        }
        variance = varSum / n;
    }
    
    /**
     * Predict MSPT at a future tick offset.
     * Returns the predicted value, or -1 if not enough data.
     */
    public double predictMsptAt(int ticksAhead) {
        if (count < 10) return -1.0;
        return slope * (count + ticksAhead) + intercept;
    }
    
    /**
     * Get the recommended Governor action based on current trend.
     */
    public TrendAction getRecommendedAction() {
        if (count < 10) return TrendAction.HOLD;
        
        // Don't act on low-confidence trends
        if (rSquared < MIN_CONFIDENCE) return TrendAction.HOLD;
        
        double predicted = predictMsptAt(PREDICTION_HORIZON);
        
        // Is MSPT rising toward danger?
        if (slope > PREEMPTIVE_THRESHOLD && predicted > 45.0) {
            LOG.debug("[Ingenium/Trend] PREEMPTIVE TIGHTEN: slope={:.2f}ms/tick, predicted_20t={:.1f}ms, R²={:.2f}", 
                      slope, predicted, rSquared);
            return TrendAction.PREEMPTIVE_TIGHTEN;
        }
        
        if (slope > RISING_THRESHOLD && predicted > 40.0) {
            return TrendAction.WARN_RISING;
        }
        
        // Is MSPT falling? We might be able to relax
        if (slope < -RISING_THRESHOLD && currentAverage < 35.0 && rSquared > 0.6) {
            return TrendAction.SUGGEST_RELAX;
        }
        
        return TrendAction.HOLD;
    }
    
    public double slope() { return slope; }
    public double rSquared() { return rSquared; }
    public double variance() { return variance; }
    public double currentAverage() { return currentAverage; }
    public int sampleCount() { return count; }
    
    public enum TrendAction {
        HOLD,
        WARN_RISING,
        PREEMPTIVE_TIGHTEN,
        SUGGEST_RELAX
    }
}
