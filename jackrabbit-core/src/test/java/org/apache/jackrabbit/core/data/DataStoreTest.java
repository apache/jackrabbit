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
import org.apache.jackrabbit.core.data.db.DbDataStore;
import org.apache.jackrabbit.test.JUnitTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
