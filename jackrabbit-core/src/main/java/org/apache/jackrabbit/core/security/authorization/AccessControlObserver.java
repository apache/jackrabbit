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
package org.apache.jackrabbit.core.security.authorization;

import org.apache.jackrabbit.core.observation.SynchronousEventListener;

import java.util.HashSet;
import java.util.Set;

/**
 * <code>AccessControlObserver</code>...
 */
public abstract class AccessControlObserver implements SynchronousEventListener {

    public static final int POLICY_ADDED = 1;
    public static final int POLICY_REMOVED = 2;
    public static final int POLICY_MODIFIED = 4;
    public static final int MOVE = 8;

    private final Set<AccessControlListener> listeners = new HashSet<AccessControlListener>();
    private final Object listenerMonitor = new Object();
    
    protected void close() {
        synchronized (listenerMonitor) {
            listeners.clear();
        }
    }

    /**
     * Add a listener that needs to be informed about changes made to access
     * control.
     *
     * @param listener <code>EntryListener</code> to be added.
     */
    public void addListener(AccessControlListener listener) {
        synchronized (listenerMonitor) {
            listeners.add(listener);
        }
    }

    /**
     * Remove a listener added before.
     *
     * @param listener <code>EntryListener</code> to be removed.
     */
    public void removeListener(AccessControlListener listener) {
        synchronized (listenerMonitor) {
            listeners.remove(listener);
        }
    }

    /**
     * Notifies the listeners about AC modifications.
     *
     * @param modifications
     */
    protected void notifyListeners(AccessControlModifications modifications) {
        AccessControlListener[] lstnrs;
        synchronized (listenerMonitor) {
            lstnrs = listeners.toArray(new AccessControlListener[listeners.size()]);
        }
        for (AccessControlListener lstnr : lstnrs) {
            lstnr.acModified(modifications);
        }
    }
}