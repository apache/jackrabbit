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
package org.apache.portals.graffito.jcr.persistence.jcrnodetype;

import java.io.ByteArrayInputStream;

import java.util.Calendar;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.portals.graffito.jcr.RepositoryLifecycleTestSetup;
import org.apache.portals.graffito.jcr.TestBase;
import org.apache.portals.graffito.jcr.persistence.PersistenceManager;
import org.apache.portals.graffito.jcr.testmodel.File;
import org.apache.portals.graffito.jcr.testmodel.Folder;
import org.apache.portals.graffito.jcr.testmodel.Resource;

/**
 * Test inheritance with node type per concrete class (without  discreminator field)
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 */
public class PersistenceManagerJcrNodeTypeTest extends TestBase {
	private final static Log log = LogFactory.getLog(PersistenceManagerJcrNodeTypeTest.class);

	/**
	 * <p>Defines the test case name for junit.</p>
	 * @param testName The test case name.
	 */
	public PersistenceManagerJcrNodeTypeTest(String testName) throws Exception {
		super(testName);

	}

	public static Test suite() {
		// All methods starting with "test" will be executed in the test suite.
		return new RepositoryLifecycleTestSetup(new TestSuite(
				PersistenceManagerJcrNodeTypeTest.class));
	}

	public void tearDown() throws Exception {

		cleanUpRepisotory();
		super.tearDown();
		
	}


	public void testRetrieveSingleton() 
	{

		try 
		{
			PersistenceManager persistenceManager = this.getPersistenceManager();

			//---------------------------------------------------------------------------------------------------------
			// Insert a  folder (class mapped to jcr:folder) with one file (class mapped to jcr:file)
			//---------------------------------------------------------------------------------------------------------			
            Resource resource = new Resource();
            resource.setData(new ByteArrayInputStream("this is the content".getBytes()));            
            resource.setLastModified(Calendar.getInstance());
            resource.setMimeType("plain/text");
            File file = new File();    
            file.setResource(resource);
            
            
            Folder folder = new Folder();
            folder.setPath("/folder1");
            folder.addChild(file);
            
            persistenceManager.insert(folder);            
			persistenceManager.save();
			
			
			//---------------------------------------------------------------------------------------------------------
			// Retrieve a document object
			//---------------------------------------------------------------------------------------------------------						
			folder = (Folder) persistenceManager.getObject( "/folder1");
			assertNotNull("folder is null", folder);
			System.out.println("Folder creation date : " + folder.getCreationDate());
			assertTrue("Invalid number of children", folder.getChildren().size() == 1);
			file = (File) folder.getChildren().iterator().next();
			assertNotNull("resource is null", file.getResource());	
			System.out.println("File resource calendar: " + file.getResource().getLastModified());	// The prop is autocreated
			
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
		}
			
	}	
}