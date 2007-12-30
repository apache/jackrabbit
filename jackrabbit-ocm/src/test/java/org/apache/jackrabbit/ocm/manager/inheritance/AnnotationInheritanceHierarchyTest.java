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
import org.apache.jackrabbit.ocm.AnnotationTestBase;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.apache.jackrabbit.ocm.testmodel.Atomic;
import org.apache.jackrabbit.ocm.testmodel.inheritance.Ancestor;
import org.apache.jackrabbit.ocm.testmodel.inheritance.AnotherDescendant;
import org.apache.jackrabbit.ocm.testmodel.inheritance.Descendant;
import org.apache.jackrabbit.ocm.testmodel.inheritance.SubDescendant;

/**
 * Test inheritance with node type per hierarchy stategy (with discreminator field)
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 */
public class AnnotationInheritanceHierarchyTest extends AnnotationTestBase {
	private final static Log log = LogFactory.getLog(AnnotationInheritanceHierarchyTest.class);

	/**
	 * <p>Defines the test case name for junit.</p>
	 * @param testName The test case name.
	 */
	public AnnotationInheritanceHierarchyTest(String testName) throws Exception {
		super(testName);

	}

	public static Test suite() {
		// All methods starting with "test" will be executed in the test suite.
		return new RepositoryLifecycleTestSetup(new TestSuite(
				AnnotationInheritanceHierarchyTest.class));
	}


	public void testRetrieveSingleton() {

		try {
			ObjectContentManager ocm = this.getObjectContentManager();

			//---------------------------------------------------------------------------------------------------------
			// Insert a descendant object
			//---------------------------------------------------------------------------------------------------------			
			Descendant descendant = new Descendant();
			descendant.setDescendantField("descendantValue");
			descendant.setAncestorField("ancestorValue");
			descendant.setIntField(200);
			descendant.setPath("/test");
			ocm.insert(descendant);
			ocm.save();

			//---------------------------------------------------------------------------------------------------------
			// Retrieve a descendant object
			//---------------------------------------------------------------------------------------------------------						
			descendant = null;
			descendant = (Descendant) ocm.getObject(	 "/test");
			assertEquals("Descendant path is invalid", descendant.getPath(), "/test");
			assertEquals("Descendant ancestorField is invalid", descendant.getAncestorField(), "ancestorValue");
			assertEquals("Descendant descendantField is invalid", descendant.getDescendantField(), "descendantValue");
			assertEquals("Descendant intField is invalid", descendant.getIntField(), 200);

			//---------------------------------------------------------------------------------------------------------
			// Update  a descendant object
			//---------------------------------------------------------------------------------------------------------						
			descendant.setAncestorField("anotherAncestorValue");
			descendant.setIntField(123);
			ocm.update(descendant);
			ocm.save();

			//---------------------------------------------------------------------------------------------------------
			// Retrieve the updated descendant object
			//---------------------------------------------------------------------------------------------------------						
			descendant = null;
			descendant = (Descendant) ocm.getObject(	 "/test");
			assertEquals("Descendant path is invalid", descendant.getPath(), "/test");
			assertEquals("Descendant ancestorField is invalid", descendant.getAncestorField(), "anotherAncestorValue");
			assertEquals("Descendant descendantField is invalid", descendant	.getDescendantField(), "descendantValue");
			assertEquals("Descendant intField is invalid", descendant.getIntField(), 123);

			Ancestor ancestor = (Ancestor) ocm.getObject("/test");
			assertTrue("Invalid object instance", ancestor instanceof Descendant );
			assertEquals("Ancestor  path is invalid", ancestor.getPath(), "/test");
			assertEquals("Ancestor ancestorField is invalid", ancestor.getAncestorField(), "anotherAncestorValue");
			
			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

	}

	
	public void testRetrieveCollection() {
		ObjectContentManager ocm = this.getObjectContentManager();

		//---------------------------------------------------------------------------------------------------------	
		// Insert  descendant objects
		//---------------------------------------------------------------------------------------------------------			
		Descendant descendant = new Descendant();
		descendant.setDescendantField("descendantValue");
		descendant.setAncestorField("ancestorValue");
		descendant.setPath("/descendant1");
		ocm.insert(descendant);

		descendant = new Descendant();
		descendant.setDescendantField("descendantValue2");
		descendant.setAncestorField("ancestorValue2");
		descendant.setPath("/descendant2");
		ocm.insert(descendant);

		SubDescendant subDescendant = new SubDescendant();
		subDescendant.setDescendantField("descendantValue2");
		subDescendant.setAncestorField("ancestorValue2");
		subDescendant.setPath("/subdescendant");
		subDescendant.setSubDescendantField("subdescendantvalue");
		ocm.insert(subDescendant);		

		 subDescendant = new SubDescendant();
		subDescendant.setDescendantField("descendantValue3");
		subDescendant.setAncestorField("ancestorValue2");
		subDescendant.setPath("/subdescendant2");
		subDescendant.setSubDescendantField("subdescendantvalue1");
		ocm.insert(subDescendant);		
		
		
		AnotherDescendant anotherDescendant = new AnotherDescendant();
		anotherDescendant.setAnotherDescendantField("anotherDescendantValue");
		anotherDescendant.setAncestorField("ancestorValue3");
		anotherDescendant.setPath("/anotherdescendant1");
		ocm.insert(anotherDescendant);

		anotherDescendant = new AnotherDescendant();
		anotherDescendant.setAnotherDescendantField("anotherDescendantValue");
		anotherDescendant.setAncestorField("ancestorValue4");
		anotherDescendant.setPath("/anotherdescendant2");
		ocm.insert(anotherDescendant);

		anotherDescendant = new AnotherDescendant();
		anotherDescendant.setAnotherDescendantField("anotherDescendantValue2");
		anotherDescendant.setAncestorField("ancestorValue5");
		anotherDescendant.setPath("/anotherdescendant3");
		ocm.insert(anotherDescendant);

		
		Atomic a = new Atomic();
		a.setPath("/atomic");
		a.setBooleanPrimitive(true);
		ocm.insert(a);

		ocm.save();

		//---------------------------------------------------------------------------------------------------------	
		// Retrieve Descendant class
		//---------------------------------------------------------------------------------------------------------			
		QueryManager queryManager = ocm.getQueryManager();
		Filter filter = queryManager.createFilter(Descendant.class);
		Query query = queryManager.createQuery(filter);

		Collection result = ocm.getObjects(query);
		assertEquals("Invalid number of Descendant found", result.size(), 4);
		assertTrue("Invalid item in the collection", this.contains(result, "/descendant1", Descendant.class));
		assertTrue("Invalid item in the collection", this.contains(result, "/descendant2", Descendant.class));
		assertTrue("Invalid item in the collection", this.contains(result, "/subdescendant", SubDescendant.class));
		assertTrue("Invalid item in the collection", this.contains(result, "/subdescendant2", SubDescendant.class));
		

		//---------------------------------------------------------------------------------------------------------	
		// Retrieve AnotherDescendant class
		//---------------------------------------------------------------------------------------------------------			
		queryManager = ocm.getQueryManager();
		filter = queryManager.createFilter(AnotherDescendant.class);
		filter.addEqualTo("anotherDescendantField", "anotherDescendantValue");
		query = queryManager.createQuery(filter);

		result = ocm.getObjects(query);
		assertEquals("Invalid number of AnotherDescendant found", result.size(),2);
		assertTrue("Invalid item in the collection", this.contains(result, "/anotherdescendant1", AnotherDescendant.class));
		assertTrue("Invalid item in the collection", this.contains(result, "/anotherdescendant2", AnotherDescendant.class));

		//---------------------------------------------------------------------------------------------------------	
		// Retrieve some descendants & subdescendants
		//---------------------------------------------------------------------------------------------------------			
		queryManager = ocm.getQueryManager();
		filter = queryManager.createFilter(Descendant.class);		
		filter.addEqualTo("descendantField","descendantValue2");
		query = queryManager.createQuery(filter);

		result = ocm.getObjects(query);
		assertEquals("Invalid ancestor object found", result.size(),2);
		assertTrue("Invalid item in the collection", this.contains(result, "/descendant2", Descendant.class));
		assertTrue("Invalid item in the collection", this.contains(result, "/subdescendant", SubDescendant.class));
		
		//---------------------------------------------------------------------------------------------------------	
		// Retrieve all class
		//---------------------------------------------------------------------------------------------------------			
		queryManager = ocm.getQueryManager();
		filter = queryManager.createFilter(Ancestor.class);		
		query = queryManager.createQuery(filter);

		result = ocm.getObjects(query);
		assertEquals("Invalid ancestor object found", result.size(),7);
		assertTrue("Invalid item in the collection", this.contains(result, "/descendant1", Descendant.class));
		assertTrue("Invalid item in the collection", this.contains(result, "/descendant2", Descendant.class));
		assertTrue("Invalid item in the collection", this.contains(result, "/subdescendant", SubDescendant.class));
		assertTrue("Invalid item in the collection", this.contains(result, "/subdescendant2", SubDescendant.class));
		assertTrue("Invalid item in the collection", this.contains(result, "/anotherdescendant1", AnotherDescendant.class));
		assertTrue("Invalid item in the collection", this.contains(result, "/anotherdescendant2", AnotherDescendant.class));
		assertTrue("Invalid item in the collection", this.contains(result, "/anotherdescendant3", AnotherDescendant.class));		

 
	}
	    
}