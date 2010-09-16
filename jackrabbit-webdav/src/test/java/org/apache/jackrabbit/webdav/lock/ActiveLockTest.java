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
package org.apache.jackrabbit.webdav.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import junit.framework.TestCase;

import javax.xml.parsers.ParserConfigurationException;
import java.util.List;

/**
 * <code>ActiveLockTest</code>...
 */
public class ActiveLockTest extends TestCase {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(ActiveLockTest.class);

    public void testGetLockRoot() {
        ActiveLock lock = new DefaultActiveLock();
        lock.setLockroot("lockroot");

        assertEquals("lockroot", lock.getLockroot());
    }

    public void testParsing() throws ParserConfigurationException {
        Document doc = DomUtil.createDocument();

        ActiveLock lock = new DefaultActiveLock();
        lock.setLockroot("lockroot");
        lock.setOwner("owner");
        lock.setIsDeep(true);

        LockDiscovery disc = LockDiscovery.createFromXml(new LockDiscovery(lock).toXml(doc));
        List<ActiveLock> l  = disc.getValue();

        assertFalse(l.isEmpty());
        assertEquals(1, l.size());
        ActiveLock al = l.get(0);

        assertEquals("lockroot", al.getLockroot());
        assertEquals("owner", al.getOwner());
        assertTrue(al.isDeep());
    }
}
