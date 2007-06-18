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
package org.apache.jackrabbit.ocm.manager.auto;

import javax.jcr.Repository;
import javax.jcr.UnsupportedRepositoryOperationException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.TestBase;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.impl.PersistenceManagerImpl;
import org.apache.jackrabbit.ocm.repository.RepositoryUtil;
import org.apache.jackrabbit.ocm.testmodel.inheritance.impl.DocumentImpl;
import org.apache.jackrabbit.ocm.testmodel.inheritance.impl.DocumentStream;
import org.apache.jackrabbit.ocm.testmodel.inheritance.impl.FolderImpl;
import org.apache.jackrabbit.ocm.testmodel.interfaces.Document;
import org.apache.jackrabbit.ocm.testmodel.interfaces.Folder;

/**
 * Test autoupdate setting
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 */
public class PersistenceManagerAutoTest extends TestBase {
	private final static Log log = LogFactory.getLog(PersistenceManagerAutoTest.class);

	/**
	 * <p>Defines the test case name for junit.</p>
	 * @param testName The test case name.
	 */
	public PersistenceManagerAutoTest(String testName) throws Exception {
		super(testName);

	}

	public static Test suite() {
		// All methods starting with "test" will be executed in the test suite.
		return new RepositoryLifecycleTestSetup(new TestSuite(
				PersistenceManagerAutoTest.class));
	}

	public void tearDown() throws Exception {

		cleanUpRepisotory();
		super.tearDown();
		
	}

	
	public void testAuto() {
		
		ObjectContentManager persistenceManager = this.getPersistenceManager();

		//---------------------------------------------------------------------------------------------------------
		// Insert cmsobjects
		//---------------------------------------------------------------------------------------------------------
	    Folder  folder = new FolderImpl();
	    folder.setPath("/folder2");
	    folder.setName("folder2");        
		
	    Document document = new DocumentImpl();
	    document.setPath("/folder2/document4");
	    document.setName("document4");
	    document.setContentType("plain/text"); 
	    DocumentStream documentStream = new DocumentStream();
	    documentStream.setEncoding("utf-8");
	    documentStream.setContent("Test Content 4".getBytes());
	    document.setDocumentStream(documentStream);       

	    Folder subFolder = new FolderImpl();
	    subFolder.setName("subfolder");
	    subFolder.setPath("/folder2/subfolder");
	    	    	    
	    folder.addChild(document);
	    folder.addChild(subFolder);
	    persistenceManager.insert(folder);               		
		persistenceManager.save();
		
		//---------------------------------------------------------------------------------------------------------	
		// Retrieve folder2 
		//---------------------------------------------------------------------------------------------------------	
		Folder folder2 = (Folder) persistenceManager.getObject( "/folder2");
		assertNotNull("folder 2 is null", folder2);
		assertEquals("Invalid number of cms object  found in folder2 children", folder2.getChildren().size() ,0); // autoInsert = false
		
		//---------------------------------------------------------------------------------------------------------	
		// Insert nested objects
		//---------------------------------------------------------------------------------------------------------
		persistenceManager.insert(subFolder);
		persistenceManager.insert(document);
		persistenceManager.save();
		
		//---------------------------------------------------------------------------------------------------------	
		// Retrieve folder2 
		//---------------------------------------------------------------------------------------------------------	
		 folder2 = (Folder) persistenceManager.getObject( "/folder2");
		assertNotNull("folder 2 is null", folder2);
		assertEquals("Invalid number of cms object  found in folder2 children", folder2.getChildren().size() ,0); // autoInsert = false

		//---------------------------------------------------------------------------------------------------------	
		// Retrieve children attribute 
		//---------------------------------------------------------------------------------------------------------			
		persistenceManager.retrieveMappedAttribute(folder2, "children");
		assertNotNull("folder 2 is null", folder2);
		assertEquals("Invalid number of cms object  found in folder2 children", folder2.getChildren().size() ,2);
		assertTrue("Invalid item in the collection", this.contains(folder2.getChildren(), "/folder2/document4", DocumentImpl.class));
		assertTrue("Invalid item in the collection", this.contains(folder2.getChildren(), "/folder2/subfolder", FolderImpl.class));		
		
		//---------------------------------------------------------------------------------------------------------	
		// Update 
		//---------------------------------------------------------------------------------------------------------	
		folder2.setChildren(null);
		persistenceManager.update(folder2); // autoupdate = true for the children attribute. So no update on the children collection
		persistenceManager.save();

		//---------------------------------------------------------------------------------------------------------	
		// Retrieve children attribute 
		//---------------------------------------------------------------------------------------------------------			
		persistenceManager.retrieveMappedAttribute(folder2, "children");
		assertNotNull("folder 2 is null", folder2);
		assertEquals("Invalid number of cms object  found in folder2 children", folder2.getChildren().size() ,2);
		assertTrue("Invalid item in the collection", this.contains(folder2.getChildren(), "/folder2/document4", DocumentImpl.class));
		assertTrue("Invalid item in the collection", this.contains(folder2.getChildren(), "/folder2/subfolder", FolderImpl.class));		
		
		
	}
	
	protected void initPersistenceManager() throws UnsupportedRepositoryOperationException, javax.jcr.RepositoryException
	{
		Repository repository = RepositoryUtil.getRepository("repositoryTest");
		String[] files = { "./src/test/test-config/jcrmapping-auto.xml"};
		session = RepositoryUtil.login(repository, "superuser", "superuser");

		persistenceManager = new PersistenceManagerImpl(session, files);
		
	}	
	
}