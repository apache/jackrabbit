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
package org.apache.jackrabbit.core.security.authorization.principalbased;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.AccessControlModifications;
import org.apache.jackrabbit.core.security.authorization.AccessControlObserver;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.ObservationManager;
import javax.jcr.security.AccessControlEntry;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <code>EntriesCache</code>...
 */
class EntriesCache extends AccessControlObserver implements AccessControlConstants {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(EntriesCache.class);

    private final SessionImpl systemSession;
    private final ACLEditor systemEditor;

    private final String repPolicyName;

    /**
     * Cache to look up the list of access control entries defined for a given
     * set of principals.
     */
    private final Map<Object, List<AccessControlEntry>> cache;
    private final Object monitor = new Object();

    /**
     *
     * @param systemSession
     * @param systemEditor
     * @param accessControlRootPath
     * @throws javax.jcr.RepositoryException
     */
    EntriesCache(SessionImpl systemSession, ACLEditor systemEditor,
                          String accessControlRootPath) throws RepositoryException {
        this.systemSession = systemSession;
        this.systemEditor = systemEditor;

        repPolicyName = systemSession.getJCRName(N_POLICY);

        cache = new LRUMap<>(1000);

        ObservationManager observationMgr = systemSession.getWorkspace().getObservationManager();
        /*
         Make sure the collector and all subscribed listeners are informed upon
         ACL modifications. Interesting events are:
         - new ACL (NODE_ADDED)
         - new ACE (NODE_ADDED)
         - changing ACE (PROPERTY_CHANGED)
         - removed ACL (NODE_REMOVED)
         - removed ACE (NODE_REMOVED)
        */
        int events = Event.PROPERTY_CHANGED | Event.NODE_ADDED | Event.NODE_REMOVED;
        String[] ntNames = new String[] {
                systemSession.getJCRName(NT_REP_ACCESS_CONTROLLABLE),
                systemSession.getJCRName(NT_REP_ACL),
                systemSession.getJCRName(NT_REP_ACE)
        };
        observationMgr.addEventListener(this, events, accessControlRootPath, true, null, ntNames, false);
    }

    /**
     * Release all resources contained by this instance. It will no longer be
     * used. This implementation only stops listening to ac modification events.
     */
    @Override
    protected void close() {
        super.close();
        try {
            systemSession.getWorkspace().getObservationManager().removeEventListener(this);
        } catch (RepositoryException e) {
            log.error("Unexpected error while closing CachingEntryCollector", e);
        }
    }

    List<AccessControlEntry> getEntries(Collection<Principal> principals) throws RepositoryException {
        String key = getCacheKey(principals);
        List<AccessControlEntry> entries;
        synchronized (monitor) {
            entries = cache.get(key);
            if (entries == null) {
                // acNodes must be ordered in the same order as the principals
                // in order to obtain proper acl-evaluation in case the given
                // principal-set is ordered.
                entries = new ArrayList<AccessControlEntry>();
                // build acl-hierarchy assuming that principal-order determines
                // the acl-inheritance.
                for (Principal p : principals) {
                    ACLTemplate acl = systemEditor.getACL(p);
                    if (acl != null && !acl.isEmpty()) {
                        AccessControlEntry[] aces = acl.getAccessControlEntries();
                        entries.addAll(Arrays.asList(aces));
                    }
                }
                cache.put(key, entries);
            }
        }
        return entries;
    }

    private static String getCacheKey(Collection<Principal> principals) {
        StringBuilder sb = new StringBuilder();
        for (Principal p : principals) {
            sb.append(p.getName()).append('/');
        }
        return sb.toString();
    }

    //------------------------------------------------------< EventListener >---
    /**
     * @see javax.jcr.observation.EventListener#onEvent(javax.jcr.observation.EventIterator)
     */
    public synchronized void onEvent(EventIterator events) {
        /* map of path-string to modification type.
           the path identifies the access-controlled node for a given principal.*/
        Map<String,Integer> modMap = new HashMap<String,Integer>();

        // collect the paths of all access controlled nodes that have been affected
        // by the events and thus need their cache entries updated or cleared.
        while (events.hasNext()) {
            try {
                Event ev = events.nextEvent();
                String identifier = ev.getIdentifier();
                String path = ev.getPath();

                switch (ev.getType()) {
                    case Event.NODE_ADDED:
                        NodeImpl n = (NodeImpl) systemSession.getNodeByIdentifier(identifier);
                        if (n.isNodeType(NT_REP_ACL)) {
                            // a new ACL was added -> use the added node to update
                            // the cache.
                            modMap.put(Text.getRelativeParent(path, 1), POLICY_ADDED);
                        } else if (n.isNodeType(NT_REP_ACE)) {
                            // a new ACE was added -> use the parent node (acl)
                            // to update the cache.
                            modMap.put(Text.getRelativeParent(path, 2), POLICY_MODIFIED);
                        } /* else: some other node added below an access controlled
                             parent node -> not interested. */
                        break;
                    case Event.NODE_REMOVED:
                        String parentPath = Text.getRelativeParent(path, 1);
                        if (systemSession.nodeExists(parentPath)) {
                            NodeImpl parent = (NodeImpl) systemSession.getNode(parentPath);
                            if (repPolicyName.equals(Text.getName(path))){
                                // the complete acl was removed -> clear cache entry
                                modMap.put(parentPath, POLICY_REMOVED);
                            } else if (parent.isNodeType(NT_REP_ACL)) {
                                // an ace was removed -> refresh cache for the
                                // containing access control list upon next access
                                modMap.put(Text.getRelativeParent(path, 2), POLICY_MODIFIED);
                            } /* else:
                             a) some other child node of an access controlled
                                node -> not interested.
                             b) a child node of an ACE. not relevant for this
                                implementation -> ignore
                           */
                        } else {
                            log.debug("Cannot process NODE_REMOVED event. Parent " + parentPath + " doesn't exist (anymore).");
                        }
                        break;
                    case Event.PROPERTY_CHANGED:
                        // test if the changed prop belongs to an ACE
                        NodeImpl parent = (NodeImpl) systemSession.getNodeByIdentifier(identifier);
                        if (parent.isNodeType(NT_REP_ACE)) {
                            modMap.put(Text.getRelativeParent(path, 3), POLICY_MODIFIED);
                        } /* some other property below an access controlled node
                             changed -> not interested. (NOTE: rep:ACL doesn't
                             define any properties. */
                        break;
                    default:
                        // illegal event-type: should never occur. ignore
                }

            } catch (RepositoryException e) {
                // should never get here
                log.warn("Internal error: {}", e.getMessage());
            }

        }

        if (!modMap.isEmpty()) {
            // notify listeners and eventually clean up internal caches.
            synchronized (monitor) {
                cache.clear();
            }
            notifyListeners(new AccessControlModifications<String>(modMap));
        }
    }
}
