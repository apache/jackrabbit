package org.apache.jackrabbit.core;

import org.apache.jackrabbit.util.Text;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * <code>org.apache.jackrabbit.core.ConcurrentRenameTest</code>...
 */
public class ConcurrentRenameTest extends AbstractConcurrencyTest {

    private static final int NUM_MOVES = 100;
    private static final int NUM_THREADS = 2;

    public void testConcurrentRename() throws Exception {
        runTask(new Task() {

            public void execute(Session session, Node test)
                    throws RepositoryException {
                String name = Thread.currentThread().getName();
                // create node
                Node n = test.addNode(name);
                session.save();
                // do moves
                for (int i = 0; i < NUM_MOVES; i++) {
                    String path = n.getPath();
                    String newName = name + "-" + i;
                    String newPath = Text.getRelativeParent(path, 1) + "/" + newName;
                    session.move(path, newPath);
                    session.save();
                    n = session.getNode(newPath);
                }
            }
        }, NUM_THREADS, testRootNode.getPath());
    }
}