/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.jcr2spi.state;

/**
 * <code>Status</code>...
 */
public final class Status {

    /**
     * Avoid instantiation
     */
    private Status() {}

    public static final int _UNDEFINED_ = -1;

    /**
     * A state once read from persistent storage has been set to invalid. This
     * means that the state needs to be re-fetched from persistent storage when
     * accessed the next time.
     */
    public static final int INVALIDATED = 0;

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
     * Temporary status used to mark a state, this is permanently modified
     * either by saving transient changes, by workspace operations or by
     * external modification.
     */
    public static final int MODIFIED = 7;

    /**
     * a new state was removed and is now 'removed'
     * or an existing item has been removed by a workspace operation or
     * by an external modification.
     */
    public static final int REMOVED = 8;

    private static final String[] STATUS_NAMES = new String[] {
        "INVALIDATED",
        "EXISTING",
        "EXISTING_MODIFIED",
        "EXISTING_REMOVED",
        "NEW",
        "STALE_MODIFIED",
        "STALE_DESTROYED",
        "MODIFIED",
        "REMOVED"
    };

    /**
     * Returns <code>true</code> if the given status is a terminal status, i.e.
     * the given status one of:
     * <ul>
     * <li>{@link #REMOVED}</li>
     * <li>{@link #STALE_DESTROYED}</li>
     * </ul>
     *
     * @param status
     * @return true if the given status is terminal.
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
     * @return true if the given status indicates a valid ItemState.
     */
    public static boolean isValid(int status) {
        return status == EXISTING || status == EXISTING_MODIFIED || status == NEW;
    }

    /**
     * Returns <code>true</code> if <code>status</code> is one of:
     * <ul>
     * <li>{@link #STALE_DESTROYED}</li>
     * <li>{@link #STALE_MODIFIED}</li>
     * </ul>
     *
     * @param status the status to check.
     * @return <code>true</code> if <code>status</code> indicates that an item
     *         state is stale.
     */
    public static boolean isStale(int status) {
        return status == STALE_DESTROYED || status == STALE_MODIFIED;
    }

    /**
     * Returns <code>true</code> if <code>status</code> is one of:
     * <ul>
     * <li>{@link #EXISTING_MODIFIED}</li>
     * <li>{@link #EXISTING_REMOVED}</li>
     * <li>{@link #NEW}</li>
     * </ul>
     *
     * @param status the status to check.
     * @return <code>true</code> if <code>status</code> indicates that an item
     *         state is transiently modified.
     */
    public static boolean isTransient(int status) {
        return status == EXISTING_MODIFIED || status == EXISTING_REMOVED || status == NEW;
    }

    /**
     * Returns true, if the status of an item state can be changed from
     * <code>oldStatus</code> to <code>newStatus</code>, and false if the
     * change is illegal or if any of the given status flags is illegal.
     *
     * @param oldStatus
     * @param newStatus
     * @return true if a status change from <code>oldStatus</code> to
     * <code>newStatus</code> is allowed or if the two status are the same.
     */
    public static boolean isValidStatusChange(int oldStatus, int newStatus) {
        if (oldStatus == newStatus) {
            return true;
        }
        boolean isValid = false;
        // valid status changes for session-states
        switch (newStatus) {
            case INVALIDATED:
                isValid = (oldStatus == EXISTING); // invalidate
                break;
            case EXISTING:
                switch (oldStatus) {
                    case INVALIDATED: /* refresh */
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
                isValid = (oldStatus == EXISTING_MODIFIED || oldStatus == EXISTING_REMOVED || oldStatus == STALE_MODIFIED);
                break;
            case REMOVED:
                // removal always possible -> getNewStatus(int, int)
                isValid = true;
                break;
            case MODIFIED:
                // except for NEW states an external modification is always valid
                if (oldStatus != NEW) {
                    isValid = true;
                }
                break;
                /* default:
                    NEW cannot change state to NEW -> false */
        }
        return isValid;
    }

    /**
     * Returns the given <code>newStatusHint</code> unless the new status
     * collides with a pending modification or removal which results in a
     * stale item state.
     *
     * @param oldStatus
     * @param newStatusHint
     * @return new status that takes transient modification/removal into account.
     */
    public static int getNewStatus(int oldStatus, int newStatusHint) {
        int newStatus;
        switch (newStatusHint) {
            case Status.MODIFIED:
                // underlying state has been modified by external changes
                if (oldStatus == Status.EXISTING || oldStatus == Status.INVALIDATED) {
                    // temporarily set the state to MODIFIED in order to inform listeners.
                    newStatus = Status.MODIFIED;
                } else if (oldStatus == Status.EXISTING_MODIFIED) {
                    newStatus = Status.STALE_MODIFIED;
                } else {
                    // old status is EXISTING_REMOVED (or any other) => ignore.
                    // a NEW state may never be marked modified.
                    newStatus = oldStatus;
                }
                break;
            case Status.REMOVED:
                if (oldStatus == Status.EXISTING_MODIFIED || oldStatus == Status.STALE_MODIFIED) {
                    newStatus = Status.STALE_DESTROYED;
                } else {
                    // applies both to NEW or to any other status
                    newStatus = newStatusHint;
                }
                break;
            default:
                newStatus = newStatusHint;
                break;

        }
        return newStatus;
    }

    /**
     * @param status A valid status constant.
     * @return Human readable status name for the given int.
     */
    public static String getName(int status) {
        if (status == _UNDEFINED_) {
            return "_UNDEFINED_";
        }
        if (status < 0 || status >= STATUS_NAMES.length) {
            throw new IllegalArgumentException("Invalid status " + status);
        }
        return STATUS_NAMES[status];
    }
}
