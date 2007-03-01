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
import org.apache.jackrabbit.test.api.PropertyTypeTest;
import org.apache.jackrabbit.test.api.BinaryPropertyTest;
import org.apache.jackrabbit.test.api.BooleanPropertyTest;
import org.apache.jackrabbit.test.api.DatePropertyTest;
import org.apache.jackrabbit.test.api.DoublePropertyTest;
import org.apache.jackrabbit.test.api.LongPropertyTest;
import org.apache.jackrabbit.test.api.NamePropertyTest;
import org.apache.jackrabbit.test.api.PathPropertyTest;
import org.apache.jackrabbit.test.api.ReferencePropertyTest;
import org.apache.jackrabbit.test.api.StringPropertyTest;
import org.apache.jackrabbit.test.api.UndefinedPropertyTest;
import org.apache.jackrabbit.test.api.PropertyReadMethodsTest;
import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * <code>TestPropertyRead</code>...
 */
public class TestPropertyRead extends TestCase {

    private static Logger log = LoggerFactory.getLogger(TestPropertyRead.class);

    public static Test suite() {

        TestSuite suite = new TestSuite("javax.jcr Property-Read");

        suite.addTestSuite(PropertyTypeTest.class);
        suite.addTestSuite(BinaryPropertyTest.class);
        suite.addTestSuite(BooleanPropertyTest.class);
        suite.addTestSuite(DatePropertyTest.class);
        suite.addTestSuite(DoublePropertyTest.class);
        suite.addTestSuite(LongPropertyTest.class);
        suite.addTestSuite(NamePropertyTest.class);
        suite.addTestSuite(PathPropertyTest.class);
        suite.addTestSuite(ReferencePropertyTest.class);
        suite.addTestSuite(StringPropertyTest.class);
        suite.addTestSuite(UndefinedPropertyTest.class);
        suite.addTestSuite(PropertyReadMethodsTest.class);

        return suite;
    }
}