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
package org.apache.jackrabbit.core.observation;

import java.util.List;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Date;
import java.util.Collections;
import java.text.DateFormat;

import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import javax.jcr.observation.EventJournal;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.cluster.PrivilegeRecord;
import org.apache.jackrabbit.core.journal.Journal;
import org.apache.jackrabbit.core.journal.RecordIterator;
import org.apache.jackrabbit.core.journal.JournalException;
import org.apache.jackrabbit.core.journal.Record;
import org.apache.jackrabbit.core.cluster.ClusterRecordDeserializer;
import org.apache.jackrabbit.core.cluster.ClusterRecord;
import org.apache.jackrabbit.core.cluster.ClusterRecordProcessor;
import org.apache.jackrabbit.core.cluster.ChangeLogRecord;
import org.apache.jackrabbit.core.cluster.LockRecord;
import org.apache.jackrabbit.core.cluster.NamespaceRecord;
import org.apache.jackrabbit.core.cluster.NodeTypeRecord;
import org.apache.jackrabbit.core.cluster.WorkspaceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>EventJournalImpl</code> implements the JCR 2.0 {@link EventJournal}.
 */
public class EventJournalImpl implements EventJournal {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(EventJournalImpl.class);

    /**
     * The minimum buffer size for events in {@link #eventBundleBuffer}.
     */
    private static final int MIN_BUFFER_SIZE = 1024;

    /**
     * Map of skip maps. Key=Journal, Value=SortedMap
     * </p>
     * Each sorted map has the following structure:
     * Key=Long (timestamp), Value=Long (revision)
     */
    private static final Map<Journal, SortedMap<Long, Long>> REVISION_SKIP_MAPS = new WeakHashMap<Journal, SortedMap<Long, Long>>();

    /**
     * Last revision seen by this event journal.
     */
    private Long lastRevision;

    /**
     * The event filter.
     */
    private final EventFilter filter;

    /**
     * The journal of this repository.
     */
    private final Journal journal;

    /**
     * The producer id to filter journal records.
     */
    private final String producerId;

    /**
     * Target session.
     */
    private final SessionImpl session;

    /**
     * Buffer of {@link EventBundle}s.
     */
    private final List<EventBundle> eventBundleBuffer = new LinkedList<EventBundle>();

    /**
     * The current position of this iterator.
     */
    private long position;

    /**
     * Creates a new event journal.
     *
     * @param filter for filtering the events read from the journal.
     * @param journal the cluster journal.
     * @param producerId the producer id of the cluster node.
     * @param session target session
     */
    public EventJournalImpl(
            EventFilter filter, Journal journal,
            String producerId, SessionImpl session) {
        this.filter = filter;
        this.journal = journal;
        this.producerId = producerId;
        this.session = session;
    }

    //------------------------< EventJournal >---------------------------------

    /**
     * {@inheritDoc}
     */
    public void skipTo(long date) {
        long time = System.currentTimeMillis();

        // get skip map for this journal
        SortedMap<Long, Long> skipMap = getSkipMap();
        synchronized (skipMap) {
            SortedMap<Long, Long> head = skipMap.headMap(new Long(date));
            if (!head.isEmpty()) {
                eventBundleBuffer.clear();
                lastRevision = head.get(head.lastKey());
            }
        }

        try {
            while (hasNext()) {
                EventBundle bundle = getCurrentBundle();
                if (bundle.timestamp <= date) {
                    eventBundleBuffer.remove(0);
                } else {
                    break;
                }
            }
        } finally {
            time = System.currentTimeMillis() - time;
            log.debug("Skipped event bundles in {} ms.", new Long(time));
        }
    }

    //------------------------< EventIterator >---------------------------------

    /**
     * {@inheritDoc}
     */
    public Event nextEvent() {
        // calling hasNext() will also trigger refill if necessary!
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        EventBundle bundle = getCurrentBundle();
        // above hasNext() call ensures that there is bundle with an event state
        assert bundle != null && bundle.events.hasNext();

        Event next = (Event) bundle.events.next();
        if (!bundle.events.hasNext()) {
            // done with this bundle -> remove from buffer
            eventBundleBuffer.remove(0);
        }
        position++;
        return next;
    }

    //------------------------< RangeIterator >---------------------------------

    /**
     * {@inheritDoc}
     */
    public void skip(long skipNum) {
        while (skipNum-- > 0) {
            nextEvent();
        }
    }

    /**
     * @return always -1.
     */
    public long getSize() {
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    public long getPosition() {
        // TODO: what happens to position when skipped
        return position;
    }

    //--------------------------< Iterator >------------------------------------

    /**
     * @throws UnsupportedOperationException always.
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        if (!eventBundleBuffer.isEmpty()) {
            return true;
        }
        // try refill
        refill();
        // check again
        return !eventBundleBuffer.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public Object next() {
        return nextEvent();
    }

    //----------------------< ClusterRecordProcessor >--------------------------

    /**
     * Implements {@link ClusterRecordProcessor} and keeps track of the number
     * of events read and the timestamp of the last record processed.
     */
    private class RecordProcessor implements ClusterRecordProcessor {

        /**
         * Number of events read so far.
         */
        private int numEvents;

        /**
         * The timestamp of the last record processed.
         */
        private long lastTimestamp;

        /**
         * @return the number of events read so far.
         */
        private int getNumEvents() {
            return numEvents;
        }

        /**
         * @return the timestamp of the last record processed.
         */
        private long getLastTimestamp() {
            return lastTimestamp;
        }

        /**
         * {@inheritDoc}
         */
        public void process(ChangeLogRecord record) {
            List<EventState> events = record.getEvents();
            if (!events.isEmpty()) {
                EventBundle bundle = new EventBundle(
                        events, record.getTimestamp(), record.getUserData());
                if (bundle.events.hasNext()) {
                    // only queue bundle if there is an event
                    eventBundleBuffer.add(bundle);
                    numEvents += events.size();
                    lastTimestamp = record.getTimestamp();
                }
            }
        }

        public void process(LockRecord record) {
            // ignore
        }

        public void process(NamespaceRecord record) {
            // ignore
        }

        public void process(NodeTypeRecord record) {
            // ignore
        }

        public void process(PrivilegeRecord record) {
            // ignore
        }

        public void process(WorkspaceRecord record) {
            // ignore
        }
    }

    //-------------------------------< internal >-------------------------------

    /**
     * @return the current event bundle or <code>null</code> if there is none.
     */
    private EventBundle getCurrentBundle() {
        while (!eventBundleBuffer.isEmpty()) {
            EventBundle bundle = eventBundleBuffer.get(0);
            if (bundle.events.hasNext()) {
                return bundle;
            } else {
                eventBundleBuffer.remove(0);
            }
        }
        return null;
    }

    /**
     * Refills the {@link #eventBundleBuffer}.
     */
    private void refill() {
        assert eventBundleBuffer.isEmpty();
        try {
            RecordProcessor processor = new RecordProcessor();
            ClusterRecordDeserializer deserializer = new ClusterRecordDeserializer();
            RecordIterator records;
            if (lastRevision != null) {
                log.debug("refilling event bundle buffer starting at revision {}",
                        lastRevision);
                records = journal.getRecords(lastRevision.longValue());
            } else {
                log.debug("refilling event bundle buffer starting at journal beginning");
                records = journal.getRecords();
            }
            try {
                while (processor.getNumEvents() < MIN_BUFFER_SIZE && records.hasNext()) {
                    Record record = records.nextRecord();
                    if (record.getProducerId().equals(producerId)) {
                        ClusterRecord cr = deserializer.deserialize(record);
                        if (!session.getWorkspace().getName().equals(cr.getWorkspace())) {
                            continue;
                        }
                        cr.process(processor);
                        lastRevision = new Long(cr.getRevision());
                    }
                }

                if (processor.getNumEvents() >= MIN_BUFFER_SIZE) {
                    // remember in skip map
                    SortedMap<Long, Long> skipMap = getSkipMap();
                    Long timestamp = new Long(processor.getLastTimestamp());
                    synchronized (skipMap) {
                        if (log.isDebugEnabled()) {
                            DateFormat df = DateFormat.getDateTimeInstance();
                            log.debug("remember record in skip map: {} -> {}",
                                    df.format(new Date(timestamp.longValue())),
                                            lastRevision);
                        }
                        skipMap.put(timestamp, lastRevision);
                    }
                }
            } finally {
                records.close();
            }
        } catch (JournalException e) {
            log.warn("Unable to read journal records", e);
        }
    }

    /**
     * @return the revision skip map for this journal.
     */
    private SortedMap<Long, Long> getSkipMap() {
        synchronized (REVISION_SKIP_MAPS) {
            SortedMap<Long, Long> map = REVISION_SKIP_MAPS.get(journal);
            if (map == null) {
                map = new TreeMap<Long, Long>();
                REVISION_SKIP_MAPS.put(journal, map);
            }
            return map;
        }
    }

    /**
     * Simple class to associate an {@link EventState} iterator with a timestamp.
     */
    private final class EventBundle {

        /**
         * An iterator of {@link Event}s.
         */
        final EventIterator events;

        /**
         * Timestamp when the events were created.
         */
        final long timestamp;

        /**
         * Creates a new event bundle.
         *
         * @param eventStates the {@link EventState}s that belong to this bundle.
         * @param timestamp the timestamp when the events were created.
         * @param userData the user data associated with this event.
         */
        private EventBundle(
                List<EventState> eventStates, long timestamp, String userData) {
            this.events = new FilteredEventIterator(
                    session, eventStates.iterator(),
                    timestamp, userData, filter, Collections.emptySet(), true);
            this.timestamp = timestamp;
        }
    }
}
