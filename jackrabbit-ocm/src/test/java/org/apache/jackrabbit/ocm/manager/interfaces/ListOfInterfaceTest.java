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
package org.apache.jackrabbit.ocm.manager.interfaces;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.AnnotationTestBase;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.testmodel.interfaces.EntityA;
import org.apache.jackrabbit.ocm.testmodel.interfaces.EntityB;
import org.apache.jackrabbit.ocm.testmodel.interfaces.MyInterface;


public class ListOfInterfaceTest extends AnnotationTestBase
{
	private final static Log log = LogFactory.getLog(ListOfInterfaceTest.class);

	/**
	 * <p>Defines the test case name for junit.</p>
	 * @param testName The test case name.
	 */
	public ListOfInterfaceTest(String testName) throws Exception {
		super(testName);

	}

	public static Test suite() {
		// All methods starting with "test" will be executed in the test suite.
		return new RepositoryLifecycleTestSetup(new TestSuite(
				ListOfInterfaceTest.class));
	}



	public void testListOfInterface() {

		try {
			ObjectContentManager ocm = this.getObjectContentManager();

			EntityA a = new EntityA();
			a.setPath("/test");

			List<MyInterface> bList = new ArrayList<MyInterface>();
			EntityB b = new EntityB();
			b.setId("ID_B");
			b.setName("NAME_B");
			bList.add(b);

			a.setEntityB(bList);

			ocm.insert(a);
			ocm.save();
			
			
			a = (EntityA) ocm.getObject("/test");
			bList = a.getEntityB();
			assertNotNull(bList);
			assertEquals(1, bList.size());
			
			
			ocm.remove("/test");
			ocm.save();
			


			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

	}
	


	
}