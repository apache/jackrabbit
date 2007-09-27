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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.DigesterTestBase;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.apache.jackrabbit.ocm.testmodel.Atomic;


/**
 * Test Query on atomic fields
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 */
public class ObjectContentManagerRemoveTest extends DigesterTestBase
{
	private final static Log log = LogFactory.getLog(ObjectContentManagerRemoveTest.class);
	private Date date = new Date();
	/**
	 * <p>Defines the test case name for junit.</p>
	 * @param testName The test case name.
	 */
	public ObjectContentManagerRemoveTest(String testName) throws Exception
	{
		super(testName);

	}

	public static Test suite()
	{
		// All methods starting with "test" will be executed in the test suite.
		return new RepositoryLifecycleTestSetup(
                new TestSuite(ObjectContentManagerRemoveTest.class));
	}

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
		this.importData(date);
        
    }
	
	public void tearDown() throws Exception {

		cleanUpRepisotory();
		super.tearDown();
		
	}

	public void testRemove()
	{

		try
		{
			
			ObjectContentManager ocm = this.getObjectContentManager();
			ocm.remove("/test5");
			ocm.save();

			assertFalse("Test5 has not been removed", ocm.objectExists("/test5"));

			QueryManager queryManager = this.getQueryManager();
			Filter filter = queryManager.createFilter(Atomic.class);
			filter.addEqualTo("booleanObject" , new Boolean(false));
			Query query = queryManager.createQuery(filter);
			ocm.remove(query);
			ocm.save();

			filter = queryManager.createFilter(Atomic.class);
			filter.setScope("//");
			query = queryManager.createQuery(filter);			
			Collection result = ocm.getObjects(query);
			assertEquals("Invalid number of objects", 5, result.size());

		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
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
				a.setPath("/test" + i);
				a.setBooleanObject(new Boolean(i % 2 == 0));
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

}