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
package org.apache.jackrabbit.core.cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.spi.PrivilegeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;

/**
 * Simple event listener that can be registered for all cluster event listener
 * types and records external events in an array list.
 */
public class SimpleEventListener implements LockEventListener,
        NodeTypeEventListener, NamespaceEventListener, PrivilegeEventListener, UpdateEventListener {

    /**
     * List of cluster events received.
     */
    public List clusterEvents = new ArrayList();

    //-------------------------------------------------------- LockEventListener

    /**
     * {@inheritDoc}
     */
    public void externalLock(NodeId nodeId, boolean isDeep, String lockOwner)
            throws RepositoryException {

        clusterEvents.add(new LockEvent(nodeId, isDeep, lockOwner));
    }

    /**
     * Lock event auxiliary class.
     */
    public static class LockEvent {

        /**
         * Node id.
         */
        private final NodeId nodeId;

        /**
         * Deep flag.
         */
        private final boolean isDeep;

        /**
         * User id.
         */
        private final String userId;

        /**
         * Create a new instance of this class.
         *
         * @param nodeId node id
         * @param isDeep deep flag
         * @param userId user id
         */
        public LockEvent(NodeId nodeId, boolean isDeep, String userId) {
            this.nodeId = nodeId;
            this.isDeep = isDeep;
            this.userId = userId;
        }

        /**
         * Return the node id.
         *
         * @return the node id
         */
        public NodeId getNodeId() {
            return nodeId;
        }

        /**
         * Return a flag indicating whether the lock is deep.
         *
         * @return <code>true</code> if the lock is deep;
         *         <code>false</code> otherwise
         */
        public boolean isDeep() {
            return isDeep;
        }

        /**
         * Return the user owning the lock.
         *
         * @return user id
         */
        public String getUserId() {
            return userId;
        }

        /**
         * {@inheritDoc}
         */
        public int hashCode() {
            return nodeId.hashCode() ^ userId.hashCode();
        }

        /**
         * {@inheritDoc}
         */
        public boolean equals(Object obj) {
            if (obj instanceof LockEvent) {
                LockEvent other = (LockEvent) obj;
                return nodeId.equals(other.nodeId) &&
                    isDeep == other.isDeep &&
                    userId.equals(other.userId);
            }
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void externalUnlock(NodeId nodeId) throws RepositoryException {
        clusterEvents.add(new UnlockEvent(nodeId));
    }

    /**
     * Unlock event auxiliary class.
     */
    public static class UnlockEvent {

        /**
         * Node id.
         */
        private final NodeId nodeId;

        /**
         * Create a new instance of this class.
         *
         * @param nodeId node id
         */
        public UnlockEvent(NodeId nodeId) {
            this.nodeId = nodeId;
        }

        /**
         * Return the node id.
         *
         * @return node id
         */
        public NodeId getNodeId() {
            return nodeId;
        }

        /**
         * {@inheritDoc}
         */
        public int hashCode() {
            return nodeId.hashCode();
        }

        /**
         * {@inheritDoc}
         */
        public boolean equals(Object obj) {
            if (obj instanceof UnlockEvent) {
                UnlockEvent other = (UnlockEvent) obj;
                return nodeId.equals(other.nodeId);
            }
            return false;
        }
    }

    //---------------------------------------------------- NodeTypeEventListener

    /**
     * {@inheritDoc}
     */
    public void externalRegistered(Collection ntDefs)
            throws RepositoryException, InvalidNodeTypeDefException {

        clusterEvents.add(new NodeTypeEvent(NodeTypeEvent.REGISTER, ntDefs));
    }

    /**
     * {@inheritDoc}
     */
    public void externalReregistered(QNodeTypeDefinition ntDef)
            throws NoSuchNodeTypeException, InvalidNodeTypeDefException,
            RepositoryException {

        ArrayList ntDefs = new ArrayList();
        ntDefs.add(ntDef);

        clusterEvents.add(new NodeTypeEvent(NodeTypeEvent.REREGISTER, ntDefs));
    }

    /**
     * {@inheritDoc}
     */
    public void externalUnregistered(Collection ntNames)
            throws RepositoryException, NoSuchNodeTypeException {

        clusterEvents.add(new NodeTypeEvent(NodeTypeEvent.UNREGISTER, ntNames));
    }

    /**
     * Node type event auxiliary class.
     */
    public static class NodeTypeEvent {

        /**
         * Operation type: registration.
         */
        public static final int REGISTER = NodeTypeRecord.REGISTER;

        /**
         * Operation type: re-registration.
         */
        public static final int REREGISTER = NodeTypeRecord.REREGISTER;

        /**
         * Operation type: unregistration.
         */
        public static final int UNREGISTER = NodeTypeRecord.UNREGISTER;

        /**
         * Operation.
         */
        private int operation;

        /**
         * Collection of node type definitions or node type names.
         */
        private Collection collection;

        /**
         * Create a new instance of this class.
         *
         * @param operation operation
         * @param collection collection of node type definitions or node
         *                   type names
         */
        public NodeTypeEvent(int operation, Collection collection) {
            this.operation = operation;
            this.collection = collection;
        }

        /**
         * Return the operation.
         *
         * @return operation
         */
        public int getOperation() {
            return operation;
        }

        /**
         * Return the collection.
         *
         * @return collection
         */
        public Collection getCollection() {
            return collection;
        }

        /**
         * {@inheritDoc}
         */
        public int hashCode() {
            return operation ^ collection.hashCode();
        }

        /**
         * {@inheritDoc}
         */
        public boolean equals(Object obj) {
            if (obj instanceof NodeTypeEvent) {
                NodeTypeEvent other = (NodeTypeEvent) obj;
                return operation == other.operation &&
                    SimpleEventListener.equals(collection, other.collection);
            }
            return false;
        }
    }

    //--------------------------------------------------- NamespaceEventListener

    /**
     * {@inheritDoc}
     */
    public void externalRemap(String oldPrefix, String newPrefix, String uri)
            throws RepositoryException {

        clusterEvents.add(new NamespaceEvent(oldPrefix, newPrefix, uri));
    }

    /**
     * Namespace event auxiliary class.
     */
    public static class NamespaceEvent {

        /**
         * Old prefix.
         */
        private final String oldPrefix;

        /**
         * New prefix.
         */
        private final String newPrefix;

        /**
         * URI.
         */
        private final String uri;

        /**
         * Create a new instance of this class.
         *
         * @param oldPrefix old prefix
         * @param newPrefix new prefix
         * @param uri URI
         */
        public NamespaceEvent(String oldPrefix, String newPrefix, String uri) {
            this.oldPrefix = oldPrefix;
            this.newPrefix = newPrefix;
            this.uri = uri;
        }

        /**
         * Return the old prefix.
         *
         * @return old prefix
         */
        public String getOldPrefix() {
            return oldPrefix;
        }

        /**
         * Return the new prefix.
         *
         * @return new prefix
         */
        public String getNewPrefix() {
            return newPrefix;
        }

        /**
         * Return the URI.
         *
         * @return URI
         */
        public String getUri() {
            return uri;
        }

        /**
         * {@inheritDoc}
         */
        public int hashCode() {
             int hashCode = 0;
             if (oldPrefix != null) {
                 hashCode ^= oldPrefix.hashCode();
             }
             if (newPrefix != null) {
                 hashCode ^= newPrefix.hashCode();
             }
             if (uri != null) {
                 hashCode ^= uri.hashCode();
             }
             return hashCode;
        }

        /**
         * {@inheritDoc}
         */
        public boolean equals(Object obj) {
            if (obj instanceof NamespaceEvent) {
                NamespaceEvent other = (NamespaceEvent) obj;
                return SimpleEventListener.equals(oldPrefix, other.oldPrefix) &&
                    SimpleEventListener.equals(newPrefix, other.newPrefix) &&
                    SimpleEventListener.equals(uri, other.uri);
            }
            return false;
        }
    }

    //---------------------------------------------< PrivilegeEventListener >---
    /**
     * {@inheritDoc}
     */
    public void externalRegisteredPrivileges(Collection<PrivilegeDefinition> definitions) throws RepositoryException {
        clusterEvents.add(new PrivilegeEvent(definitions));
    }

    /**
     * privilege event auxiliary class.
     */
    public static class PrivilegeEvent {

        /**
         * Collection of node type definitions or node type names.
         */
        private Collection<PrivilegeDefinition> definitions;

        /**
         * Create a new instance of this class.
         *
         * @param definitions
         */
        public PrivilegeEvent(Collection<PrivilegeDefinition> definitions) {
            this.definitions = definitions;
        }

        /**
         * Return the definitions.
         *
         * @return definitions
         */
        public Collection<PrivilegeDefinition> getDefinitions() {
            return definitions;
        }

        /**
         * {@inheritDoc}
         */
        public int hashCode() {
            return definitions.hashCode();
        }

        /**
         * {@inheritDoc}
         */
        public boolean equals(Object obj) {
            if (obj instanceof PrivilegeEvent) {
                PrivilegeEvent other = (PrivilegeEvent) obj;
                return SimpleEventListener.equals(definitions, other.definitions);
            }
            return false;
        }
    }

    //------------------------------------------------------ UpdateEventListener

    /**
     * {@inheritDoc}
     */
    public void externalUpdate(ChangeLog changes, List events,
                               long timestamp, String userData)
            throws RepositoryException {

        clusterEvents.add(new UpdateEvent(changes, events, timestamp, userData));

    }

    /**
     * Update event auxiliary class.
     */
    public static class UpdateEvent implements Update {

        /**
         * Change log.
         */
        private final ChangeLog changes;

        /**
         * List of <code>EventState</code>s.
         */
        private final List events;

        /**
         * Attributes to be stored.
         */
        private final transient Map attributes = new HashMap();

        /**
         * Timestamp when the changes in this update event occured.
         */
        private final long timestamp;

        /**
         * The user data associated with this update.
         */
        private final String userData;

        /**
         * Create a new instance of this class.
         *
         * @param changes change log
         * @param events list of <code>EventState</code>s
         * @param timestamp time when the changes in this event occured.
         * @param userData the user data associated with this update.
         */
        public UpdateEvent(ChangeLog changes, List events,
                           long timestamp, String userData) {
            this.changes = changes;
            this.events = events;
            this.timestamp = timestamp;
            this.userData = userData;
        }

        /**
         * Return the change log.
         *
         * @return the change log
         */
        public ChangeLog getChanges() {
            return changes;
        }

        /**
         * Return the list of <code>EventState</code>s
         *
         * @return list of <code>EventState</code>s
         */
        public List getEvents() {
            return events;
        }

        /**
         * {@inheritDoc}
         */
        public long getTimestamp() {
            return timestamp;
        }

        public String getUserData() {
            return userData;
        }

        /**
         * {@inheritDoc}
         */
        public void setAttribute(String name, Object value) {
            attributes.put(name, value);
        }

        /**
         * {@inheritDoc}
         */
        public Object getAttribute(String name) {
            return attributes.get(name);
        }

        /**
         * {@inheritDoc}
         */
        public int hashCode() {
            int h = changes.hashCode() ^ events.hashCode() ^ (int) (timestamp ^ (timestamp >>> 32));
            if (userData != null) {
                h = h ^ userData.hashCode();
            }
            return h;
        }

        /**
         * {@inheritDoc}
         */
        public boolean equals(Object obj) {
            if (obj instanceof UpdateEvent) {
                UpdateEvent other = (UpdateEvent) obj;
                return SimpleEventListener.equals(changes, other.changes) &&
                    SimpleEventListener.equals(events, other.events) &&
                    timestamp == other.timestamp &&
                    SimpleEventListener.equals(userData, other.userData);
            }
            return false;
        }

    }

    /**
     * Return the collected cluster events.
     *
     * @return cluster events
     */
    public List getClusterEvents() {
        return Collections.unmodifiableList(clusterEvents);
    }

    /**
     * Check whether two objects are equals, allowing <code>null</code> values.
     *
     * @param o1 object 1
     * @param o2 object 2
     * @return <code>true</code> if they are equal; <code>false</code> otherwise
     */
    private static boolean equals(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        } else {
            return o1.equals(o2);
        }
    }

    /**
     * Check whether two collections contain the same elements. Made necessary
     * because the <code>Collections.unmodifiableXXX</code> methods do not
     * return objects that override <code>equals</code> and <code>hashCode</code>.
     *
     * @param c1 collection 1
     * @param c2 collection 2
     * @return <code>true</code> if they are equal; <code>false</code> otherwise
     */
    private static boolean equals(Collection c1, Collection c2) {
        if (c1.size() != c2.size()) {
            return false;
        }
        Iterator iter1 = c1.iterator();
        Iterator iter2 = c2.iterator();

        while (iter1.hasNext()) {
            Object o1 = iter1.next();
            Object o2 = iter2.next();
            if (!o1.equals(o2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check whether two changes logs contain the same elements. Not feasible
     * by comparing the maps because <code>ItemState</code>s do not override
     * {@link Object#equals(Object)}.
     *
     * @param changes1 change log
     * @param changes2 change log
     * @return <code>true</code> if the change logs are equals;
     *         <code>false</code> otherwise.
     */
    private static boolean equals(ChangeLog changes1, ChangeLog changes2) {
        return equals(changes1.addedStates().iterator(), changes2.addedStates().iterator()) &&
            equals(changes1.deletedStates().iterator(), changes2.deletedStates().iterator()) &&
            equals(changes1.modifiedStates().iterator(), changes2.modifiedStates().iterator());
    }

    /**
     * Check whether two iterators return the same item states, where "same"
     * means having the same <code>ItemId</code>
     * @param iter1 first iterator
     * @param iter2 second iterator
     * @return <code>true</code> if the two iterators are equal;
     *         <code>false</code> otherwise
     */
    private static boolean equals(Iterator iter1, Iterator iter2) {
        for (;;) {
            if (!iter1.hasNext() && !iter2.hasNext()) {
                return true;
            }
            if (iter1.hasNext() && !iter2.hasNext()) {
                return false;
            }
            if (!iter1.hasNext() && iter2.hasNext()) {
                return false;
            }
            ItemState state1 = (ItemState) iter1.next();
            ItemState state2 = (ItemState) iter2.next();
            return state1.getId().equals(state2.getId());
        }
    }
}
