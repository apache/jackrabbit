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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import EDU.oswego.cs.dl.util.concurrent.Mutex;

/**
 * File-based journal implementation.
 */
public class FileJournal implements Journal {

    /**
     * Global revision counter name, located in the journal directory.
     */
    private static final String REVISION_NAME = "revision";

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
     * Callback.
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
     * Journal root directory.
     */
    private File root;

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
        return directory;
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
        root = new File(directory);
        if (!root.exists() || !root.isDirectory()) {
            String msg = "Directory specified does either not exist or is not a directory: " + directory;
            throw new JournalException(msg);
        }
        instanceRevision = new FileRevision(new File(revision));
        globalRevision = new FileRevision(new File(root, REVISION_NAME));

        log.info("FileJournal initialized at path: " + directory);
    }

    /**
     * {@inheritDoc}
     */
    public void sync() throws JournalException {
        final long instanceValue = instanceRevision.get();
        final long globalValue = globalRevision.get();

        File[] files = root.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (name.endsWith(FileRecord.EXTENSION)) {
                    int sep = name.indexOf('.');
                    if (sep > 0) {
                        try {
                            long counter = Long.parseLong(name.substring(0, sep), 16);
                            return counter > instanceValue && counter <= globalValue;
                        } catch (NumberFormatException e) {
                            String msg = "Skipping bogusly named journal file '" + name + "': " + e.getMessage();
                            log.warn(msg);
                        }
                    }
                }
                return false;
            }
        });
        Arrays.sort(files, new Comparator() {
            public int compare(Object o1, Object o2) {
                File f1 = (File) o1;
                File f2 = (File) o2;
                return f1.getName().compareTo(f2.getName());
            }
        });
        if (files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                try {
                    FileRecord record = new FileRecord(files[i]);
                    if (!record.getJournalId().equals(id)) {
                        process(record);
                    } else {
                        log.info("Log entry matches journal id, skipped: " + files[i]);
                    }
                    instanceRevision.set(record.getCounter());
                } catch (IllegalArgumentException e) {
                    String msg = "Skipping bogusly named journal file '" + files[i] + ": " + e.getMessage();
                    log.warn(msg);
                }
            }
            log.info("Sync finished, instance revision is: " + FileRecord.toHexString(instanceRevision.get()));
        }
    }

    /**
     * Process a record.
     *
     * @param record record to process
     * @throws JournalException if an error occurs
     */
    void process(FileRecord record) throws JournalException {
        File file = record.getFile();

        log.info("Processing: " + file);

        FileRecordInput in = null;
        String workspace = null;

        try {
            in = new FileRecordInput(new FileInputStream(file), resolver);

            workspace = in.readString();
            if (workspace.equals("")) {
                workspace = null;
            }
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
                    processor.process(type, parentId, parentPath, childId,
                            childRelPath, ntName, mixins, userId);
                } else if (c == 'L') {
                    NodeId nodeId = in.readNodeId();
                    boolean isDeep = in.readBoolean();
                    String owner = in.readString();

                    processor.process(nodeId, isDeep, owner);
                } else if (c == 'U') {
                    NodeId nodeId = in.readNodeId();
                    processor.process(nodeId);
                } else {
                    throw new IllegalArgumentException("Unknown entry type: " + c);
                }
            }
            processor.end();

        } catch (NameException e) {
            String msg = "Unable to read journal entry " + file + ": " + e.getMessage();
            throw new JournalException(msg);
        } catch (IOException e) {
            String msg = "Unable to read journal entry " + file + ": " + e.getMessage();
            throw new JournalException(msg);
        } catch (IllegalArgumentException e) {
            String msg = "Error while processing journal file " + file + ": " + e.getMessage();
            throw new JournalException(msg);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    String msg = "I/O error while closing " + file + ": " + e.getMessage();
                    log.warn(msg);
                }
            }
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
            out = new FileRecordOutput(new FileOutputStream(tempLog), resolver);
            out.writeString(workspace != null ? workspace : "");

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
    public void log(NodeId nodeId, boolean isDeep, String owner) throws JournalException {
        try {
            out.writeChar('L');
            out.writeNodeId(nodeId);
            out.writeBoolean(isDeep);
            out.writeString(owner);
        } catch (IOException e) {
            String msg = "Unable to write to journal log " + tempLog + ": " + e.getMessage();
            throw new JournalException(msg);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void log(NodeId nodeId) throws JournalException {
        try {
            out.writeChar('U');
            out.writeNodeId(nodeId);
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
     * {@inheritDoc}
     */
    public void prepare() throws JournalException {
        globalRevision.lock(false);

        boolean prepared = false;

        try {
            sync();
            record = new FileRecord(root, globalRevision.get() + 1, id);

            prepared = true;
        } finally {
            if (!prepared) {
                globalRevision.unlock();
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

            if (!tempLog.renameTo(record.getFile())) {
                throw new JournalException("Unable to rename " + tempLog + " to " + record.getFile());
            }
            globalRevision.set(record.getCounter());

        } catch (IOException e) {
            String msg = "Unable to close journal log " + tempLog + ": " + e.getMessage();
            throw new JournalException(msg);
        } finally {
            globalRevision.unlock();
            writeMutex.release();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cancel() throws JournalException {
        try {
            out.close();
            tempLog.delete();
        } catch (IOException e) {
            String msg = "Unable to close journal log " + tempLog + ": " + e.getMessage();
            log.warn(msg);
        } finally {
            globalRevision.unlock();
            writeMutex.release();
        }
    }
}
