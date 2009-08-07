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
package org.apache.jackrabbit.test.api;

import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.Repository;
import javax.jcr.Session;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;

/**
 * Tests if the required repository descriptors are available.
 *
 * @test
 * @sources RepositoryDescriptorTest.java
 * @executeClass org.apache.jackrabbit.test.api.RepositoryDescriptorTest
 * @keywords level1
 */
public class RepositoryDescriptorTest extends AbstractJCRTest {

    private static final Set requiredDescriptorKeys = new HashSet();

    static {
        requiredDescriptorKeys.add(Repository.LEVEL_1_SUPPORTED);
        requiredDescriptorKeys.add(Repository.LEVEL_2_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_LOCKING_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_OBSERVATION_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_QUERY_SQL_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_TRANSACTIONS_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_VERSIONING_SUPPORTED);
        requiredDescriptorKeys.add(Repository.QUERY_XPATH_DOC_ORDER);
        requiredDescriptorKeys.add(Repository.QUERY_XPATH_POS_INDEX);
        requiredDescriptorKeys.add(Repository.REP_NAME_DESC);
        requiredDescriptorKeys.add(Repository.REP_VENDOR_DESC);
        requiredDescriptorKeys.add(Repository.REP_VENDOR_URL_DESC);
        requiredDescriptorKeys.add(Repository.REP_VERSION_DESC);
        requiredDescriptorKeys.add(Repository.SPEC_NAME_DESC);
        requiredDescriptorKeys.add(Repository.SPEC_VERSION_DESC);
    }

    /** The session for the tests */
    private Session session;

    /**
     * Sets up the fixture for this test.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();
        session = helper.getReadOnlySession();
    }

    /**
     * Releases the session aquired in {@link #setUp}.
     */
    protected void tearDown() throws Exception {
        if (session != null) {
            session.logout();
            session = null;
        }
        super.tearDown();
    }

    /**
     * Tests that the required repository descriptors are available.
     */
    public void testRequiredDescriptors() {
        for (Iterator it = requiredDescriptorKeys.iterator(); it.hasNext();) {
            String descriptor = session.getRepository().getDescriptor((String) it.next());
            assertNotNull("Not all required descriptors are available.",
                    descriptor);
        }
    }

    /**
     * Tests if {@link Repository#getDescriptorKeys()} returns all required
     * descriptors keys.
     */
    public void testGetDescriptorKeys() {
        List keys = Arrays.asList(session.getRepository().getDescriptorKeys());
        for (Iterator it = requiredDescriptorKeys.iterator(); it.hasNext();) {
            String key = (String) it.next();
            assertTrue(key + " is missing from the required descriptor keys.",
                    keys.contains(key));
        }
    }

}
