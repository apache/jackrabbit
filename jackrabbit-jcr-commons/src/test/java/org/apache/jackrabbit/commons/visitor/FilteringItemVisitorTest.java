package org.apache.jackrabbit.commons.visitor;


import org.junit.Assert;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.test.AbstractJCRTest;

import java.util.ArrayList;
import java.util.Iterator;

public class FilteringItemVisitorTest extends AbstractJCRTest {

    final private String TEST_ROOT_NODE = "testNode";

    public void testFilteringVisitor() throws RepositoryException {

        ArrayList<Node> insertedNodeList = new ArrayList<Node>();

        Node root = superuser.getRootNode();
        Node testNode = root.addNode(TEST_ROOT_NODE);
        insertedNodeList.add(testNode);

        for (int i = 1; i < 101; i++) {
            Node firstLevelChildNode = testNode.addNode("a"+i);
            insertedNodeList.add(i, firstLevelChildNode);

            Node secondLevelChildNode = firstLevelChildNode.addNode("b"+i);
            insertedNodeList.add(2*i, secondLevelChildNode);

            Node thirdLevelChildNode = secondLevelChildNode.addNode("c"+i);
            insertedNodeList.add(3*i, thirdLevelChildNode);

        }


        testNode.getSession().save();

        final Iterator<Node> insertedNodes = insertedNodeList.iterator();

        FilteringItemVisitor v = new FilteringItemVisitor() {

            @Override
            protected void entering(Property property, int level) throws RepositoryException {


            }

            @Override
            protected void entering(Node node, int level) throws RepositoryException {
                int currentDepth = Thread.currentThread().getStackTrace().length;
                if (currentDepth > 200){
                    Assert.fail("Stack depth is more than 200");
                }

                if (insertedNodes.hasNext()) {
                    Node currNode= insertedNodes.next();

                    if (currNode.getPath().equals(node.getPath())) {
                        return;
                    }

                }
                Assert.fail("Traversal in not Breath First Traversal");
            }

            @Override
            protected void leaving(Property property, int level) throws RepositoryException {

            }

            @Override
            protected void leaving(Node node, int level) throws RepositoryException {

            }
        };

        v.setBreadthFirst(true);
        testNode.accept(v);

    }

    @Override
    protected void tearDown() throws Exception {
        superuser.getRootNode().getNode(TEST_ROOT_NODE).remove();
        superuser.save();
        super.tearDown();

    }
}
