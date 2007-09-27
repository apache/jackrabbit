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
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.AnnotationTestBase;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.testmodel.Atomic;

/**
 * Test atomic persistence fields
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 */
public class AnnotationAtomicTest extends AnnotationTestBase
{
    private final static Log log = LogFactory.getLog(AnnotationAtomicTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public AnnotationAtomicTest(String testName) throws Exception
    {
        super(testName);
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(new TestSuite(AnnotationAtomicTest.class));
    }


	public void tearDown() throws Exception {

		cleanUpRepisotory();
		super.tearDown();
		
	}
    
    public void testAtomicFields()
    {
        try
        {
        	ObjectContentManager ocm = getObjectContentManager();
        	Date date = new Date();
        	Calendar calendar = Calendar.getInstance();
            // --------------------------------------------------------------------------------
            // Create and store an object graph in the repository
            // --------------------------------------------------------------------------------
            Atomic a = new Atomic();
            a.setPath("/test");
            a.setBooleanObject(new Boolean(true));
            a.setBooleanPrimitive(true);
            a.setIntegerObject(new Integer(100));
            a.setIntPrimitive(200);
            a.setString("Test String");
            a.setDate(date);
            a.setInt2boolean(true);
            
            byte[] content = "Test Byte".getBytes();
            a.setByteArray(content);
            a.setCalendar(calendar);
            a.setDoubleObject(new Double(2.12));
            a.setDoublePrimitive(1.23);
            long now = System.currentTimeMillis();
            a.setTimestamp(new Timestamp(now));
            
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream("Test Stream".getBytes());
            a.setInputStream(byteArrayInputStream);
            a.setNamedProperty("ocm:test");
            a.setPathProperty("/node1/node2");
            a.setUndefinedProperty("aStringData");
            
            ocm.insert(a);
            ocm.save();

             
            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------
            a = null;
            a = (Atomic) ocm.getObject( "/test");
            assertNotNull("a is null", a);
            assertNotNull("Boolean object is null", a.getBooleanObject());
            assertTrue("Incorrect boolean object", a.getBooleanObject().booleanValue());
            assertTrue("Incorrect boolean primitive", a.isBooleanPrimitive());
            assertNotNull("Integer Object is null", a.getIntegerObject());
            assertTrue("Incorrect Integer object", a.getIntegerObject().intValue() == 100);
            assertTrue("Incorrect int primitive", a.getIntPrimitive() == 200);
            assertNotNull("String object is null", a.getString());
            assertTrue("Incorrect boolean object", a.getString().equals("Test String"));
            assertNotNull("Byte array object is null", a.getByteArray());
            assertTrue("Incorrect byte object", new String(a.getByteArray()).equals("Test Byte"));
            
            assertNotNull("date object is null", a.getDate());
            assertTrue("Invalid date", a.getDate().equals(date));            
            assertNotNull("calendar object is null", a.getCalendar());
            
            log.debug("Calendar : " + a.getCalendar().get(Calendar.YEAR) + "-" + a.getCalendar().get(Calendar.MONTH) + "-" + a.getCalendar().get(Calendar.DAY_OF_MONTH));
            assertTrue("Invalid calendar object", a.getCalendar().equals(calendar));
            
            assertNotNull("Double object is null", a.getDoubleObject());
            assertTrue("Incorrect double object", a.getDoubleObject().doubleValue() == 2.12);
            assertTrue("Incorrect double primitive", a.getDoublePrimitive() == 1.23);
            
            assertNotNull("Incorrect input stream primitive", a.getInputStream());
            assertNotNull("Incorrect timestamp", a.getTimestamp());
            assertTrue("Invalid timestamp value ", a.getTimestamp().getTime() == now);            
            assertTrue("Invalid int2boolean value ", a.isInt2boolean());
            
            assertTrue("Invalid namedProperty value ", a.getNamedProperty().equals("ocm:test"));
            assertTrue("Invalid pathProperty value ", a.getPathProperty().equals("/node1/node2"));
            assertTrue("Invalid undefinedProperty value ", ((String) a.getUndefinedProperty()).equals("aStringData"));
            // --------------------------------------------------------------------------------
            // Update the property "namedProperty" with an invalid value
            // --------------------------------------------------------------------------------            
            try 
            {
               // update with an incorrect namespace - Should throws an exception
               a.setNamedProperty("unknown:test");               
               ocm.update(a);
               fail("Exception was not triggered with an invalid namespace");
               ocm.save();
            }
            catch (Exception e)
            {
               
                
            }
            
            // --------------------------------------------------------------------------------
            // Update the property "pathProperty" with an invalid value
            // --------------------------------------------------------------------------------            
            try 
            {
               // update with an incorrect namespace - Should throws an exception
               a.setPathProperty("//node1");               
               ocm.update(a);
               fail("Exception was not triggered with an invalid path");
               ocm.save();
            }
            catch (Exception e)
            {
               
                
            }
            
            // --------------------------------------------------------------------------------
            // Update the property "undefinedProperty" with an invalid value
            // --------------------------------------------------------------------------------            
            a = null;
            a = (Atomic) ocm.getObject( "/test");

            a.setUndefinedProperty(new Double(1.2));
            ocm.update(a);
            ocm.save();
            
            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------
            a = null;
            a = (Atomic) ocm.getObject( "/test");
            assertNotNull("a is null", a);
            assertTrue("Invalid undefinedProperty value ", ((Double) a.getUndefinedProperty()).doubleValue() == 1.2);
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }
        
    }
    
}