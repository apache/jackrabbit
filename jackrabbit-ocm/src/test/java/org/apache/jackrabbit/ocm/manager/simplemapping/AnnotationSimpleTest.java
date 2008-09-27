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
package org.apache.jackrabbit.ocm.manager.simplemapping;


import java.util.Collection;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.AnnotationTestBase;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.apache.jackrabbit.ocm.testmodel.SimpleAnnotedClass;
import org.apache.jackrabbit.ocm.testmodel.SimpleInterface;

/**
 * Test atomic persistence fields
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 */
public class AnnotationSimpleTest extends AnnotationTestBase
{
    private final static Log log = LogFactory.getLog(AnnotationSimpleTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public AnnotationSimpleTest(String testName) throws Exception
    {
        super(testName);
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(new TestSuite(AnnotationSimpleTest.class));
    }


    public void testSimpleAnnotedClasses()
    {
        try
        {
        	ObjectContentManager ocm = getObjectContentManager();
        	SimpleAnnotedClass a = new SimpleAnnotedClass(); 
        	a.setPath("/test");
        	a.setTest("test");
        	
            ocm.insert(a);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------
            a = null;
            a = (SimpleAnnotedClass) ocm.getObject( "/test");
            assertNotNull("A is null", a);
            assertEquals("Invalid value for test", "test", a.getTest());
            
            // --------------------------------------------------------------------------------
            // Update
            // --------------------------------------------------------------------------------
            a.setTest("another test");
            ocm.update(a);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------
            a = null;
            a = (SimpleAnnotedClass) ocm.getObject( "/test");
            assertNotNull("A is null", a);
            assertEquals("Invalid value for test", "another test", a.getTest());
            
            // --------------------------------------------------------------------------------
            // Search on the interface
            // --------------------------------------------------------------------------------
            QueryManager queryManager = ocm.getQueryManager();
            Filter filter = queryManager.createFilter(SimpleInterface.class); 
            filter.addEqualTo("test", "another test");
            Query q = queryManager.createQuery(filter);
            
            Collection result = ocm.getObjects(q);
            assertNotNull(result);
            assertEquals("Invalid number of SimpleInterface found", 1, result.size());
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }

    }

}