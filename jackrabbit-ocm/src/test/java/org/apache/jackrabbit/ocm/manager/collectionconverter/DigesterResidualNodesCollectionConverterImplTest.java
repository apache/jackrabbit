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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.DigesterTestBase;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.ManagedHashMap;
import org.apache.jackrabbit.ocm.testmodel.Paragraph;
import org.apache.jackrabbit.ocm.testmodel.Residual;

/**
 * Test ResidualNodesCollectionConverterImpl
 *
 * @author <a href="mailto:fmeschbe[at]apache[dot]com">Felix Meschberger</a>
 * 
 */
public class DigesterResidualNodesCollectionConverterImplTest extends DigesterTestBase
{
    private final static Log log = LogFactory.getLog(DigesterResidualNodesCollectionConverterImplTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public DigesterResidualNodesCollectionConverterImplTest(String testName)  throws Exception
    {
        super(testName);
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(new TestSuite(DigesterResidualNodesCollectionConverterImplTest.class));
    }
   
    public void testResidualNodes()
    {
        try
        {
        	ObjectContentManager ocm = getObjectContentManager();

            // --------------------------------------------------------------------------------
            // Create and store an object graph in the repository with null values
            // --------------------------------------------------------------------------------

            Residual residual = new Residual.ResidualNodes();
            residual.setPath("/test");
            ocm.insert(residual);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------           
            residual = (Residual) ocm.getObject( "/test");
            assertNotNull("Object is null", residual);
            assertNull("Map is not null", residual.getElements());
            
            // --------------------------------------------------------------------------------
            // Update an object graph in the repository
            // --------------------------------------------------------------------------------
            residual = new Residual.ResidualNodes();
            residual.setPath("/test");
            
            ManagedHashMap map = new ManagedHashMap();
            map.put("value1", new Paragraph("Value1"));
            map.put("value2", new Paragraph("Value2"));
            map.put("value3", new Paragraph("Value3"));
            map.put("value4", new Paragraph("Value4"));
            residual.setElements(map);
            
            ocm.update(residual);
            ocm.save();
            
            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------           
            residual = (Residual) ocm.getObject( "/test");
            assertNotNull("Object is null", residual);
            assertTrue("Incorrect number of values", residual.getElements().size() == 4);            
            assertTrue("Incorrect collection element type", (residual.getElements().get("value2") instanceof Paragraph));
            assertEquals("Incorrect collection element text", ((Paragraph) residual.getElements().get("value2")).getText(), "Value2");
            
            // --------------------------------------------------------------------------------
            // Update the object
            // --------------------------------------------------------------------------------
            map = new ManagedHashMap();
            map.put("value11", new Paragraph("Value11"));
            map.put("value12", new Paragraph("Value12"));
            map.put("value13", new Paragraph("Value13"));
            map.put("value14", new Paragraph("Value14"));
            map.put("value15", new Paragraph("Value15"));
            residual.setElements(map);
            
            ocm.update(residual);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------           

            residual = (Residual) ocm.getObject( "/test");
            assertNotNull("Object is null", residual);
            assertTrue("Incorrect number of values", residual.getElements().size() == 5);
            assertNull("Unexpected collection element", residual.getElements().get("value2"));
            assertTrue("Incorrect collection element type", (residual.getElements().get("value15") instanceof Paragraph));
            assertEquals("Incorrect collection element text", ((Paragraph) residual.getElements().get("value15")).getText(), "Value15");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }
        
    }

   
}
