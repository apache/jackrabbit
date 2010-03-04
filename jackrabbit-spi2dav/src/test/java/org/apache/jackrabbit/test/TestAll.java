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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Execute all API tests from jackrabbit-jcr-test and jackrabbit-jcr2spi.
 */
public class TestAll extends TestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite("JCR Test : jcr-test and jcr2spi");
        suite.addTest(new org.apache.jackrabbit.test.JCRTestSuite());
        suite.addTest(new org.apache.jackrabbit.jcr2spi.Jcr2SpiTestSuite());
        
        return suite;
    }
}