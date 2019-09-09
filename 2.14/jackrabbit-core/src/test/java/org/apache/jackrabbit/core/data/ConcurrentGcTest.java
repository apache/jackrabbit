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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.data.db.DbDataStore;
import org.apache.jackrabbit.core.util.db.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests concurrent garbage collection, see JCR-2026
 */
public class ConcurrentGcTest extends TestCase {

    static final Logger LOG = LoggerFactory.getLogger(ConcurrentGcTest.class);

    private static final String TEST_DIR = "target/ConcurrentGcTest";

    protected DataStore store;
    private Thread gcLoopThread;

    protected Set<DataIdentifier> ids = Collections.synchronizedSet(new HashSet<DataIdentifier>());

    protected volatile boolean gcLoopStop;
    protected volatile Exception gcException;

    public void setUp() throws IOException {
        deleteAll();
    }

    public void tearDown() throws IOException {
        deleteAll();
    }

    private void deleteAll() throws IOException {
        FileUtils.deleteDirectory(new File(TEST_DIR));
    }

    public void testDatabases() throws Exception {
//        doTestDatabase(
//                "org.h2.Driver",
//                "jdbc:h2:" + TEST_DIR + "/db",
//                "sa", "sa");

        // not enabled by default
        // doTestDatabase(
        //        "org.postgresql.Driver",
        //        "jdbc:postgresql:test",
        //        "sa", "sa");

        // not enabled by default
        // doTestDatabase(
        //         "com.mysql.jdbc.Driver",
        //         "jdbc:postgresql:test",
        //         "sa", "sa");

        // fails with a deadlock
        // doTestDatabase(
        //        "org.apache.derby.jdbc.EmbeddedDriver",
        //        "jdbc:derby:" + TEST_DIR + "/db;create=true",
        //        "sa", "sa");
    }

    private void doTestDatabase(String driver, String url, String user, String password) throws Exception {
        ConnectionFactory pool = new ConnectionFactory();
        try {
            DbDataStore store = new DbDataStore();
            store.setConnectionFactory(pool);

            ids.clear();

            store.setDriver(driver);
            store.setUrl(url);
            store.setUser(user);
            store.setPassword(password);

            store.init("target/test-db-datastore");
            store.setMinRecordLength(0);
            doTest(store);
        } finally {
            pool.close();
        }
    }

    public void testFile() throws Exception {
        FileDataStore store = new FileDataStore();
        store.setPath(TEST_DIR + "/fs");
        store.init(TEST_DIR + "/fs");
        store.setMinRecordLength(0);
        doTest(store);
    }

    void doTest(DataStore store) throws Exception {
        this.store = store;

        Random r = new Random();

        concurrentGcLoopStart();

        int len = 100;
        if (getTestScale() > 1) {
            len = 1000;
        }

        for (int i = 0; i < len && gcException == null; i++) {
            LOG.info("test " + i);
            byte[] data = new byte[3];
            r.nextBytes(data);
            DataRecord rec = store.addRecord(new ByteArrayInputStream(data));
            LOG.debug("  added " + rec.getIdentifier());
            if (r.nextBoolean()) {
                LOG.debug("  added " + rec.getIdentifier() + " -> keep reference");
                ids.add(rec.getIdentifier());
                store.getRecord(rec.getIdentifier());
            }
            if (r.nextInt(100) == 0) {
                LOG.debug("clear i: " + i);
                ids.clear();
            }
        }
        concurrentGcLoopStop();
        store.close();
    }

    private void concurrentGcLoopStart() {
        gcLoopStop = false;
        gcException = null;

        gcLoopThread = new Thread() {
            public void run() {
                try {
                    while (!gcLoopStop) {
                        if (ids.size() > 0) {
                            // store.clearInUse();
                            long now = System.currentTimeMillis();
                            LOG.debug("gc now: " + now);
                            store.updateModifiedDateOnAccess(now);
                            for (DataIdentifier id : new ArrayList<DataIdentifier>(ids)) {
                                LOG.debug("   gc touch " + id);
                                store.getRecord(id);
                            }
                            int count = store.deleteAllOlderThan(now);
                            LOG.debug("gc now: " + now + " done, deleted: " + count);
                        }
                    }
                } catch (DataStoreException e) {
                    gcException = e;
                }
            }
        };
        gcLoopThread.start();
    }

    private void concurrentGcLoopStop() throws Exception {
        gcLoopStop = true;
        gcLoopThread.join();
        if (gcException != null) {
            throw gcException;
        }
    }

    static int getTestScale() {
        return Integer.parseInt(System.getProperty("jackrabbit.test.scale", "1"));
    }

}
