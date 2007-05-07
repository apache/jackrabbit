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
package org.apache.portals.graffito.jcr.persistence.lock;

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
 * Test Persistence Manager lock feature
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 */
public class PersistenceManagerLockTest extends TestBase
{
    private final static Log log = LogFactory.getLog(PersistenceManagerLockTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public PersistenceManagerLockTest(String testName)  throws Exception
    {
        super(testName);
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(
                new TestSuite(PersistenceManagerLockTest.class));
    }


    /**
     * @see junit.framework.TestCase#tearDown()
     */
    public void tearDown() throws Exception
    {

        super.tearDown();
    }
    
    public void testBasicLock()
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
            
            // --------------------------------------------------------------------------------
            // Check if the object is locked
            // --------------------------------------------------------------------------------
            assertFalse("the object is locked", persistenceManager.isLocked("/test"));
            
            // --------------------------------------------------------------------------------
            // Lock the object
            // --------------------------------------------------------------------------------           
            String lockToken = persistenceManager.lock("/test", true, false);
            
            // --------------------------------------------------------------------------------
            // Check if the object is locked
            // --------------------------------------------------------------------------------
            assertTrue("the object is not locked", persistenceManager.isLocked("/test"));
            
            // --------------------------------------------------------------------------------
            // Unlock the object
            // --------------------------------------------------------------------------------           
            persistenceManager.unlock("/test", lockToken);

            // --------------------------------------------------------------------------------
            // Check if the object is locked
            // --------------------------------------------------------------------------------
            assertFalse("the object is locked", persistenceManager.isLocked("/test"));
            
            // --------------------------------------------------------------------------------
            // Remove the object
            // --------------------------------------------------------------------------------           
            persistenceManager.remove(a);
            persistenceManager.save();
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }
        
    }        

}