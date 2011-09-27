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
package org.apache.jackrabbit.core;

import java.util.ConcurrentModificationException;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NPEandCMETest extends AbstractJCRTest {

    /** Logger instance */
    private static final Logger log =
            LoggerFactory.getLogger(NPEandCMETest.class);

    private final static int NUM_THREADS = 10;
    private final static boolean SHOW_STACKTRACE = true;
    
    protected void setUp() throws Exception {
        super.setUp();
        Session session = getHelper().getSuperuserSession();
        session.getRootNode().addNode("test");
        session.save();
    }
    
    protected void tearDown() throws Exception {
        try {
            Session session = getHelper().getSuperuserSession();
            if (session.getRootNode().hasNode("test")) {
                session.getRootNode().getNode("test").remove();
                session.save();
            }
        } finally {
            super.tearDown();
        }
    }
    
    public void testDo() throws Exception {
        Thread[] threads = new Thread[NUM_THREADS];
        TestTask[] tasks = new TestTask[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            Session session = getHelper().getSuperuserSession();
            tasks[i] = new TestTask(i, session);
        }
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new Thread(tasks[i]);
            threads[i].start();
        }
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i].join();
        }
        int npes = 0, cmes = 0;
        for(int i = 0; i < NUM_THREADS; i++) {
            npes += tasks[i].npes;
            cmes += tasks[i].cmes;
        }
        assertEquals("Total NPEs > 0", 0, npes);
        assertEquals("Total CMEs > 0", 0, cmes);
    }
    
    private static class TestTask implements Runnable {

        private final Session session;
        private final int id;
        private final Node test;
        
        private int npes = 0;
        private int cmes = 0;
        
        private TestTask(int id, Session session) throws RepositoryException {
            this.id = id;
            this.session = session;
            test = this.session.getRootNode().getNode("test");
        }
        
        public void run() {
            try {
                for (int i = 0; i < 500; i++) {
                    NodeIterator nodes = test.getNodes();
                    if (nodes.getSize() > 100) {
                        long count = nodes.getSize() - 100;
                        while (nodes.hasNext() && count-- > 0) {
                            Node node = nodes.nextNode();
                            if (node != null) {
                                try {
                                    node.remove();
                                }
                                catch (ItemNotFoundException e) {
                                    // item was already removed
                                }
                                catch (InvalidItemStateException e) {
                                    // ignorable
                                }
                            }
                        }
                        session.save();
                    }
                    test.addNode("test-" + id + "-" + i);
                    session.save();
                }
                
            }
            catch (InvalidItemStateException e) {
                // ignorable
            }
            catch (RepositoryException e) {
                Throwable cause = e.getCause();
                if (!(cause instanceof NoSuchItemStateException)) {
                    log.warn("Unexpected RepositoryException caught", e);
                }
                // else ignorable
            }
            catch (NullPointerException e) {
                log.error("NPE caught", e);
                npes++;
            }
            catch (ConcurrentModificationException e) {
                log.error("CME caught", e);
                cmes++;
            }
        }
        
    }
}
