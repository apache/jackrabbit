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
package org.apache.jackrabbit.ocm.manager.basic;

import java.util.ArrayList;
import java.util.Collection;

import javax.jcr.Node;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.DigesterTestBase;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.testmodel.A;
import org.apache.jackrabbit.ocm.testmodel.Atomic;
import org.apache.jackrabbit.ocm.testmodel.B;
import org.apache.jackrabbit.ocm.testmodel.C;


/**
 * Test Copy & move objects
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 */
public class ObjectContentManagerCopyMoveTest extends DigesterTestBase
{
	private final static Log log = LogFactory.getLog(ObjectContentManagerCopyMoveTest.class);
	
	/**
	 * <p>Defines the test case name for junit.</p>
	 * @param testName The test case name.
	 */
	public ObjectContentManagerCopyMoveTest(String testName) throws Exception
	{
		super(testName);

	}

	public static Test suite()
	{
		// All methods starting with "test" will be executed in the test suite.
		return new RepositoryLifecycleTestSetup(
                new TestSuite(ObjectContentManagerCopyMoveTest.class));
	}

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        
    }
	
	public void tearDown() throws Exception {

		cleanUpRepisotory();
		super.tearDown();
		
	}

	public void testCopy()
	{

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
        // Copy the object 
        // --------------------------------------------------------------------------------
        ocm.copy("/test", "/test2");      
        
        // --------------------------------------------------------------------------------
        // Get the object 
        // --------------------------------------------------------------------------------
        a = (A) ocm.getObject("/test2");
        assertNotNull("a is null", a);
        assertTrue("Invalid field a1", a.getA1().equals("a1"));
        assertTrue("Invalid field b.b1", a.getB().getB1().equals("b1"));
        assertTrue("Invalid number of items in field collection", a.getCollection().size() == 3);

        
        // --------------------------------------------------------------------------------
        // Check exceptions 
        // --------------------------------------------------------------------------------
       
        try 
        {
			ocm.copy("/incorrectpath", "/test2");			
			fail("the copy method accepts an incorrect source path");
		} catch (ObjectContentManagerException e) 
		{
			// Nothing to do  - Expected behaviour
		}       

        try 
        {
			ocm.copy("/test", "incorrectpath");			
			fail("the copy method accepts an incorrect destination path");
		} catch (ObjectContentManagerException e) 
		{
			// Nothing to do  - Expected behaviour
		}
		
        // --------------------------------------------------------------------------------
        // Remove objects 
        // --------------------------------------------------------------------------------
        ocm.remove("/test");
        ocm.remove("/test2");
        ocm.save();
        
	}

	public void testSimpleMove()
	{

        try {
			// --------------------------------------------------------------------------------
			// Create and store an object graph in the repository
			// --------------------------------------------------------------------------------

        	Atomic atomic =  new Atomic();
        	atomic.setPath("/source");
        	atomic.setString("test atomic");
        	ocm.insert(atomic);
        	ocm.save();
			
			// --------------------------------------------------------------------------------
			// Copy the object 
			// --------------------------------------------------------------------------------
        	ocm.move("/source", "/result");

			// --------------------------------------------------------------------------------
			// Get the object 
			// --------------------------------------------------------------------------------
			atomic = (Atomic) ocm.getObject("/result");
			assertNotNull("atomic is null", atomic);
			assertTrue("Invalid field a1", atomic.getString().equals("test atomic"));			        

			assertFalse("Object with path /source still exists", ocm.objectExists("/source"));

			// --------------------------------------------------------------------------------
			// Check exceptions 
			// --------------------------------------------------------------------------------      
			try 
			{
				ocm.move("/incorrectpath", "/test2");			
				fail("the copy method accepts an incorrect source path");
			} catch (ObjectContentManagerException e) 
			{
				// Nothing to do  - Expected behaviour
			}       

			try 
			{
				ocm.move("/test", "incorrectpath");			
				fail("the copy method accepts an incorrect destination path");
			} catch (ObjectContentManagerException e) 
			{
				// Nothing to do  - Expected behaviour
			}
			
			// --------------------------------------------------------------------------------
			// Remove objects 
			// --------------------------------------------------------------------------------
			ocm.remove("/result");
			ocm.save();
		} 
        catch (Exception e) 
		{
        	e.printStackTrace();
        	fail();
		}
		
        
	}
	
	public void testObjectGraphMove()
	{

        try {
			// --------------------------------------------------------------------------------
			// Create and store an object graph in the repository
			// --------------------------------------------------------------------------------
			A a = new A();
			a.setPath("/source");
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
			// Copy the object 
			// --------------------------------------------------------------------------------			
        	ocm.move("/source", "/result");            
        	// --------------------------------------------------------------------------------
			// Get the object 
			// --------------------------------------------------------------------------------
			a = (A) ocm.getObject("/result");
			assertNotNull("a is null", a);
			assertTrue("Invalid field a1", a.getA1().equals("a1"));
			assertTrue("Invalid field b.b1", a.getB().getB1().equals("b1"));
			assertTrue("Invalid number of items in field collection", a.getCollection().size() == 3);
			        
			assertFalse("Object with path /source still exists", ocm.objectExists("/source"));
			
			// --------------------------------------------------------------------------------
			// Remove objects 
			// --------------------------------------------------------------------------------
			ocm.remove("/result");
			ocm.save();
		} 
        catch (Exception e) 
		{
        	e.printStackTrace();
        	fail();
		}
	}
	
	
}