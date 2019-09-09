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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.data.db.DbDataStore;
import org.apache.jackrabbit.test.JUnitTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Tests for the data store.
 * Both file data store and database data store are tested,
 * with single threaded and multi-threaded tests.
 */
public class DataStoreTest extends JUnitTest {

    private static final boolean TEST_DATABASE = false;
    private File testDir = new File(System.getProperty("java.io.tmpdir"), "dataStore");

    public void setUp() {
        testDir.mkdirs();
    }

    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(testDir);
    }

    public void test() throws Exception {
        try {

            if (TEST_DATABASE) {
                DbDataStore dds = new DbDataStore();
                String dbPath = (testDir + "/db").replace('\\', '/');

                // 3 sec
                String url = "jdbc:h2:mem:" + dbPath + "/db";

                // 4 sec
                // String url = "jdbc:h2:" + dbPath + "/db";

                // 26 sec
                // String url = "jdbc:derby:" + dbPath + "/db";

                new File(dbPath).mkdirs();
                dds.setUrl(url + ";create=true");
                dds.setUser("sa");
                dds.setPassword("sa");
                dds.setCopyWhenReading(false);
                dds.init(dbPath);
                // doTest(dds, 0);
                doTestMultiThreaded(dds, 4);
                dds.close();
                shutdownDatabase(url);

                FileUtils.deleteDirectory(testDir);
                new File(dbPath).mkdirs();
                dds = new DbDataStore();
                dds.setUrl(url + ";create=true");
                dds.setUser("sa");
                dds.setPassword("sa");
                dds.setCopyWhenReading(true);
                dds.init(dbPath);
                // doTest(dds, 0);
                doTestMultiThreaded(dds, 4);
                dds.close();
                shutdownDatabase(url);
            }

            FileDataStore fds = new FileDataStore();
            fds.init(testDir + "/file");
            doTest(fds, 0);
            // doTestMultiThreaded(fds, 4);
            fds.close();

        } catch (Throwable t) {
            t.printStackTrace();
            throw new Error(t);
        }
    }

    public static void main(String... args) throws NoSuchAlgorithmException {
        // create and print a "directory-collision", that is, two byte arrays
        // where the hash starts with the same bytes
        // those values can be used for testDeleteRecordWithParentCollision
        HashMap<Long, Long> map = new HashMap<Long, Long>();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        ByteBuffer input = ByteBuffer.allocate(8);
        byte[] array = input.array();
        for(long x = 0;; x++) {
            input.putLong(x).flip();
            long h = ByteBuffer.wrap(digest.digest(array)).getLong();
            Long old = map.put(h & 0xffffffffff000000L, x);
            if (old != null) {
                System.out.println(Long.toHexString(old) + " " + Long.toHexString(x));
                break;
            }
        }
    }

    public void testDeleteRecordWithParentCollision() throws Exception {
        FileDataStore fds = new FileDataStore();
        fds.init(testDir + "/fileDeleteCollision");

        ByteArrayInputStream c1 = new ByteArrayInputStream(ByteBuffer
                .allocate(8).putLong(0x181c7).array());
        ByteArrayInputStream c2 = new ByteArrayInputStream(ByteBuffer
                .allocate(8).putLong(0x11fd78).array());
        DataRecord d1 = fds.addRecord(c1);
        DataRecord d2 = fds.addRecord(c2);
        fds.deleteRecord(d1.getIdentifier());
        DataRecord testRecord = fds.getRecordIfStored(d2.getIdentifier());

        assertNotNull(testRecord);
        assertEquals(d2.getIdentifier(), testRecord.getIdentifier());
        // Check the presence of the parent directory (relies on internal details of the FileDataStore)
        File parentDirD1 = new File(
            fds.getPath() + System.getProperty("file.separator") + d1.getIdentifier().toString().substring(0, 2));
        assertTrue(parentDirD1.exists());
    }

    public void testDeleteRecordWithoutParentCollision() throws Exception {
        FileDataStore fds = new FileDataStore();
        fds.init(testDir + "/fileDelete");

        String c1 = "idhfigjhehgkdfgk";
        String c2 = "02c60cb75083ceef";
        DataRecord d1 = fds.addRecord(IOUtils.toInputStream(c1));
        DataRecord d2 = fds.addRecord(IOUtils.toInputStream(c2));
        fds.deleteRecord(d1.getIdentifier());
        DataRecord testRecord = fds.getRecordIfStored(d2.getIdentifier());

        assertNotNull(testRecord);
        assertEquals(d2.getIdentifier(), testRecord.getIdentifier());
        // Check the absence of the parent directory (relies on internal details of the FileDataStore)
        File parentDirD1 = new File(
            fds.getPath() + System.getProperty("file.separator") + d1.getIdentifier().toString().substring(0, 2));
        assertFalse(parentDirD1.exists());
    }

    public void testReference() throws Exception {
        byte[] data = new byte[12345];
        new Random(12345).nextBytes(data);
        String reference;

        FileDataStore store = new FileDataStore();
        store.init(testDir + "/reference");
        try {
            DataRecord record = store.addRecord(new ByteArrayInputStream(data));
            reference = record.getReference();

            assertReference(data, reference, store);
        } finally {
            store.close();
        }

        store = new FileDataStore();
        store.init(testDir + "/reference");
        try {
            assertReference(data, reference, store);
        } finally {
            store.close();
        }
    }

    private void assertReference(
            byte[] expected, String reference, DataStore store)
            throws Exception {
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

    private void shutdownDatabase(String url) {
        if (url.startsWith("jdbc:derby:") || url.startsWith("jdbc:hsqldb:")) {
            try {
                DriverManager.getConnection(url + ";shutdown=true");
            } catch (SQLException e) {
                // ignore
            }
        }
    }

    private void doTestMultiThreaded(final DataStore ds, int threadCount) throws Exception {
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

    void doTest(DataStore ds, int offset) throws Exception {
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
            RandomInputStream expected = new RandomInputStream(size + offset, size);
            if (random.nextBoolean()) {
                in = readInputStreamRandomly(in, random);
            }
            assertEquals(expected, in);
            in.close();
        }
    }

    InputStream readInputStreamRandomly(InputStream in, Random random) throws IOException {
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

    void assertEquals(InputStream a, InputStream b) throws IOException {
        while (true) {
            int ai = a.read();
            int bi = b.read();
            assertEquals(ai, bi);
            if (ai < 0) {
                break;
            }
        }
    }

}
