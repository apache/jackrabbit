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

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * <code>AbstractAccessControlTest</code>...
 */
public abstract class AbstractRetentionTest extends AbstractJCRTest {

    protected RetentionManager retentionMgr;

    protected void setUp() throws Exception {
        super.setUp();

        retentionMgr = getRetentionManager(superuser);
    }

    protected static RetentionManager getRetentionManager(Session s) throws RepositoryException, NotExecutableException {
        // TODO: fix (Replace by Session) test as soon as jackrabbit implements 283
        if (!(s instanceof SessionImpl)) {
            throw new NotExecutableException();
        }
        // TODO: uncomment again.
        // checkSupportedOption(Repository.OPTION_RETENTION_SUPPORTED);
        try {
            return ((SessionImpl) s).getRetentionManager();
        } catch (UnsupportedRepositoryOperationException e) {
            throw new NotExecutableException();
        }
    }

    protected static void checkSupportedOption(Session s, String option) throws NotExecutableException {
        if (Boolean.FALSE.toString().equals(s.getRepository().getDescriptor(option))) {
            throw new NotExecutableException();
        }
    }
}
