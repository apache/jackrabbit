package org.apache.jackrabbit.jcr2spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.lock.LockException;

/**
 * <code>ReorderNewAndSavedTest</code>...
 */
public class ReorderNewAndSavedTest extends ReorderTest {

    private static Logger log = LoggerFactory.getLogger(ReorderNewAndSavedTest.class);

    protected void createOrderableChildren() throws RepositoryException, LockException, ConstraintViolationException, NoSuchNodeTypeException, ItemExistsException, VersionException {
        child1 = testRootNode.addNode(nodeName1, testNodeType);
        child2 = testRootNode.addNode(nodeName2, testNodeType);
        testRootNode.save();

        child3 = testRootNode.addNode(nodeName3, testNodeType);
        child4 = testRootNode.addNode(nodeName4, testNodeType);
    }

    public void testRevertReorder() throws RepositoryException {
        testRootNode.orderBefore(getRelPath(child4), getRelPath(child2));
        testOrder(testRootNode, new Node[] { child1, child4, child2, child3});

        testRootNode.refresh(false);
        testOrder(testRootNode, new Node[] { child1, child2 });
    }

    public void testRevertReorderToEnd() throws RepositoryException {
        testRootNode.orderBefore(getRelPath(child1), null);
        testOrder(testRootNode, new Node[] { child2, child3, child4, child1});

        testRootNode.refresh(false);
        testOrder(testRootNode, new Node[] { child1, child2 });
    }
}
