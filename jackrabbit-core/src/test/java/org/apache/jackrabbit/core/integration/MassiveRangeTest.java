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
package org.apache.jackrabbit.core.integration;

import org.apache.jackrabbit.core.query.AbstractQueryTest;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.query.QueryManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

/**
 * Tests if a range query with a lot of results does not throw an error.
 */
public class MassiveRangeTest extends AbstractQueryTest {

    /**
     * Executes a range query covering 2'000 different property values.
     */
    public void testRangeQuery() throws RepositoryException {
        int count = 0;
        for (int i = 0; i < 20; i++) {
            Node child = testRootNode.addNode("node" + i);
            for (int j = 0; j < 100; j++) {
                Node n = child.addNode("node" + j);
                n.setProperty("foo", count++);
            }
            // save every 100 nodes
            testRootNode.save();
        }

        QueryManager qm = superuser.getWorkspace().getQueryManager();
        String stmt = testPath + "//*[@foo >= 0]";
        QueryResult res = qm.createQuery(stmt, Query.XPATH).execute();
        checkResult(res, 2000);
    }
}
