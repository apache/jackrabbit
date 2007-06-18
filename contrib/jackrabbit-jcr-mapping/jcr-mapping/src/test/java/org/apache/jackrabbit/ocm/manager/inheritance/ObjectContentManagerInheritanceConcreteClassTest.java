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
public class ObjectContentManagerInheritanceConcreteClassTest extends TestBase {
	private final static Log log = LogFactory.getLog(ObjectContentManagerInheritanceConcreteClassTest.class);

	/**
	 * <p>Defines the test case name for junit.</p>
	 * @param testName The test case name.
	 */
	public ObjectContentManagerInheritanceConcreteClassTest(String testName) throws Exception {
		super(testName);

	}

	public static Test suite() {
		// All methods starting with "test" will be executed in the test suite.
		return new RepositoryLifecycleTestSetup(new TestSuite(
				ObjectContentManagerInheritanceConcreteClassTest.class));
	}

	public void tearDown() throws Exception {

		cleanUpRepisotory();
		super.tearDown();
		
	}


	public void testRetrieveSingleton() {

		try {
			ObjectContentManager ocm = this.getObjectContentManager();

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
            
            ocm.insert(document);
			ocm.save();
			
			
			//---------------------------------------------------------------------------------------------------------
			// Retrieve a document object
			//---------------------------------------------------------------------------------------------------------						

			document = (DocumentImpl) ocm.getObject( "/document1");
			assertEquals("Document path is invalid", document.getPath(), "/document1");
			assertEquals("Content type  is invalid", document.getContentType(), "plain/text");
			assertNotNull("document stream is null", document.getDocumentStream());
			assertTrue("Invalid document stream ", document.getDocumentStream().getEncoding().equals("utf-8"));
			
			
			//---------------------------------------------------------------------------------------------------------
			// Update  a descendant object
			//---------------------------------------------------------------------------------------------------------						
			document.setName("anotherName");
			ocm.update(document);
			ocm.save();

			//---------------------------------------------------------------------------------------------------------
			// Retrieve the updated descendant object
			//---------------------------------------------------------------------------------------------------------						
			document = (DocumentImpl) ocm.getObject( "/document1");
			assertEquals("document name is incorrect", document.getName(), "anotherName");
			assertEquals("Document path is invalid", document.getPath(), "/document1");
			assertEquals("Content type  is invalid", document.getContentType(), "plain/text");
			assertNotNull("document stream is null", document.getDocumentStream());
			assertTrue("Invalid document stream", document.getDocumentStream().getEncoding().equals("utf-8"));

			CmsObjectImpl cmsObject = (CmsObjectImpl) ocm.getObject( "/document1");
			assertEquals("cmsObject name is incorrect", cmsObject.getName(), "anotherName");
			assertEquals("cmsObject path is invalid", cmsObject.getPath(), "/document1");
           			
	
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

	}

	
	public void testRetrieveCollection() {
		ObjectContentManager ocm = this.getObjectContentManager();

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
        ocm.insert(document);
        
        document = new DocumentImpl();
        document.setPath("/document2");        
        document.setName("document name 2");
        document.setContentType("plain/text"); 
        documentStream = new DocumentStream();
        documentStream.setEncoding("utf-8");
        documentStream.setContent("Test Content".getBytes());
        document.setDocumentStream(documentStream);       
        ocm.insert(document);

        document = new DocumentImpl();
        document.setPath("/document3");        
        document.setName("document 3");
        document.setContentType("plain/text"); 
        documentStream = new DocumentStream();
        documentStream.setEncoding("utf-8");
        documentStream.setContent("Test Content 3".getBytes());
        document.setDocumentStream(documentStream);       
        ocm.insert(document);
        
        FolderImpl folder = new FolderImpl();
        folder.setPath("/folder1");
        folder.setName("folder1");
        ocm.insert(folder);
 

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
        ocm.insert(folder);               		
        
        
		Atomic a = new Atomic();
		a.setPath("/atomic");
		a.setBooleanPrimitive(true);
		ocm.insert(a);

		ocm.save();

		//---------------------------------------------------------------------------------------------------------	
		// Retrieve Folders found on the root level
		//---------------------------------------------------------------------------------------------------------			
		QueryManager queryManager = ocm.getQueryManager();
		Filter filter = queryManager.createFilter(FolderImpl.class);
		Query query = queryManager.createQuery(filter);
		filter.setScope("/");
		Collection result = ocm.getObjects(query);
		assertEquals("Invalid number of folders found", result.size(), 2);
		assertTrue("Invalid item in the collection", this.contains(result, "/folder1",FolderImpl.class));
		assertTrue("Invalid item in the collection", this.contains(result, "/folder2", FolderImpl.class));		
		
	
		//---------------------------------------------------------------------------------------------------------	
		// Retrieve Documents 
		//---------------------------------------------------------------------------------------------------------			
		queryManager = ocm.getQueryManager();
		filter = queryManager.createFilter(DocumentImpl.class);
		
		filter.addLike("name", "document name%");
		query = queryManager.createQuery(filter);

		result = ocm.getObjects(query);
		assertEquals("Invalid number of documents  found", result.size(),2);
		assertTrue("Invalid item in the collection", this.contains(result, "/document1", DocumentImpl.class));
		assertTrue("Invalid item in the collection", this.contains(result, "/document2", DocumentImpl.class));

		
		//---------------------------------------------------------------------------------------------------------	
		// Retrieve folder2 
		//---------------------------------------------------------------------------------------------------------	
		FolderImpl folder2 = (FolderImpl) ocm.getObject( "/folder2");
		assertNotNull("folder 2 is null", folder2);
		assertEquals("Invalid number of cms object  found in folder2 children", folder2.getChildren().size() ,2);
		assertTrue("Invalid item in the collection", this.contains(folder2.getChildren(), "/folder2/document4", DocumentImpl.class));
		assertTrue("Invalid item in the collection", this.contains(folder2.getChildren(), "/folder2/subfolder", FolderImpl.class));
		
		
		CmsObjectImpl cmsObject = (CmsObjectImpl) ocm.getObject( "/folder2");
		assertNotNull("folder 2 is null", cmsObject);
		assertTrue("Invalid instance for folder 2",  cmsObject instanceof FolderImpl);
		assertEquals("Invalid number of documents  found in folder2 children",  folder2.getChildren().size(),2);
		assertTrue("Invalid item in the collection", this.contains(folder2.getChildren(), "/folder2/document4", DocumentImpl.class));
		assertTrue("Invalid item in the collection", this.contains(folder2.getChildren(), "/folder2/subfolder", FolderImpl.class));
		
		//---------------------------------------------------------------------------------------------------------	
		// Retrieve Contents (ancestor of Documents) 
		//---------------------------------------------------------------------------------------------------------			
		queryManager = ocm.getQueryManager();
		filter = queryManager.createFilter(ContentImpl.class);
		filter.addLike("name", "document name%");
		query = queryManager.createQuery(filter);

		result = ocm.getObjects(query);
		assertEquals("Invalid number of documents  found", result.size(),2);
		assertTrue("Invalid item in the collection", this.contains(result, "/document1", DocumentImpl.class));
		assertTrue("Invalid item in the collection", this.contains(result, "/document2", DocumentImpl.class));
		
				
		//---------------------------------------------------------------------------------------------------------	
		// Retrieve all cmsobjects found on the root level
		//---------------------------------------------------------------------------------------------------------					
		queryManager = ocm.getQueryManager();
		filter = queryManager.createFilter(CmsObjectImpl.class);
		filter.setScope("/");
		query = queryManager.createQuery(filter);

		result = ocm.getObjects(query);
		assertEquals("Invalid ancestor object found", result.size(),5);
		assertTrue("Invalid item in the collection", this.contains(result, "/document1", DocumentImpl.class));
		assertTrue("Invalid item in the collection", this.contains(result, "/document2", DocumentImpl.class));	
		assertTrue("Invalid item in the collection", this.contains(result, "/document3", DocumentImpl.class));		
		assertTrue("Invalid item in the collection", this.contains(result, "/folder1",FolderImpl.class));	
		assertTrue("Invalid item in the collection", this.contains(result, "/folder2",FolderImpl.class));

		
		//---------------------------------------------------------------------------------------------------------	
		// Retrieve all cmsobjects found anywhere
		//---------------------------------------------------------------------------------------------------------					
		queryManager = ocm.getQueryManager();
		filter = queryManager.createFilter(CmsObjectImpl.class);		
		query = queryManager.createQuery(filter);

		result = ocm.getObjects(query);
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
		ObjectContentManager ocm = this.getObjectContentManager();

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
	     ocm.insert(folder);
	     ocm.save();
	     log.debug("End - Save the folder and its 200 children");

		//---------------------------------------------------------------------------------------------------------	
		// Retrieve Folder
		//---------------------------------------------------------------------------------------------------------			
		folder  = (FolderImpl) ocm.getObject("/mainfolder");
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