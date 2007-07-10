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

import org.apache.jackrabbit.ocm.annotation.manager.AnnotatedObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.annotation.mapper.AnnotatedObjectMapper;
import org.apache.jackrabbit.ocm.annotation.model.Address;
import org.apache.jackrabbit.ocm.annotation.model.Person;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;

/**
 * 
 * Simple test of the ability to store annotated objects
 * 
 * @author Philip Dodds
 * 
 */
public class BasicObjectTest extends TestBase {

//	public void testBasicCollectionInsert() throws Exception {
//		List<String> classNames = new ArrayList<String>();
//
//		classNames.add(Address.class.getName());
//		classNames.add(Person.class.getName());
//
//		ObjectContentManager objectContentManager = new AnnotatedObjectContentManagerImpl(
//				session, new AnnotatedObjectMapper(session, classNames));
//
//		Person philip = new Person();
//		philip.setPath("/philip");
//		Address address = new Address();
//		address.setType("work");
//		address.setCity("Los Angeles");
//		address.setState("CA");
//		philip.addAddress(address);
//
//		objectContentManager.insert(philip);
//		session.save();
//	}
	
	public void testBasicInsert() throws Exception {
		List<String> classNames = new ArrayList<String>();

		classNames.add(Address.class.getName());

		ObjectContentManager objectContentManager = new AnnotatedObjectContentManagerImpl(
				session, new AnnotatedObjectMapper(session, classNames));

		Address address = new Address();
		address.setPath("/test");
		address.setType("work");
		address.setCity("Los Angeles");
		address.setState("CA");

		objectContentManager.insert(address);
		session.save();
		
		address = (Address)objectContentManager.getObject("/test");
		assertNotNull(address);
		assertTrue("Invalid address",address.getState().equals("CA"));
		
		objectContentManager.remove("/test");
		objectContentManager.save();
		
	}
	
}
