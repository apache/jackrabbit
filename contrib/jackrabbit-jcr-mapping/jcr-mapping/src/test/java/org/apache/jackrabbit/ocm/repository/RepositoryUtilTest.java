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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;
import javax.transaction.UserTransaction;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.repository.RepositoryUtil;
import org.apache.jackrabbit.ocm.transaction.jackrabbit.UserTransactionImpl;

/** Testcase for RepositoryUtil.
 * 
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class RepositoryUtilTest extends TestCase
{
    private final static Log log = LogFactory.getLog(RepositoryUtilTest.class);
    private static boolean isInit = false;
    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public RepositoryUtilTest(String testName)
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
        TestSuite suite = new TestSuite(RepositoryUtilTest.class); 
        // All methods starting with "test" will be executed in the test suite.
        return new TestSetup(suite) {
            protected void setUp() throws Exception {
                super.setUp();
                RepositoryUtil.registerRepository("repositoryTest", "./src/test/test-config/repository-xml.xml", "target/repository");
            }

            protected void tearDown() throws Exception {
                RepositoryUtil.unRegisterRepository("repositoryTest");
                super.tearDown();
            }
            
        };
    }

    /**
     * Test for getRepository() and login
     *
     */
    public void testRegistryAndLogin()
    {
        try
        {
            Repository repository = RepositoryUtil.getRepository("repositoryTest");
            assertNotNull("The repository is null", repository);
            Session session = RepositoryUtil.login(repository, "superuser", "superuser");
            Node root = session.getRootNode();
            assertNotNull("Root node is null", root);
            session.logout();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Unable to find the repository : " + e);
        }

    }
    
    /**
     * Simple unit test to check if custome node types are well defined
     *
     */
    public void testCustomNodeType()
    {
        try
        {
            Repository repository = RepositoryUtil.getRepository("repositoryTest");           
            Session session = RepositoryUtil.login(repository, "superuser", "superuser");
            NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
            
            // TODO custom node types not implemented yet
            
            //NodeType nodeType = nodeTypeManager.getNodeType("ocm:folder");
            //assertNotNull("Root node is null", nodeType);
            
            session.logout();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Unable to find the repository : " + e);
        }
    }

    /**
     * Test for getParentPath() 
     *
     */
    public void testGetParentPath()
    {
        try
        {
            String parentPath = RepositoryUtil.getParentPath("/test");
            assertNotNull("parent path is null for /test", parentPath);
            assertTrue("parent path is incorrect for /test", parentPath.equals("/"));

            parentPath = RepositoryUtil.getParentPath("/test/test2");
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
            String nodeName = RepositoryUtil.getNodeName("/test");
            assertNotNull("node name is null for /test", nodeName);
            assertTrue("node name is incorrect for /test", nodeName.equals("test"));
            
            nodeName = RepositoryUtil.getNodeName("/test/test2");
            assertNotNull("node name is null for /test/test2", nodeName);
            assertTrue("node name is incorrect for /test/test2", nodeName.equals("test2"));
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Unable to find the repository : " + e);
        }
    }   
    
    public void testEncodePath()
    {
         String encodedPath = RepositoryUtil.encodePath("/files/test/1.0");
         assertTrue("Incorrect encoded path", encodedPath.equals("/files/test/_x0031_.0"));

         encodedPath = RepositoryUtil.encodePath("/files/test/12aa/b/34/rrr/1.0");
         assertTrue("Incorrect encoded path", encodedPath.equals("/files/test/_x0031_2aa/b/_x0033_4/rrr/_x0031_.0"));

    }
    
    public void testUserTransaction()
    {
    	try
		{
			Repository repository = RepositoryUtil.getRepository("repositoryTest");
			assertNotNull("The repository is null", repository);
			Session session = RepositoryUtil.login(repository, "superuser",
					"superuser");

			UserTransaction utx = new UserTransactionImpl(session);

			// start transaction
			utx.begin();

			// add node and save
			Node root = session.getRootNode();
			Node n = root.addNode("test");
			root.save();
			utx.commit();
			
			assertTrue("test node doesn't exist", session.itemExists("/test"));
			
			utx = new UserTransactionImpl(session);
			utx.begin();
			Node test = (Node) session.getItem("/test");
			test.remove();
			session.save();
			utx.rollback();
			
			assertTrue("test node doesn't exist", session.itemExists("/test"));			

			utx = new UserTransactionImpl(session);
			utx.begin();
			test = (Node) session.getItem("/test");
			test.remove();
			session.save();
			utx.commit();
			
			assertFalse("test node exists", session.itemExists("/test"));			
			
		}
		catch (Exception e)
		{
            e.printStackTrace();
            fail("Unable to run user transaction : " + e);
		}
    }
    
}