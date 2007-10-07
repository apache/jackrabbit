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
package org.apache.jackrabbit.ocm.manager.basic;

import java.io.ByteArrayInputStream;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import javax.jcr.Repository;
import javax.jcr.UnsupportedRepositoryOperationException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.DigesterTestBase;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.apache.jackrabbit.ocm.repository.RepositoryUtil;
import org.apache.jackrabbit.ocm.testmodel.Atomic;
import org.apache.jackrabbit.ocm.testmodel.Page;
import org.apache.jackrabbit.ocm.testmodel.Paragraph;

/**
 * Test Query on atomic fields
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 */
public class AnnotationSameNameSiblingTest extends DigesterTestBase
{
	private final static Log log = LogFactory.getLog(AnnotationSameNameSiblingTest.class);
	private Date date = new Date();
	
	/**
	 * <p>Defines the test case name for junit.</p>
	 * @param testName The test case name.
	 */
	public AnnotationSameNameSiblingTest(String testName) throws Exception
	{
		super(testName);
		
	}

	public static Test suite()
	{
		// All methods starting with "test" will be executed in the test suite.
		return new RepositoryLifecycleTestSetup(
                new TestSuite(AnnotationSameNameSiblingTest.class));
	}

	public void tearDown() throws Exception 
	{

		cleanUpRepisotory();
		super.tearDown();
		
	}
	public void testSameNameSiblings()
	{

		try
		{
			this.importData(date);
			ObjectContentManager ocm = this.getObjectContentManager();
				
			// Query all objects 
			QueryManager queryManager = this.getQueryManager();
			Filter filter = queryManager.createFilter(Atomic.class);	
			filter.setScope("/");
			Query query = queryManager.createQuery(filter);
			Collection result = ocm.getObjects(query);
            assertEquals("Incorrect number of objects found", 10, result.size());
                         
            // Get objects
            Atomic atomic = (Atomic) ocm.getObject( "/test[2]");
            assertNotNull("Object /test[2] not found", atomic);
            
            atomic = (Atomic) ocm.getObject( "/test[10]");
            assertNotNull("Object /test[2] not found", atomic);            
            
            // Update the object 
            atomic.setString("Modified Test String 10");
            ocm.update(atomic);
            ocm.save();

            // Query on the attribute "string"
            queryManager = this.getQueryManager();
			filter = queryManager.createFilter(Atomic.class);	
			filter.addLike("string", "Modified%");			
			query = queryManager.createQuery(filter);
			result = ocm.getObjects(query);
			assertTrue("Incorrect number of objects found", result.size() == 1);
            
			atomic = (Atomic) ocm.getObject(query);
			assertNotNull("Object not found", atomic);
			assertTrue("Incorrect Object", atomic.getString().equals("Modified Test String 10"));   
			
            // Delete all objects
            queryManager = this.getQueryManager();
			filter = queryManager.createFilter(Atomic.class);	
			filter.setScope("/");
			query = queryManager.createQuery(filter) ;           
            ocm.remove(query);
            ocm.save();

			result = ocm.getObjects(query);
            assertTrue("Incorrect number of objects found", result.size() == 0);
            
            
		}
		catch (Exception e)
		{
			 e.printStackTrace();
             fail();
		}

	}

	public void testUnsupportedSameNameSiblings()
	{
		     ObjectContentManager ocm = getObjectContentManager();
             try
             {
            	 
            	 Page page = new Page();
            	 page.setPath("/page");
            	 page.setTitle("Page Title");            	 
            	 ocm.insert(page);
            	 ocm.save();
            	 
            	 Paragraph p1 = new Paragraph("para1");
            	 p1.setPath("/page/paragraph");
            	 ocm.insert(p1);
            	 
            	 Paragraph p2 = new Paragraph("para1");
            	 p2.setPath("/page/paragraph");
            	 ocm.insert(p2);
             fail();            	 
            	 
             }
             catch(Exception e)
             {
    		            	 ocm.remove("/page");
            	        ocm.save();
             }
	}
	
	
	private void importData(Date date)
	{
		try
		{

			ObjectContentManager ocm = getObjectContentManager();
			
			
			for (int i = 1; i <= 10; i++)
			{
				Atomic a = new Atomic();
				a.setPath("/test");
				a.setBooleanObject(new Boolean(i%2==0));
				a.setBooleanPrimitive(true);
				a.setIntegerObject(new Integer(100 * i));
				a.setIntPrimitive(200 + i);
				a.setString("Test String " + i);
				a.setDate(date);
				Calendar calendar = Calendar.getInstance();
				calendar.set(1976, 4, 20, 15, 40);
				a.setCalendar(calendar);
				a.setDoubleObject(new Double(2.12 + i));
				a.setDoublePrimitive(1.23 + i);
				long now = System.currentTimeMillis();
				a.setTimestamp(new Timestamp(now));
				if ((i % 2) == 0)
				{
				     a.setByteArray("This is small object stored in a JCR repository".getBytes());
				     a.setInputStream(new ByteArrayInputStream("Test inputstream".getBytes()));
				}
				else
				{
					 a.setByteArray("This is small object stored in the repository".getBytes());
					 a.setInputStream(new ByteArrayInputStream("Another Stream".getBytes()));
				}
				ocm.insert(a);
				
				
			}
			ocm.save();

		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail("Exception occurs during the unit test : " + e);
		}

	}
	
	protected void initObjectContentManager() throws UnsupportedRepositoryOperationException, javax.jcr.RepositoryException
	{
		Repository repository = RepositoryUtil.getRepository("repositoryTest");
		String[] files = { "./src/test/test-config/jcrmapping-sibling.xml" };
		session = RepositoryUtil.login(repository, "superuser", "superuser");

		
		ocm = new ObjectContentManagerImpl(session, files);
		
	}	
	
}