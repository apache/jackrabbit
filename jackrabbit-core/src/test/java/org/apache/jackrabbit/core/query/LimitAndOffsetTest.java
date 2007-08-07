package org.apache.jackrabbit.core.query;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.api.JackrabbitQuery;

public class LimitAndOffsetTest extends AbstractQueryTest {

    private Node node1;
    private Node node2;
    private Node node3;

    private JackrabbitQuery query;

    protected void setUp() throws Exception {
        super.setUp();

        node1 = testRootNode.addNode("foo");
        node1.setProperty("name", "1");
        node2 = testRootNode.addNode("foo");
        node2.setProperty("name", "2");
        node3 = testRootNode.addNode("foo");
        node3.setProperty("name", "3");

        testRootNode.save();

        query = createXPathQuery("/jcr:root" + testRoot + "/* order by @name");
    }

    private JackrabbitQuery createXPathQuery(String xpath)
            throws InvalidQueryException, RepositoryException {
        QueryManager queryManager = superuser.getWorkspace().getQueryManager();
        return (JackrabbitQuery) queryManager.createQuery(xpath, Query.XPATH);
    }

    public void testLimit() throws Exception {
        query.setLimit(1);
        QueryResult result = query.execute();
        checkResult(result, new Node[] { node1 });

        query.setLimit(2);
        result = query.execute();
        checkResult(result, new Node[] { node1, node2 });

        query.setLimit(3);
        result = query.execute();
        checkResult(result, new Node[] { node1, node2, node3 });
    }

    public void testOffset() throws Exception {
        query.setOffset(0);
        QueryResult result = query.execute();
        checkResult(result, new Node[] { node1, node2, node3 });

        query.setOffset(1);
        result = query.execute();
        checkResult(result, new Node[] { node2, node3 });

        query.setOffset(2);
        result = query.execute();
        checkResult(result, new Node[] { node3 });
    }

    public void testOffsetAndLimit() throws Exception {
        query.setOffset(0);
        query.setLimit(1);
        QueryResult result = query.execute();
        checkResult(result, new Node[] { node1 });

        query.setOffset(1);
        query.setLimit(1);
        result = query.execute();
        checkResult(result, new Node[] { node2 });

        query.setOffset(1);
        query.setLimit(2);
        result = query.execute();
        checkResult(result, new Node[] { node2, node3 });

        query.setOffset(0);
        query.setLimit(2);
        result = query.execute();
        checkResult(result, new Node[] { node1, node2 });
    }

}
