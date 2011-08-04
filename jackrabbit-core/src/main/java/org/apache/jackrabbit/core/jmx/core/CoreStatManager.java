package org.apache.jackrabbit.core.jmx.core;

public class CoreStatManager implements CoreStatManagerMBean {

    private final CoreStat coreStat;

    public CoreStatManager(final CoreStat coreStat) {
        this.coreStat = coreStat;
    }

    public long getNumberOfSessions() {
        return coreStat.getNumberOfSessions();
    }

    public void resetNumberOfSessions() {
        this.coreStat.resetNumberOfSessions();
    }

    public boolean isEnabled() {
        return this.coreStat.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        this.coreStat.setEnabled(enabled);
    }

    public void reset() {
        this.coreStat.reset();
    }

    public double getReadOpsPerSecond() {
        return this.coreStat.getReadOpsPerSecond();
    }

    public double getWriteOpsPerSecond() {
        return this.coreStat.getWriteOpsPerSecond();
    }

    public void resetNumberOfOperations() {
        this.coreStat.resetNumberOfOperations();

    }
}
