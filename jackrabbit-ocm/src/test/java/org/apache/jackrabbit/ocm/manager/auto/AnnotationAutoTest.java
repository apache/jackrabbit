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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Repository;
import javax.jcr.UnsupportedRepositoryOperationException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.DigesterTestBase;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.AnnotationMapperImpl;
import org.apache.jackrabbit.ocm.repository.RepositoryUtil;
import org.apache.jackrabbit.ocm.testmodel.auto.CmsObject;
import org.apache.jackrabbit.ocm.testmodel.auto.Content;
import org.apache.jackrabbit.ocm.testmodel.auto.Document;
import org.apache.jackrabbit.ocm.testmodel.auto.Folder;
import org.apache.jackrabbit.ocm.testmodel.auto.impl.CmsObjectImpl;
import org.apache.jackrabbit.ocm.testmodel.auto.impl.ContentImpl;
import org.apache.jackrabbit.ocm.testmodel.auto.impl.DocumentImpl;
import org.apache.jackrabbit.ocm.testmodel.auto.impl.DocumentStream;
import org.apache.jackrabbit.ocm.testmodel.auto.impl.FolderImpl;

/**
 * Test autoupdate setting
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 */
public class AnnotationAutoTest extends DigesterTestBase {
	private final static Log log = LogFactory.getLog(AnnotationAutoTest.class);

	/**
	 * <p>Defines the test case name for junit.</p>
	 * @param testName The test case name.
	 */
	public AnnotationAutoTest(String testName) throws Exception {
		super(testName);

	}

	public static Test suite() {
		// All methods starting with "test" will be executed in the test suite.
		return new RepositoryLifecycleTestSetup(new TestSuite(
				AnnotationAutoTest.class));
	}

	
	public void testAuto() {
		
		ObjectContentManager ocm = this.getObjectContentManager();

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
	    ocm.insert(folder);               		
		ocm.save();
		
		//---------------------------------------------------------------------------------------------------------	
		// Retrieve folder2 
		//---------------------------------------------------------------------------------------------------------	
		Folder folder2 = (Folder) ocm.getObject( "/folder2");
		assertNotNull("folder 2 is null", folder2);
		assertEquals("Invalid number of cms object  found in folder2 children", folder2.getChildren().size() ,0); // autoInsert = false
		
		//---------------------------------------------------------------------------------------------------------	
		// Insert nested objects
		//---------------------------------------------------------------------------------------------------------
		ocm.insert(subFolder);
		ocm.insert(document);
		ocm.save();
		
		//---------------------------------------------------------------------------------------------------------	
		// Retrieve folder2 
		//---------------------------------------------------------------------------------------------------------	
		 folder2 = (Folder) ocm.getObject( "/folder2");
		assertNotNull("folder 2 is null", folder2);
		assertEquals("Invalid number of cms object  found in folder2 children", folder2.getChildren().size() ,0); // autoInsert = false

		//---------------------------------------------------------------------------------------------------------	
		// Retrieve children attribute 
		//---------------------------------------------------------------------------------------------------------			
		ocm.retrieveMappedAttribute(folder2, "children");
		assertNotNull("folder 2 is null", folder2);
		assertEquals("Invalid number of cms object  found in folder2 children", folder2.getChildren().size() ,2);
		assertTrue("Invalid item in the collection", this.contains(folder2.getChildren(), "/folder2/document4", DocumentImpl.class));
		assertTrue("Invalid item in the collection", this.contains(folder2.getChildren(), "/folder2/subfolder", FolderImpl.class));		
		
		//---------------------------------------------------------------------------------------------------------	
		// Update 
		//---------------------------------------------------------------------------------------------------------	
		folder2.setChildren(null);
		ocm.update(folder2); // autoupdate = false for the children attribute. So no update on the children collection
		ocm.save();

		//---------------------------------------------------------------------------------------------------------	
		// Retrieve children attribute 
		//---------------------------------------------------------------------------------------------------------			
		ocm.retrieveMappedAttribute(folder2, "children");
		assertNotNull("folder 2 is null", folder2);
		assertEquals("Invalid number of cms object  found in folder2 children", folder2.getChildren().size() ,2);
		assertTrue("Invalid item in the collection", this.contains(folder2.getChildren(), "/folder2/document4", DocumentImpl.class));
		assertTrue("Invalid item in the collection", this.contains(folder2.getChildren(), "/folder2/subfolder", FolderImpl.class));		
		
		
	}
	
	protected void initObjectContentManager() throws UnsupportedRepositoryOperationException, javax.jcr.RepositoryException
	{
		Repository repository = RepositoryUtil.getRepository("repositoryTest");	
		session = RepositoryUtil.login(repository, "superuser", "superuser");
		List<Class> classes = new ArrayList<Class>();
		
		classes.add(CmsObject.class);
		classes.add(Content.class);
		classes.add(Folder.class);
		classes.add(Document.class);
		classes.add(CmsObjectImpl.class);
		classes.add(ContentImpl.class);
		classes.add(FolderImpl.class);
		classes.add(DocumentImpl.class);
		
		Mapper mapper = new AnnotationMapperImpl(classes);
		ocm = new ObjectContentManagerImpl(session, mapper);

		
	}	
}