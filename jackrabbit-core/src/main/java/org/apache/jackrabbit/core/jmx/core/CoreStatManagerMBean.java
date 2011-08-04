package org.apache.jackrabbit.core.jmx.core;

import org.apache.jackrabbit.core.jmx.JackrabbitBaseMBean;

public interface CoreStatManagerMBean extends JackrabbitBaseMBean {

    String NAME = BASE_NAME + ":type=CoreStats";

    long getNumberOfSessions();

    void resetNumberOfSessions();

    double getReadOpsPerSecond();

    double getWriteOpsPerSecond();

    void resetNumberOfOperations();

}
