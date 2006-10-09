/*
 * $Id$
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.jackrabbit.jcr2spi.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>Status</code>...
 */
public class Status {

    private static Logger log = LoggerFactory.getLogger(Status.class);

    /**
     * 'existing', i.e. persistent state
     */
    public static final int EXISTING = 1;
    /**
     * 'existing', i.e. persistent state that has been transiently modified (copy-on-write)
     */
    public static final int EXISTING_MODIFIED = 2;
    /**
     * 'existing', i.e. persistent state that has been transiently removed (copy-on-write)
     */
    public static final int EXISTING_REMOVED = 3;
    /**
     * 'new' state
     */
    public static final int NEW = 4;
    /**
     * 'existing', i.e. persistent state that has been persistently modified by somebody else
     */
    public static final int STALE_MODIFIED = 5;
    /**
     * 'existing', i.e. persistent state that has been destroyed by somebody else
     */
    public static final int STALE_DESTROYED = 6;

    /**
     * a state is permanently modified either by saving transient changes or
     * by wsp operations or be external modification
     * TODO: improve. status only temporarily used to indicate to a SessionISM-state to copy changes
     */
    public static final int MODIFIED = 7;

    /**
     * a new state was deleted and is now 'removed'
     * or an existing item has been removed by a workspace operation or
     * by an external modification.
     */
    public static final int REMOVED = 8;

    
    public static boolean isTerminalStatus(int status) {
        return status == REMOVED || status == STALE_DESTROYED;
    }

}
