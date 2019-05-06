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
package org.apache.jackrabbit.jcr2dav;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.apache.jackrabbit.jcr2spi.security.Jcr2SpiSecurityTestSuite;
import org.apache.jackrabbit.jcr2spi.Jcr2SpiTestSuite;
import org.apache.jackrabbit.test.JCRTestSuite;

/**
 * JCR API conformance test suite.
 */
public class ConformanceTest extends TestCase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        if (Boolean.getBoolean("jackrabbit.test.integration")) {
            suite.addTest(new JCRTestSuite());
            suite.addTest(new Jcr2SpiTestSuite());
            suite.addTest(new Jcr2SpiSecurityTestSuite());
            suite.addTest(new StopRepository());
        }
        return suite;
    }

    private static class StopRepository implements Test {

        public int countTestCases() {
            return 1;
        }

        public void run(TestResult result) {
            try {
                RepositoryStubImpl.stopServer(); 
            } catch (Exception e) {
                result.addError(this, e);
            }
        }
    }
}
