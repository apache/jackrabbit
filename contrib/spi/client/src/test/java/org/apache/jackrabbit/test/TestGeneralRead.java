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
package org.apache.jackrabbit.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.test.api.NamespaceRegistryReadMethodsTest;
import org.apache.jackrabbit.test.api.NamespaceRemappingTest;
import org.apache.jackrabbit.test.api.RepositoryDescriptorTest;
import org.apache.jackrabbit.test.api.SessionReadMethodsTest;
import org.apache.jackrabbit.test.api.WorkspaceReadMethodsTest;
import org.apache.jackrabbit.test.api.ExportSysViewTest;
import org.apache.jackrabbit.test.api.ExportDocViewTest;
import org.apache.jackrabbit.test.api.RepositoryLoginTest;
import org.apache.jackrabbit.test.api.ImpersonateTest;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

/**
 * <code>TestGeneralRead</code>...
 */
public class TestGeneralRead extends TestCase {

    private static Logger log = LoggerFactory.getLogger(TestGeneralRead.class);

    public static Test suite() {

        TestSuite suite = new TestSuite("javax.jcr General-Read");

        suite.addTestSuite(NamespaceRegistryReadMethodsTest.class);
        suite.addTestSuite(NamespaceRemappingTest.class);
        suite.addTestSuite(RepositoryDescriptorTest.class);
        suite.addTestSuite(SessionReadMethodsTest.class);
        suite.addTestSuite(WorkspaceReadMethodsTest.class);

        suite.addTestSuite(ExportSysViewTest.class);
        suite.addTestSuite(ExportDocViewTest.class);

        suite.addTestSuite(RepositoryLoginTest.class);
        suite.addTestSuite(ImpersonateTest.class);


        return suite;
    }
}