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
package org.apache.jackrabbit.core.data;

import org.apache.jackrabbit.test.ConcurrentTestSuite;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test suite that includes all testcases for the data module.
 */
public class TestAll extends TestCase {

    /**
     * Returns a test suite that executes all tests inside this package.
     *
     * @return a test suite that executes all tests inside this package
     */
    public static Test suite() {
        TestSuite suite = new ConcurrentTestSuite("Data tests");

        suite.addTestSuite(ConcurrentGcTest.class);
        suite.addTestSuite(CopyValueTest.class);
        suite.addTestSuite(DataStoreAPITest.class);
        suite.addTestSuite(DataStoreTest.class);
        suite.addTestSuite(DBDataStoreTest.class);
        suite.addTestSuite(ExportImportTest.class);
        suite.addTestSuite(GarbageCollectorTest.class);
        suite.addTestSuite(GCConcurrentTest.class);
        suite.addTestSuite(GCEventListenerTest.class);
        suite.addTestSuite(LazyFileInputStreamTest.class);
        suite.addTestSuite(NodeTypeTest.class);
        suite.addTestSuite(OpenFilesTest.class);
        suite.addTestSuite(PersistenceManagerIteratorTest.class);
        suite.addTestSuite(TestTwoGetStreams.class);
        suite.addTestSuite(WriteWhileReadingTest.class);
        suite.addTestSuite(GCSubtreeMoveTest.class);

        return suite;
    }

}
