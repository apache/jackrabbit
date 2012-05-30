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
package org.apache.jackrabbit.test.api.query.qom;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import javax.jcr.query.QueryManager;
import javax.jcr.query.Query;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>GetQueryTest</code> contains test cases that check
 * {@link QueryManager#getQuery(Node)}.
 */
public class GetQueryTest extends AbstractQOMTest {

    public void testGetQuery() throws RepositoryException, NotExecutableException {
        checkNtQuery();
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        superuser.save();
        List<Query> queries = new ArrayList<Query>();
        QueryObjectModel qom = qf.createQuery(
                qf.selector(testNodeType, "s"),
                qf.childNode("s", testRoot),
                null,
                null
        );
        queries.add(qom);
        queries.add(qm.createQuery(qom.getStatement(), Query.JCR_SQL2));
        if (isSupportedLanguage(qsXPATH)) {
            String xpath = testPath + "/element(*, " + testNodeType + ")";
            queries.add(qm.createQuery(xpath, qsXPATH));
        }
        if (isSupportedLanguage(qsSQL)) {
            String sql = "select * from " + testNodeType + " where jcr:path like '" + testRoot + "/%'";
            queries.add(qm.createQuery(sql, qsSQL));
        }
        for (Iterator<Query> it = queries.iterator(); it.hasNext(); ) {
            Query q = it.next();
            String lang = q.getLanguage();
            checkResult(q.execute(), new Node[]{n});

            Node stored = q.storeAsNode(testRoot + "/storedQuery");
            q = qm.getQuery(stored);
            assertEquals("language of stored query does not match", lang, q.getLanguage());
            checkResult(q.execute(), new Node[]{n});
            stored.remove();
        }
    }

    public void testInvalidQueryException() throws RepositoryException {
        try {
            qm.getQuery(testRootNode);
            fail("getQuery() must throw InvalidQueryException when node is not of type nt:query");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    /**
     * Checks if the repository supports the nt:query node type otherwise throws
     * a <code>NotExecutableException</code>.
     *
     * @throws NotExecutableException if nt:query is not supported.
     * @throws RepositoryException if another error occurs.
     */
    private void checkNtQuery() throws RepositoryException, NotExecutableException {
        try {
            superuser.getWorkspace().getNodeTypeManager().getNodeType(ntQuery);
        } catch (NoSuchNodeTypeException e) {
            // not supported
            throw new NotExecutableException("repository does not support nt:query");
        }
    }
}
