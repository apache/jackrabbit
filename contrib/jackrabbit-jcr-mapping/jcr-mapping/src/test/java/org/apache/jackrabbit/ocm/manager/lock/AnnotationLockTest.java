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
package org.apache.jackrabbit.ocm.manager.lock;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.AnnotationTestBase;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.lock.Lock;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.testmodel.A;
import org.apache.jackrabbit.ocm.testmodel.B;
import org.apache.jackrabbit.ocm.testmodel.C;
import org.apache.jackrabbit.ocm.testmodel.Lockable;

/**
 * Test object content Manager lock feature
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 */
public class AnnotationLockTest extends AnnotationTestBase
{
    private final static Log log = LogFactory.getLog(AnnotationLockTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public AnnotationLockTest(String testName)  throws Exception
    {
        super(testName);
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(
                new TestSuite(AnnotationLockTest.class));
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
    
    public void testBasicLock()
    {
        try
        {
        	ObjectContentManager ocm = getObjectContentManager();


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
            
            ocm.insert(a);
            ocm.save();
            

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------           
            a = (A) ocm.getObject( "/test");
            assertNotNull("a is null", a);
            
            // --------------------------------------------------------------------------------
            // Check if the object is locked
            // --------------------------------------------------------------------------------
            assertFalse("the object is locked", ocm.isLocked("/test"));
            
            // --------------------------------------------------------------------------------
            // Lock the object
            // --------------------------------------------------------------------------------           
            
            Lock lock = ocm.lock("/test", true, false);
            assertTrue("the Lock owner is not correct", lock.getLockOwner().equals("superuser"));
            
            // --------------------------------------------------------------------------------
            // Check if the object is locked
            // --------------------------------------------------------------------------------
            assertTrue("the object is not locked", ocm.isLocked("/test"));
            
            // --------------------------------------------------------------------------------
            // Unlock the object
            // --------------------------------------------------------------------------------           
            ocm.unlock("/test", lock.getLockToken());

            // --------------------------------------------------------------------------------
            // Check if the object is locked
            // --------------------------------------------------------------------------------
            assertFalse("the object is locked", ocm.isLocked("/test"));

            // --------------------------------------------------------------------------------
            // Lock & update 
            // --------------------------------------------------------------------------------
            lock = ocm.lock("/test", true, false);
            a = (A) ocm.getObject("/test");
            a.setA1("new a1 Value");
            ocm.update(a);
            ocm.save();
            ocm.unlock("/test", lock.getLockToken());
            
            
            // --------------------------------------------------------------------------------
            // Remove the object
            // --------------------------------------------------------------------------------           
            ocm.remove(a);
            ocm.save();
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }
        
    }        

    /**
     *  Lock object which is assigned to a custome node type. This jcr node type inherits from mix:lockable
     *
     */
    public void testLockWithNodeType()
    {
        try
        {
        	ObjectContentManager ocm = getObjectContentManager();


            // --------------------------------------------------------------------------------
            // Create an object which is associated to the 
            // --------------------------------------------------------------------------------
            Lockable lockable = new Lockable();
            lockable.setPath("/test");
            lockable.setA1("a1");
            lockable.setA2("a2");
            ocm.insert(lockable);
            ocm.save();
            

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------           
            lockable = (Lockable) ocm.getObject("/test");
            assertNotNull("a is null", lockable);
            
            // --------------------------------------------------------------------------------
            // Check if the object is locked
            // --------------------------------------------------------------------------------
            assertFalse("the object is locked", ocm.isLocked("/test"));
            assertNull("Attribute lockowner is not null", lockable.getLockOwner());
            // --------------------------------------------------------------------------------
            // Lock the object
            // --------------------------------------------------------------------------------                       
            Lock lock = ocm.lock("/test", true, false);
            
            // --------------------------------------------------------------------------------
            // Check if the object is locked
            // --------------------------------------------------------------------------------
            assertTrue("the object is not locked", ocm.isLocked("/test"));
            
            // --------------------------------------------------------------------------------
            // Unlock the object
            // --------------------------------------------------------------------------------           
            ocm.unlock("/test", lock.getLockToken());

            // --------------------------------------------------------------------------------
            // Check if the object is locked
            // --------------------------------------------------------------------------------
            assertFalse("the object is locked", ocm.isLocked("/test"));


            // --------------------------------------------------------------------------------
            // Lock & update 
            // --------------------------------------------------------------------------------
            lock = ocm.lock("/test", true, false);
            assertTrue("the object is not locked", ocm.isLocked("/test"));
            lockable = (Lockable) ocm.getObject("/test");
            assertNotNull("Attribute lockowner is null", lockable.getLockOwner());
            lockable.setA1("new a1 Value");
            ocm.update(lockable);
            ocm.save();
            ocm.unlock("/test", lock.getLockToken());
            
            
            // --------------------------------------------------------------------------------
            // Remove the object
            // --------------------------------------------------------------------------------           
            ocm.remove(lockable);
            ocm.save();
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }
        
    }        
    
}