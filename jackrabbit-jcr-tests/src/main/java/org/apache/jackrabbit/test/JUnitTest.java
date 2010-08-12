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
import org.slf4j.MDC;

import junit.framework.TestCase;

/**
 * Abstract base class for any JUnit test case.
 */
public abstract class JUnitTest extends TestCase {

    /**
     * Logger instance for test cases
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Output stream for general messages from tests.
     */
    public final LogPrintWriter log = new LogPrintWriter(logger);

    protected void setUp() throws Exception {
        super.setUp();
        MDC.put("testclass", getClass().getName());
        MDC.put("testcase", getName());
        logger.info("Starting test case {}", getName());
    }
 
    protected void tearDown() throws Exception {
        logger.info("Completed test case {}", getName());
        MDC.remove("testcase");
        MDC.remove("testclass");
        super.tearDown();
    }

}
