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
package org.apache.jackrabbit.core.security.user;

import org.apache.jackrabbit.test.ConcurrentTestSuite;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestAll extends TestCase {

    /**
     * Returns a <code>Test</code> suite that executes all test in this package.
     *
     * @return a <code>Test</code> suite that executes all test in this package.
     */
    public static Test suite() {
        TestSuite suite = new ConcurrentTestSuite("core.security.user tests");

        suite.addTestSuite(UserManagerImplTest.class);
        suite.addTestSuite(AuthorizableImplTest.class);
        suite.addTestSuite(UserImplTest.class);
        suite.addTestSuite(GroupImplTest.class);
        suite.addTestSuite(ImpersonationImplTest.class);
        suite.addTestSuite(AuthorizableActionTest.class);

        suite.addTestSuite(UserAdministratorTest.class);
        suite.addTestSuite(NotUserAdministratorTest.class);
        suite.addTestSuite(GroupAdministratorTest.class);
        suite.addTestSuite(AdministratorTest.class);

        suite.addTestSuite(IndexNodeResolverTest.class);
        suite.addTestSuite(TraversingNodeResolverTest.class);

        suite.addTestSuite(NodeCreationTest.class);

        suite.addTestSuite(UserImporterTest.class);

        suite.addTestSuite(UserAccessControlProviderTest.class);
        suite.addTestSuite(DefaultPrincipalProviderTest.class);        

        suite.addTestSuite(PasswordUtilityTest.class);
        return suite;
    }
}
