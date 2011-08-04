package org.apache.jackrabbit.core.jmx.core;

public interface CoreStat {

    /** -- SESSION INFO -- **/

    void sessionCreated();

    void sessionLoggedOut();

    long getNumberOfSessions();

    void resetNumberOfSessions();

    /**
     * @param timeNs
     *            as given by timeNs = System.nanoTime() - timeNs;
     */
    void onSessionOperation(boolean isWrite, long timeNs);

    double getReadOpsPerSecond();

    double getWriteOpsPerSecond();

    void resetNumberOfOperations();

    /**
     * If this service is currently registering stats
     * 
     * @return <code>true</code> if the service is enabled
     */
    boolean isEnabled();

    /**
     * Enables/Disables the service
     * 
     * @param enabled
     */
    void setEnabled(boolean enabled);

    /**
     * clears all data
     */
    void reset();

}
