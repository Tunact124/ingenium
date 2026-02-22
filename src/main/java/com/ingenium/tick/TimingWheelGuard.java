package com.ingenium.tick;

import com.ingenium.compat.BuddyLogic;
import com.ingenium.compat.BuddyLogic.KnownMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guards all timing wheel access. If Lithium owns the tick scheduler, we disable it.
 */
public final class TimingWheelGuard {

    private static final Logger LOG = LoggerFactory.getLogger("Ingenium/TimingWheel");

    private static final boolean ACTIVE;

    static {
        boolean lithiumOwnsScheduler = BuddyLogic.isPresent(KnownMod.LITHIUM)
            && BuddyLogic.getResult(KnownMod.LITHIUM).hasCapability("tick_scheduler");

        if (lithiumOwnsScheduler) {
            ACTIVE = false;
            BuddyLogic.logYield("TimingWheel", "lithium", "Lithium owns tick scheduling — timing wheel disabled");
        } else {
            ACTIVE = true;
            LOG.info("[Ingenium] Timing wheel ACTIVE (Lithium not managing ticks)");
        }
    }

    public static boolean isActive() {
        return ACTIVE;
    }

    private TimingWheelGuard() {}
}
