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
package org.apache.jackrabbit.ocm.manager.inheritance;

import java.util.Collection;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.TestBase;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.apache.jackrabbit.ocm.testmodel.Atomic;
import org.apache.jackrabbit.ocm.testmodel.inheritance.impl.CmsObjectImpl;
import org.apache.jackrabbit.ocm.testmodel.inheritance.impl.ContentImpl;
import org.apache.jackrabbit.ocm.testmodel.inheritance.impl.DocumentImpl;
import org.apache.jackrabbit.ocm.testmodel.inheritance.impl.DocumentStream;
import org.apache.jackrabbit.ocm.testmodel.inheritance.impl.FolderImpl;

/**
 * Test inheritance with node type per concrete class (without  discreminator field)
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 */
public class PersistenceManagerInheritanceConcreteClassTest extends TestBase {
	private final static Log log = LogFactory.getLog(PersistenceManagerInheritanceConcreteClassTest.class);

	/**
	 * <p>Defines the test case name for junit.</p>
	 * @param testName The test case name.
	 */
	public PersistenceManagerInheritanceConcreteClassTest(String testName) throws Exception {
		super(testName);

	}

	public static Test suite() {
		// All methods starting with "test" will be executed in the test suite.
		return new RepositoryLifecycleTestSetup(new TestSuite(
				PersistenceManagerInheritanceConcreteClassTest.class));
	}

	public void tearDown() throws Exception {

		cleanUpRepisotory();
		super.tearDown();
		
	}


	public void testRetrieveSingleton() {

		try {
			ObjectContentManager persistenceManager = this.getPersistenceManager();

			//---------------------------------------------------------------------------------------------------------
			// Insert a  Document 
			//---------------------------------------------------------------------------------------------------------			
            DocumentImpl document = new DocumentImpl();
            document.setPath("/document1");
            document.setName("document name");
            document.setContentType("plain/text"); 
            DocumentStream documentStream = new DocumentStream();
            documentStream.setEncoding("utf-8");
            documentStream.setContent("Test Content".getBytes());
            document.setDocumentStream(documentStream);
            
            persistenceManager.insert(document);
			persistenceManager.save();
			
			
			//---------------------------------------------------------------------------------------------------------
			// Retrieve a document object
			//---------------------------------------------------------------------------------------------------------						

			document = (DocumentImpl) persistenceManager.getObject( "/document1");
			assertEquals("Document path is invalid", document.getPath(), "/document1");
			assertEquals("Content type  is invalid", document.getContentType(), "plain/text");
			assertNotNull("document stream is null", document.getDocumentStream());
			assertTrue("Invalid document stream ", document.getDocumentStream().getEncoding().equals("utf-8"));
			
			
			//---------------------------------------------------------------------------------------------------------
			// Update  a descendant object
			//---------------------------------------------------------------------------------------------------------						
			document.setName("anotherName");
			persistenceManager.update(document);
			persistenceManager.save();

			//---------------------------------------------------------------------------------------------------------
			// Retrieve the updated descendant object
			//---------------------------------------------------------------------------------------------------------						
			document = (DocumentImpl) persistenceManager.getObject( "/document1");
			assertEquals("document name is incorrect", document.getName(), "anotherName");
			assertEquals("Document path is invalid", document.getPath(), "/document1");
			assertEquals("Content type  is invalid", document.getContentType(), "plain/text");
			assertNotNull("document stream is null", document.getDocumentStream());
			assertTrue("Invalid document stream", document.getDocumentStream().getEncoding().equals("utf-8"));

			CmsObjectImpl cmsObject = (CmsObjectImpl) persistenceManager.getObject( "/document1");
			assertEquals("cmsObject name is incorrect", cmsObject.getName(), "anotherName");
			assertEquals("cmsObject path is invalid", cmsObject.getPath(), "/document1");
           			
	
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

	}

	
	public void testRetrieveCollection() {
		ObjectContentManager persistenceManager = this.getPersistenceManager();

		//---------------------------------------------------------------------------------------------------------
		// Insert cmsobjects
		//---------------------------------------------------------------------------------------------------------			
        DocumentImpl document = new DocumentImpl();
        document.setPath("/document1");
        document.setName("document name 1");
        document.setContentType("plain/text"); 
        DocumentStream documentStream = new DocumentStream();
        documentStream.setEncoding("utf-8");
        documentStream.setContent("Test Content".getBytes());
        document.setDocumentStream(documentStream);        
        persistenceManager.insert(document);
        
        document = new DocumentImpl();
        document.setPath("/document2");        
        document.setName("document name 2");
        document.setContentType("plain/text"); 
        documentStream = new DocumentStream();
        documentStream.setEncoding("utf-8");
        documentStream.setContent("Test Content".getBytes());
        document.setDocumentStream(documentStream);       
        persistenceManager.insert(document);

        document = new DocumentImpl();
        document.setPath("/document3");        
        document.setName("document 3");
        document.setContentType("plain/text"); 
        documentStream = new DocumentStream();
        documentStream.setEncoding("utf-8");
        documentStream.setContent("Test Content 3".getBytes());
        document.setDocumentStream(documentStream);       
        persistenceManager.insert(document);
        
        FolderImpl folder = new FolderImpl();
        folder.setPath("/folder1");
        folder.setName("folder1");
        persistenceManager.insert(folder);
 

        document = new DocumentImpl();        
        document.setName("document4");
        document.setContentType("plain/text"); 
        documentStream = new DocumentStream();
        documentStream.setEncoding("utf-8");
        documentStream.setContent("Test Content 4".getBytes());
        document.setDocumentStream(documentStream);       

        FolderImpl subFolder = new FolderImpl();
        subFolder.setName("subfolder");
        
        folder = new FolderImpl();
        folder.setPath("/folder2");
        folder.setName("folder2");        
        folder.addChild(document);
        folder.addChild(subFolder);
        persistenceManager.insert(folder);               		
        
        
		Atomic a = new Atomic();
		a.setPath("/atomic");
		a.setBooleanPrimitive(true);
		persistenceManager.insert(a);

		persistenceManager.save();

		//---------------------------------------------------------------------------------------------------------	
		// Retrieve Folders found on the root level
		//---------------------------------------------------------------------------------------------------------			
		QueryManager queryManager = persistenceManager.getQueryManager();
		Filter filter = queryManager.createFilter(FolderImpl.class);
		Query query = queryManager.createQuery(filter);
		filter.setScope("/");
		Collection result = persistenceManager.getObjects(query);
		assertEquals("Invalid number of folders found", result.size(), 2);
		assertTrue("Invalid item in the collection", this.contains(result, "/folder1",FolderImpl.class));
		assertTrue("Invalid item in the collection", this.contains(result, "/folder2", FolderImpl.class));		
		
	
		//---------------------------------------------------------------------------------------------------------	
		// Retrieve Documents 
		//---------------------------------------------------------------------------------------------------------			
		queryManager = persistenceManager.getQueryManager();
		filter = queryManager.createFilter(DocumentImpl.class);
		
		filter.addLike("name", "document name%");
		query = queryManager.createQuery(filter);

		result = persistenceManager.getObjects(query);
		assertEquals("Invalid number of documents  found", result.size(),2);
		assertTrue("Invalid item in the collection", this.contains(result, "/document1", DocumentImpl.class));
		assertTrue("Invalid item in the collection", this.contains(result, "/document2", DocumentImpl.class));

		
		//---------------------------------------------------------------------------------------------------------	
		// Retrieve folder2 
		//---------------------------------------------------------------------------------------------------------	
		FolderImpl folder2 = (FolderImpl) persistenceManager.getObject( "/folder2");
		assertNotNull("folder 2 is null", folder2);
		assertEquals("Invalid number of cms object  found in folder2 children", folder2.getChildren().size() ,2);
		assertTrue("Invalid item in the collection", this.contains(folder2.getChildren(), "/folder2/document4", DocumentImpl.class));
		assertTrue("Invalid item in the collection", this.contains(folder2.getChildren(), "/folder2/subfolder", FolderImpl.class));
		
		
		CmsObjectImpl cmsObject = (CmsObjectImpl) persistenceManager.getObject( "/folder2");
		assertNotNull("folder 2 is null", cmsObject);
		assertTrue("Invalid instance for folder 2",  cmsObject instanceof FolderImpl);
		assertEquals("Invalid number of documents  found in folder2 children",  folder2.getChildren().size(),2);
		assertTrue("Invalid item in the collection", this.contains(folder2.getChildren(), "/folder2/document4", DocumentImpl.class));
		assertTrue("Invalid item in the collection", this.contains(folder2.getChildren(), "/folder2/subfolder", FolderImpl.class));
		
		//---------------------------------------------------------------------------------------------------------	
		// Retrieve Contents (ancestor of Documents) 
		//---------------------------------------------------------------------------------------------------------			
		queryManager = persistenceManager.getQueryManager();
		filter = queryManager.createFilter(ContentImpl.class);
		filter.addLike("name", "document name%");
		query = queryManager.createQuery(filter);

		result = persistenceManager.getObjects(query);
		assertEquals("Invalid number of documents  found", result.size(),2);
		assertTrue("Invalid item in the collection", this.contains(result, "/document1", DocumentImpl.class));
		assertTrue("Invalid item in the collection", this.contains(result, "/document2", DocumentImpl.class));
		
				
		//---------------------------------------------------------------------------------------------------------	
		// Retrieve all cmsobjects found on the root level
		//---------------------------------------------------------------------------------------------------------					
		queryManager = persistenceManager.getQueryManager();
		filter = queryManager.createFilter(CmsObjectImpl.class);
		filter.setScope("/");
		query = queryManager.createQuery(filter);

		result = persistenceManager.getObjects(query);
		assertEquals("Invalid ancestor object found", result.size(),5);
		assertTrue("Invalid item in the collection", this.contains(result, "/document1", DocumentImpl.class));
		assertTrue("Invalid item in the collection", this.contains(result, "/document2", DocumentImpl.class));	
		assertTrue("Invalid item in the collection", this.contains(result, "/document3", DocumentImpl.class));		
		assertTrue("Invalid item in the collection", this.contains(result, "/folder1",FolderImpl.class));	
		assertTrue("Invalid item in the collection", this.contains(result, "/folder2",FolderImpl.class));

		
		//---------------------------------------------------------------------------------------------------------	
		// Retrieve all cmsobjects found anywhere
		//---------------------------------------------------------------------------------------------------------					
		queryManager = persistenceManager.getQueryManager();
		filter = queryManager.createFilter(CmsObjectImpl.class);		
		query = queryManager.createQuery(filter);

		result = persistenceManager.getObjects(query);
		assertEquals("Invalid ancestor object found", result.size(),7);
		assertTrue("Invalid item in the collection", this.contains(result, "/document1", DocumentImpl.class));
		assertTrue("Invalid item in the collection", this.contains(result, "/document2", DocumentImpl.class));	
		assertTrue("Invalid item in the collection", this.contains(result, "/document3", DocumentImpl.class));
		assertTrue("Invalid item in the collection", this.contains(result, "/folder2/document4", DocumentImpl.class));		
		assertTrue("Invalid item in the collection", this.contains(result, "/folder1",FolderImpl.class));	
		assertTrue("Invalid item in the collection", this.contains(result, "/folder2",FolderImpl.class));
		assertTrue("Invalid item in the collection", this.contains(result, "/folder2/subfolder",FolderImpl.class));
		
	}
	  
	public void testBeanCollection() {
		ObjectContentManager persistenceManager = this.getPersistenceManager();

		//---------------------------------------------------------------------------------------------------------
		// Insert cmsobjects
		//---------------------------------------------------------------------------------------------------------
		 FolderImpl folder = new FolderImpl();		
	     folder.setPath("/mainfolder");
	     folder.setName("Main folder");        
	    
	     for (int i=1; i<=100;i++)
	     {
	         DocumentImpl document = new DocumentImpl();	        
	         document.setName("document" + i);
	         document.setContentType("plain/text"); 
	         DocumentStream documentStream = new DocumentStream();
	         documentStream.setEncoding("utf-8");
	         documentStream.setContent("Test Content".getBytes());
	         document.setDocumentStream(documentStream);
	         folder.addChild(document);
	         
	         FolderImpl subFolder = new FolderImpl();
	         subFolder.setName("folder" + i);
	         subFolder.addChild(document);
	         folder.addChild(subFolder);
	         	    	 
	     }
	     log.debug("Save the folder and its 200 children");   
	     persistenceManager.insert(folder);
	     persistenceManager.save();
	     log.debug("End - Save the folder and its 200 children");

		//---------------------------------------------------------------------------------------------------------	
		// Retrieve Folder
		//---------------------------------------------------------------------------------------------------------			
		folder  = (FolderImpl) persistenceManager.getObject("/mainfolder");
		assertNotNull("Folder is null",folder);		
		Collection children = folder.getChildren();
		assertEquals("Invalid number of children", children.size(), 200);
	     for (int i=1; i<=100;i++)
	     {
		     assertTrue("Invalid item in the collection : " +"/mainfolder/document" + i , this.contains(children, "/mainfolder/document" + i,DocumentImpl.class));
		    assertTrue("Invalid item in the collection : " + "/mainfolder/folder" + i, this.contains(children, "/mainfolder/folder" + i, FolderImpl.class));
	     }
		
	
	}	
}