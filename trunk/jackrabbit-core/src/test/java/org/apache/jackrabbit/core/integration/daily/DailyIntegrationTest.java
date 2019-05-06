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
package org.apache.jackrabbit.core.integration.daily;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.jackrabbit.core.ConcurrencyTest;
import org.apache.jackrabbit.core.ConcurrentAddMoveRemoveTest;
import org.apache.jackrabbit.core.ConcurrentCheckinMixedTransactionTest;
import org.apache.jackrabbit.core.ConcurrentLoginTest;
import org.apache.jackrabbit.core.ConcurrentNodeModificationTest;
import org.apache.jackrabbit.core.ConcurrentReadWriteTest;
import org.apache.jackrabbit.core.ConcurrentSaveTest;
import org.apache.jackrabbit.core.ConcurrentVersioningTest;
import org.apache.jackrabbit.core.ConcurrentVersioningWithTransactionsTest;
import org.apache.jackrabbit.core.LockTest;
import org.apache.jackrabbit.core.ReadVersionsWhileModified;
import org.apache.jackrabbit.core.integration.ConcurrentQueriesWithUpdatesTest;
import org.apache.jackrabbit.core.query.lucene.LargeResultSetTest;
import org.apache.jackrabbit.core.lock.ConcurrentLockingTest;
import org.apache.jackrabbit.core.lock.ConcurrentLockingWithTransactionsTest;

/**
 * Contains tests that are run on a daily basis.
 */
public class DailyIntegrationTest extends TestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite("Daily integration tests");

        // random operation test
        suite.addTestSuite(RandomOperationTest.class);

        // multi-threading tests
        suite.addTestSuite(ConcurrencyTest.class);
        suite.addTestSuite(ConcurrentLoginTest.class);
        suite.addTestSuite(ConcurrentNodeModificationTest.class);
        suite.addTestSuite(ConcurrentReadWriteTest.class);
        suite.addTestSuite(ConcurrentSaveTest.class);
        suite.addTestSuite(ConcurrentVersioningTest.class);
        suite.addTestSuite(ConcurrentVersioningWithTransactionsTest.class);
        suite.addTestSuite(ConcurrentCheckinMixedTransactionTest.class);
        suite.addTestSuite(ConcurrentAddMoveRemoveTest.class);
        suite.addTestSuite(LockTest.class);
        suite.addTestSuite(ReadVersionsWhileModified.class);
        suite.addTestSuite(ConcurrentLockingTest.class);
        suite.addTestSuite(ConcurrentLockingWithTransactionsTest.class);
        suite.addTestSuite(LargeResultSetTest.class);
        suite.addTestSuite(ConcurrentQueriesWithUpdatesTest.class);

        return suite;
    }
}
