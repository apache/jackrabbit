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
package org.apache.jackrabbit.ocm.manager.collectionconverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.DigesterTestBase;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.testmodel.collection.ArrayListElement;
import org.apache.jackrabbit.ocm.testmodel.collection.Element;
import org.apache.jackrabbit.ocm.testmodel.collection.Main;
import org.apache.jackrabbit.ocm.testmodel.collection.Person;

/**
 * @author <a href="mailto:boni.g@bioimagene.com">Boni Gopalan</a>
 */
public class DigesterPersonListTest extends DigesterTestBase
{
    private final static Log log = LogFactory.getLog(DigesterPersonListTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public DigesterPersonListTest(String testName)  throws Exception
    {
        super(testName);
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(new TestSuite(DigesterPersonListTest.class));
    }

    public void testPersonList()
    {
        try
        {
        	ObjectContentManager ocm = getObjectContentManager();
        	Person aPerson = buildPerson("PERSON1");
        	aPerson.setPath("/person");
        	ocm.insert(aPerson);
        	ocm.save();
        	assertNotNull(aPerson.getId());
        	String oldParentId = new String(aPerson.getId().toCharArray());
        	List<String> childIds = new ArrayList<String>(); 
        	for (Person p : aPerson.getChildren()){
        		assertNotNull(p.getId());
        		childIds.add(new String(p.getId().toCharArray()));
        	}
        	aPerson.setName("UPDATED1");
        	ocm.update(aPerson);
        	Person fb1Person = (Person)ocm.getObject("/person");
        	assertNotNull(fb1Person);
        	assertEquals("UPDATED1", fb1Person.getName());
        	assertEquals(oldParentId, fb1Person.getId());
        	
        	//To assert that the ids of the objects in the 
        	//collection has not changed during update.
        	for (Person p : fb1Person.getChildren()){
        		assertTrue(childIds.contains(p.getId()));
        	}
        	
        	Person newChild = new Person();
        	newChild.setName("CHILD2");
        	
        	fb1Person.getChildren().add(newChild);
        	ocm.update(fb1Person);
        	
        	Person fb2Person = (Person)ocm.getObject("/person");
        	assertNotNull(fb2Person);
        	assertEquals("UPDATED1", fb2Person.getName());
        	assertEquals(oldParentId, fb2Person.getId());
        	
        	//To assert that the ids of the objects in the 
        	//collection has not changed during update.
        	String child2Id = null;
        	for (Person p : fb2Person.getChildren()){
        		if (!"CHILD2".equals(p.getName()))
        			assertTrue(childIds.contains(p.getId()));
        		else{
        			assertNotNull(p.getId());
        			child2Id = new String(p.getId().toCharArray());
        			assertFalse(childIds.contains(p.getId()));
        		}
        	}
        	
        	//Now remove everyone but CHILD2 and do the update once again
        	List<Person> peopleToRemove = new ArrayList<Person>();
        	for (Person p : fb2Person.getChildren()){
        		if (!"CHILD2".equals(p.getName()))
        			peopleToRemove.add(p);
        	}
        	
        	for (Person p : peopleToRemove){
        		fb2Person.getChildren().remove(p);
        	}
        	
        	ocm.update(fb2Person);
        	
        	Person fb3Person = (Person)ocm.getObject("/person");
        	assertNotNull(fb3Person);
        	assertEquals(1, fb3Person.getChildren().size());
        	assertEquals(child2Id, fb3Person.getChildren().get(0).getId());
        	
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }
    }
    
    public Person buildPerson(String name){
    	Person p = new Person();
    	p.setName(name);
    	Person aChild = new Person();
    	aChild.setName("CHILD1");
    	List<Person> children = new ArrayList<Person>();
    	children.add(aChild);
    	p.setChildren(children);
    	return p;
    }



}
