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
package org.apache.jackrabbit.core.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.jcr.RepositoryException;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test base class which covers all scenarios.
 */
public abstract class TestCaseBase extends TestCase {

    /**
     * Logger
     */
    protected static final Logger LOG = LoggerFactory.getLogger(TestCaseBase.class);

    /**
     * temp directory
     */
    private static final String TEST_DIR = "target/temp";

    /**
     * Constant describing aws properties file path.
     */
    public static final String CONFIG = "config";

    /**
     * length of record to be added
     */
    protected int dataLength = 123456;

    /**
     * datastore directory path
     */
    protected String dataStoreDir;

    protected DataStore ds;

    /**
     * Random number generator to populate data
     */
    protected Random randomGen = new Random();

    /**
     * Delete temporary directory.
     */
    @Override
    protected void setUp() throws Exception {
        dataStoreDir = TEST_DIR + "-" + String.valueOf(randomGen.nextInt(9999))
            + "-" + String.valueOf(randomGen.nextInt(9999));
        // delete directory if it exists
        File directory = new File(dataStoreDir);
        if (directory.exists()) {
            boolean delSuccessFul = FileUtils.deleteQuietly(directory);
            int retry = 2, count = 0;
            while (!delSuccessFul && count <= retry) {
                // try once more
                delSuccessFul = FileUtils.deleteQuietly(new File(dataStoreDir));
                count++;
            }
            LOG.info("setup : directory [{}] deleted [{}]", dataStoreDir, delSuccessFul);
        }
    }

    @Override
    protected void tearDown() {
        boolean delSuccessFul = FileUtils.deleteQuietly(new File(dataStoreDir));
        int retry = 2, count = 0;
        while (!delSuccessFul && count <= retry) {
            // try once more
            delSuccessFul = FileUtils.deleteQuietly(new File(dataStoreDir));
            count++;
        }
        LOG.info("tearDown : directory [{}] deleted [{}]", dataStoreDir, delSuccessFul);
    }
    /**
     * Testcase to validate {@link DataStore#addRecord(InputStream)} API.
     */
    public void testAddRecord() {
        try {
            long start = System.currentTimeMillis();
            LOG.info("Testcase: {}#addRecord, testDir={}", getClass().getName(), dataStoreDir);
            doAddRecordTest();
            LOG.info("Testcase: {}#addRecord finished, time taken = [{}]ms", getClass().getName(), System.currentTimeMillis() - start);
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
            long start = System.currentTimeMillis();
            LOG.info("Testcase: {}#testGetRecord, testDir={}", getClass().getName(), dataStoreDir);
            doGetRecordTest();
            LOG.info("Testcase: {}#testGetRecord finished, time taken = [{}]ms", getClass().getName(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            LOG.error("error:", e);
        }
    }
    
    /**
     * Testcase to validate {@link DataStore#getAllIdentifiers()} API.
     */
    public void testGetAllIdentifiers() {
        try {
            long start = System.currentTimeMillis();
            LOG.info("Testcase: {}#testGetAllIdentifiers, testDir={}", getClass().getName(), dataStoreDir);
            doGetAllIdentifiersTest();
            LOG.info("Testcase: {}#testGetAllIdentifiers finished, time taken = [{}]ms", getClass().getName(), System.currentTimeMillis() - start);
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
            long start = System.currentTimeMillis();
            LOG.info("Testcase: {}#testUpdateLastModifiedOnAccess, testDir={}", getClass().getName(), dataStoreDir);
            doUpdateLastModifiedOnAccessTest();
            LOG.info("Testcase: {}#testUpdateLastModifiedOnAccess finished, time taken = [{}]ms", getClass().getName(), System.currentTimeMillis() - start);
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
            long start = System.currentTimeMillis();
            LOG.info("Testcase: {}#testDeleteRecord, testDir={}", getClass().getName(), dataStoreDir);
            doDeleteRecordTest();
            LOG.info("Testcase: {}#testDeleteRecord finished, time taken = [{}]ms", getClass().getName(), System.currentTimeMillis() - start);
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
            long start = System.currentTimeMillis();
            LOG.info("Testcase: {}#testDeleteAllOlderThan, testDir={}", getClass().getName(), dataStoreDir);
            doDeleteAllOlderThan();
            LOG.info("Testcase: {}#testDeleteAllOlderThan finished, time taken = [{}]ms", getClass().getName(), System.currentTimeMillis() - start);
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
            long start = System.currentTimeMillis();
            LOG.info("Testcase: {}#testReference, testDir={}", getClass().getName(), dataStoreDir);
            doReferenceTest();
            LOG.info("Testcase: {}#testReference finished, time taken = [{}]ms", getClass().getName(), System.currentTimeMillis() - start);
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
            long start = System.currentTimeMillis();
            LOG.info("Testcase: {}#testSingleThread, testDir={}", getClass().getName(), dataStoreDir);
            doTestSingleThread();
            LOG.info("Testcase: {}#testSingleThread finished, time taken = [{}]ms", getClass().getName(), System.currentTimeMillis() - start);
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
            long start = System.currentTimeMillis();
            LOG.info("Testcase: {}#testMultiThreaded, testDir={}", getClass().getName(), dataStoreDir);
            doTestMultiThreaded();
            LOG.info("Testcase: {}#testMultiThreaded finished, time taken = [{}]ms", getClass().getName(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            LOG.error("error:", e);
            fail(e.getMessage());
        }

    }

    protected abstract DataStore createDataStore() throws RepositoryException ;

    /**
     * Test {@link DataStore#addRecord(InputStream)} and assert length of added
     * record.
     */
    protected void doAddRecordTest() throws Exception {
        ds = createDataStore();
        byte[] data = new byte[dataLength];
        randomGen.nextBytes(data);
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
        ds = createDataStore();
        byte[] data = new byte[dataLength];
        randomGen.nextBytes(data);
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
        ds = createDataStore();
        Random random = randomGen;
        byte[] data1 = new byte[dataLength];
        random.nextBytes(data1);
        DataRecord rec1 = ds.addRecord(new ByteArrayInputStream(data1));

        byte[] data2 = new byte[dataLength];
        random.nextBytes(data2);
        DataRecord rec2 = ds.addRecord(new ByteArrayInputStream(data2));

        byte[] data3 = new byte[dataLength];
        random.nextBytes(data3);
        DataRecord rec3 = ds.addRecord(new ByteArrayInputStream(data3));

        ((MultiDataStoreAware)ds).deleteRecord(rec2.getIdentifier());

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
        ds = createDataStore();
        List<DataIdentifier> list = new ArrayList<DataIdentifier>();
        Random random = randomGen;
        byte[] data = new byte[dataLength];
        random.nextBytes(data);
        DataRecord rec = ds.addRecord(new ByteArrayInputStream(data));
        list.add(rec.getIdentifier());

        data = new byte[dataLength];
        random.nextBytes(data);
        rec = ds.addRecord(new ByteArrayInputStream(data));
        list.add(rec.getIdentifier());

        data = new byte[dataLength];
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
        ds = createDataStore();
        Random random = randomGen;
        byte[] data = new byte[dataLength];
        random.nextBytes(data);
        DataRecord rec1 = ds.addRecord(new ByteArrayInputStream(data));

        data = new byte[dataLength];
        random.nextBytes(data);
        DataRecord rec2 = ds.addRecord(new ByteArrayInputStream(data));
        LOG.debug("rec2 timestamp={}", rec2.getLastModified());

        // sleep for some time to ensure that async upload completes in backend.
        sleep(6000);
        long updateTime = System.currentTimeMillis();
        LOG.debug("updateTime={}", updateTime);
        ds.updateModifiedDateOnAccess(updateTime);

        // sleep to workaround System.currentTimeMillis granularity.
        sleep(3000);
        data = new byte[dataLength];
        random.nextBytes(data);
        DataRecord rec3 = ds.addRecord(new ByteArrayInputStream(data));

        data = new byte[dataLength];
        random.nextBytes(data);
        DataRecord rec4 = ds.addRecord(new ByteArrayInputStream(data));

        rec1 = ds.getRecord(rec1.getIdentifier());

        assertEquals("rec1 touched", true, rec1.getLastModified() > updateTime);
        LOG.debug("rec2 timestamp={}", rec2.getLastModified());
        assertEquals("rec2 not touched", true,
            rec2.getLastModified() < updateTime);
        assertEquals("rec3 touched", true, rec3.getLastModified() > updateTime);
        assertEquals("rec4 touched", true, rec4.getLastModified() > updateTime);
        ds.close();

    }

    /**
     * Asserts that {@link DataStore#deleteAllOlderThan(long)} only deleted
     * records older than argument passed.
     */
    protected void doDeleteAllOlderThan() throws Exception {
        ds = createDataStore();
        Random random = randomGen;
        byte[] data = new byte[dataLength];
        random.nextBytes(data);
        DataRecord rec1 = ds.addRecord(new ByteArrayInputStream(data));

        data = new byte[dataLength];
        random.nextBytes(data);
        DataRecord rec2 = ds.addRecord(new ByteArrayInputStream(data));

        // sleep for some time to ensure that async upload completes in backend.
        sleep(10000);
        long updateTime = System.currentTimeMillis();
        ds.updateModifiedDateOnAccess(updateTime);
        
        // sleep to workaround System.currentTimeMillis granularity.
        sleep(3000);
        data = new byte[dataLength];
        random.nextBytes(data);
        DataRecord rec3 = ds.addRecord(new ByteArrayInputStream(data));

        data = new byte[dataLength];
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
        assertEquals("rec1 touched", true, rec1.getLastModified() > updateTime);
        assertEquals("rec3 touched", true, rec3.getLastModified() > updateTime);
        assertEquals("rec4 touched", true, rec4.getLastModified() > updateTime);
        ds.close();
    }

    /**
     * Test if record can be accessed via
     * {@link DataStore#getRecordFromReference(String)}
     */
    protected void doReferenceTest() throws Exception {
        ds = createDataStore();
        byte[] data = new byte[dataLength];
        randomGen.nextBytes(data);
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
        ds = createDataStore();
        doTestMultiThreaded(ds, 1);
        ds.close();
    }

    /**
     * Method to validate mixed scenario use of {@link DataStore} in
     * multi-threaded concurrent environment.
     */
    protected void doTestMultiThreaded() throws Exception {
        ds = createDataStore();
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
        ArrayList<DataRecord> list = new ArrayList<DataRecord>();
        HashMap<DataRecord, Integer> map = new HashMap<DataRecord, Integer>();
        for (int i = 0; i < 10; i++) {
            int size = 100000 - (i * 100);
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
            RandomInputStream expected = new RandomInputStream(size + offset,
                size);
            InputStream in = rec.getStream();
            // Workaround for race condition that can happen with low cache size relative to the test
            // read immediately
            byte[] buffer = new byte[1];
            in.read(buffer);
            in = new SequenceInputStream(new ByteArrayInputStream(buffer), in);

            if (random.nextBoolean()) {
                in = readInputStreamRandomly(in, random);
            }
            assertEquals(expected, in);
        }
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
        try {
            assertTrue("binary not equal",
                org.apache.commons.io.IOUtils.contentEquals(a, b));
        } finally {
            try {
                a.close();
            } catch (Exception ignore) {
            }
            try {
                b.close();
            } catch (Exception ignore) {
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
            assertTrue("binary not equal",
                org.apache.commons.io.IOUtils.contentEquals(
                    new ByteArrayInputStream(expected), stream));
        } finally {
            stream.close();
        }
    }
    
    /**
     * Utility method to stop execution for duration time.
     * 
     * @param duration
     *            time in milli seconds
     */
    protected void sleep(long duration) {
        long expected = System.currentTimeMillis() + duration;
        while (System.currentTimeMillis() < expected) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ie) {

            }
        }
    }
    
    /**
     * Return {@link Properties} from class resource. Return empty
     * {@link Properties} if not found.
     */
    protected Properties loadProperties(String resource) {
        Properties configProp = new Properties();
        try {
            configProp.load(this.getClass().getResourceAsStream(resource));
        } catch (Exception ignore) {

        }
        return configProp;
    }
}
