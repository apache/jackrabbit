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
import javax.jcr.Session;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test concurrent reads to the data store.
 */
public class TestTwoGetStreams extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(TestTwoGetStreams.class);

    private static final int STREAM_LENGTH = 1 * 1024 * 1024;

    /**
     * Test reading from two concurrently opened streams.
     */
    public void testTwoGetStreams() throws Exception {
        Session session = helper.getSuperuserSession();
        Node root = session.getRootNode();
        root.setProperty("p1", new RandomInputStream(1, STREAM_LENGTH));
        root.setProperty("p2", new RandomInputStream(2, STREAM_LENGTH));
        session.save();

        InputStream i1 = root.getProperty("p1").getStream();
        InputStream i2 = root.getProperty("p2").getStream();
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
        Session session = helper.getSuperuserSession();
        Node root = session.getRootNode();
        root.setProperty("p1", new RandomInputStream(1, STREAM_LENGTH));
        root.setProperty("p2", new RandomInputStream(1, STREAM_LENGTH));
        session.save();

        InputStream i1 = root.getProperty("p1").getStream();
        InputStream i2 = root.getProperty("p2").getStream();
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
        Session session = helper.getSuperuserSession();
        Node root = session.getRootNode();
        root.setProperty("p1", new RandomInputStream(1, STREAM_LENGTH));
        session.save();

        InputStream i1 = root.getProperty("p1").getStream();
        InputStream i2 = root.getProperty("p1").getStream();
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
