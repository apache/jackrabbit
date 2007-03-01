package org.apache.jackrabbit.jcr2spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Node;

/**
 * <code>ReorderSNSTest</code>...
 */
public class ReorderReferenceableSNSTest extends ReorderTest {

    private static Logger log = LoggerFactory.getLogger(ReorderReferenceableSNSTest.class);

    protected void createOrderableChildren() throws RepositoryException, NotExecutableException {
        child1 = testRootNode.addNode(nodeName2, testNodeType);
        child2 = testRootNode.addNode(nodeName2, testNodeType);
        child3 = testRootNode.addNode(nodeName2, testNodeType);
        child4 = testRootNode.addNode(nodeName2, testNodeType);
        Node[] children = new Node[] { child1, child2, child3, child4};
        for (int i = 0; i < children.length; i++) {
            if (children[i].canAddMixin(mixReferenceable)) {
                children[i].addMixin(mixReferenceable);
            } else {
                throw new NotExecutableException();
            }
        }
        testRootNode.save();
    }
}
