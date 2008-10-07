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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.Session;
import javax.jcr.observation.Event;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.journal.JournalException;
import org.apache.jackrabbit.core.journal.Record;
import org.apache.jackrabbit.core.observation.EventState;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

/**
 * Cluster record representing a workspace or version update.
 */
public class ChangeLogRecord extends ClusterRecord {

    /**
     * Identifier: NODE.
     */
    static final char NODE_IDENTIFIER = 'N';

    /**
     * Identifier: PROPERTY.
     */
    static final char PROPERTY_IDENTIFIER = 'P';

    /**
     * Identifier: EVENT.
     */
    static final char EVENT_IDENTIFIER = 'E';

    /**
     * Operation type: added.
     */
    private static final int ADDED = 1;

    /**
     * Operation type: modified.
     */
    private static final int MODIFIED = 2;

    /**
     * Operation type: deleted.
     */
    private static final int DELETED = 3;

    /**
     * Changes.
     */
    private ChangeLog changes;

    /**
     * List of <code>EventState</code>s.
     */
    private List events;

    /**
     * First identifier read.
     */
    private int identifier;

    /**
     * Last used session for event sources.
     */
    private Session lastSession;

    /**
     * Create a new instance of this class. Used when serializing.
     *
     * @param changes changes
     * @param list of <code>EventState</code>s
     * @param record record
     * @param workspace workspace
     */
    public ChangeLogRecord(ChangeLog changes, List events,
                           Record record, String workspace) {
        super(record, workspace);

        this.changes = changes;
        this.events = events;
    }

    /**
     * Create a new instance of this class. Used when deserializing.
     *
     * @param identifier first identifier read
     * @param record record
     * @param workspace workspace
     */
    ChangeLogRecord(int identifier, Record record, String workspace) {
        super(record, workspace);

        this.identifier = identifier;
        this.changes = new ChangeLog();
        this.events = new ArrayList();
    }

    /**
     * {@inheritDoc}
     */
    protected void doRead() throws JournalException {
        int identifier = this.identifier;

        while (identifier != END_MARKER) {
            switch (identifier) {
            case NODE_IDENTIFIER:
                readNodeRecord();
                break;
            case PROPERTY_IDENTIFIER:
                readPropertyRecord();
                break;
            case EVENT_IDENTIFIER:
                readEventRecord();
                break;
            default:
                String msg = "Unknown identifier: " + identifier;
                throw new JournalException(msg);
            }
            identifier = record.readChar();
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void readEndMarker() throws JournalException {
        // This record type uses the end marker itself to indicate that
        // no more node/property/event records are available, so
        // do not try read it twice
    }

    /**
     * Read a node record.
     *
     * @throws JournalException if an error occurs
     */
    private void readNodeRecord() throws JournalException {
        int operation = record.readByte();
        NodeState state = new NodeState(record.readNodeId(), null, null,
                ItemState.STATUS_NEW, false);

        apply(operation, state);
    }

    /**
     * Read a property record.
     *
     * @throws JournalException if an error occurs
     */
    private void readPropertyRecord() throws JournalException {
        int operation = record.readByte();
        PropertyState state = new PropertyState(record.readPropertyId(),
                ItemState.STATUS_NEW, false);

        apply(operation, state);
    }

    /**
     * Apply an item state to the internal change log.
     *
     * @param operation operation
     * @param state item state
     * @throws JournalException if an error occurs
     */
    private void apply(int operation, ItemState state) throws JournalException {
        switch (operation) {
        case ADDED:
            state.setStatus(ItemState.STATUS_EXISTING);
            changes.added(state);
            break;
        case DELETED:
            state.setStatus(ItemState.STATUS_EXISTING_REMOVED);
            changes.deleted(state);
            break;
        case MODIFIED:
            state.setStatus(ItemState.STATUS_EXISTING_MODIFIED);
            changes.modified(state);
            break;
        default:
            String msg = "Unknown item operation: " + operation;
            throw new JournalException(msg);
        }
    }

    /**
     * Read an event record.
     *
     * @throws JournalException if an error occurs
     */
    private void readEventRecord() throws JournalException {
        int type = record.readByte();
        NodeId parentId = record.readNodeId();
        Path parentPath = record.readPath();
        NodeId childId = record.readNodeId();
        Path.Element childRelPath = record.readPathElement();
        Name ntName = record.readQName();

        Set mixins = new HashSet();
        int mixinCount = record.readInt();
        for (int i = 0; i < mixinCount; i++) {
            mixins.add(record.readQName());
        }
        String userId = record.readString();
        events.add(createEventState(type, parentId, parentPath, childId,
                childRelPath, ntName, mixins, userId));
    }

    /**
     * Create an event state.
     *
     * @param type event type
     * @param parentId parent id
     * @param parentPath parent path
     * @param childId child id
     * @param childRelPath child relative path
     * @param ntName node type name
     * @param mixins mixins
     * @param userId user id
     * @return event state
     */
    private EventState createEventState(int type, NodeId parentId, Path parentPath,
                                        NodeId childId, Path.Element childRelPath,
                                        Name ntName, Set mixins, String userId) {
        switch (type) {
        case Event.NODE_ADDED:
            return EventState.childNodeAdded(parentId, parentPath, childId, childRelPath,
                    ntName, mixins, getOrCreateSession(userId), true);
        case Event.NODE_REMOVED:
            return EventState.childNodeRemoved(parentId, parentPath, childId, childRelPath,
                    ntName, mixins, getOrCreateSession(userId), true);
        case Event.PROPERTY_ADDED:
            return EventState.propertyAdded(parentId, parentPath, childRelPath,
                    ntName, mixins, getOrCreateSession(userId), true);
        case Event.PROPERTY_CHANGED:
            return EventState.propertyChanged(parentId, parentPath, childRelPath,
                    ntName, mixins, getOrCreateSession(userId), true);
        case Event.PROPERTY_REMOVED:
            return EventState.propertyRemoved(parentId, parentPath, childRelPath,
                    ntName, mixins, getOrCreateSession(userId), true);
        default:
            String msg = "Unexpected event type: " + type;
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Return a session matching a certain user id.
     *
     * @param userId user id
     * @return session
     */
    private Session getOrCreateSession(String userId) {
        if (lastSession == null || !lastSession.getUserID().equals(userId)) {
            lastSession = new ClusterSession(userId);
        }
        return lastSession;
    }

    /**
     * {@inheritDoc}
     */
    protected void doWrite() throws JournalException {
        Iterator deletedStates = changes.deletedStates();
        while (deletedStates.hasNext()) {
            ItemState state = (ItemState) deletedStates.next();
            if (state.isNode()) {
                writeNodeRecord(DELETED, (NodeState) state);
            } else {
                writePropertyRecord(DELETED, (PropertyState) state);
            }
        }
        Iterator modifiedStates = changes.modifiedStates();
        while (modifiedStates.hasNext()) {
            ItemState state = (ItemState) modifiedStates.next();
            if (state.isNode()) {
                writeNodeRecord(MODIFIED, (NodeState) state);
            } else {
                writePropertyRecord(MODIFIED, (PropertyState) state);
            }
        }
        Iterator addedStates = changes.addedStates();
        while (addedStates.hasNext()) {
            ItemState state = (ItemState) addedStates.next();
            if (state.isNode()) {
                writeNodeRecord(ADDED, (NodeState) state);
            } else {
                writePropertyRecord(ADDED, (PropertyState) state);
            }
        }

        Iterator iter = events.iterator();
        while (iter.hasNext()) {
            EventState event = (EventState) iter.next();
            writeEventRecord(event);
        }
    }

    /**
     * Write a node record
     *
     * @param operation operation
     * @param state node state
     * @throws JournalException if an error occurs
     */
    private void writeNodeRecord(int operation, NodeState state)
            throws JournalException {

        record.writeChar(NODE_IDENTIFIER);
        record.writeByte(operation);
        record.writeNodeId(state.getNodeId());
    }

    /**
     * Write a property record
     *
     * @param operation operation
     * @param state property state
     * @throws JournalException if an error occurs
     */
    private void writePropertyRecord(int operation, PropertyState state)
            throws JournalException {

        record.writeChar(PROPERTY_IDENTIFIER);
        record.writeByte(operation);
        record.writePropertyId(state.getPropertyId());
    }

    /**
     * Write an event record
     *
     * @param event event state
     * @throws JournalException if an error occurs
     */
    private void writeEventRecord(EventState event) throws JournalException {
        record.writeChar(EVENT_IDENTIFIER);
        record.writeByte(event.getType());
        record.writeNodeId(event.getParentId());
        record.writePath(event.getParentPath());
        record.writeNodeId(event.getChildId());
        record.writePathElement(event.getChildRelPath());
        record.writeQName(event.getNodeType());

        Set mixins = event.getMixinNames();
        record.writeInt(mixins.size());
        Iterator iter = mixins.iterator();
        while (iter.hasNext()) {
            record.writeQName((Name) iter.next());
        }
        record.writeString(event.getUserId());
    }

    /**
     * {@inheritDoc}
     */
    public void process(ClusterRecordProcessor processor) {
        processor.process(this);
    }

    /**
     * Return the changes.
     *
     * @return changes
     */
    public ChangeLog getChanges() {
        return changes;
    }

    /**
     * Return the events.
     *
     * @return events
     * @return
     */
    public List getEvents() {
        return Collections.unmodifiableList(events);
    }
}
