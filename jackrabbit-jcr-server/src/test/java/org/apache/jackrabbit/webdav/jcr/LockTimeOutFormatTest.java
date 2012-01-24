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
package org.apache.jackrabbit.webdav.jcr;

import java.net.URISyntaxException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.apache.jackrabbit.webdav.jcr.lock.JcrActiveLock;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * <code>LockTimeOutFormatTest</code>...
 */
public class LockTimeOutFormatTest extends TestCase {

    public void testOneSec() throws RepositoryException, URISyntaxException, ParserConfigurationException {
        testfmt(1, "Second-1");
    }

    public void testInf() throws RepositoryException, URISyntaxException, ParserConfigurationException {
        testfmt(Long.MAX_VALUE, "Infinite");
    }

    public void testTooLong() throws RepositoryException, URISyntaxException, ParserConfigurationException {
        testfmt(Integer.MAX_VALUE + 100000L, "Infinite");
    }

    public void testNeg() throws RepositoryException, URISyntaxException, ParserConfigurationException {
        // expired
        testfmt(-1, null);
    }

    private void testfmt(long jcrtimeout, String expectedString) throws RepositoryException, URISyntaxException, ParserConfigurationException {

        Lock l = new TestLock(jcrtimeout);
        JcrActiveLock al = new JcrActiveLock(l);

        Document d = DomUtil.createDocument();
        Element activeLock = al.toXml(d);
        assertEquals("activelock", activeLock.getLocalName());
        NodeList nl = activeLock.getElementsByTagNameNS("DAV:", "timeout");

        if (expectedString == null) {
            assertEquals(0, nl.getLength());
        }
        else {
            assertEquals(1, nl.getLength());
            Element timeout = (Element)nl.item(0);
            String t = DomUtil.getText(timeout);
            assertEquals(expectedString, t);
        }
    }

    /**
     * Minimal Lock impl for tests above
     */
    private static class TestLock implements Lock {

        private final long timeout;

        public TestLock(long timeout) {
            this.timeout = timeout;
        }

        public String getLockOwner() {
            return null;
        }

        public boolean isDeep() {
            return false;
        }

        public Node getNode() {
            return null;
        }

        public String getLockToken() {
            return "foo";
        }

        public long getSecondsRemaining() throws RepositoryException {
            return timeout;
        }

        public boolean isLive() throws RepositoryException {
            return timeout >= 0;
        }

        public boolean isSessionScoped() {
            return false;
        }

        public boolean isLockOwningSession() {
            return false;
        }

        public void refresh() throws LockException, RepositoryException {
        }
    }
}
