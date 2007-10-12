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
package org.apache.jackrabbit.ocm.querymanager;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.DigesterTestBase;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.apache.jackrabbit.ocm.testmodel.C;


/**
 * Test QueryManagerImpl methods
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 */
public class DigesterQueryManagerTest extends DigesterTestBase
{
    private final static Log log = LogFactory.getLog(DigesterQueryManagerTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public DigesterQueryManagerTest(String testName)  throws Exception
    {
        super(testName);
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(
                new TestSuite(DigesterQueryManagerTest.class));
    }

    public void testBuildExpression1()
    {
    	try
    	{
    	      QueryManager queryManager = this.getQueryManager();
    	      Filter filter = queryManager.createFilter(C.class);
    	      filter.addEqualTo("name", "a test value")
                    .addEqualTo("id", new Integer(1));
    	      filter.setScope("/test//");
    	      
    	      Query query = queryManager.createQuery(filter);
    	      String jcrExpression = queryManager.buildJCRExpression(query);
    	      assertNotNull("jcrExpression is null", jcrExpression);
    	      assertTrue("Invalid JcrExpression", jcrExpression.equals("/jcr:root/test//element(*, ocm:C) [@ocm:name = 'a test value' and @ocm:id = 1]"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }
        
    }

    public void testBuildExpression2()
    {

    	try
    	{
    	      QueryManager queryManager = this.getQueryManager();
    	      Filter filter = queryManager.createFilter(C.class);
    	      filter.addEqualTo("name", "a test value")
    	            .addEqualTo("id", new Integer(1));
    	      
    	      Query query = queryManager.createQuery(filter);
    	      String jcrExpression = queryManager.buildJCRExpression(query);
    	      assertNotNull("jcrExpression is null", jcrExpression);
    	      assertTrue("Invalid JcrExpression", jcrExpression.equals("//element(*, ocm:C) [@ocm:name = 'a test value' and @ocm:id = 1]"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }
        
    }
    

}