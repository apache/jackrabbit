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
package org.apache.jackrabbit.test.api.query;

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.query.Query;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

/**
 * Test the method {@link javax.jcr.query.Query#getStoredQueryPath()}.
 *
 * @tck.config testroot node that allows to create a child node of type nt:query.
 * @tck.config nodename1 name of an nt:query node that can becreated below the
 *  testroot.
 *
 * @test
 * @sources GetPersistentQueryPathTest.java
 * @executeClass org.apache.jackrabbit.test.api.query.GetPersistentQueryPathTest
 * @keywords level2
 */
public class GetPersistentQueryPathTest extends AbstractQueryTest {

    /**
     * Tests if {@link Query#getStoredQueryPath()} returns the correct path
     * where the query had been saved.
     *
     * @throws NotExecutableException if the repository does not support the
     *                                node type nt:query.
     */
    public void testGetPersistentQueryPath() throws RepositoryException, NotExecutableException {
        try {
            superuser.getWorkspace().getNodeTypeManager().getNodeType(ntQuery);
        } catch (NoSuchNodeTypeException e) {
            // not supported
            throw new NotExecutableException("repository does not support nt:query");
        }
        String statement = "/" + jcrRoot;
        Query q = superuser.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
        String path = testRoot + "/" + nodeName1;
        q.storeAsNode(path);
        assertEquals("Query.getPersistentQueryPath() does not return the correct path.",
                path,
                q.getStoredQueryPath());
    }
}
