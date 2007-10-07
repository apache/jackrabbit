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

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.DigesterTestBase;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.testmodel.A;
import org.apache.jackrabbit.ocm.testmodel.C;
import org.apache.jackrabbit.ocm.testmodel.MultiValue;
import org.apache.jackrabbit.ocm.testmodel.Page;
import org.apache.jackrabbit.ocm.testmodel.Paragraph;

/**
 * Test NTCollectionConverterImpl
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 */
public class DigesterMultiValueCollectionConverterImplTest extends DigesterTestBase
{
    private final static Log log = LogFactory.getLog(DigesterMultiValueCollectionConverterImplTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public DigesterMultiValueCollectionConverterImplTest(String testName)  throws Exception
    {
        super(testName);
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(new TestSuite(DigesterMultiValueCollectionConverterImplTest.class));
    }

    
    /**
     * @see junit.framework.TestCase#tearDown()
     */
    public void tearDown() throws Exception
    {
    	this.cleanUpRepisotory();          
    	
        super.tearDown();
    }    

    public void testMultiValue()
    {
        try
        {
        	ObjectContentManager ocm = getObjectContentManager();

            // --------------------------------------------------------------------------------
            // Create and store an object graph in the repository
            // --------------------------------------------------------------------------------

            MultiValue multiValue = new MultiValue();
            multiValue.setPath("/test");
            
            ArrayList values = new ArrayList();
            values.add("Value1");
            values.add("Value2");
            values.add("Value3");
            values.add("Value4");
            multiValue.setMultiValues(values);
            
            ocm.insert(multiValue);
            ocm.save();
            
            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------           
            multiValue = (MultiValue) ocm.getObject( "/test");
            assertNotNull("Object is null", multiValue);
            assertNull("nullMultiValues field is not null", multiValue.getNullMultiValues());
            assertTrue("Incorrect number of values", multiValue.getMultiValues().size() == 4);            
            assertTrue("Incorrect collection element", ((String) multiValue.getMultiValues().iterator().next()).equals("Value1"));
            
            // --------------------------------------------------------------------------------
            // Update the object
            // --------------------------------------------------------------------------------
            values = new ArrayList();
            values.add("Value1");
            values.add("Value2");
            values.add("Value3");
            values.add("Value4");
            values.add("Value5");
            multiValue.setMultiValues(values);
            
            ocm.update(multiValue);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------           

            multiValue = (MultiValue) ocm.getObject( "/test");
            assertNotNull("Object is null", multiValue);
            assertNull("nullMultiValues field is not null", multiValue.getNullMultiValues());
            assertTrue("Incorrect number of values", multiValue.getMultiValues().size() == 5);            
            assertTrue("Incorrect collection element", ((String) multiValue.getMultiValues().iterator().next()).equals("Value1"));
            
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }
        
    }

   
}