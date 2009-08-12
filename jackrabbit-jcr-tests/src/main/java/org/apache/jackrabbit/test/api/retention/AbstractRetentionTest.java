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
package org.apache.jackrabbit.test.api.retention;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.retention.RetentionManager;
import javax.jcr.retention.RetentionPolicy;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.RepositoryStub;

/**
 * <code>AbstractAccessControlTest</code>...
 */
public abstract class AbstractRetentionTest extends AbstractJCRTest {

    protected RetentionManager retentionMgr;
    protected String testNodePath;

    protected void setUp() throws Exception {
        checkSupportedOption(Repository.OPTION_RETENTION_SUPPORTED);

        super.setUp();

        try {
            retentionMgr = getRetentionManager(superuser);
        } catch (NotExecutableException e) {
            cleanUp();
            throw e;
        }
        testNodePath = testRootNode.getPath();
    }

    protected String getHoldName() throws RepositoryException, NotExecutableException {
        String holdName = getProperty(RepositoryStub.PROP_HOLD_NAME);
        if (holdName == null) {
            throw new NotExecutableException();
        }
        return holdName;
    }

    protected RetentionPolicy getApplicableRetentionPolicy() throws NotExecutableException, RepositoryException {
        return superuser.getRetentionManager().getRetentionPolicy(getProperty(RepositoryStub.RETENTION_POLICY_HOLDER));
    }

    protected static RetentionManager getRetentionManager(Session s) throws RepositoryException, NotExecutableException {
        try {
            return s.getRetentionManager();
        } catch (UnsupportedRepositoryOperationException e) {
            throw new NotExecutableException();
        }
    }
}