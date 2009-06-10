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

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.data.db.DbDataStore;

/**
 * Test the Database Data Store.
 */
public class DBDataStoreTest extends TestCase {

    private DbDataStore store = new DbDataStore();

    private byte[] data = new byte[1024];

    private DataIdentifier identifier;

    protected void setUp() throws Exception {
        FileUtils.deleteQuietly(new File("target/test-db-datastore"));

        // Initialize the data store
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
