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
package org.apache.jackrabbit.core.query;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.jackrabbit.core.TestHelper;

/**
 * <code>AbstractIndexingTest</code> is a base class for all indexing
 * configuration tests.
 */
public class AbstractIndexingTest extends AbstractQueryTest {

    private static final String WORKSPACE_NAME = "indexing-test";

    protected Session session;

    protected Node testRootNode;

    protected void setUp() throws Exception {
        super.setUp();
        session = getHelper().getSuperuserSession(getWorkspaceName());
        testRootNode = cleanUpTestRoot(session);
        // overwrite query manager
        qm = session.getWorkspace().getQueryManager();
    }

    protected void tearDown() throws Exception {
        if (session != null) {
            cleanUpTestRoot(session);
            session.logout();
            session = null;
        }
        testRootNode = null;
        super.tearDown();
    }

    protected String getWorkspaceName() {
        return WORKSPACE_NAME;
    }

    /**
     * wait for async text-extraction tasks to finish
     */
    protected void waitForTextExtractionTasksToFinish() throws Exception {
        TestHelper.waitForTextExtractionTasksToFinish(session);
        getSearchIndex().flush();
    }
}
