package org.apache.jackrabbit.jcr2spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.ItemExistsException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.lock.LockException;

/**
 * <code>ReorderSNSTest</code>...
 */
public class ReorderSNSTest extends ReorderTest {

    private static Logger log = LoggerFactory.getLogger(ReorderSNSTest.class);

    protected void createOrderableChildren() throws RepositoryException, LockException, ConstraintViolationException, NoSuchNodeTypeException, ItemExistsException, VersionException {
        child1 = testRootNode.addNode(nodeName2, testNodeType);
        child2 = testRootNode.addNode(nodeName2, testNodeType);
        child3 = testRootNode.addNode(nodeName2, testNodeType);
        child4 = testRootNode.addNode(nodeName2, testNodeType);

        testRootNode.save();
    }


    public void testReorder3() throws RepositoryException {
        String pathBefore = child3.getPath();

        testRootNode.orderBefore(getRelPath(child3), getRelPath(child1));
        testRootNode.save();

        Item itemIndex3 = testRootNode.getSession().getItem(pathBefore);
        assertTrue(itemIndex3.isSame(child2));

        Item item3 = testRootNode.getSession().getItem(child3.getPath());
        assertTrue(item3.isSame(child3));
    }
}
