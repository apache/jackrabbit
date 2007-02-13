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
import org.apache.jackrabbit.test.api.PropertyTest;
import org.apache.jackrabbit.test.api.SetValueBinaryTest;
import org.apache.jackrabbit.test.api.SetValueBooleanTest;
import org.apache.jackrabbit.test.api.SetValueDateTest;
import org.apache.jackrabbit.test.api.SetValueDoubleTest;
import org.apache.jackrabbit.test.api.SetValueLongTest;
import org.apache.jackrabbit.test.api.SetValueReferenceTest;
import org.apache.jackrabbit.test.api.SetValueStringTest;
import org.apache.jackrabbit.test.api.SetValueConstraintViolationExceptionTest;
import org.apache.jackrabbit.test.api.SetValueValueFormatExceptionTest;
import org.apache.jackrabbit.test.api.SetValueVersionExceptionTest;
import org.apache.jackrabbit.test.api.SetPropertyBooleanTest;
import org.apache.jackrabbit.test.api.SetPropertyCalendarTest;
import org.apache.jackrabbit.test.api.SetPropertyDoubleTest;
import org.apache.jackrabbit.test.api.SetPropertyInputStreamTest;
import org.apache.jackrabbit.test.api.SetPropertyLongTest;
import org.apache.jackrabbit.test.api.SetPropertyNodeTest;
import org.apache.jackrabbit.test.api.SetPropertyStringTest;
import org.apache.jackrabbit.test.api.SetPropertyValueTest;
import org.apache.jackrabbit.test.api.SetPropertyConstraintViolationExceptionTest;
import org.apache.jackrabbit.test.api.SetPropertyAssumeTypeTest;
import org.apache.jackrabbit.test.api.PropertyItemIsModifiedTest;
import org.apache.jackrabbit.test.api.PropertyItemIsNewTest;
import junit.framework.TestSuite;
import junit.framework.Test;
import junit.framework.TestCase;

/**
 * <code>TestPropertyWrite</code>...
 */
public class TestPropertyWrite extends TestCase {

    private static Logger log = LoggerFactory.getLogger(TestPropertyWrite.class);

    public static Test suite() {

        TestSuite suite = new TestSuite("javax.jcr Property-Write");

        suite.addTestSuite(PropertyTest.class);

        suite.addTestSuite(SetValueBinaryTest.class);
        suite.addTestSuite(SetValueBooleanTest.class);
        suite.addTestSuite(SetValueDateTest.class);
        suite.addTestSuite(SetValueDoubleTest.class);
        suite.addTestSuite(SetValueLongTest.class);
        suite.addTestSuite(SetValueReferenceTest.class);
        suite.addTestSuite(SetValueStringTest.class);
        suite.addTestSuite(SetValueConstraintViolationExceptionTest.class);
        suite.addTestSuite(SetValueValueFormatExceptionTest.class);
        suite.addTestSuite(SetValueVersionExceptionTest.class);

        suite.addTestSuite(SetPropertyBooleanTest.class);
        suite.addTestSuite(SetPropertyCalendarTest.class);
        suite.addTestSuite(SetPropertyDoubleTest.class);
        suite.addTestSuite(SetPropertyInputStreamTest.class);
        suite.addTestSuite(SetPropertyLongTest.class);
        suite.addTestSuite(SetPropertyNodeTest.class);
        suite.addTestSuite(SetPropertyStringTest.class);
        suite.addTestSuite(SetPropertyValueTest.class);
        suite.addTestSuite(SetPropertyConstraintViolationExceptionTest.class);
        suite.addTestSuite(SetPropertyAssumeTypeTest.class);

        suite.addTestSuite(PropertyItemIsModifiedTest.class);
        suite.addTestSuite(PropertyItemIsNewTest.class);

        return suite;
    }
}