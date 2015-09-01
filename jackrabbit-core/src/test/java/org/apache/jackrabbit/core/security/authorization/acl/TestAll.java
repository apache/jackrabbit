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
package org.apache.jackrabbit.core.security.authorization.acl;

import org.apache.jackrabbit.test.ConcurrentTestSuite;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test suite
 */
public class TestAll extends TestCase {

    /**
     * Returns a <code>Test</code> suite that executes all tests inside this
     * package.
     *
     * @return a <code>Test</code> suite that executes all tests inside this
     *         package.
     */
    public static Test suite() {
        TestSuite suite = new ConcurrentTestSuite("security.authorization.acl tests");

        suite.addTestSuite(ACLTemplateTest.class);
        suite.addTestSuite(ACLTemplateEntryTest.class);
        suite.addTestSuite(EntryTest.class);
        suite.addTestSuite(EntryCollectorTest.class);

        suite.addTestSuite(ReadTest.class);
        suite.addTestSuite(WriteTest.class);
        suite.addTestSuite(AcReadWriteTest.class);
        suite.addTestSuite(LockTest.class);
        suite.addTestSuite(VersionTest.class);
        suite.addTestSuite(NodeTypeTest.class);
        suite.addTestSuite(EffectivePolicyTest.class);
        suite.addTestSuite(ACLEditorTest.class);
        suite.addTestSuite(RepositoryOperationTest.class);
        suite.addTestSuite(MoveTest.class);
        suite.addTestSuite(RestrictionTest.class);

        return suite;
    }

}
