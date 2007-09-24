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
package org.apache.jackrabbit.ocm.repository;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/** Testcase for RepositoryUtil.
 * 
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class NodeUtilTest extends TestCase
{

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public NodeUtilTest(String testName)
    {
        super(testName);
    }

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(NodeUtilTest.class); 
        // All methods starting with "test" will be executed in the test suite.
        return new TestSetup(suite) {
            protected void setUp() throws Exception {
                super.setUp();
                RepositoryUtil.registerRepository("repositoryTest", "./src/test/test-config/repository-derby.xml", "target/repository");
            }

            protected void tearDown() throws Exception {
                RepositoryUtil.unRegisterRepository("repositoryTest");
                super.tearDown();
            }
            
        };
    }


    /**
     * Test for getParentPath() 
     *
     */
    public void testGetParentPath()
    {
        try
        {
            String parentPath = NodeUtil.getParentPath("/test");
            assertNotNull("parent path is null for /test", parentPath);
            assertTrue("parent path is incorrect for /test", parentPath.equals("/"));

            parentPath = NodeUtil.getParentPath("/test/test2");
            assertNotNull("parent path is null for /test/test2", parentPath);
            assertTrue("parent path is incorrect for /test/test2", parentPath.equals("/test"));
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Unable to find the repository : " + e);
        }
    }
    
    /**
     * Test for getNodeName() 
     *
     */
    public void testGetNodeName()
    {
        try
        {
            String nodeName = NodeUtil.getNodeName("/test");
            assertNotNull("node name is null for /test", nodeName);
            assertTrue("node name is incorrect for /test", nodeName.equals("test"));
            
            nodeName = NodeUtil.getNodeName("/test/test2");
            assertNotNull("node name is null for /test/test2", nodeName);
            assertTrue("node name is incorrect for /test/test2", nodeName.equals("test2"));
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Unable to find the repository : " + e);
        }
    }   
    
     
}