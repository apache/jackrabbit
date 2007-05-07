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
package org.apache.portals.graffito.jcr.persistence.atomic;

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.Date;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.portals.graffito.jcr.RepositoryLifecycleTestSetup;
import org.apache.portals.graffito.jcr.TestBase;
import org.apache.portals.graffito.jcr.persistence.PersistenceManager;
import org.apache.portals.graffito.jcr.testmodel.Atomic;

/**
 * Test Atomic perisstence fields
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 */
public class NullAtomicTest extends TestBase
{
    private final static Log log = LogFactory.getLog(NullAtomicTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public NullAtomicTest(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(new TestSuite(NullAtomicTest.class));
    }
    

	public void tearDown() throws Exception {

		cleanUpRepisotory();
		super.tearDown();
		
	}  

    public void testNullValueAtomicFields()
    {
        try
        {

        	PersistenceManager persistenceManager = getPersistenceManager();
     
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
            
            persistenceManager.insert(a);
            persistenceManager.save();
             
            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------
            a = null;
            a = (Atomic) persistenceManager.getObject( "/test");
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
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }
        
    }
    
}