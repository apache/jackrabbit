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
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.DigesterTestBase;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.testmodel.collection.ArrayListElement;
import org.apache.jackrabbit.ocm.testmodel.collection.Element;
import org.apache.jackrabbit.ocm.testmodel.collection.Main;

/**
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 */
public class DigesterArrayListTest extends DigesterTestBase
{
    private final static Log log = LogFactory.getLog(DigesterArrayListTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public DigesterArrayListTest(String testName)  throws Exception
    {
        super(testName);
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(new TestSuite(DigesterArrayListTest.class));
    }

    public void testArrayList()
    {
        try
        {
        	ObjectContentManager ocm = getObjectContentManager();


            // --------------------------------------------------------------------------------
            // Create and store an object graph in the repository
        	// with a null value for the arraylist
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
            ArrayListElement arrayList = main.getArrayListElement();
            List<Element> list = main.getList();
            assertNull("main.getArrayListElement is not null", arrayList );
            assertNull("main.getList is not null", list );

            // --------------------------------------------------------------------------------
            // Update the object
            // --------------------------------------------------------------------------------

            ArrayListElement arrayListElement = new ArrayListElement();
            list = new ArrayList<Element>();

            Element e1 = new Element();
            e1.setId("e1");
            e1.setText("Element 1");
            arrayListElement.addObject(e1);
            list.add(e1);

            Element e2 = new Element();
            e2.setId("e2");
            e2.setText("Element 2");
            arrayListElement.addObject(e2);
            list.add(e2);

            main.setArrayListElement(arrayListElement);
            main.setList(list);
            ocm.update(main);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------
            main = (Main) ocm.getObject( "/test");
            arrayList = main.getArrayListElement();
            list = main.getList();
            assertNotNull("main.getArrayListElemnt is null", arrayList );
            assertNotNull("main.getList is null", list );
            Collection<Element> objects = (Collection<Element>) arrayList.getObjects();
            Element[] elements = (Element[]) objects.toArray(new Element[objects.size()]);
            assertTrue("Incorrect para element", elements[0].getText().equals("Element 1"));
            assertTrue("Incorrect para element", list.get(0).getText().equals("Element 1"));



            // --------------------------------------------------------------------------------
            // Update the object
            // --------------------------------------------------------------------------------
            arrayListElement = new ArrayListElement();
            list = new ArrayList<Element>();
            e1 = new Element();
            e1.setId("e1");
            e1.setText("Element 1");
            arrayListElement.addObject(e1);
            list.add(e1);

            e2 = new Element();
            e2.setId("e3");
            e2.setText("Element 3");
            arrayListElement.addObject(e2);
            list.add(e2);

            Element e3 = new Element();
            e3.setId("e4");
            e3.setText("Element 4");
            arrayListElement.addObject(e3);
            list.add(e3);

            main.setArrayListElement(arrayListElement);
            main.setList(list);

            ocm.update(main);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------
            main = (Main) ocm.getObject( "/test");
            arrayList = main.getArrayListElement();
            list = main.getList();
            assertNotNull("main.getArrayListElement() is null", arrayList );
            assertNotNull("main.getList() is null", list );
            objects = (Collection<Element>) arrayList.getObjects();
            elements = (Element[]) objects.toArray(new Element[objects.size()]);
            assertTrue("Incorrect element", elements[2].getText().equals("Element 4"));
            assertTrue("Incorrect element", list.get(2).getText().equals("Element 4"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }

    }



}