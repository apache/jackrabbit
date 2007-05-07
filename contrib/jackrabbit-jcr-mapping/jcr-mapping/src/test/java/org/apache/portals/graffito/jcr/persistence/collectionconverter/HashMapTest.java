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
package org.apache.portals.graffito.jcr.persistence.collectionconverter;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.portals.graffito.jcr.RepositoryLifecycleTestSetup;
import org.apache.portals.graffito.jcr.TestBase;
import org.apache.portals.graffito.jcr.persistence.PersistenceManager;
import org.apache.portals.graffito.jcr.testmodel.hashmap.Element;
import org.apache.portals.graffito.jcr.testmodel.hashmap.HashMapElement;
import org.apache.portals.graffito.jcr.testmodel.hashmap.Main;

/**
 * Test NTCollectionConverterImpl
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 */
public class HashMapTest extends TestBase
{
    private final static Log log = LogFactory.getLog(HashMapTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public HashMapTest(String testName)  throws Exception
    {
        super(testName);
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(new TestSuite(HashMapTest.class));
    }

    
    /**
     * @see junit.framework.TestCase#tearDown()
     */
    public void tearDown() throws Exception
    {
        if (getPersistenceManager().objectExists("/test"))
        {
            getPersistenceManager().remove("/test");
            getPersistenceManager().save();
        }        
    	
        super.tearDown();
    }    
    
    public void testHashMap()
    {
        try
        {
        	PersistenceManager persistenceManager = getPersistenceManager();
        	

            // --------------------------------------------------------------------------------
            // Create and store an object graph in the repository
            // --------------------------------------------------------------------------------

            Main main = new Main();
            main.setPath("/test");
            main.setText("Main text");
            
            HashMapElement hashMapElement = new HashMapElement();
            Element e1 = new Element();
            e1.setId("e1");
            e1.setText("Element 1");
            hashMapElement.addObject(e1);
            
            Element e2 = new Element();
            e2.setId("e2");
            e2.setText("Element 2");
            hashMapElement.addObject(e2);
            
            main.setElements(hashMapElement);
            
            persistenceManager.insert(main);
            persistenceManager.save();
            
            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------           
            main = (Main) persistenceManager.getObject( "/test");
            assertNotNull("main.getElements() is null", main.getElements());
            assertTrue("Incorrect text", main.getText().equals("Main text"));           
            assertTrue("Incorrect para element", ((Element) main.getElements().get("e1")).getText().equals("Element 1"));
            
            // --------------------------------------------------------------------------------
            // Update the object
            // --------------------------------------------------------------------------------
            hashMapElement = new HashMapElement();
            e1 = new Element();
            e1.setId("e1");
            e1.setText("Element 1");
            hashMapElement.addObject(e1);
            
            e2 = new Element();
            e2.setId("e3");
            e2.setText("Element 3");
            hashMapElement.addObject(e2);

            Element e3 = new Element();
            e3.setId("e4");
            e3.setText("Element 4");
            hashMapElement.addObject(e3);
            main.setElements(hashMapElement);
            
            persistenceManager.update(main);
            persistenceManager.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------           
            assertNotNull("main.getElements() is null", main.getElements());
            assertTrue("Incorrect text", main.getText().equals("Main text"));           
            assertTrue("Incorrect para element", ((Element) main.getElements().get("e4")).getText().equals("Element 4"));
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }
        
    }


   
}