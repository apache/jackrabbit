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

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.AnnotationTestBase;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.testmodel.collection.Element;
import org.apache.jackrabbit.ocm.testmodel.collection.HashMapElement;
import org.apache.jackrabbit.ocm.testmodel.collection.Main;

/**
 * Test NTCollectionConverterImpl
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 */
public class AnnotationHashMapTest extends AnnotationTestBase
{
    private final static Log log = LogFactory.getLog(AnnotationHashMapTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public AnnotationHashMapTest(String testName)  throws Exception
    {
        super(testName);
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(new TestSuite(AnnotationHashMapTest.class));
    }


    public void testHashMap()
    {
        try
        {
        	ObjectContentManager ocm = getObjectContentManager();

            // --------------------------------------------------------------------------------
            // Create and store an object graph in the repository with null hashmap
            // --------------------------------------------------------------------------------

            Main main = new Main();
            main.setPath("/test");
            main.setText("Main text");

            ocm.insert(main);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------
            main = (Main) ocm.getObject( "/test");
            assertTrue("Incorrect text", main.getText().equals("Main text"));
            assertNull("HashMap is not null", main.getHashMapElement());
            assertNull("Map is not null", main.getMap());

            // --------------------------------------------------------------------------------
            // Update an object graph in the repository
            // --------------------------------------------------------------------------------

            main = new Main();
            main.setPath("/test");
            main.setText("Main text");

            HashMapElement hashMapElement = new HashMapElement();
            Map<String, Element> map = new HashMap<String, Element>();

            Element e1 = new Element();
            e1.setId("e1");
            e1.setText("Element 1");
            hashMapElement.addObject(e1);
            map.put("keyE1", e1);

            Element e2 = new Element();
            e2.setId("e2");
            e2.setText("Element 2");
            hashMapElement.addObject(e2);
            map.put("keyE2", e2);

            main.setHashMapElement(hashMapElement);
            main.setMap(map);

            ocm.update(main);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------
            main = (Main) ocm.getObject( "/test");
            assertNotNull("main.getHashMap() is null", main.getHashMapElement());
            assertNotNull("main.getHashMap() is null", main.getMap());
            assertTrue("Incorrect text", main.getText().equals("Main text"));
            map = (Map) main.getHashMapElement().getObjects();
            assertTrue("Incorrect para element", map.get("e1").getText().equals("Element 1"));
            assertTrue("Incorrect para element", map.get("e2").getText().equals("Element 2"));
            
            map = main.getMap();
            assertTrue("Incorrect para element", map.get("keyE1").getText().equals("Element 1"));
            assertTrue("Incorrect para element", map.get("keyE2").getText().equals("Element 2"));

            // --------------------------------------------------------------------------------
            // Update the object
            // --------------------------------------------------------------------------------
            hashMapElement = new HashMapElement();
            map = new HashMap<String, Element>();

            e1 = new Element();
            e1.setId("e1");
            e1.setText("Element 1");
            hashMapElement.addObject(e1);
            map.put("keyE1", e1);

            e2 = new Element();
            e2.setId("e3");
            e2.setText("Element 3");
            hashMapElement.addObject(e2);
            map.put("keyE3", e2);

            Element e3 = new Element();
            e3.setId("e4");
            e3.setText("Element 4");
            hashMapElement.addObject(e3);
            map.put("keyE4", e3);

            main.setHashMapElement(hashMapElement);
            main.setMap(map);

            ocm.update(main);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------
            assertNotNull("main.getElements() is null", main.getHashMapElement());
            assertTrue("Incorrect text", main.getText().equals("Main text"));
            map = (Map) main.getHashMapElement().getObjects();
            assertTrue("Incorrect para element", map.get("e4").getText().equals("Element 4"));
            assertTrue("Incorrect para element", map.get("e4").getText().equals("Element 4"));
            
            map = main.getMap();
            assertTrue("Incorrect para element", map.get("keyE4").getText().equals("Element 4"));
            assertTrue("Incorrect para element", map.get("keyE4").getText().equals("Element 4"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }

    }



}