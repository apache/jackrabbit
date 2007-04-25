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
package org.apache.portals.graffito.jcr;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
import org.apache.portals.graffito.jcr.mapper.Mapper;
import org.apache.portals.graffito.jcr.mapper.impl.DigesterMapperImpl;
import org.apache.portals.graffito.jcr.persistence.PersistenceManager;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.AtomicTypeConverterProvider;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl.BinaryTypeConverterImpl;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl.BooleanTypeConverterImpl;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl.ByteArrayTypeConverterImpl;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl.CalendarTypeConverterImpl;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl.DefaultAtomicTypeConverterProvider;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl.DoubleTypeConverterImpl;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl.IntTypeConverterImpl;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl.LongTypeConverterImpl;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl.StringTypeConverterImpl;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl.TimestampTypeConverterImpl;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl.UtilDateTypeConverterImpl;
import org.apache.portals.graffito.jcr.persistence.impl.PersistenceManagerImpl;
import org.apache.portals.graffito.jcr.persistence.inheritance.PersistenceManagerInheritanceHierarchyTest;
import org.apache.portals.graffito.jcr.persistence.objectconverter.ObjectConverter;
import org.apache.portals.graffito.jcr.persistence.objectconverter.impl.ObjectConverterImpl;
import org.apache.portals.graffito.jcr.query.QueryManager;
import org.apache.portals.graffito.jcr.query.impl.QueryManagerImpl;
import org.apache.portals.graffito.jcr.reflection.ReflectionUtils;
import org.apache.portals.graffito.jcr.repository.RepositoryUtil;
import org.apache.portals.graffito.jcr.testmodel.inheritance.Ancestor;
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

	protected PersistenceManager persistenceManager;

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
	            initPersistenceManager();
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
	 * Getter for property persistenceManager.
	 * 
	 * @return jcrSession
	 */
	public PersistenceManager getPersistenceManager()
	{
		try
		{
			if (persistenceManager == null)
			{
				initPersistenceManager();
                registerNodeTypes(getSession());
			}
			return persistenceManager;
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
                "./src/test-config/nodetypes/custom_nodetypes.xml");

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
    
	protected void initPersistenceManager() throws UnsupportedRepositoryOperationException, javax.jcr.RepositoryException
	{
		Repository repository = RepositoryUtil.getRepository("repositoryTest");
		String[] files = { "./src/test-config/jcrmapping.xml", 
						   "./src/test-config/jcrmapping-proxy.xml",
                           "./src/test-config/jcrmapping-atomic.xml",
                           "./src/test-config/jcrmapping-beandescriptor.xml",
                           "./src/test-config/jcrmapping-inheritance.xml",
                           "./src/test-config/jcrmapping-jcrnodetypes.xml", 
                           "./src/test-config/jcrmapping-uuid.xml"};
		session = RepositoryUtil.login(repository, "superuser", "superuser");
		persistenceManager = new PersistenceManagerImpl(session, files);
		
	}

	/**
	 * Setter for property jcrSession.
	 * 
	 * @param persistenceManager
	 *            The persistence manager
	 */
	public void setPersistenceManager(PersistenceManager persistenceManager)
	{
		this.persistenceManager = persistenceManager;
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
		return persistenceManager.getQueryManager();
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