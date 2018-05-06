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

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.api.JackrabbitValue;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test concurrent reads to the data store.
 */
public class TestTwoGetStreams extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(TestTwoGetStreams.class);

    private static final int STREAM_LENGTH = 256 * 1024;

    /**
     * Test the JackrabbitValue.getContentIdentity feature.
     */
    public void testContentIdentity() throws Exception {
        Node root = superuser.getRootNode();
        ValueFactory vf = superuser.getValueFactory();

        long time = System.currentTimeMillis();
        root.setProperty("p1", vf.createBinary(new RandomInputStream(1, STREAM_LENGTH)));
        superuser.save();
        long saveOne = System.currentTimeMillis() - time;

        root.setProperty("p2", vf.createBinary(new RandomInputStream(1, STREAM_LENGTH)));
        superuser.save();

        Value v1 = root.getProperty("p1").getValue();
        Value v2 = root.getProperty("p2").getValue();
        if (v1 instanceof JackrabbitValue && v2 instanceof JackrabbitValue) {
            JackrabbitValue j1 = (JackrabbitValue) v1;
            JackrabbitValue j2 = (JackrabbitValue) v2;
            String id1 = j1.getContentIdentity();
            String id2 = j2.getContentIdentity();
            assertNotNull(id1);
            assertEquals(id1, id2);
        }

        // copying a value should not stream the content
        time = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            Value v = root.getProperty("p1").getValue();
            root.setProperty("p3", v);
        }
        superuser.save();
        time = System.currentTimeMillis() - time;
        // streaming 1 MB again and again takes about 4.3 seconds
        // on my computer; copying the data identifier takes about 16 ms
        // here we test if copying 100 objects took less than saving 50 new objects
        assertTrue("time: " + time, time < saveOne * 50);

    }

    /**
     * Test reading from two concurrently opened streams.
     */
    public void testTwoGetStreams() throws Exception {
        Node root = superuser.getRootNode();
        ValueFactory vf = superuser.getValueFactory();
        root.setProperty("p1", vf.createBinary(new RandomInputStream(1, STREAM_LENGTH)));
        root.setProperty("p2", vf.createBinary(new RandomInputStream(2, STREAM_LENGTH)));
        superuser.save();

        InputStream i1 = root.getProperty("p1").getBinary().getStream();
        InputStream i2 = root.getProperty("p2").getBinary().getStream();
        assertEquals("p1", i1, new RandomInputStream(1, STREAM_LENGTH));
        assertEquals("p2", i2, new RandomInputStream(2, STREAM_LENGTH));
        try {
            i1.close();
        } catch (IOException e) {
            log.info("Could not close first input stream: ", e);
        }
        try {
            i2.close();
        } catch (IOException e) {
            log.info("Could not close second input stream: ", e);
        }
    }

    /**
     * Tests reading concurrently from two different streams.
     */
    public void testTwoStreamsConcurrently() throws Exception {
        Node root = superuser.getRootNode();
        ValueFactory vf = superuser.getValueFactory();
        root.setProperty("p1", vf.createBinary(new RandomInputStream(1, STREAM_LENGTH)));
        root.setProperty("p2", vf.createBinary(new RandomInputStream(1, STREAM_LENGTH)));
        superuser.save();

        InputStream i1 = root.getProperty("p1").getBinary().getStream();
        InputStream i2 = root.getProperty("p2").getBinary().getStream();
        assertEquals("Streams are different", i1, i2);
        try {
            i1.close();
        } catch (IOException e) {
            log.info("Could not close first input stream: ", e);
        }
        try {
            i2.close();
        } catch (IOException e) {
            log.info("Could not close second input stream: ", e);
        }
    }

    /**
     * Tests reading concurrently from two different streams coming from the
     * same property.
     */
    public void testTwoStreamsFromSamePropertyConcurrently() throws Exception {
        Node root = superuser.getRootNode();
        ValueFactory vf = superuser.getValueFactory();
        root.setProperty("p1", vf.createBinary(new RandomInputStream(1, STREAM_LENGTH)));
        superuser.save();

        InputStream i1 = root.getProperty("p1").getBinary().getStream();
        InputStream i2 = root.getProperty("p1").getBinary().getStream();
        assertEquals("Streams are different", i1, i2);
        try {
            i1.close();
        } catch (IOException e) {
            log.info("Could not close first input stream: ", e);
        }
        try {
            i2.close();
        } catch (IOException e) {
            log.info("Could not close second input stream: ", e);
        }
    }

    /**
     * Asserts that two input streams are equal.
     */
    protected void assertEquals(String message, InputStream i1, InputStream i2) {
        try {
            int b1 = 0, b2 = 0;
            int i = 0;
            while (b1 != -1 || b2 != -1) {
                b1 = i1.read();
                b2 = i2.read();
                assertEquals(message + "; byte #" + i + " mismatch!", b2, b1);
                ++i;
            }
        } catch (Exception e) {
            fail("Could not read inputstream! " + e);
        }
    }
}
