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
package org.apache.jackrabbit.spi2davex;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestAll extends TestCase {

    public static Test suite() {

        TestSuite suite = new TestSuite("org.apache.jackrabbit.spi2davex tests");

        // spi tests
        suite.addTest(org.apache.jackrabbit.spi.TestAll.suite());

        // impl specific spi tests
        suite.addTestSuite(ConnectionTest.class);
        suite.addTestSuite(ReadTest.class);
        suite.addTestSuite(BatchTest.class);
        suite.addTestSuite(CopyTest.class);
        suite.addTestSuite(CloneTest.class);
        suite.addTestSuite(ExtensionTest.class);
        suite.addTestSuite(CreateFileTest.class);

        return suite;
    }
}