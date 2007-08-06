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
package org.apache.jackrabbit.ocm.annotation;

import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.ocm.annotation.mapper.AnnotatedObjectMapper;
import org.apache.jackrabbit.ocm.annotation.model.unstructured.UnstructuredAddress;
import org.apache.jackrabbit.ocm.annotation.model.unstructured.UnstructuredPerson;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.nodemanagement.impl.jackrabbit.NodeTypeManagerImpl;

/**
 * 
 * 
 * Unit test for object mapping into a nt:unstructured jcr node type 
 * 
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
 * 
 */
public class UnstructuredMappingTest extends TestBase {
	
	public void tearDown() throws Exception {

		cleanUpRepisotory();
		super.tearDown();
		
	}
	
	public void testBasic() throws Exception {
		List<String> classNames = new ArrayList<String>();

		classNames.add(UnstructuredAddress.class.getName());

		ObjectContentManager objectContentManager = new ObjectContentManagerImpl(
				session, (Mapper) new AnnotatedObjectMapper(session, classNames, new NodeTypeManagerImpl()));

		// -----------------------------------------------------------------------------
		// Insert 
        // -----------------------------------------------------------------------------
		UnstructuredAddress address = new UnstructuredAddress();
		address.setPath("/test");
		address.setType("work");
		address.setCity("Los Angeles");
		address.setState("CA");

		objectContentManager.insert(address);
		session.save();

		// -----------------------------------------------------------------------------
		// Retrieve
        // -----------------------------------------------------------------------------		
		address = (UnstructuredAddress)objectContentManager.getObject("/test");
		assertNotNull(address);
		assertTrue("Invalid address",address.getType().equals("work"));
		
		// -----------------------------------------------------------------------------
		// Update 
        // -----------------------------------------------------------------------------		
		address.setType("home");
		objectContentManager.update(address);
		session.save();

		// -----------------------------------------------------------------------------
		// Retrieve
        // -----------------------------------------------------------------------------		
		address = (UnstructuredAddress)objectContentManager.getObject("/test");
		assertNotNull(address);
		assertTrue("Invalid address",address.getType().equals("home"));

		// -----------------------------------------------------------------------------
		// Remove
        // -----------------------------------------------------------------------------		
		objectContentManager.remove("/test");
		objectContentManager.save();
		
	}
	
	public void testCollection() throws Exception 
	{
		
			List<String> classNames = new ArrayList<String>();

			classNames.add(UnstructuredAddress.class.getName());
			classNames.add(UnstructuredPerson.class.getName());

			ObjectContentManager ocm = new ObjectContentManagerImpl(
					session, (Mapper) new AnnotatedObjectMapper(session, classNames, new NodeTypeManagerImpl()));

			
			// -----------------------------------------------------------------------------
			// Insert 
	        // -----------------------------------------------------------------------------
			UnstructuredPerson philip = new UnstructuredPerson();
			philip.setPath("/test");
			UnstructuredAddress address = new UnstructuredAddress();
			address.setType("work");
			address.setCity("Los Angeles");
			address.setState("CA");
			philip.addAddress(address);

			address = new UnstructuredAddress();
			address.setType("home");
			address.setCity("Santa Monica");
			address.setState("CA");
			philip.addAddress(address);
			
			
			
			ocm.insert(philip);
			session.save();
			
					
			// -----------------------------------------------------------------------------
			// Retrieve
	        // -----------------------------------------------------------------------------
			UnstructuredPerson person = (UnstructuredPerson) ocm.getObject("/test");
			assertNotNull(person);
			assertTrue("Invalid person", person.getPath().equals("/test"));
			assertEquals("Invalid number of adress", person.getAddresses().size(), 2);
			assertTrue("Invalid adress", person.getAddresses().get(0).getType().equals("work"));

			// -----------------------------------------------------------------------------
			// Update 
	        // -----------------------------------------------------------------------------		
			address = new UnstructuredAddress();
			address.setType("vacation");
			address.setCity("AA");
			address.setState("BB");
			person.addAddress(address);
	
			address = new UnstructuredAddress();
			address.setType("anotheradress");
			address.setCity("acity");
			address.setState("state");
			person.setAnotherAdress(address);
			
			ocm.update(person);
			session.save();

			// -----------------------------------------------------------------------------
			// Retrieve
	        // -----------------------------------------------------------------------------
			person = (UnstructuredPerson) ocm.getObject("/test");
			assertNotNull(person);
			assertNotNull(person.getAnotherAdress());
			assertTrue("Invalid person", person.getPath().equals("/test"));
			assertEquals("Invalid number of adress", person.getAddresses().size(), 3);
			assertTrue("Invalid adress", person.getAddresses().get(2).getType().equals("vacation"));
			
			// -----------------------------------------------------------------------------
			// Remove
	        // -----------------------------------------------------------------------------		
			ocm.remove("/test");
			ocm.save();
			
			
			
		}	
}
