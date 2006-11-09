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

    /**
     * Returns <code>true</code> if the given status is a terminal status, i.e.
     * the given status one of:
     * <ul>
     * <li>{@link #REMOVED}</li>
     * <li>{@link #STALE_DESTROYED}</li>
     * </ul>
     *
     * @param status
     * @return
     */
    public static boolean isTerminal(int status) {
        return status == REMOVED || status == STALE_DESTROYED;
    }

    /**
     * Returns <code>true</code> if this item state is valid, that is its status
     * is one of:
     * <ul>
     * <li>{@link #EXISTING}</li>
     * <li>{@link #EXISTING_MODIFIED}</li>
     * <li>{@link #NEW}</li>
     * </ul>
     *
     * @param status
     * @return
     */
    public static boolean isValid(int status) {
        return status == EXISTING || status == EXISTING_MODIFIED || status == NEW;
    }

    public static boolean isStale(int status) {
        return status == STALE_DESTROYED || status == STALE_MODIFIED;
    }

    public static boolean isValidStatusChange(int oldStatus, int newStatus,
                                              boolean isWorkspaceState) {
        if (oldStatus == newStatus) {
            return true;
        }
        boolean isValid = false;
        if (isWorkspaceState) {
            switch (newStatus) {
                case EXISTING:
                    isValid = (oldStatus == MODIFIED);
                    break;
                case MODIFIED:
                    isValid = (oldStatus == EXISTING);
                    break;
                case REMOVED:
                    isValid = (oldStatus == EXISTING);
                    break;
                // default: no other status possible : -> false
            }
        } else {
            switch (newStatus) {
               case EXISTING:
                   switch (oldStatus) {
                       case NEW: /* save */
                       case EXISTING_MODIFIED: /* save, revert */
                       case EXISTING_REMOVED:  /* revert */
                       case STALE_MODIFIED:    /* revert */
                       case MODIFIED:
                           isValid = true;
                           break;
                       /* REMOVED, STALE_DESTROYED -> false */
                   }
                   break;
               case EXISTING_MODIFIED:
                   isValid = (oldStatus == EXISTING);
                   break;
               case EXISTING_REMOVED:
                   isValid = (oldStatus == EXISTING || oldStatus == EXISTING_MODIFIED);
                   break;
               case STALE_MODIFIED:
               case STALE_DESTROYED:
                   isValid = (oldStatus == EXISTING_MODIFIED);
                   break;
               case REMOVED:
                   isValid = (oldStatus == NEW || oldStatus == EXISTING || oldStatus == EXISTING_REMOVED);
                   break;
               case MODIFIED:
                   isValid = (oldStatus == EXISTING);
                   break;
                   /* default:
                   NEW cannot change state to NEW -> false */
            }
        }
        return isValid;
    }
}
