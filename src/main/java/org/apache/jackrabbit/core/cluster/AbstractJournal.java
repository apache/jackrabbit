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

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.compact.ParseException;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.observation.EventState;
import org.apache.jackrabbit.core.observation.EventStateCollection;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.NameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;

import EDU.oswego.cs.dl.util.concurrent.Mutex;

import javax.jcr.observation.Event;
import javax.jcr.Session;

/**
 * Base journal implementation, providing common functionality.
 * <p/>
 * It manages the following bean properties :
 * <ul>
 * <li><code>revision</code>: the filename where the parent cluster node's revision
 * file should be written to; this is a required property with no default value</li>
 * </ul>
 */
public abstract class AbstractJournal implements Journal {

    /**
     * Logger.
     */
    private static Logger log = LoggerFactory.getLogger(AbstractJournal.class);

    /**
     * Journal id.
     */
    protected String id;

    /**
     * Namespace resolver used to map prefixes to URIs and vice-versa.
     */
    protected NamespaceResolver resolver;

    /**
     * Record processor.
     */
    private RecordProcessor processor;

    /**
     * Mutex used when writing journal.
     */
    private final Mutex writeMutex = new Mutex();

    /**
     * Revision file name, bean property.
     */
    private String revision;

    /**
     * Current temporary journal log.
     */
    private File tempLog;

    /**
     * Current file record output.
     */
    private RecordOutput out;

    /**
     * Last used session for event sources.
     */
    private Session lastSession;

    /**
     * Next revision that will be available.
     */
    private long nextRevision;

    /**
     * Instance counter, file-based.
     */
    private FileRevision instanceRevision;

    /**
     * Bean getter for revision file.
     * @return revision file
     */
    public String getRevision() {
        return revision;
    }

    /**
     * Bean setter for journal directory.
     * @param revision directory used for journaling
     */
    public void setRevision(String revision) {
        this.revision = revision;
    }

    /**
     * {@inheritDoc}
     */
    public void init(String id, RecordProcessor processor, NamespaceResolver resolver)
            throws JournalException {

        this.id = id;
        this.resolver = resolver;
        this.processor = processor;

        if (revision == null) {
            String msg = "Revision not specified.";
            throw new JournalException(msg);
        }
        instanceRevision = new FileRevision(new File(revision));
    }

    /**
     * Process a record.
     *
     * @param revision revision
     * @param in record data
     * @throws JournalException if an error occurs
     */
    protected void process(long revision, RecordInput in) throws JournalException {
        log.info("Processing revision: " + revision);

        String workspace = null;

        try {
            workspace = in.readString();
            processor.start(workspace);

            for (;;) {
                char c = in.readChar();
                if (c == '\0') {
                    break;
                }
                if (c == 'N') {
                    NodeOperation operation = NodeOperation.create(in.readByte());
                    operation.setId(in.readNodeId());
                    processor.process(operation);
                } else if (c == 'P') {
                    PropertyOperation operation = PropertyOperation.create(in.readByte());
                    operation.setId(in.readPropertyId());
                    processor.process(operation);
                } else if (c == 'E') {
                    int type = in.readByte();
                    NodeId parentId = in.readNodeId();
                    Path parentPath = in.readPath();
                    NodeId childId = in.readNodeId();
                    Path.PathElement childRelPath = in.readPathElement();
                    QName ntName = in.readQName();

                    Set mixins = new HashSet();
                    int mixinCount = in.readInt();
                    for (int i = 0; i < mixinCount; i++) {
                        mixins.add(in.readQName());
                    }
                    String userId = in.readString();
                    processor.process(createEventState(type, parentId, parentPath, childId,
                            childRelPath, ntName, mixins, userId));
                } else if (c == 'L') {
                    NodeId nodeId = in.readNodeId();
                    boolean isLock = in.readBoolean();
                    if (isLock) {
                        boolean isDeep = in.readBoolean();
                        String owner = in.readString();
                        processor.process(nodeId, isDeep, owner);
                    } else {
                        processor.process(nodeId);
                    }
                } else if (c == 'S') {
                    String oldPrefix = in.readString();
                    String newPrefix = in.readString();
                    String uri = in.readString();
                    processor.process(oldPrefix, newPrefix, uri);
                } else if (c == 'T') {
                    int size = in.readInt();
                    HashSet ntDefs = new HashSet();
                    for (int i = 0; i < size; i++) {
                        ntDefs.add(in.readNodeTypeDef());
                    }
                    processor.process(ntDefs);
                } else {
                    throw new IllegalArgumentException("Unknown entry type: " + c);
                }
            }
            processor.end();

        } catch (NameException e) {
            String msg = "Unable to read revision '" + revision + "'.";
            throw new JournalException(msg, e);
        } catch (ParseException e) {
            String msg = "Unable to read revision '" + revision + "'.";
            throw new JournalException(msg, e);
        } catch (IOException e) {
            String msg = "Unable to read revision '" + revision + "'.";
            throw new JournalException(msg, e);
        } catch (IllegalArgumentException e) {
            String msg = "Error while processing revision " +
                    revision + ": " + e.getMessage();
            throw new JournalException(msg);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void begin(String workspace) throws JournalException {
        try {
            writeMutex.acquire();
        } catch (InterruptedException e) {
            String msg = "Interrupted while waiting for write lock.";
            throw new JournalException(msg);
        }

        boolean succeeded = false;

        try {
            sync();

            tempLog = File.createTempFile("journal", ".tmp");

            out = new RecordOutput(new DataOutputStream(
                    new FileOutputStream(tempLog)), resolver);
            out.writeString(workspace);

            succeeded = true;
        } catch (IOException e) {
            String msg = "Unable to create journal log " + tempLog + ": " + e.getMessage();
            throw new JournalException(msg);
        } finally {
            if (!succeeded) {
                writeMutex.release();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void log(ChangeLog changeLog, EventStateCollection esc) throws JournalException {
        Iterator addedStates = changeLog.addedStates();
        while (addedStates.hasNext()) {
            ItemState state = (ItemState) addedStates.next();
            if (state.isNode()) {
                log(NodeAddedOperation.create((NodeState) state));
            } else {
                log(PropertyAddedOperation.create((PropertyState) state));
            }
        }
        Iterator modifiedStates = changeLog.modifiedStates();
        while (modifiedStates.hasNext()) {
            ItemState state = (ItemState) modifiedStates.next();
            if (state.isNode()) {
                log(NodeModifiedOperation.create((NodeState) state));
            } else {
                log(PropertyModifiedOperation.create((PropertyState) state));
            }
        }
        Iterator deletedStates = changeLog.deletedStates();
        while (deletedStates.hasNext()) {
            ItemState state = (ItemState) deletedStates.next();
            if (state.isNode()) {
                log(NodeDeletedOperation.create((NodeState) state));
            } else {
                log(PropertyDeletedOperation.create((PropertyState) state));
            }
        }

        Iterator events = esc.getEvents().iterator();
        while (events.hasNext()) {
            EventState event = (EventState) events.next();
            log(event);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void log(String oldPrefix, String newPrefix, String uri) throws JournalException {
        try {
            out.writeChar('S');
            out.writeString(oldPrefix);
            out.writeString(newPrefix);
            out.writeString(uri);
        } catch (IOException e) {
            String msg = "Unable to write to journal log " + tempLog + ": " + e.getMessage();
            throw new JournalException(msg);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void log(NodeId nodeId, boolean isDeep, String owner) throws JournalException {
        log(nodeId, true, isDeep, owner);
    }

    /**
     * {@inheritDoc}
     */
    public void log(NodeId nodeId) throws JournalException {
        log(nodeId, false, false, null);
    }

    /**
     * {@inheritDoc}
     */
    public void log(Collection ntDefs) throws JournalException {
        try {
            out.writeChar('T');
            out.writeInt(ntDefs.size());

            Iterator iter = ntDefs.iterator();
            while (iter.hasNext()) {
                out.writeNodeTypeDef((NodeTypeDef) iter.next());
            }
        } catch (IOException e) {
            String msg = "Unable to write to journal log " + tempLog + ": " + e.getMessage();
            throw new JournalException(msg);
        }

    }

    /**
     * Log a property operation.
     *
     * @param operation property operation
     */
    protected void log(PropertyOperation operation) throws JournalException {
        try {
            out.writeChar('P');
            out.writeByte(operation.getOperationType());
            out.writePropertyId(operation.getId());
        } catch (NoPrefixDeclaredException e) {
            String msg = "Unable to write to journal log " + tempLog + ": " + e.getMessage();
            throw new JournalException(msg);
        } catch (IOException e) {
            String msg = "Unable to write to journal log " + tempLog + ": " + e.getMessage();
            throw new JournalException(msg);
        }
    }

    /**
     * Log a node operation.
     *
     * @param operation node operation
     */
    protected void log(NodeOperation operation) throws JournalException {
        try {
            out.writeChar('N');
            out.writeByte(operation.getOperationType());
            out.writeNodeId(operation.getId());
        } catch (IOException e) {
            String msg = "Unable to write to journal log " + tempLog + ": " + e.getMessage();
            throw new JournalException(msg);
        }
    }

    /**
     * Log an event. Subclass responsibility.
     *
     * @param event event to log
     */
    protected void log(EventState event) throws JournalException {
        try {
            out.writeChar('E');
            out.writeByte(event.getType());
            out.writeNodeId(event.getParentId());
            out.writePath(event.getParentPath());
            out.writeNodeId(event.getChildId());
            out.writePathElement(event.getChildRelPath());
            out.writeQName(event.getNodeType());

            Set mixins = event.getMixinNames();
            out.writeInt(mixins.size());
            Iterator iter = mixins.iterator();
            while (iter.hasNext()) {
                out.writeQName((QName) iter.next());
            }
            out.writeString(event.getUserId());
        } catch (NoPrefixDeclaredException e) {
            String msg = "Unable to write to journal log " + tempLog + ": " + e.getMessage();
            throw new JournalException(msg);
        } catch (IOException e) {
            String msg = "Unable to write to journal log " + tempLog + ": " + e.getMessage();
            throw new JournalException(msg);
        }
    }

    /**
     * Log either a lock or an unlock operation.
     *
     * @param nodeId node id
     * @param isLock <code>true</code> if this is a lock;
     *               <code>false</code> if this is an unlock
     * @param isDeep flag indicating whether lock is deep
     * @param owner lock owner
     */
    protected void log(NodeId nodeId, boolean isLock, boolean isDeep, String owner)
            throws JournalException {

        try {
            out.writeChar('L');
            out.writeNodeId(nodeId);
            out.writeBoolean(isLock);
            if (isLock) {
                out.writeBoolean(isDeep);
                out.writeString(owner);
            }
        } catch (IOException e) {
            String msg = "Unable to write to journal log " + tempLog + ": " + e.getMessage();
            throw new JournalException(msg);
        }
    }

    /**
     * Lock the global revision, disallowing changes from other sources until
     * {@link #unlockRevision} has been called.
     *
     * @return current global revision, passed to {@link #append} when changes
     *         are committed
     * @throws JournalException
     */
    protected abstract long lockRevision() throws JournalException;

    /**
     * Unlock the global revision. An additional flag indicates whether the
     * append operation was successful.
     *
     * @param successful whether the append operation was successful
     */
    protected abstract void unlockRevision(boolean successful);

    /**
     * {@inheritDoc}
     */
    public void prepare() throws JournalException {
        nextRevision = lockRevision();

        boolean succeeded = false;

        try {
            sync();

            succeeded = true;
        } finally {
            if (!succeeded) {
                unlockRevision(false);
                writeMutex.release();
            }
        }
    }

    /**
     * Append the given record to the journal. On exit, the global and local revision
     * should have been updated as well.
     *
     * @param revision record revision, as returned by {@link #lockRevision()}
     * @param record record to append
     * @throws JournalException if an error occurs
     */
    protected abstract void append(long revision, File record) throws JournalException;

    /**
     * Returns the current local revision.
     *
     * @return current local revision
     * @throws JournalException if an error occurs
     */
    protected long getLocalRevision() throws JournalException {
        return instanceRevision.get();
    }

    /**
     * Sets the current local revision.
     *
     * @param revision revision
     * @throws JournalException if an error occurs
     */
    protected void setLocalRevision(long revision) throws JournalException {
        instanceRevision.set(revision);
    }

    /**
     * {@inheritDoc}
     */
    public void commit() throws JournalException {
        boolean succeeded = false;

        try {
            out.writeChar('\0');
            out.close();

            append(nextRevision, tempLog);

            succeeded = true;

        } catch (IOException e) {
            String msg = "Unable to close journal log " + tempLog + ": " + e.getMessage();
            throw new JournalException(msg);
        } finally {
            out = null;
            tempLog.delete();
            unlockRevision(succeeded);
            writeMutex.release();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cancel() {
        if (out != null) {
            try {
                out.close();
                tempLog.delete();
            } catch (IOException e) {
                String msg = "Unable to close journal log " + tempLog + ": " + e.getMessage();
                log.warn(msg);
            } finally {
                out = null;
                unlockRevision(false);
                writeMutex.release();
            }
        }
    }

    /**
     * Create an event state.
     *
     * @param type event type
     * @param parentId parent id
     * @param parentPath parent path
     * @param childId child id
     * @param childRelPath child relative path
     * @param ntName ndoe type name
     * @param userId user id
     * @return event
     */
    protected EventState createEventState(int type, NodeId parentId, Path parentPath,
                                          NodeId childId, Path.PathElement childRelPath,
                                          QName ntName, Set mixins, String userId) {
        switch (type) {
            case Event.NODE_ADDED:
                return EventState.childNodeAdded(parentId, parentPath, childId, childRelPath,
                        ntName, mixins, getOrCreateSession(userId));
            case Event.NODE_REMOVED:
                return EventState.childNodeRemoved(parentId, parentPath, childId, childRelPath,
                        ntName, mixins, getOrCreateSession(userId));
            case Event.PROPERTY_ADDED:
                return EventState.propertyAdded(parentId, parentPath, childRelPath,
                        ntName, mixins, getOrCreateSession(userId));
            case Event.PROPERTY_CHANGED:
                return EventState.propertyChanged(parentId, parentPath, childRelPath,
                        ntName, mixins, getOrCreateSession(userId));
            case Event.PROPERTY_REMOVED:
                return EventState.propertyRemoved(parentId, parentPath, childRelPath,
                        ntName, mixins, getOrCreateSession(userId));
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
    protected Session getOrCreateSession(String userId) {
        if (lastSession == null || !lastSession.getUserID().equals(userId)) {
            lastSession = new ClusterSession(userId);
        }
        return lastSession;
    }
}
