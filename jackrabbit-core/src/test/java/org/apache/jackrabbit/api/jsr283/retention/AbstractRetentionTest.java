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
package org.apache.jackrabbit.api.jsr283.retention;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.RepositoryStub;
import org.apache.jackrabbit.core.retention.RetentionPolicyImpl;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * <code>AbstractAccessControlTest</code>...
 */
public abstract class AbstractRetentionTest extends AbstractJCRTest {

    protected RetentionManager retentionMgr;
    protected String testNodePath;

    protected void setUp() throws Exception {
        // TODO: uncomment again.
        // checkSupportedOption(Repository.OPTION_RETENTION_SUPPORTED);

        super.setUp();

        retentionMgr = getRetentionManager(superuser);
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
        return getApplicableRetentionPolicy("retentionPolicyName");
    }

    protected RetentionPolicy getApplicableRetentionPolicy(String jcrName) throws NotExecutableException, RepositoryException {
        // TODO: move to repositoryStub/helper and adjust accordingly
        return RetentionPolicyImpl.createRetentionPolicy(jcrName, superuser);
    }

    protected static RetentionManager getRetentionManager(Session s) throws RepositoryException, NotExecutableException {
        try {
            return getJsr283Session(s).getRetentionManager();
        } catch (UnsupportedRepositoryOperationException e) {
            throw new NotExecutableException();
        }
    }

    protected static org.apache.jackrabbit.api.jsr283.Session getJsr283Session(Session s) throws NotExecutableException {
        // TODO: get rid of method once jsr 283 is released
        if (s instanceof org.apache.jackrabbit.api.jsr283.Session) {
            return (org.apache.jackrabbit.api.jsr283.Session) s;
        } else {
            throw new NotExecutableException();
        }
    }
}