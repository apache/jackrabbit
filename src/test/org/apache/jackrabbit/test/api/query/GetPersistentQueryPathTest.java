/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.apache.jackrabbit.test.api.query;

import javax.jcr.query.Query;
import javax.jcr.RepositoryException;

/**
 * Test the method {@link Query#getPersistentQueryPath()}.
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
     * Tests if {@link Query#getPersistentQueryPath()} returns the correct
     * path where the query had been saved.
     */
    public void testGetPersistentQueryPath() throws RepositoryException {
        String statement = "/" + jcrRoot;
        Query q = superuser.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
        String path = testRoot + "/" + nodeName1;
        q.save(path);
        assertEquals("Query.getPersistentQueryPath() does not return the correct path.",
                path,
                q.getPersistentQueryPath());
    }
}
