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

import java.util.Collection;

import javax.jcr.Repository;
import javax.jcr.UnsupportedRepositoryOperationException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.portals.graffito.jcr.RepositoryLifecycleTestSetup;
import org.apache.portals.graffito.jcr.TestBase;
import org.apache.portals.graffito.jcr.persistence.PersistenceManager;
import org.apache.portals.graffito.jcr.persistence.impl.PersistenceManagerImpl;
import org.apache.portals.graffito.jcr.repository.RepositoryUtil;
import org.apache.portals.graffito.jcr.testmodel.crossreference.A;
import org.apache.portals.graffito.jcr.testmodel.crossreference.B;


/**
 * Basic test for PersistenceManager
 * Test when objects are cross referenced 
 * eg. object 'a' contains a reference to an object 'b' and object 'b' contains a reference to 'a'.
 *
 * @author <a href="mailto:christophe.lombart@gmail.com>Christophe Lombart</a>
 */
public class PersistenceManagerAvoidRecursiveLoopTest extends TestBase
{
    private final static Log log = LogFactory.getLog(PersistenceManagerAvoidRecursiveLoopTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public PersistenceManagerAvoidRecursiveLoopTest(String testName)  throws Exception
    {
        super(testName);
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(
                new TestSuite(PersistenceManagerAvoidRecursiveLoopTest.class));
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
    
    public void testCrossReferences()
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
            // Add crossreference between b and a 
            a.setB(b);
            b.setA(a); 

            B b1 = new B();
            b1.setB1("b1.1");
            b1.setB2("b1.2");            
            b1.setA(a);
            a.addB(b1);

            B b2 = new B();
            b2.setB1("b2.1");
            b2.setB2("b2.2");            
            b2.setA(a);
            a.addB(b2);

            persistenceManager.insert(a);
            persistenceManager.save();
            

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------           
            a = (A) persistenceManager.getObject( "/test");
            assertNotNull("a is null", a);
            assertTrue("Duplicate instance a", a == a.getB().getA());
            
//            Collection collection = a.getCollection();
//            assertTrue("Invalid number of items in the collection", collection.size() == 2);
//            B[] bs = (B[])collection.toArray();
//            assertTrue("Duplicate instance a", a == bs[0].getA());
//            assertTrue("Duplicate instance a", a == bs[1].getA());
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }
        
    }

	
	protected void initPersistenceManager() throws UnsupportedRepositoryOperationException, javax.jcr.RepositoryException
	{
		Repository repository = RepositoryUtil.getRepository("repositoryTest");
		String[] files = { "./src/test/test-config/jcrmapping-avoidrecursiveloop.xml" };
		session = RepositoryUtil.login(repository, "superuser", "superuser");
       
		persistenceManager = new PersistenceManagerImpl(session, files);
		
	}	

    

}