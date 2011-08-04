package org.apache.jackrabbit.core.jmx.core;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.jackrabbit.core.jmx.util.CachingOpsPerSecondDto;

public class CoreStatImpl implements CoreStat {

    /** -- SESSION INFO -- **/

    private final AtomicLong sessions = new AtomicLong(0);

    private boolean enabled = false;

    private final CachingOpsPerSecondDto reads = new CachingOpsPerSecondDto();

    private final CachingOpsPerSecondDto writes = new CachingOpsPerSecondDto();

    public void sessionCreated() {
        if (!enabled) {
            return;
        }
        sessions.incrementAndGet();
    }

    public void sessionLoggedOut() {
        if (!enabled || sessions.get() == 0) {
            return;
        }
        sessions.decrementAndGet();
    }

    public long getNumberOfSessions() {
        return sessions.get();
    }

    public void resetNumberOfSessions() {
        sessions.set(0);
    }

    public void onSessionOperation(boolean isWrite, long timeNs) {
        if (!enabled) {
            return;
        }
        if (isWrite) {
            writes.onOp(timeNs);
        } else {
            reads.onOp(timeNs);
        }
    }

    public double getReadOpsPerSecond() {
        return reads.getOpsPerSecond();
    }

    public double getWriteOpsPerSecond() {
        return writes.getOpsPerSecond();
    }

    public void resetNumberOfOperations() {
        reads.reset();
        writes.reset();
    }

    /** -- GENERAL INFO -- **/

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!this.enabled) {
            reset();
        }
    }

    public void reset() {
        resetNumberOfSessions();
        resetNumberOfOperations();
    }
}
