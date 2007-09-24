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
package org.apache.jackrabbit.ocm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeManager;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;
import org.apache.jackrabbit.ocm.repository.RepositoryUtil;
import org.xml.sax.ContentHandler;

/**
 * Base class for testcases. Provides priviledged access to the jcr test
 * repository.
 * 
 * @author <a href="mailto:okiessler@apache.org">Oliver Kiessler</a>
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 * @version $Id: Exp $
 */
public abstract class TestBase extends TestCase
{

	private final static Log log = LogFactory.getLog(TestBase.class);
	
	protected Session session;

	protected ObjectContentManager ocm;

	protected Mapper mapper;
    
    protected boolean isInit = false;

	/**
	 * <p>
	 * Defines the test case name for junit.
	 * </p>
	 * 
	 * @param testName
	 *            The test case name.
	 */
	public TestBase(String testName)
	{
		super(testName);
	}

	/**
	 * Setting up the testcase.
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception
	{
		try {
			super.setUp();
	        
	        if (!isInit) {
	            initObjectContentManager();
	            RepositoryUtil.setupSession(getSession());
	            registerNodeTypes(getSession());
	            isInit = true;
	        }
			
		}
		catch (Exception e )
		{
			e.printStackTrace();
		}
	}

	/**
	 * @see junit.framework.TestCase#tearDown()
	 */
	public void tearDown() throws Exception
	{
		super.tearDown();
	}

	/**
	 * Getter for property ocm.
	 * 
	 * @return jcrSession
	 */
	public ObjectContentManager getObjectContentManager()
	{
		try
		{
			if (ocm == null)
			{
				initObjectContentManager();
				RepositoryUtil.setupSession(getSession());
                registerNodeTypes(getSession());
			}
			return ocm;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

    protected void registerNodeTypes(Session session) 
    throws InvalidNodeTypeDefException, javax.jcr.RepositoryException, IOException {
        InputStream xml = new FileInputStream(
                "./src/test/test-config/nodetypes/custom_nodetypes.xml");

        // HINT: throws InvalidNodeTypeDefException, IOException
        NodeTypeDef[] types = NodeTypeReader.read(xml);

        Workspace workspace = session.getWorkspace();
        NodeTypeManager ntMgr = workspace.getNodeTypeManager();
        NodeTypeRegistry ntReg = ((NodeTypeManagerImpl) ntMgr).getNodeTypeRegistry();

        for (int j = 0; j < types.length; j++) {
            NodeTypeDef def = types[j];

            try {
                ntReg.getNodeTypeDef(def.getName());
            }
            catch (NoSuchNodeTypeException nsne) {
                // HINT: if not already registered than register custom node type
                ntReg.registerNodeType(def);
            }

        }
    }
    
	protected void initObjectContentManager() throws UnsupportedRepositoryOperationException, javax.jcr.RepositoryException
	{
		Repository repository = RepositoryUtil.getRepository("repositoryTest");
		String[] files = { "./src/test/test-config/jcrmapping.xml", 
						   "./src/test/test-config/jcrmapping-proxy.xml",
						   "./src/test/test-config/jcrmapping-atomic.xml",
                           "./src/test/test-config/jcrmapping-default.xml",
                           "./src/test/test-config/jcrmapping-beandescriptor.xml",
                           "./src/test/test-config/jcrmapping-inheritance.xml",
                           "./src/test/test-config/jcrmapping-jcrnodetypes.xml", 
                           "./src/test/test-config/jcrmapping-uuid.xml"};
		session = RepositoryUtil.login(repository, "superuser", "superuser");
		ocm = new ObjectContentManagerImpl(session, files);
		
	}

	/**
	 * Setter for property jcrSession.
	 * 
	 * @param ocm
	 *            The object content manager
	 */
	public void setObjectContentManager(ObjectContentManager ocm)
	{
		this.ocm = ocm;
	}

	public void exportDocument(String filePath, String nodePath, boolean skipBinary, boolean noRecurse)
	{
		try
		{
			BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(filePath));
			ContentHandler handler = new org.apache.xml.serialize.XMLSerializer(os, null).asContentHandler();
			session.exportDocumentView(nodePath, handler, skipBinary, noRecurse);
			os.flush();
			os.close();
		}
		catch (Exception e)
		{
			System.out.println("Impossible to export the content from : " + nodePath);
			e.printStackTrace();
		}
	}

	public void importDocument(String filePath, String nodePath)
	{
		try
		{
			BufferedInputStream is = new BufferedInputStream(new FileInputStream(filePath));
			session.importXML(nodePath, is, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
			session.save();
			is.close();
		}
		catch (Exception e)
		{
			System.out.println("Impossible to import the content from : " + nodePath);
			e.printStackTrace();
		}

	}

	protected Session getSession()
	{
		return this.session;
	}

	public QueryManager getQueryManager()
	{
		return ocm.getQueryManager();
	}
	
    protected boolean contains(Collection result, String path, Class objectClass)
    {
            Iterator iterator = result.iterator();
            while (iterator.hasNext())
            {
                Object  object = iterator.next();
                String itemPath = (String)  ReflectionUtils.getNestedProperty(object, "path");
                if (itemPath.equals(path))
                {
                    if (object.getClass() == objectClass)
                    {
                       return true;	
                    }
                    else
                    {
                    	   return false;
                    }
 
                }
            }
            return false;
    }

	
	protected  void cleanUpRepisotory() {
		try 
		{
				Session session = this.getSession();		
				NodeIterator nodeIterator = session.getRootNode().getNodes();
		
				while (nodeIterator.hasNext())
				{
					Node node = nodeIterator.nextNode();
					if (! node.getName().startsWith("jcr:"))
					{
					    log.debug("tearDown - remove : " + node.getPath());
					    node.remove();
					}
				}
				session.save();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}


}