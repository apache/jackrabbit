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

public class NPEandCMETest extends AbstractJCRTest {

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
        System.err.println("Total NPEs: " + npes);
        System.err.println("Total CMEs: " + cmes);
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
                if (e.getCause() == null || !(e.getCause() instanceof NoSuchItemStateException)) {
                    System.err.println("thread" + id + ":" + e);
                    e.printStackTrace();
                }
                // else ignorable
            }
            catch (NullPointerException e) {
                System.err.println("====> " + e);
                if (SHOW_STACKTRACE) {
                    e.printStackTrace();
                }
                npes++;
            }
            catch (ConcurrentModificationException e) {
                System.err.println("====> " + e);
                if (SHOW_STACKTRACE) {
                    e.printStackTrace();
                }
                cmes++;
            }
        }
        
    }
}
