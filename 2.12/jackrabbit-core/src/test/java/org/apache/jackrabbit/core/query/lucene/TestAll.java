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
package org.apache.jackrabbit.core.query.lucene;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.jackrabbit.core.query.lucene.hits.ArrayHitsTest;
import org.apache.jackrabbit.test.ConcurrentTestSuite;

/**
 * Test suite that includes all testcases for the Search module.
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
        TestSuite suite = new ConcurrentTestSuite("Search tests");

        suite.addTestSuite(IndexingQueueTest.class);
        suite.addTestSuite(DecimalConvertTest.class);
        suite.addTestSuite(IndexingAggregateTest.class);
        suite.addTestSuite(IndexMigrationTest.class);
        suite.addTestSuite(ChainedTermEnumTest.class);
        suite.addTestSuite(IndexingConfigurationImplTest.class);
        suite.addTestSuite(SQL2IndexingAggregateTest.class);
        suite.addTestSuite(SQL2IndexingAggregateTest2.class);
        suite.addTestSuite(LazyTextExtractorFieldTest.class);
        suite.addTestSuite(IndexInfosTest.class);
        suite.addTestSuite(IndexingRuleTest.class);
        suite.addTestSuite(TextExtractionQueryTest.class);
        suite.addTestSuite(ArrayHitsTest.class);
        suite.addTestSuite(IndexFormatVersionTest.class);
        suite.addTestSuite(SynonymProviderTest.class);

        return suite;
    }
}