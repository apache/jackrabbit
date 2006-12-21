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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;

import EDU.oswego.cs.dl.util.concurrent.Mutex;

import javax.jcr.observation.Event;
import javax.jcr.Session;

/**
 * File-based journal implementation. A directory specified as <code>directory</code>
 * bean property will contain log files and a global revision file, containing the
 * latest revision file. When the current log file's size exceeds <code>maxSize</code>
 * bytes, it gets renamed to its name appended by '1'. At the same time, all log files
 * already having a version counter, get their version counter incremented by <code>1</code>.
 * <p/>
 * It is configured through the following properties:
 * <ul>
 * <li><code>directory</code>: the shared directory where journal logs and read from
 * and written to; this is a required property with no default value</li>
 * <li><code>revision</code>: the filename where the parent cluster node's revision
 * file should be written to; this is a required property with no default value</li>
 * <li><code>basename</code>: this is the basename of the journal logs created in
 * the shared directory; its default value is <code>journal</code></li>
 * <li><code>maximumSize</code>: this is the maximum size in bytes of a journal log
 * before a new log will be created; its default value is <code>1048576</code> (1MB)</li>
 * </ul>
 *
 * todo after some iterations, old files should be automatically compressed to save space
 */
public class FileJournal implements Journal {

    /**
     * Global revision counter name, located in the journal directory.
     */
    private static final String REVISION_NAME = "revision";

    /**
     * Log extension.
     */
    private static final String LOG_EXTENSION = "log";

    /**
     * Default base name for journal files.
     */
    private static final String DEFAULT_BASENAME = "journal";

    /**
     * Default max size of a journal file (1MB).
     */
    private static final int DEFAULT_MAXSIZE = 1048576;

    /**
     * Logger.
     */
    private static Logger log = LoggerFactory.getLogger(FileJournal.class);

    /**
     * Journal id.
     */
    private String id;

    /**
     * Namespace resolver used to map prefixes to URIs and vice-versa.
     */
    private NamespaceResolver resolver;

    /**
     * Record processor.
     */
    private RecordProcessor processor;

    /**
     * Directory name, bean property.
     */
    private String directory;

    /**
     * Revision file name, bean property.
     */
    private String revision;

    /**
     * Journal file base name, bean property.
     */
    private String basename;

    /**
     * Maximum size of a journal file before a rotation takes place, bean property.
     */
    private int maximumSize;

    /**
     * Journal root directory.
     */
    private File root;

    /**
     * Journal file.
     */
    private File journal;

    /**
     * Instance counter.
     */
    private FileRevision instanceRevision;

    /**
     * Global journal counter.
     */
    private FileRevision globalRevision;

    /**
     * Mutex used when writing journal.
     */
    private final Mutex writeMutex = new Mutex();

    /**
     * Current temporary journal log.
     */
    private File tempLog;

    /**
     * Current file record output.
     */
    private FileRecordOutput out;

    /**
     * Current file record.
     */
    private FileRecord record;

    /**
     * Last used session for event sources.
     */
    private Session lastSession;

    /**
     * Bean getter for journal directory.
     * @return directory
     */
    public String getDirectory() {
        return directory;
    }

    /**
     * Bean setter for journal directory.
     * @param directory directory used for journaling
     */
    public void setDirectory(String directory) {
        this.directory = directory;
    }

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
     * Bean getter for base name.
     * @return base name
     */
    public String getBasename() {
        return basename;
    }

    /**
     * Bean setter for basename.
     * @param basename base name
     */
    public void setBasename(String basename) {
        this.basename = basename;
    }

    /**
     * Bean getter for maximum size.
     * @return maximum size
     */
    public int getMaximumSize() {
        return maximumSize;
    }

    /**
     * Bean setter for maximum size.
     * @param maximumSize maximum size
     */
    public void setMaximumSize(int maximumSize) {
        this.maximumSize = maximumSize;
    }

    /**
     * {@inheritDoc}
     */
    public void init(String id, RecordProcessor processor, NamespaceResolver resolver) throws JournalException {
        this.id = id;
        this.resolver = resolver;
        this.processor = processor;

        if (directory == null) {
            String msg = "Directory not specified.";
            throw new JournalException(msg);
        }
        if (revision == null) {
            String msg = "Revision not specified.";
            throw new JournalException(msg);
        }
        if (basename == null) {
            basename = DEFAULT_BASENAME;
        }
        if (maximumSize == 0) {
            maximumSize = DEFAULT_MAXSIZE;
        }
        root = new File(directory);
        if (!root.exists() || !root.isDirectory()) {
            String msg = "Directory specified does either not exist or is not a directory: " + directory;
            throw new JournalException(msg);
        }
        journal = new File(root, basename + "." + LOG_EXTENSION);

        instanceRevision = new FileRevision(new File(revision));
        globalRevision = new FileRevision(new File(root, REVISION_NAME));

        log.info("FileJournal initialized at path: " + directory);
    }

    /**
     * {@inheritDoc}
     */
    public void sync() throws JournalException {
        File[] logFiles = root.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(basename + ".");
            }
        });
        Arrays.sort(logFiles, new Comparator() {
            public int compare(Object o1, Object o2) {
                File f1 = (File) o1;
                File f2 = (File) o2;
                return f1.compareTo(f2);
            }
        });

        long instanceValue = instanceRevision.get();
        long globalValue = globalRevision.get();

        if (instanceValue < globalValue) {
            FileRecordCursor cursor = new FileRecordCursor(logFiles,
                    instanceValue, globalValue);
            try {
                while (cursor.hasNext()) {
                    FileRecord record = cursor.next();
                    if (!record.getCreator().equals(id)) {
                        process(record);
                    } else {
                        log.info("Log entry matches journal id, skipped: " + record.getRevision());
                    }
                    instanceRevision.set(record.getNextRevision());
                }
            } catch (IOException e) {
                String msg = "Unable to iterate over modified records: " + e.getMessage();
                throw new JournalException(msg, e);

            } finally {
                try {
                    cursor.close();
                } catch (IOException e) {
                    String msg = "I/O error while closing record cursor: " + e.getMessage();
                    log.warn(msg);
                }
            }
            log.info("Sync finished, instance revision is: " + instanceRevision.get());
        }
    }

    /**
     * Process a record.
     *
     * @param record record to process
     * @throws JournalException if an error occurs
     */
    void process(FileRecord record) throws JournalException {
        log.info("Processing revision: " + record.getRevision());

        FileRecordInput in = record.getInput(resolver);
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
            String msg = "Unable to read revision " + record.getRevision() +
                    ": " + e.getMessage();
            throw new JournalException(msg);
        } catch (ParseException e) {
            String msg = "Unable to read revision " + record.getRevision() +
                    ": " + e.getMessage();
            throw new JournalException(msg);
        } catch (IOException e) {
            String msg = "Unable to read revision " + record.getRevision() +
                    ": " + e.getMessage();
            throw new JournalException(msg);
        } catch (IllegalArgumentException e) {
            String msg = "Error while processing revision " +
                    record.getRevision() + ": " + e.getMessage();
            throw new JournalException(msg);
        } finally {
            in.close();
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

            tempLog = File.createTempFile("journal", ".tmp", root);

            record = new FileRecord(id, tempLog);
            out = record.getOutput(resolver);
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
     * {@inheritDoc}
     */
    public void prepare() throws JournalException {
        globalRevision.lock(false);

        boolean prepared = false;

        try {
            sync();

            record.setRevision(globalRevision.get());

            prepared = true;
        } finally {
            if (!prepared) {
                globalRevision.unlock();
                writeMutex.release();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void commit() throws JournalException {
        try {
            out.writeChar('\0');
            out.close();

            long nextRevision = record.getNextRevision();

            FileRecordLog recordLog = new FileRecordLog(journal);
            if (!recordLog.isNew()) {
                if (nextRevision - recordLog.getFirstRevision() > maximumSize) {
                    switchLogs();
                    recordLog = new FileRecordLog(journal);
                }
            }
            recordLog.append(record);

            tempLog.delete();
            globalRevision.set(nextRevision);
            instanceRevision.set(nextRevision);

        } catch (IOException e) {
            String msg = "Unable to close journal log " + tempLog + ": " + e.getMessage();
            throw new JournalException(msg);
        } finally {
            out = null;
            globalRevision.unlock();
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
                globalRevision.unlock();
                writeMutex.release();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() {}

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

    /**
     * Move away current journal file (and all other files), incrementing their
     * version counter. A file named <code>journal.N.log</code> gets renamed to
     * <code>journal.(N+1).log</code>, whereas the main journal file gets renamed
     * to <code>journal.1.log</code>.
     */
    private void switchLogs() {
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(basename + ".");
            }
        };
        File[] files = root.listFiles(filter);
        Arrays.sort(files, new Comparator() {
            public int compare(Object o1, Object o2) {
                File f1 = (File) o1;
                File f2 = (File) o2;
                return f2.compareTo(f1);
            }
        });
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            String name = file.getName();
            int sep = name.lastIndexOf('.');
            if (sep != -1) {
                String ext = name.substring(sep + 1);
                if (ext.equals(LOG_EXTENSION)) {
                    file.renameTo(new File(root, name + ".1"));
                } else {
                    try {
                        int version = Integer.parseInt(ext);
                        String newName = name.substring(0, sep + 1) +
                                String.valueOf(version + 1);
                        file.renameTo(new File(newName));
                    } catch (NumberFormatException e) {
                        log.warn("Bogusly named journal file, skipped: " + file);
                    }
                }
            }
        }
    }
}
