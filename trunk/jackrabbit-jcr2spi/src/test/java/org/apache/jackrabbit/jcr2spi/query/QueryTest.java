/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.jcr2spi.query;

import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.api.query.AbstractQueryTest;

import javax.jcr.RepositoryException;
import javax.jcr.NodeIterator;
import javax.jcr.Node;
import javax.jcr.NamespaceRegistry;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.util.List;
import java.util.ArrayList;

/**
 * <code>QueryTest</code> performs various query tests.
 */
public class QueryTest extends AbstractQueryTest {

    /**
     * Queries the child nodes of the root node.
     * @throws NotExecutableException 
     */
    public void testQueryChildNodesOfRoot() throws RepositoryException, NotExecutableException {
        List<Node> nodes = new ArrayList<Node>();
        for (NodeIterator it = superuser.getRootNode().getNodes(); it.hasNext(); ) {
            nodes.add(it.nextNode());
        }
        Node[] children = nodes.toArray(new Node[nodes.size()]);
        executeXPathQuery(superuser, "/jcr:root/*", children);
    }

    public void testRemappedNamespace() throws RepositoryException, NotExecutableException {
        String namespaceURI = "http://jackrabbit.apache.org/spi/test";
        String defaultPrefix = "spiTest";

        NamespaceRegistry nsReg = superuser.getWorkspace().getNamespaceRegistry();
        try {
            nsReg.getPrefix(namespaceURI);
        } catch (RepositoryException e) {
            nsReg.registerNamespace(defaultPrefix, namespaceURI);
        }

        Node n = testRootNode.addNode("spiTest:node");
        superuser.save();

        for (int i = 0; i < 10; i++) {
            String prefix = defaultPrefix + i;
            superuser.setNamespacePrefix(prefix, namespaceURI);
            executeXPathQuery(superuser, testPath + "/" + prefix + ":node", new Node[]{n});
        }
    }

    /**
     * https://issues.apache.org/jira/browse/JCR-3089
     */
    public void testSQL2Simple() throws Exception {
        Query q = qm.createQuery("SELECT * FROM [nt:unstructured]",
                Query.JCR_SQL2);
        QueryResult r = q.execute();
        assertTrue(r.getNodes().hasNext());
    }

    /**
     * https://issues.apache.org/jira/browse/JCR-2543
     */
    public void testSQL2Limit() throws Exception {
        Query q = qm.createQuery("SELECT * FROM [nt:unstructured]",
                Query.JCR_SQL2);
        q.setLimit(1);
        QueryResult r = q.execute();

        NodeIterator it = r.getNodes();
        assertTrue(it.hasNext());
        it.next();
        assertFalse(it.hasNext());
    }

    /**
     * https://issues.apache.org/jira/browse/JCR-3089
     */
    public void testSQL2Join() throws Exception {
        // he query is not supposed to return anything, it will just check that
        // the back and forth between the client and the server works
        Query q = qm
                .createQuery(
                        "SELECT * FROM [nt:unstructured] AS a INNER JOIN [nt:unstructured] AS b ON b.[refid] = a.[jcr:uuid]",
                        Query.JCR_SQL2);
        assertNotNull(q.execute());
    }
}
