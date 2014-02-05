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
package org.apache.jackrabbit.aws.ext.ds;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.jcr.RepositoryException;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.data.CachingDataStore;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataRecord;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.apache.jackrabbit.core.data.RandomInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test base class which covers all scenarios.
 */
public abstract class TestCaseBase extends TestCase {
    
    /**
     * Constant describing aws properties file path.
     */
    public static final String CONFIG = "config";
    
    /**
     * Logger
     */
    protected static final Logger LOG = LoggerFactory.getLogger(TestCaseBase.class);

    /**
     * temp directory
     */
    private static final String TEST_DIR = "target/temp";

    /**
     * File path of aws properties.
     */
    protected String config;

    /**
     * Parameter to use in-memory backend. If false {@link S3Backend}
     */
    protected boolean memoryBackend = true;

    /**
     * Parameter to use local cache. If true local cache {@link LocalCache} is
     * not used.
     */
    protected boolean noCache;

    /**
     * Delete temporary directory.
     */
    @Override
    protected void setUp() {
        FileUtils.deleteQuietly(new File(TEST_DIR));
    }

    /**
     * Delete temporary directory.
     */
    @Override
    protected void tearDown() throws IOException {
        FileUtils.deleteQuietly(new File(TEST_DIR));
    }

    /**
     * Testcase to validate {@link DataStore#addRecord(InputStream)} API.
     */
    public void testAddRecord() {
        try {
            doAddRecordTest();
        } catch (Exception e) {
            LOG.error("error:", e);
            fail(e.getMessage());
        }
    }

    /**
     * Testcase to validate {@link DataStore#getRecord(DataIdentifier)} API.
     */
    public void testGetRecord() {
        try {
            doGetRecordTest();
        } catch (Exception e) {
            LOG.error("error:", e);
        }
    }

    /**
     * Testcase to validate {@link DataStore#getAllIdentifiers()} API.
     */
    public void testGetAllIdentifiers() {
        try {
            doGetAllIdentifiersTest();
        } catch (Exception e) {
            LOG.error("error:", e);
            fail(e.getMessage());
        }
    }

    /**
     * Testcase to validate {@link DataStore#updateModifiedDateOnAccess(long)}
     * API.
     */
    public void testUpdateLastModifiedOnAccess() {
        try {
            doUpdateLastModifiedOnAccessTest();
        } catch (Exception e) {
            LOG.error("error:", e);
        }
    }

    /**
     * Testcase to validate
     * {@link MultiDataStoreAware#deleteRecord(DataIdentifier)}.API.
     */
    public void testDeleteRecord() {
        try {
            doDeleteRecordTest();
        } catch (Exception e) {
            LOG.error("error:", e);
            fail(e.getMessage());
        }
    }

    /**
     * Testcase to validate {@link DataStore#deleteAllOlderThan(long)} API.
     */
    public void testDeleteAllOlderThan() {
        try {
            doDeleteAllOlderThan();
        } catch (Exception e) {
            LOG.error("error:", e);
            fail(e.getMessage());
        }
    }

    /**
     * Testcase to validate {@link DataStore#getRecordFromReference(String)}
     */
    public void testReference() {
        try {
            doReferenceTest();
        } catch (Exception e) {
            LOG.error("error:", e);
            fail(e.getMessage());
        }
    }

    /**
     * Testcase to validate mixed scenario use of {@link DataStore}.
     */
    public void testSingleThread() {
        try {
            doTestSingleThread();
        } catch (Exception e) {
            LOG.error("error:", e);
            fail(e.getMessage());
        }
    }

    /**
     * Testcase to validate mixed scenario use of {@link DataStore} in
     * multi-threaded concurrent environment.
     */
    public void testMultiThreaded() {
        try {
            doTestMultiThreaded();
        } catch (Exception e) {
            LOG.error("error:", e);
            fail(e.getMessage());
        }

    }
    
    private CachingDataStore createDataStore() throws RepositoryException {
        CachingDataStore ds = memoryBackend
                ? new InMemoryDataStore()
                : new S3DataStore();
        ds.setConfig(config);
        if (noCache) {
            ds.setCacheSize(0);
        }
        ds.init(TEST_DIR);
        return ds;
    }
    
    /**
     * Test {@link DataStore#addRecord(InputStream)} and assert length of added
     * record.
     */
    protected void doAddRecordTest() throws Exception {
        CachingDataStore ds = createDataStore();
        byte[] data = new byte[12345];
        new Random(12345).nextBytes(data);
        DataRecord rec = ds.addRecord(new ByteArrayInputStream(data));
        assertEquals(data.length, rec.getLength());
        assertRecord(data, rec);
        ds.close();
    }

    /**
     * Test {@link DataStore#getRecord(DataIdentifier)} and assert length and
     * inputstream.
     */
    protected void doGetRecordTest() throws Exception {
        CachingDataStore ds = createDataStore();
        byte[] data = new byte[12345];
        new Random(12345).nextBytes(data);
        DataRecord rec = ds.addRecord(new ByteArrayInputStream(data));
        rec = ds.getRecord(rec.getIdentifier());
        assertEquals(data.length, rec.getLength());
        assertRecord(data, rec);
        ds.close();
    }

    /**
     * Test {@link MultiDataStoreAware#deleteRecord(DataIdentifier)}.
     */
    protected void doDeleteRecordTest() throws Exception {
        CachingDataStore ds = createDataStore();
        Random random = new Random(12345);
        byte[] data1 = new byte[12345];
        random.nextBytes(data1);
        DataRecord rec1 = ds.addRecord(new ByteArrayInputStream(data1));

        byte[] data2 = new byte[12345];
        random.nextBytes(data2);
        DataRecord rec2 = ds.addRecord(new ByteArrayInputStream(data2));

        byte[] data3 = new byte[12345];
        random.nextBytes(data3);
        DataRecord rec3 = ds.addRecord(new ByteArrayInputStream(data3));

        ds.deleteRecord(rec2.getIdentifier());

        assertNull("rec2 should be null",
            ds.getRecordIfStored(rec2.getIdentifier()));
        assertEquals(new ByteArrayInputStream(data1),
            ds.getRecord(rec1.getIdentifier()).getStream());
        assertEquals(new ByteArrayInputStream(data3),
            ds.getRecord(rec3.getIdentifier()).getStream());
        ds.close();
    }

    /**
     * Test {@link DataStore#getAllIdentifiers()} and asserts all identifiers
     * are returned.
     */
    protected void doGetAllIdentifiersTest() throws Exception {
        CachingDataStore ds = createDataStore();
        List<DataIdentifier> list = new ArrayList<DataIdentifier>();
        Random random = new Random(12345);
        byte[] data = new byte[12345];
        random.nextBytes(data);
        DataRecord rec = ds.addRecord(new ByteArrayInputStream(data));
        list.add(rec.getIdentifier());

        data = new byte[12345];
        random.nextBytes(data);
        rec = ds.addRecord(new ByteArrayInputStream(data));
        list.add(rec.getIdentifier());

        data = new byte[12345];
        random.nextBytes(data);
        rec = ds.addRecord(new ByteArrayInputStream(data));
        list.add(rec.getIdentifier());

        Iterator<DataIdentifier> itr = ds.getAllIdentifiers();
        while (itr.hasNext()) {
            assertTrue("record found on list", list.remove(itr.next()));
        }
        assertEquals(0, list.size());
        ds.close();
    }

    /**
     * Asserts that timestamp of all records accessed after
     * {@link DataStore#updateModifiedDateOnAccess(long)} invocation.
     */
    protected void doUpdateLastModifiedOnAccessTest() throws Exception {
        CachingDataStore ds = createDataStore();
        Random random = new Random(12345);
        byte[] data = new byte[12345];
        random.nextBytes(data);
        DataRecord rec1 = ds.addRecord(new ByteArrayInputStream(data));

        data = new byte[12345];
        random.nextBytes(data);
        DataRecord rec2 = ds.addRecord(new ByteArrayInputStream(data));

        Thread.sleep(1000);
        long updateTime = System.currentTimeMillis();
        ds.updateModifiedDateOnAccess(updateTime);
        Thread.sleep(1000);
        data = new byte[12345];
        random.nextBytes(data);
        DataRecord rec3 = ds.addRecord(new ByteArrayInputStream(data));

        data = new byte[12345];
        random.nextBytes(data);
        DataRecord rec4 = ds.addRecord(new ByteArrayInputStream(data));

        rec1 = ds.getRecord(rec1.getIdentifier());

        assertEquals("rec1 touched", true,
            ds.getLastModified(rec1.getIdentifier()) > updateTime);
        assertEquals("rec2 not touched", true,
            ds.getLastModified(rec2.getIdentifier()) < updateTime);
        assertEquals("rec3 touched", true,
            ds.getLastModified(rec3.getIdentifier()) > updateTime);
        assertEquals("rec4 touched", true,
            ds.getLastModified(rec4.getIdentifier()) > updateTime);
        ds.close();

    }

    /**
     * Asserts that {@link DataStore#deleteAllOlderThan(long)} only deleted
     * records older than argument passed.
     */
    protected void doDeleteAllOlderThan() throws Exception {
        CachingDataStore ds = createDataStore();
        Random random = new Random(12345);
        byte[] data = new byte[12345];
        random.nextBytes(data);
        DataRecord rec1 = ds.addRecord(new ByteArrayInputStream(data));

        data = new byte[12345];
        random.nextBytes(data);
        DataRecord rec2 = ds.addRecord(new ByteArrayInputStream(data));
        
        Thread.sleep(2000);
        long updateTime = System.currentTimeMillis();
        ds.updateModifiedDateOnAccess(updateTime);

        data = new byte[12345];
        random.nextBytes(data);
        DataRecord rec3 = ds.addRecord(new ByteArrayInputStream(data));

        data = new byte[12345];
        random.nextBytes(data);
        DataRecord rec4 = ds.addRecord(new ByteArrayInputStream(data));

        rec1 = ds.getRecord(rec1.getIdentifier());
        ds.clearInUse();
        assertEquals("only rec2 should be deleted", 1,
            ds.deleteAllOlderThan(updateTime));
        assertNull("rec2 should be null",
            ds.getRecordIfStored(rec2.getIdentifier()));

        Iterator<DataIdentifier> itr = ds.getAllIdentifiers();
        List<DataIdentifier> list = new ArrayList<DataIdentifier>();
        list.add(rec1.getIdentifier());
        list.add(rec3.getIdentifier());
        list.add(rec4.getIdentifier());
        while (itr.hasNext()) {
            assertTrue("record found on list", list.remove(itr.next()));
        }

        assertEquals("touched records found", 0, list.size());
        assertEquals("rec1 touched", true,
            ds.getLastModified(rec1.getIdentifier()) > updateTime);
        assertEquals("rec3 touched", true,
            ds.getLastModified(rec3.getIdentifier()) > updateTime);
        assertEquals("rec4 touched", true,
            ds.getLastModified(rec4.getIdentifier()) > updateTime);
        ds.close();

    }

    /**
     * Test if record can be accessed via
     * {@link DataStore#getRecordFromReference(String)}
     */
    public void doReferenceTest() throws Exception {
        CachingDataStore ds = createDataStore();
        ds.setSecret("12345");
        byte[] data = new byte[12345];
        new Random(12345).nextBytes(data);
        String reference;
        DataRecord record = ds.addRecord(new ByteArrayInputStream(data));
        reference = record.getReference();
        assertReference(data, reference, ds);
        ds.close();
    }

    /**
     * Method to validate mixed scenario use of {@link DataStore}.
     */
    protected void doTestSingleThread() throws Exception {
        CachingDataStore ds = createDataStore();
        doTestMultiThreaded(ds, 1);
        ds.close();
    }

    /**
     * Method to validate mixed scenario use of {@link DataStore} in
     * multi-threaded concurrent environment.
     */
    protected void doTestMultiThreaded() throws Exception {
        CachingDataStore ds = createDataStore();
        doTestMultiThreaded(ds, 4);
        ds.close();
    }

    /**
     * Method to assert record with byte array.
     */
    protected void assertRecord(byte[] expected, DataRecord record)
            throws DataStoreException, IOException {
        InputStream stream = record.getStream();
        try {
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i] & 0xff, stream.read());
            }
            assertEquals(-1, stream.read());
        } finally {
            stream.close();
        }
    }

    /**
     * Method to run {@link TestCaseBase#doTest(DataStore, int)} in multiple
     * concurrent threads.
     */
    protected void doTestMultiThreaded(final DataStore ds, int threadCount)
            throws Exception {
        final Exception[] exception = new Exception[1];
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int x = i;
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        doTest(ds, x);
                    } catch (Exception e) {
                        exception[0] = e;
                    }
                }
            };
            threads[i] = t;
            t.start();
        }
        for (int i = 0; i < threadCount; i++) {
            threads[i].join();
        }
        if (exception[0] != null) {
            throw exception[0];
        }
    }

    /**
     * Assert randomly read stream from record.
     */
    void doTest(DataStore ds, int offset) throws Exception {
        LOG.info(Thread.currentThread().getName() + " started.");
        ArrayList<DataRecord> list = new ArrayList<DataRecord>();
        HashMap<DataRecord, Integer> map = new HashMap<DataRecord, Integer>();
        for (int i = 0; i < 100; i++) {
            int size = 100 + i * 10;
            RandomInputStream in = new RandomInputStream(size + offset, size);
            DataRecord rec = ds.addRecord(in);
            list.add(rec);
            map.put(rec, new Integer(size));
        }
        Random random = new Random(1);
        for (int i = 0; i < list.size(); i++) {
            int pos = random.nextInt(list.size());
            DataRecord rec = list.get(pos);
            int size = map.get(rec);
            rec = ds.getRecord(rec.getIdentifier());
            assertEquals(size, rec.getLength());
            InputStream in = rec.getStream();
            RandomInputStream expected = new RandomInputStream(size + offset,
                size);
            if (random.nextBoolean()) {
                in = readInputStreamRandomly(in, random);
            }
            assertEquals(expected, in);
            in.close();
        }
        LOG.info(Thread.currentThread().getName() + " finished.");
    }

    InputStream readInputStreamRandomly(InputStream in, Random random)
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8000];
        while (true) {
            if (random.nextBoolean()) {
                int x = in.read();
                if (x < 0) {
                    break;
                }
                out.write(x);
            } else {
                if (random.nextBoolean()) {
                    int l = in.read(buffer);
                    if (l < 0) {
                        break;
                    }
                    out.write(buffer, 0, l);
                } else {
                    int offset = random.nextInt(buffer.length / 2);
                    int len = random.nextInt(buffer.length / 2);
                    int l = in.read(buffer, offset, len);
                    if (l < 0) {
                        break;
                    }
                    out.write(buffer, offset, l);
                }
            }
        }
        in.close();
        return new ByteArrayInputStream(out.toByteArray());
    }

    /**
     * Assert two inputstream
     */
    protected void assertEquals(InputStream a, InputStream b)
            throws IOException {
        while (true) {
            int ai = a.read();
            int bi = b.read();
            assertEquals(ai, bi);
            if (ai < 0) {
                break;
            }
        }
    }

    /**
     * Assert inputstream read from reference.
     */
    protected void assertReference(byte[] expected, String reference,
            DataStore store) throws Exception {
        DataRecord record = store.getRecordFromReference(reference);
        assertNotNull(record);
        assertEquals(expected.length, record.getLength());

        InputStream stream = record.getStream();
        try {
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i] & 0xff, stream.read());
            }
            assertEquals(-1, stream.read());
        } finally {
            stream.close();
        }
    }

}
