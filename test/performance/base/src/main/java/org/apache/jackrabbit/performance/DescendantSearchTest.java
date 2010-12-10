package org.apache.jackrabbit.performance;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

/**
 * Performance test to check performance of queries on sub-trees.
 */
public class DescendantSearchTest extends AbstractTest {

    private static final int NODE_COUNT = 100;

    private Session session;

    private Node root;

    protected Query createQuery(QueryManager manager, int i)
            throws RepositoryException {
        return manager.createQuery("/testroot//*[@testcount=" + i + "]", Query.XPATH);
    }

    public void beforeSuite() throws RepositoryException {
        session = getRepository().login(getCredentials());

        root = session.getRootNode().addNode("testroot", "nt:unstructured");
        for (int i = 0; i < NODE_COUNT; i++) {
            Node node = root.addNode("node" + i, "nt:unstructured");
            for (int j = 0; j < NODE_COUNT; j++) {
                Node child = node.addNode("node" + j, "nt:unstructured");
                child.setProperty("testcount", j);
            }
            session.save();
        }
    }

    public void runTest() throws Exception {
        QueryManager manager = session.getWorkspace().getQueryManager();
        for (int i = 0; i < NODE_COUNT; i++) {
            Query query = createQuery(manager, i);
            NodeIterator iterator = query.execute().getNodes();
            while (iterator.hasNext()) {
                Node node = iterator.nextNode();
                if (node.getProperty("testcount").getLong() != i) {
                    throw new Exception("Invalid test result: " + node.getPath());
                }
            }
        }
    }

    public void afterSuite() throws RepositoryException {
        for (int i = 0; i < NODE_COUNT; i++) {
            root.getNode("node" + i).remove();
            session.save();
        }

        root.remove();
        session.save();
        session.logout();
    }

}
