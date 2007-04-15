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
package org.apache.jackrabbit.classloader;

import java.util.ArrayList;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>DynamicPatternPath</code> class is a {@link PatternPath}
 * which registers for modifications in the repository which may affect the
 * result of calling the <code>getExpandedPaths</code> method. If also supports
 * for clients registering with instances of this class to be notified if such
 * an event happens.
 * <p>
 * To free the system from too much work, instances of this class are only
 * registered with the session's observation manager if at least one listener is
 * interested in notification to changes in the matched path list.
 *
 * @author Felix Meschberger
 */
/* package */ class DynamicPatternPath extends PatternPath
        implements EventListener {

    /** default logger */
    private static final Logger log =
        LoggerFactory.getLogger(DynamicPatternPath.class);

    /** The list of registered listeners for this list */
    private final ArrayList listeners = new ArrayList();

    /**
     * <code>true</code> if this instance is registered with the session's
     * observation manager.
     */
    private boolean isRegistered;

    /**
     * Creates an instance of the <code>DynamicPatternPath</code> from
     * a collection of path patterns.
     *
     * @param session The session to access the Repository to expand the paths
     *      and to register as an event listener.
     * @param pathPatterns The array of path patterns to add.
     *
     * @throws NullPointerException if the <code>pathPatterns</code> array is
     *      <code>null</code>.
     *
     * @see PatternPath#PathPatternList(Session, String[])
     */
    /* package */ DynamicPatternPath(Session session, String[] pathPatterns) {
        super(session, pathPatterns);
    }

    //---------- notification listener registration and interface -------------

    /**
     * Adds the given listener to the list of registered listeners. If the
     * listener is already registered, it is not added a second time.
     * <p>
     * This is synchronized to prevent multiple parallel modification of the
     * listeners list by mutliple threads.
     *
     * @param listener The listener to register. This must not be
     *      <code>null</code>.
     *
     * @throws NullPointerException if the <code>listener</code> parameter is
     *      <code>null</code>.
     */
    /* package */ synchronized void addListener(Listener listener) {

        // check listener
        if (listener == null) {
            throw new NullPointerException("listener");
        }

        // make sure we get updated on changes to be able to notify listeners
        // we are pretty sure our listeners list will not be empty :-)
        if (!isRegistered) {
            log.debug("addListener: Register with observation service");
            registerEventListener();
        }

        // guarded add
        if (!listeners.contains(listener)) {
            log.debug("addListener: Listener {}", listener);
            listeners.add(listener);
        } else {
            log.info("addListener: Listener {} already added", listener);
        }
    }

    /**
     * Removes the given listener from the list of registered listeners. If the
     * listener is not registered, the list of registered listeners is not
     * modified.
     * <p>
     * This is synchronized to prevent multiple parallel modification of the
     * listeners list by mutliple threads.
     *
     * @param listener The listener to deregister. This must not be
     *      <code>null</code>.
     *
     * @throws NullPointerException if the <code>listener</code> parameter is
     *      <code>null</code>.
     */
    /* package */ synchronized void removeListener(Listener listener) {

        // check listener
        if (listener == null) {
            throw new NullPointerException("listener");
        }

        // guarded removal
        if (listeners.remove(listener)) {
            log.debug("removeListener: Listener {}", listener);
        } else {
            log.info("removeListener: Listener {} not registered", listener);
        }

        // deregister if no listener is registered anymore
        // we are pretty sure to be registered
        if (listeners.size() == 0) {
            log.debug("removeListener: Deregister from observation service");
            unregisterEventListener();
        }
    }

    //---------- EventListener interface --------------------------------------

    /**
     * Handles the case where any change occurrs to the set of matched paths.
     * This is, if either a newly created item matches or a previously matching
     * item has been removed.
     * <p>
     * This method ignores <code>PROPERTY_CHANGED</code> events, as these
     * events do not have an influence on the set of matched paths.
     * <p>
     * The events in the iterator are analyzed until any non-property-change
     * event has an influence on the set of matched paths. As soon as such a
     * path is encountered, the listeners are notified and this method
     * terminates without further inspection of the events.
     *
     * @param events The iterator on the events being sent
     */
    public void onEvent(EventIterator events) {
        // check whether any of the events match the pattern list. If so
        // notify listeners on first match found and ignore rest for testing
        while (events.hasNext()) {
            Event event = events.nextEvent();

            // ignore property modifications
            if (event.getType() == Event.PROPERTY_CHANGED) {
                continue;
            }

            try {
                String path= event.getPath();
                if (matchPath(path)) {
                    log.debug("onEvent: Listener Notification due to {}", path);
                    notifyListeners();
                    return;
                }
            } catch (RepositoryException re) {
                log.info("onEvent: Cannot check events", re);
            }
        }
    }

    /**
     * Registers this list object with the session's observation manager to get
     * information on item updates.
     */
    private void registerEventListener() {

        // make sure we are not registered yet
        if (isRegistered) {
            log.debug("registerModificationListener: Already registered");
            return;
        }

        try {
            ObservationManager om =
                getSession().getWorkspace().getObservationManager();
            om.addEventListener(this, 0xffff, "/", true, null, null, false);
            isRegistered = true;
        } catch (RepositoryException re) {
            log.warn("registerModificationListener", re);
        }
    }

    /**
     * Unregisters this list object from the observation manager to not get
     * information on item updates anymore. This method is called when no more
     * listeners are interested on updates. This helps garbage collect this
     * object in the case no reference is held to the list anymore. If no one
     * is interested in changes anymore, we are not interested either, so we
     * may as well unregister.
     */
    private void unregisterEventListener() {

        // make sure we are registered
        if (!isRegistered) {
            log.debug("deregisterModificationListener: Not registered");
            return;
        }

        try {
            ObservationManager om =
                getSession().getWorkspace().getObservationManager();
            om.removeEventListener(this);
            isRegistered = false;
        } catch (RepositoryException re) {
            log.warn("deregisterModificationListener", re);
        }
    }

    /**
     * Notifies all registered listeners on the change in the set of matched
     * paths by calling their <code>pathListChanged</code> method.
     */
    private void notifyListeners() {
        for (int i=0; i < listeners.size(); i++) {
            Listener listener = (Listener) listeners.get(i);
            log.debug("notifyListeners: Notifying listener {}", listener);
            try {
                listener.pathChanged();
            } catch (Exception e) {
                log.warn("notifyListeners: Listener {} failed", listener, e);
            }
        }
    }

    /**
     * The <code>PatternPath.Listener</code> interface may be implemented
     * by interested classes to be notified as soon as the
     * {@link PatternPath#getExpandedPaths} method will return a
     * different result on the next invocation. This happens as soon as the set
     * of paths to which the list of patterns matches would change.
     */
    /* package */ interface Listener {

        /**
         * This method is called if the listener is to be notified of an event
         * resulting in the set of paths matched by the list of patterns to be
         * different.
         */
        public void pathChanged();
    }
}
