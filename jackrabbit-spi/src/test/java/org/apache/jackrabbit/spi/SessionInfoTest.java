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
package org.apache.jackrabbit.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** <code>SessionInfoTest</code>... */
public class SessionInfoTest extends AbstractSPITest {

    private static Logger log = LoggerFactory.getLogger(SessionInfoTest.class);

    private String workspaceName;
    private SessionInfo sessionInfo;

    protected void setUp() throws Exception {
        super.setUp();

        workspaceName = getProperty(RepositoryServiceStub.PROP_WORKSPACE);
        sessionInfo = helper.getRepositoryService().obtain(helper.getAdminCredentials(), workspaceName);
    }

    protected void tearDown() throws Exception {
        if (sessionInfo != null) {
            helper.getRepositoryService().dispose(sessionInfo);
        }
        super.tearDown();
    }

    public void testGetWorkspaceName() {
        if (workspaceName == null) {
            assertNotNull(sessionInfo.getWorkspaceName());
        } else {
            assertEquals(workspaceName, sessionInfo.getWorkspaceName());
        }
    }

    // TODO: add more tests
}