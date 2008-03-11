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
package org.apache.jackrabbit.ocm.manager.uuid;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.DigesterTestBase;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.testmodel.uuid.A;
import org.apache.jackrabbit.ocm.testmodel.uuid.B;
import org.apache.jackrabbit.ocm.testmodel.uuid.B2;
import org.apache.jackrabbit.ocm.testmodel.uuid.Descendant;


/**
 * Test on UUID & references
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 */
public class DigesterUuidTest extends DigesterTestBase
{
    private final static Log log = LogFactory.getLog(DigesterUuidTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public DigesterUuidTest(String testName)  throws Exception
    {
        super(testName);
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(
                new TestSuite(DigesterUuidTest.class));
    }


    /**
     *
     *  Map the jcr uuid into a String attribute
     *
     */
    public void testUuid()
    {
        try
        {
        	ObjectContentManager ocm = getObjectContentManager();


            // --------------------------------------------------------------------------------
            // Create and store an object A in the repository
            // --------------------------------------------------------------------------------
            A a = new A();
            a.setPath("/test");
            a.setStringData("testdata");
            ocm.insert(a);
            String uuidA = a.getUuid();
            assertNotNull("uuid is null", uuidA);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------
            a = (A) ocm.getObject( "/test");
            assertNotNull("a is null", a);
            uuidA = a.getUuid();
            assertNotNull("uuid is null", uuidA);
            System.out.println("UUID : " + uuidA);

            // --------------------------------------------------------------------------------
            // Update the object
            // --------------------------------------------------------------------------------
            a.setStringData("testdata2");
            ocm.update(a);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------
            a = (A) ocm.getObject("/test");
            assertNotNull("a is null", a);
            assertTrue("The uuid has been modified", uuidA.equals(a.getUuid()));

            // --------------------------------------------------------------------------------
            // Get the object with the uuid
            // --------------------------------------------------------------------------------
            a = (A) ocm.getObjectByUuid(uuidA);
            assertNotNull("a is null", a);
            assertTrue("Invalid object found with the uuid ", "testdata2".equals(a.getStringData()));

            // --------------------------------------------------------------------------------
            // Get the object with an invalid uuid
            // --------------------------------------------------------------------------------
            try
            {
                a = (A) ocm.getObjectByUuid("1234");
                fail("Exception not throw");
            }
            catch(Exception e)
            {
            	//Throws an exception due to an invalid uuid
            	System.out.println(e);

            }


        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }

    }
    /**
     *
     * Map a Reference into a String attribute.
     * Object B has an attribute containing the object A uuid.
     *
     */
    public void testFieldReference()
    {
        try
        {
        	ObjectContentManager ocm = getObjectContentManager();

            // --------------------------------------------------------------------------------
            // Create and store an object A in the repository
            // --------------------------------------------------------------------------------
            A a = new A();
            a.setPath("/test");
            a.setStringData("testdata");
            ocm.insert(a);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------
            a = (A) ocm.getObject( "/test");
            assertNotNull("a is null", a);
            String uuidA = a.getUuid();
            assertNotNull("uuid is null", uuidA);
            System.out.println("UUID : " + uuidA);

            // --------------------------------------------------------------------------------
            // Create and store an object B in the repository which has a reference to A
            // --------------------------------------------------------------------------------
            B b = new B();
            b.setReference2A(uuidA);
            b.setPath("/testB");
            ocm.insert(b);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Retrieve the object B with an invalid reference
            // --------------------------------------------------------------------------------
            b = (B) ocm.getObject("/testB");
            assertNotNull("b is null", b);
            assertTrue("Invalid uuid property", b.getReference2A().equals(uuidA));

            // --------------------------------------------------------------------------------
            // Update the object B with an invalid reference
            // --------------------------------------------------------------------------------
            b.setReference2A("1245");
            try
            {
            	ocm.update(b);            	
            	fail("Exception not throw");
            }
            catch(Exception e)
            {
            	//Throws an exception due to an invalid uuid
            	System.out.println("Invalid uuid : " + e);
            	
            }


        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }

    }

    /**
     *
     * Map a Reference into a bean attribute.
     * Object B has an attribute containing the object A.
     * The jcr node matching to the object B contains a reference (the jcr node matching to the object B).
     *
     */
    public void testBeanReference()
    {
        try
        {
        	ObjectContentManager ocm = getObjectContentManager();

            // --------------------------------------------------------------------------------
            // Create and store an object A in the repository
            // --------------------------------------------------------------------------------
            A a = new A();
            a.setPath("/test");
            a.setStringData("testdata");
            ocm.insert(a);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object a
            // --------------------------------------------------------------------------------
            a = (A) ocm.getObject( "/test");
            assertNotNull("a is null", a);
            String uuidA = a.getUuid();
            assertNotNull("uuid is null", uuidA);
            System.out.println("UUID : " + uuidA);

            // --------------------------------------------------------------------------------
            // Create and store an object B in the repository which has a reference to A
            // --------------------------------------------------------------------------------
            B2 b = new B2();
            b.setA(a);
            b.setPath("/testB2");
            ocm.insert(b);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Retrieve object B
            // --------------------------------------------------------------------------------
            b = (B2) ocm.getObject("/testB2");
            a = b.getA();
            assertNotNull("a is null", a);
            assertTrue("Invalid object a", a.getStringData().equals("testdata"));
            assertTrue("Invalid uuid property", a.getUuid().equals(uuidA));

            // --------------------------------------------------------------------------------
            // Update object B with an null value
            // --------------------------------------------------------------------------------
            b.setA(null);
            ocm.update(b);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Retrieve object B
            // --------------------------------------------------------------------------------
            b = (B2) ocm.getObject("/testB2");
            a = b.getA();
            assertNull("a is not null", a);


        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }

    }

    /**
     * Map a list of uuid  into a collection of String
     * The list is defined in a jcr property (Referece type / multi values)
     *
     */
    public void testCollectionOfUuid()
    {
        try
        {
        	ObjectContentManager ocm = getObjectContentManager();

            // --------------------------------------------------------------------------------
            // Create and store an object A in the repository
            // --------------------------------------------------------------------------------
            A a1 = new A();
            a1.setPath("/a1");
            a1.setStringData("testdata1");
            ocm.insert(a1);

            A a2 = new A();
            a2.setPath("/a2");
            a2.setStringData("testdata2");
            ocm.insert(a2);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the objects
            // --------------------------------------------------------------------------------
            a1 = (A) ocm.getObject( "/a1");
            assertNotNull("a1 is null", a1);
            a2 = (A) ocm.getObject( "/a2");
            assertNotNull("a2 is null", a2);
            ArrayList references = new ArrayList();
            references.add(a1.getUuid());
            references.add(a2.getUuid());

            // --------------------------------------------------------------------------------
            // Create and store an object B in the repository which has a collection of A
            // --------------------------------------------------------------------------------
            B b = new B();
            b.setPath("/testB");
            b.setMultiReferences(references);
            ocm.insert(b);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Retrieve object B
            // --------------------------------------------------------------------------------
            b = (B) ocm.getObject("/testB");
            Collection allref = b.getMultiReferences();
            assertNotNull("collection is null", allref);
            assertTrue("Invalid number of items in the collection", allref.size() == 2);

            // --------------------------------------------------------------------------------
            // Update object B with invalid uuid
            // --------------------------------------------------------------------------------
            allref.add("12345");
            b.setMultiReferences(allref);
            try
            {
            	ocm.update(b);            	
            	fail("Exception not throw");
            }
            catch(Exception e)
            {
            	//Throws an exception due to an invalid uuid
            	System.out.println("Invalid uuid value in the collection : " + e);
            	
            }

            // --------------------------------------------------------------------------------
            // Update object B with an null value
            // --------------------------------------------------------------------------------
            b.setMultiReferences(null);
            ocm.update(b);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Retrieve object B
            // --------------------------------------------------------------------------------
            b = (B) ocm.getObject("/testB");
            assertNull("a is not null", b.getMultiReferences());


        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }

    }

    /**
     * Map a list of uuid  into a collection
     * The list is defined in a jcr property (multi values)
     *
     */
    public void testCollectionOfBeanWithUuid()
    {
        try
        {
        	ObjectContentManager ocm = getObjectContentManager();

            // --------------------------------------------------------------------------------
            // Create and store an object A in the repository
            // --------------------------------------------------------------------------------
            A a1 = new A();
            a1.setPath("/a1");
            a1.setStringData("testdata1");
            ocm.insert(a1);

            A a2 = new A();
            a2.setPath("/a2");
            a2.setStringData("testdata2");
            ocm.insert(a2);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the objects
            // --------------------------------------------------------------------------------
            a1 = (A) ocm.getObject( "/a1");
            assertNotNull("a1 is null", a1);
            a2 = (A) ocm.getObject( "/a2");
            assertNotNull("a2 is null", a2);
            ArrayList references = new ArrayList();
            references.add(a1);
            references.add(a2);

            // --------------------------------------------------------------------------------
            // Create and store an object B in the repository which has a collection of A
            // --------------------------------------------------------------------------------
            B2 b = new B2();
            b.setPath("/testB2");
            b.setMultiReferences(references);
            ocm.insert(b);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Retrieve object B
            // --------------------------------------------------------------------------------
            b = (B2) ocm.getObject("/testB2");
            Collection allref = b.getMultiReferences();
            assertNotNull("collection is null", allref);
            assertTrue("Invalid number of items in the collection", allref.size() == 2);
            this.contains(allref, "/a1" , A.class);
            this.contains(allref, "/a2" , A.class);

            // --------------------------------------------------------------------------------
            // Update object B with an null value
            // --------------------------------------------------------------------------------
            b.setMultiReferences(null);
            ocm.update(b);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Retrieve object B
            // --------------------------------------------------------------------------------
            b = (B2) ocm.getObject("/testB2");
            assertNull("a is not null", b.getMultiReferences());


        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }

    }


    /**
     * Test on uuid field defined in an ancestor class
     *
     */
    public void testDescendantAncestor()
    {
        try
        {
        	ObjectContentManager ocm = getObjectContentManager();


            // --------------------------------------------------------------------------------
            // Create and store an object A in the repository
            // --------------------------------------------------------------------------------
            Descendant a = new Descendant();
            a.setPath("/descendant");
            a.setStringData("testdata");
            ocm.insert(a);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------
            a = (Descendant) ocm.getObject( "/descendant");
            assertNotNull("a is null", a);
            String uuidA = a.getUuid();
            assertNotNull("uuid is null", uuidA);
            System.out.println("UUID : " + uuidA);

            // --------------------------------------------------------------------------------
            // Update the object
            // --------------------------------------------------------------------------------
            a.setStringData("testdata2");
            ocm.update(a);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------
            a = (Descendant) ocm.getObject("/descendant");
            assertNotNull("a is null", a);
            assertTrue("The uuid has been modified", uuidA.equals(a.getUuid()));

            // --------------------------------------------------------------------------------
            // Get the object with the uuid
            // --------------------------------------------------------------------------------
            a = (Descendant) ocm.getObjectByUuid(uuidA);
            assertNotNull("a is null", a);
            assertTrue("Invalid object found with the uuid ", "testdata2".equals(a.getStringData()));


        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }

    }

}