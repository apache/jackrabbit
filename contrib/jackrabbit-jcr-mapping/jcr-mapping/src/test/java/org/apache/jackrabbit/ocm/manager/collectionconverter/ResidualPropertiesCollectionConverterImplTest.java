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
package org.apache.jackrabbit.ocm.manager.collectionconverter;

import java.util.Arrays;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.DigesterTestBase;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.ManagedHashMap;
import org.apache.jackrabbit.ocm.testmodel.Residual;

/**
 * Test ResidualPropertiesCollectionConverterImpl
 *
 * @author <a href="mailto:fmeschbe[at]apache[dot]com">Felix Meschberger</a>
 * 
 */
public class ResidualPropertiesCollectionConverterImplTest extends DigesterTestBase
{
    private final static Log log = LogFactory.getLog(ResidualPropertiesCollectionConverterImplTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public ResidualPropertiesCollectionConverterImplTest(String testName)  throws Exception
    {
        super(testName);
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(new TestSuite(ResidualPropertiesCollectionConverterImplTest.class));
    }

    
    /**
     * @see junit.framework.TestCase#tearDown()
     */
    public void tearDown() throws Exception
    {
        if (getObjectContentManager().objectExists("/test"))
        {
            getObjectContentManager().remove("/test");
            getObjectContentManager().save();
        }        
    	
        super.tearDown();
    }    

    public void testResidualProperties()
    {
        try
        {
        	ObjectContentManager ocm = getObjectContentManager();

            // --------------------------------------------------------------------------------
            // Create and store an object graph in the repository with a null hashmap
            // --------------------------------------------------------------------------------

            Residual residual = new Residual.ResidualProperties();
            residual.setPath("/test");
                        
            ocm.insert(residual);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------           
            residual = (Residual) ocm.getObject( "/test");
            assertNotNull("Object is null", residual);
            assertNull("Hashmap is not null", residual.getElements());
            
            // --------------------------------------------------------------------------------
            // Update an object graph in the repository
            // --------------------------------------------------------------------------------

            residual = new Residual.ResidualProperties();
            residual.setPath("/test");
            
            ManagedHashMap map = new ManagedHashMap();
            map.put("value1", "Value1");
            map.put("value2", "Value2");
            map.put("value3", "Value3");
            map.put("value4", "Value4");
            map.put("value5", Arrays.asList(new String[]{ "Value5-1", "Value5-2" }));
            residual.setElements(map);
            
            ocm.update(residual);
            ocm.save();
            
            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------           
            residual = (Residual) ocm.getObject( "/test");
            assertNotNull("Object is null", residual);
            assertTrue("Incorrect number of values", residual.getElements().size() == 5);            
            assertTrue("Incorrect collection element", residual.getElements().get("value2").equals("Value2"));
            assertNotNull("Missing collection element", residual.getElements().get("value5"));
            assertTrue("Incorrect collection element type", (residual.getElements().get("value5") instanceof List));
            assertEquals("Incorrect collection element list size", ((List) residual.getElements().get("value5")).size(), 2);
            assertEquals("Incorrect collection element list value", ((List) residual.getElements().get("value5")).get(0), "Value5-1");
            
            // --------------------------------------------------------------------------------
            // Update the object
            // --------------------------------------------------------------------------------
            map = new ManagedHashMap();
            map.put("value11", "Value11");
            map.put("value12", "Value12");
            map.put("value13", "Value13");
            map.put("value14", "Value14");
            map.put("value15", "Value15");
            map.put("value16", Arrays.asList(new String[]{ "Value16-1", "Value16-2" }));
            residual.setElements(map);
            
            ocm.update(residual);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------           

            residual = (Residual) ocm.getObject( "/test");
            assertNotNull("Object is null", residual);
            assertTrue("Incorrect number of values", residual.getElements().size() == 6);
            assertNull("Unexpected collection element", residual.getElements().get("value2"));
            assertNull("Unexpected collection element", residual.getElements().get("value5"));
            assertTrue("Incorrect collection element", residual.getElements().get("value15").equals("Value15"));
            assertNotNull("Missing collection element", residual.getElements().get("value16"));
            assertTrue("Incorrect collection element type", (residual.getElements().get("value16") instanceof List));
            assertEquals("Incorrect collection element list size", ((List) residual.getElements().get("value16")).size(), 2);
            assertEquals("Incorrect collection element list value", ((List) residual.getElements().get("value16")).get(0), "Value16-1");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }
        
    }

   
}
