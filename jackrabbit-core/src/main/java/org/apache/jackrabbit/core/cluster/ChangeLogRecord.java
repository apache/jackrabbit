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
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import javax.jcr.Session;
import javax.jcr.PropertyType;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.journal.JournalException;
import org.apache.jackrabbit.core.journal.Record;
import org.apache.jackrabbit.core.observation.EventState;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import javax.jcr.observation.Event;

/**
 * Cluster record representing a workspace or version update.
 */
public class ChangeLogRecord extends ClusterRecord {

    /**
     * Identifier: DATE
     */
    static final char DATE_IDENTIFIER = 'D';

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
     * Identifier: USER DATA.
     */
    static final char USER_DATA_IDENTIFIER = 'U';

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
     * The time when the changes happened. Milliseconds since January 1 1970 UTC.
     */
    private long timestamp = System.currentTimeMillis();

    /**
     * List of <code>EventState</code>s.
     */
    private List<EventState> events;

    /**
     * The user data.
     */
    private String userData;

    /**
     * First identifier read.
     */
    private int identifier;

    /**
     * Last used session for event sources.
     */
    private ClusterSession lastSession;

    /**
     * Create a new instance of this class. Used when serializing.
     *
     * @param changes changes
     * @param events list of <code>EventState</code>s
     * @param record record
     * @param workspace workspace
     * @param timestamp when the changes for this record were persisted.
     * @param userData the user data associated with these changes.
     */
    public ChangeLogRecord(ChangeLog changes, List<EventState> events,
                           Record record, String workspace,
                           long timestamp, String userData) {
        super(record, workspace);

        this.changes = changes;
        this.events = events;
        this.timestamp = timestamp;
        this.userData = userData;
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
        this.events = new ArrayList<EventState>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doRead() throws JournalException {
        int identifier = this.identifier;

        while (identifier != END_MARKER) {
            switch (identifier) {
            case DATE_IDENTIFIER:
                readTimestampRecord();
                break;
            case USER_DATA_IDENTIFIER:
                readUserDataRecord();
                break;
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
    @Override
    protected void readEndMarker() throws JournalException {
        // This record type uses the end marker itself to indicate that
        // no more node/property/event records are available, so
        // do not try read it twice
    }

    /**
     * Reads the timestamp record.
     *
     * @throws JournalException if an error occurs.
     */
    private void readTimestampRecord() throws JournalException {
        timestamp = record.readLong();
    }

    /**
     * Reads the user data record.
     *
     * @throws JournalException if an error occurs.
     */
    private void readUserDataRecord() throws JournalException {
        userData = record.readString();
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
        Path childRelPath = record.readPathElement();
        Name ntName = record.readQName();

        Set<Name> mixins = new HashSet<Name>();
        int mixinCount = record.readInt();
        for (int i = 0; i < mixinCount; i++) {
            mixins.add(record.readQName());
        }
        String userId = record.readString();

        Map<String, InternalValue> info = null;
        if (type == Event.NODE_MOVED) {
            info = new HashMap<String, InternalValue>();
            // read info map
            int infoSize = record.readInt();
            for (int i = 0; i < infoSize; i++) {
                String key = record.readString();
                int propType = record.readInt();
                InternalValue value;
                if (propType == PropertyType.UNDEFINED) {
                    // indicates null value
                    value = null;
                } else {
                    value = InternalValue.valueOf(record.readString(), propType);
                }
                info.put(key, value);
            }
        }

        EventState es = createEventState(type, parentId, parentPath, childId,
                childRelPath, ntName, mixins, userId);
        if (info != null) {
            es.setInfo(info);
        }
        events.add(es);
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
                                        NodeId childId, Path childRelPath,
                                        Name ntName, Set<Name> mixins, String userId) {
        switch (type) {
            case Event.NODE_ADDED:
                return EventState.childNodeAdded(parentId, parentPath, childId, childRelPath,
                        ntName, mixins, getOrCreateSession(userId), true);
            case Event.NODE_MOVED:
                return EventState.nodeMoved(parentId, parentPath, childId, childRelPath,
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
        if (lastSession == null || !lastSession.isUserId(userId)) {
            lastSession = new ClusterSession(userId);
        }
        return lastSession;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doWrite() throws JournalException {
        writeTimestampRecord();
        writeUserDataRecord();
        for (ItemState state : changes.deletedStates()) {
            if (state.isNode()) {
                writeNodeRecord(DELETED, (NodeState) state);
            } else {
                writePropertyRecord(DELETED, (PropertyState) state);
            }
        }
        for (ItemState state : changes.modifiedStates()) {
            if (state.isNode()) {
                writeNodeRecord(MODIFIED, (NodeState) state);
            } else {
                writePropertyRecord(MODIFIED, (PropertyState) state);
            }
        }
        for (ItemState state : changes.addedStates()) {
            if (state.isNode()) {
                writeNodeRecord(ADDED, (NodeState) state);
            } else {
                writePropertyRecord(ADDED, (PropertyState) state);
            }
        }

        for (EventState event : events) {
            writeEventRecord(event);
        }
    }

    /**
     * Writes the timestamp record.
     *
     * @throws JournalException if an error occurs.
     */
    private void writeTimestampRecord() throws JournalException {
        record.writeChar(DATE_IDENTIFIER);
        record.writeLong(timestamp);
    }

    /**
     * Writes the user data record.
     *
     * @throws JournalException if an error occurs.
     */
    private void writeUserDataRecord() throws JournalException {
        if (userData != null) {
            record.writeChar(USER_DATA_IDENTIFIER);
            record.writeString(userData);
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

        Set<Name> mixins = event.getMixinNames();
        record.writeInt(mixins.size());
        for (Name mixin : mixins) {
            record.writeQName(mixin);
        }
        record.writeString(event.getUserId());

        if (event.getType() == Event.NODE_MOVED) {
            // write info map
            Map<String, InternalValue> info = event.getInfo();
            record.writeInt(info.size());
            for (Map.Entry<String, InternalValue> entry : info.entrySet()) {
                String key = entry.getKey();
                InternalValue value = entry.getValue();
                record.writeString(key);
                if (value == null) {
                    // use undefined for null value
                    record.writeInt(PropertyType.UNDEFINED);
                } else {
                    record.writeInt(value.getType());
                    record.writeString(value.toString());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
     */
    public List<EventState> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * Returns the timestamp.
     *
     * @return the timestamp.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the user data.
     *
     * @return the user data.
     */
    public String getUserData() {
        return userData;
    }
}
