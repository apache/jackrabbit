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
package org.apache.jackrabbit.performance;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

/**
 * <code>PathBasedQueryTest</code> implements a performance test executing a
 * query that has a path constraint with low selectivity, whereas the predicate
 * is very selective.
 */
public class PathBasedQueryTest extends AbstractTest {

    private Session session;

    private Node root;

    @Override
    protected void beforeSuite() throws Exception {
        session = getRepository().login(getCredentials());
        root = session.getRootNode().addNode(
                getClass().getSimpleName(), "nt:unstructured");
        int count = 0;
        for (int i = 0; i < 5; i++) {
            Node n = root.addNode("node-" + i);
            for (int j = 0; j < 100; j++) {
                n.addNode("node-" + j).setProperty("count", count++);
            }
        }
        session.save();
    }

    @Override
    protected void runTest() throws Exception {
        QueryManager qm = session.getWorkspace().getQueryManager();
        Query q = qm.createQuery("/jcr:root" + root.getPath() + "/*/*[@count = 250]",
                Query.XPATH);
        for (int i = 0; i < 10; i++) {
            q.execute().getNodes().nextNode();
        }
    }

    @Override
    protected void afterSuite() throws Exception {
        root.remove();
        session.save();
        session.logout();
    }
}
