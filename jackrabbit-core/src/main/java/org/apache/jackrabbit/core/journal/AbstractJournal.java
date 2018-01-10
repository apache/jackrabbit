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
package org.apache.jackrabbit.core.journal;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.jackrabbit.core.util.XAReentrantWriterPreferenceReadWriteLock;
import org.apache.jackrabbit.core.version.InternalVersionManagerImpl;
import org.apache.jackrabbit.core.version.VersioningLock;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base journal implementation.
 */
public abstract class AbstractJournal implements Journal {

    /**
     * Logger.
     */
    private static Logger log = LoggerFactory.getLogger(AbstractJournal.class);

    /**
     * Journal id.
     */
    private String id;

    /**
     * Namespace resolver.
     */
    private NamespaceResolver resolver;

    /**
     * NamePathResolver
     */
    private NamePathResolver npResolver;

    /**
     * Map of registered consumers.
     */
    private final Map<String, RecordConsumer> consumers = new HashMap<String, RecordConsumer>();

    /**
     * Map of registered producers.
     */
    private final Map<String, RecordProducer> producers = new HashMap<String, RecordProducer>();

    /**
     * Journal lock, allowing multiple readers (synchronizing their contents)
     * but only one writer (appending a new entry).
     */
    private final XAReentrantWriterPreferenceReadWriteLock rwLock = new XAReentrantWriterPreferenceReadWriteLock();

    /**
     * The path of the local revision file on disk. Configurable through the repository.xml.
     *
     *  Note: this field is not located in the FileJournal class for upgrade reasons (before
     *  JCR-1087 had been fixed all Journals used a revision file on the local file system.
     *  Also see {@link DatabaseJournal#initInstanceRevisionAndJanitor()}).
     */
    private String revision;

    /**
     * Repository home.
     */
    private File repHome;

    /**
     * Internal version manager.
     */
    private InternalVersionManagerImpl internalVersionManager;

    /**
     * {@inheritDoc}
     */
    public void init(String id, NamespaceResolver resolver) throws JournalException {
        this.id = id;
        this.resolver = resolver;
        this.npResolver = new DefaultNamePathResolver(resolver, true);
    }

    /**
     * {@inheritDoc}
     */
    public void register(RecordConsumer consumer) throws JournalException {
        synchronized (consumers) {
            String consumerId = consumer.getId();
            if (consumers.containsKey(consumerId)) {
                String msg = "Record consumer with identifier '"
                    + consumerId + "' already registered.";
                throw new JournalException(msg);
            }
            consumers.put(consumerId, consumer);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean unregister(RecordConsumer consumer) {
        synchronized (consumers) {
            String consumerId = consumer.getId();
            return consumers.remove(consumerId) != null;
        }
    }

    /**
     * Return the consumer given its identifier.
     *
     * @param identifier identifier
     * @return consumer associated with identifier;
     *         <code>null</code> if no consumer is associated with identifier
     */
    public RecordConsumer getConsumer(String identifier) {
        synchronized (consumers) {
            return consumers.get(identifier);
        }
    }

    /**
     * {@inheritDoc}
     */
    public RecordProducer getProducer(String identifier) {
        synchronized (producers) {
            RecordProducer producer = producers.get(identifier);
            if (producer == null) {
                producer = createProducer(identifier);
                producers.put(identifier, producer);
            }
            return producer;
        }
    }

    /**
     * Create the record producer for a given identifier. May be overridden
     * by subclasses.
     *
     * @param identifier producer identifier
     */
    protected RecordProducer createProducer(String identifier) {
        return new DefaultRecordProducer(this, identifier);
    }

    /**
     * Return the minimal revision of all registered consumers.
     */
    private long getMinimalRevision() {
        long minimalRevision = Long.MAX_VALUE;

        synchronized (consumers) {
            for (RecordConsumer consumer : consumers.values()) {
                if (consumer.getRevision() < minimalRevision) {
                    minimalRevision = consumer.getRevision();
                }
            }
        }
        return minimalRevision;
    }

    
    /**
     * {@inheritDoc}
     */
    public void sync(boolean startup) throws JournalException {
        log.debug("Synchronize to the latest change. Startup: " + startup);
        for (;;) {
            if (internalVersionManager != null) {
                VersioningLock.ReadLock lock =
                        internalVersionManager.acquireReadLock();
                try {
                    internalSync(startup);
                } finally {
                    lock.release();
                }
            } else {
                internalSync(startup);
            }
            // startup sync already done, don't do it again
            startup = false;
            if (syncAgainOnNewRecords()) {
                // sync again if there are more records available
                RecordIterator it = getRecords(getMinimalRevision());
                try {
                    if (it.hasNext()) {
                        continue;
                    }
                } finally {
                    it.close();
                }
            }
            break;
        }
    }

    private void internalSync(boolean startup) throws JournalException {
        try {
            rwLock.readLock().acquire();
        } catch (InterruptedException e) {
            String msg = "Unable to acquire read lock.";
            throw new JournalException(msg, e);
        }
        try {
            doSync(getMinimalRevision(), startup);
        } finally {
            rwLock.readLock().release();
        }
    }


    protected void doSync(long startRevision, boolean startup) throws JournalException {
        // by default ignore startup parameter for backwards compatibility
        // only needed for persistence backend that need special treatment on startup.
        doSync(startRevision);
    }
    
    /**
     * 
     * Synchronize contents from journal. May be overridden by subclasses.
     *
     * @param startRevision start point (exclusive)
     * @throws JournalException if an error occurs
     */
    protected void doSync(long startRevision) throws JournalException {
        log.debug("Synchronize contents from journal. StartRevision: " + startRevision);
        RecordIterator iterator = getRecords(startRevision);
        long stopRevision = Long.MIN_VALUE;

        try {
            while (iterator.hasNext()) {
                Record record = iterator.nextRecord();
                if (record.getJournalId().equals(id)) {
                    log.debug("Record with revision '" + record.getRevision()
                            + "' created by this journal, skipped.");
                } else {
                    RecordConsumer consumer = getConsumer(record.getProducerId());
                    if (consumer != null) {
                        consumer.consume(record);
                    }
                }
                stopRevision = record.getRevision();
            }
        } catch (IllegalStateException e) {
            log.error("Could not synchronize to revision: " + (stopRevision + 1) + " due illegal state of RecordConsumer.");
        } finally {
            iterator.close();
        }

        if (stopRevision > 0) {
            for (RecordConsumer consumer : consumers.values()) {
                consumer.setRevision(stopRevision);
            }
            log.debug("Synchronized from revision " + startRevision + " to revision: " + stopRevision);
        }
    }
    
    /**
     * Return a flag indicating whether synchronization should continue
     * in a loop until no more new records are found. Subclass overridable.
     * 
     * @return <code>true</code> if synchronization should continue;
     *         <code>false</code> otherwise
     */
    protected boolean syncAgainOnNewRecords() {
        return false;
    }

    /**
     * Lock the journal revision, disallowing changes from other sources until
     * {@link #unlock} has been called, and synchronizes to the latest change.
     *
     * @throws JournalException if an error occurs
     */
    public void lockAndSync() throws JournalException {
        log.debug("Lock the journal revision and synchronize to the latest change.");
        if (internalVersionManager != null) {
            VersioningLock.ReadLock lock =
                internalVersionManager.acquireReadLock();
            try {
                internalLockAndSync();
            } finally {
                lock.release();
            }
        } else {
            internalLockAndSync();
        }
    }

    private void internalLockAndSync() throws JournalException {
        try {
            rwLock.writeLock().acquire();
        } catch (InterruptedException e) {
            String msg = "Unable to acquire write lock.";
            throw new JournalException(msg, e);
        }

        boolean succeeded = false;

        try {
            // lock
            log.debug("internalLockAndSync.doLock()");
            doLock();
            try {
                // and sync
                doSync(getMinimalRevision());
                succeeded = true;
            } finally {
                if (!succeeded) {
                    log.debug("internalLockAndSync.doUnlock(false)");
                    doUnlock(false);
                }
            }
        } finally {
            if (!succeeded) {
                rwLock.writeLock().release();
            }
        }
    }

    /**
     * Unlock the journal revision.
     *
     * @param successful flag indicating whether the update process was
     *                   successful
     */
    public void unlock(boolean successful) {
        log.debug("Unlock the journal revision. Successful: " + successful);
    	try {
    		doUnlock(successful);
    	} finally {
    		//Should not happen that a RuntimeException will be thrown in subCode, but it's safer
    		//to release the rwLock in finally block.
            rwLock.writeLock().release();
    	}
    }

    /**
     * Lock the journal revision. Subclass responsibility.
     *
     * @throws JournalException if an error occurs
     */
    protected abstract void doLock() throws JournalException;

    /**
     * Notification method called by an appended record at creation time.
     * May be overridden by subclasses to save some context information
     * inside the appended record.
     *
     * @param record record that was appended
     */
    protected void appending(AppendRecord record) {
        // nothing to be done here
    }

    /**
     * Append a record backed by a file. On exit, the new revision must have
     * been set inside the appended record. Subclass responsibility.
     *
     * @param record record to append
     * @param in input stream
     * @param length number of bytes in input stream
     *
     * @throws JournalException if an error occurs
     */
    protected abstract void append(AppendRecord record, InputStream in, int length)
            throws JournalException;

    /**
     * Unlock the journal revision. Subclass responsibility.
     *
     * @param successful flag indicating whether the update process was
     *                   successful
     */
    protected abstract void doUnlock(boolean successful);

    /**
     * Return this journal's identifier.
     *
     * @return journal identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Return this journal's namespace resolver.
     *
     * @return namespace resolver
     */
    public NamespaceResolver getResolver() {
        return resolver;
    }

    /**
     * Return this journal's NamePathResolver.
     *
     * @return name and path resolver
     */
    public NamePathResolver getNamePathResolver() {
        return npResolver;
    }

    /**
     * Set the repository home.
     *
     * @param repHome repository home
     * @since JR 1.5
     */
    public void setRepositoryHome(File repHome) {
        this.repHome = repHome;
    }

    /**
     * Set the version manager.
     */
    public void setInternalVersionManager(InternalVersionManagerImpl internalVersionManager) {
        this.internalVersionManager = internalVersionManager;
    }

    /**
     * Return the repository home.
     *
     * @return the repository home
     * @since JR 1.5
     */
    public File getRepositoryHome() {
        return repHome;
    }

    /*
     * Bean getters and setters.
     */

     /**
      * @return the path of the cluster node's local revision file
      */
     public String getRevision() {
         return revision;
     }

     /**
      * @param revision the path of the cluster node's local revision file to set
      */
     public void setRevision(String revision) {
         this.revision = revision;
     }
}
