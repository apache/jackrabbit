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
package org.apache.jackrabbit.ocm.manager.atomic;

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.Date;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.AnnotationTestBase;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.DigesterTestBase;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.testmodel.Atomic;

/**
 * Test Atomic perisstence fields
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 */
public class AnnotationNullAtomicTest extends AnnotationTestBase
{
    private final static Log log = LogFactory.getLog(AnnotationNullAtomicTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public AnnotationNullAtomicTest(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(new TestSuite(AnnotationNullAtomicTest.class));
    }


    public void testNullValueAtomicFields()
    {
        try
        {

        	ObjectContentManager ocm = getObjectContentManager();

            // --------------------------------------------------------------------------------
            // Create and store an object graph in the repository
            // --------------------------------------------------------------------------------
            Atomic a = new Atomic();
            a.setPath("/test");
            a.setIntegerObject(new Integer(100));
            a.setDate(new Date());
            byte[] content = "Test Byte".getBytes();
            a.setByteArray(content);
            a.setCalendar(Calendar.getInstance());
            a.setDoubleObject(new Double(2.12));
            a.setDoublePrimitive(1.23);

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream("Test Stream".getBytes());
            a.setInputStream(byteArrayInputStream);

            ocm.insert(a);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------
            a = null;
            a = (Atomic) ocm.getObject( "/test");
            assertNotNull("a is null", a);
            assertNull("Boolean object is not null", a.getBooleanObject());

            assertFalse("Incorrect boolean primitive", a.isBooleanPrimitive());
            assertNotNull("Integer Object is null", a.getIntegerObject());
            assertTrue("Incorrect Integer object", a.getIntegerObject().intValue() == 100);
            assertTrue("Incorrect int primitive", a.getIntPrimitive() == 0);
            assertNull("String object is not null", a.getString());
            assertNotNull("Byte array object is null", a.getByteArray());
            assertTrue("Incorrect byte object", new String(a.getByteArray()).equals("Test Byte"));

            assertNotNull("date object is null", a.getDate());
            assertNotNull("calendar object is null", a.getCalendar());

            assertNotNull("Double object is null", a.getDoubleObject());
            assertTrue("Incorrect double object", a.getDoubleObject().doubleValue() == 2.12);
            assertTrue("Incorrect double primitive", a.getDoublePrimitive() == 1.23);

            assertNotNull("Incorrect input stream primitive", a.getInputStream());


        }
        catch (Exception e)
        {
            log.error("testNullValueAtomicFields failed", e);
            fail("Exception occurs during the unit test : " + e);
        }

    }

}