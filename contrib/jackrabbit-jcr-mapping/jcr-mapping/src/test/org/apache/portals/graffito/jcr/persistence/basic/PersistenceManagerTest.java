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
package org.apache.portals.graffito.jcr.persistence.basic;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.portals.graffito.jcr.RepositoryLifecycleTestSetup;
import org.apache.portals.graffito.jcr.TestBase;
import org.apache.portals.graffito.jcr.persistence.PersistenceManager;
import org.apache.portals.graffito.jcr.testmodel.A;
import org.apache.portals.graffito.jcr.testmodel.B;
import org.apache.portals.graffito.jcr.testmodel.C;
import org.apache.portals.graffito.jcr.testmodel.Discriminator;

/**
 * Test JcrSession
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 */
public class PersistenceManagerTest extends TestBase
{
    private final static Log log = LogFactory.getLog(PersistenceManagerTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public PersistenceManagerTest(String testName)  throws Exception
    {
        super(testName);
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(
                new TestSuite(PersistenceManagerTest.class));
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
    
    public void testClassA()
    {
        try
        {
        	PersistenceManager persistenceManager = getPersistenceManager();


            // --------------------------------------------------------------------------------
            // Create and store an object graph in the repository
            // --------------------------------------------------------------------------------
            A a = new A();
            a.setPath("/test");
            a.setA1("a1");
            a.setA2("a2");
            B b = new B();
            b.setB1("b1");
            b.setB2("b2");
            a.setB(b);
            
            C c1 = new C();
            c1.setId("first");
            c1.setName("First Element");
            C c2 = new C();
            c2.setId("second");
            c2.setName("Second Element");
            
            C c3 = new C();
            c3.setId("third");
            c3.setName("Third Element");
            
            
            Collection collection = new ArrayList();
            collection.add(c1);
            collection.add(c2);
            collection.add(c3);
            
            a.setCollection(collection);
            
            persistenceManager.insert(a);
            persistenceManager.save();
            

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------           
            a = (A) persistenceManager.getObject( "/test");
            assertNotNull("a is null", a);
            assertTrue("Incorrect a1", a.getA1().equals("a1"));
            assertNotNull("a.b is null", a.getB());
            assertTrue("Incorrect a.b.b1", a.getB().getB1().equals("b1"));
            assertNotNull("a.collection is null", a.getCollection());
            assertTrue("Incorrect a.collection", ((C) a.getCollection().iterator().next()).getId().equals("first"));
            
            // --------------------------------------------------------------------------------
            // Update the object
            // --------------------------------------------------------------------------------
            a.setA1("new value");
            B newB = new B();
            newB.setB1("new B1");
            newB.setB2("new B2");
            a.setB(newB);
            
            
            persistenceManager.update(a);
            persistenceManager.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------           
            a = (A) persistenceManager.getObject("/test");
            assertNotNull("a is null", a);
            assertTrue("Incorrect a1", a.getA1().equals("new value"));
            assertNotNull("a.b is null", a.getB());
            assertTrue("Incorrect a.b.b1", a.getB().getB1().equals("new B1"));
            

            
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }
        
    }
    
    /**
     * Test an persistent object mapped with a discriminator and with a nodetype
     *
     */
    public void testDiscriminatorAndNodeType()
    {
    	 try
         {
         	PersistenceManager persistenceManager = getPersistenceManager();


             // --------------------------------------------------------------------------------         	
             // Create and store the object in the repository
            // --------------------------------------------------------------------------------
         	Discriminator discriminatorObject = new Discriminator();
         	discriminatorObject.setPath("/test");
         	discriminatorObject.setContent("This is my content");
             persistenceManager.insert(discriminatorObject);             
             persistenceManager.save();
             

             // --------------------------------------------------------------------------------
             // Get the object
             // --------------------------------------------------------------------------------           
             discriminatorObject = (Discriminator) persistenceManager.getObject( "/test");
             assertNotNull("discriminator object  is null", discriminatorObject );
             assertTrue("Incorrect content", discriminatorObject.getContent().equals("This is my content"));
             
             // --------------------------------------------------------------------------------
             // Update the object
             // --------------------------------------------------------------------------------
             discriminatorObject.setContent("new content");             
             
             persistenceManager.update(discriminatorObject);
             persistenceManager.save();

             // --------------------------------------------------------------------------------
             // Get the object
             // --------------------------------------------------------------------------------           
             discriminatorObject = (Discriminator) persistenceManager.getObject( "/test");
             assertNotNull("discriminator object  is null", discriminatorObject );
             assertTrue("Incorrect content", discriminatorObject.getContent().equals("new content"));
             

             
         }
         catch (Exception e)
         {
             e.printStackTrace();
             fail("Exception occurs during the unit test : " + e);
         }	
    }
    
    public void testIsPersistent()
    {    
    	PersistenceManager persistenceManager = getPersistenceManager();
    	assertTrue("Class A is not persistent ", persistenceManager.isPersistent(A.class));
    	assertFalse("Class String is  persistent - hum ? ", persistenceManager.isPersistent(String.class));
    }
    

}