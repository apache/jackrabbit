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
import java.io.InputStream;
import java.util.Random;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.data.db.DbDataStore;
import org.apache.jackrabbit.core.util.db.ConnectionFactory;
import org.apache.jackrabbit.test.JUnitTest;

/**
 * Test the Database Data Store.
 */
public class DBDataStoreTest extends JUnitTest {

    private DbDataStore store = new DbDataStore();

    private byte[] data = new byte[1024];

    private DataIdentifier identifier;

    protected void setUp() throws Exception {
        FileUtils.deleteQuietly(new File("target/test-db-datastore"));

        // Initialize the data store
        store.setConnectionFactory(new ConnectionFactory());
        store.setUrl("jdbc:derby:target/test-db-datastore/db;create=true");
        store.setDriver("org.apache.derby.jdbc.EmbeddedDriver");
        store.init("target/test-db-datastore");

        // Initialize random test data
        new Random(1234567890).nextBytes(data);

        // Add a test record
        identifier =
            store.addRecord(new ByteArrayInputStream(data)).getIdentifier();
    }

    protected void tearDown() {

        try {
            store.close();
        } catch (DataStoreException expected) {
            // ignore
        }
    }

    public void testGetRecord() throws Exception {
        DataRecord record = store.getRecord(identifier);
        assertNotNull(record);
        assertEquals(identifier, record.getIdentifier());
        assertEquals(data.length, record.getLength());

        // read the stream twice to make sure that it can be re-read
        for (int i = 0; i < 2; i++) {
            InputStream stream = record.getStream();
            try {
                assertNotNull(stream);
                for (int j = 0; j < data.length; j++) {
                    assertEquals((data[j]) & 0xff, stream.read());
                }
                assertEquals(-1, stream.read());
            } finally {
                stream.close();
            }
        }
    }

    public void testDbInputStreamReset() throws Exception {
        DataRecord record = store.getRecord(identifier);
        InputStream in = record.getStream();
        try {
            // test whether mark and reset works
            assertTrue(in.markSupported());
            in.mark(data.length);
            while (-1 != in.read()) {
                // loop
            }
            assertTrue(in.markSupported());
            try {
                in.reset();
            } catch (Exception e) {
                fail("Unexpected exception while resetting input stream: " + e.getMessage());
            }

            // test integrity of replayed bytes
            byte[] replayedBytes = new byte[data.length];
            int length = in.read(replayedBytes);
            assertEquals(length, data.length);

            for (int i = 0; i < data.length; i++) {
                log.append(i + " data: " + data[i] + " replayed: " + replayedBytes[i] + "\n");
                assertEquals(data[i], replayedBytes[i]);
            }

            assertEquals(-1, in.read());


        } finally {
            in.close();
            log.flush();
        }
    }

    /*
    public void testDbInputStreamMarkTwice() throws Exception {
        DataRecord record = store.getRecord(identifier);
        InputStream in = record.getStream();
        try {
            // test whether mark and reset works
            assertTrue(in instanceof DbInputStream);
            assertTrue(in.markSupported());
            in.mark(data.length);

            // read first 100 bytes
            for (int i = 0; i < 100; i++) {
                in.read();
            }

            in.mark(data.length - 100);

            // read next 150 bytes
            for (int i = 0; i < 150; i++) {
                in.read();
            }

            try {
                log.append("attempting a reset()\n");
                in.reset();
            } catch (Exception e) {
                fail("Unexpected exception while resetting input stream: " + e.getMessage());
            }

            // test integrity of replayed bytes
            byte[] replayedBytes = new byte[data.length];
            int length = in.read(replayedBytes);
            assertEquals(length, data.length - 100 - 150);

            for (int i = 0; i < length; i++) {
                assertEquals(data[i + 100 + 150] & 0xff, replayedBytes[i] & 0xff);
            }

            assertTrue(-1 == in.read());
        } finally {
            in.close();
        }
    }
    */

    public void testConcurrentRead() throws Exception {
        InputStream[] streams = new InputStream[10];

        // retrieve many copies of the same record
        for (int i = 0; i < streams.length; i++) {
            streams[i] = store.getRecord(identifier).getStream();
        }

        // verify the contents of all the streams, reading them in parallel
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < streams.length; j++) {
                assertEquals((data[i]) & 0xff, streams[j].read());
            }
        }

        // close all streams
        for (int i = 0; i < streams.length; i++) {
            assertEquals(-1, streams[i].read());
            streams[i].close();
        }
    }

}
