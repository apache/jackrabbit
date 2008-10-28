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
package org.apache.jackrabbit.ocm.manager.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.DigesterTestBase;
import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.apache.jackrabbit.ocm.testmodel.Page;
import org.apache.jackrabbit.ocm.testmodel.Paragraph;


/**
 * Test Object Iterator
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 */
public class DigesterIteratorQueryTest extends DigesterTestBase
{
    private final static Log log = LogFactory.getLog(DigesterIteratorQueryTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public DigesterIteratorQueryTest(String testName)  throws Exception
    {
        super(testName);
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(
                new TestSuite(DigesterIteratorQueryTest.class));
    }

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
		this.importData();

    }
    	

    /**
     * Test equalTo
     *
     */
    public void testsetScope()
    {

    	try
    	{
    		
    		  // No scope    		
    	      QueryManager queryManager = this.getQueryManager();
    	      Filter filter = queryManager.createFilter(Page.class);        	
    	      Query query = queryManager.createQuery(filter);    	
    	      ObjectContentManager ocm = this.getObjectContentManager();
    	
    	      long  start = System.currentTimeMillis();
    	      Iterator iterator = ocm.getObjectIterator(query);
    	      log.info("getObject takes : " + (System.currentTimeMillis() - start));
    	          	
    	      start = System.currentTimeMillis();
    	      Collection result = ocm.getObjects(query);
    	      log.info("getObject takes : " + (System.currentTimeMillis() - start));
    	
    	      start = System.currentTimeMillis();
    	      iterator = ocm.getObjectIterator(query);
    	      log.info("getObject takes : " + (System.currentTimeMillis() - start));       	

    	
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }

    }


    private void importData() throws JcrMappingException
    {

    	try
		{
    		ObjectContentManager ocm = getObjectContentManager();
    		
        	if (ocm.objectExists("/test"))
            {
                ocm.remove("/test");
            }
        	
			ObjectContentManagerImpl ocmImpl = (ObjectContentManagerImpl) ocm;
			
			Session session = ocmImpl.getSession();
			Node root = session.getRootNode();
			root.addNode("test");
			root.addNode("test/node1");
			root.addNode("test/node2");
			
			root.save();
			
			Page page = new Page();
			page.setPath("/test/node1/page1");
			page.setTitle("Page 1 Title");
			
			ArrayList paragraphs = new ArrayList();
			
			paragraphs.add(new Paragraph("Para 1"));
			paragraphs.add(new Paragraph("Para 2"));
			paragraphs.add(new Paragraph("Para 3"));
			paragraphs.add(new Paragraph("Another Para "));
			page.setParagraphs(paragraphs);
			
			ocm.insert(page);
						
			
			page = new Page();
			page.setPath("/test/node1/page2");
			page.setTitle("Page 2 Title");
			
			paragraphs = new ArrayList();
			
			paragraphs.add(new Paragraph("Para 1"));
			paragraphs.add(new Paragraph("Para 2"));
			paragraphs.add(new Paragraph("Para 5"));
			paragraphs.add(new Paragraph("Another Para"));
			page.setParagraphs(paragraphs);
			
			ocm.insert(page);
			
			page = new Page();
			page.setPath("/test/node2/page1");
			page.setTitle("Page 3 Title");
			
			paragraphs = new ArrayList();
			
			paragraphs.add(new Paragraph("Para 1"));
			paragraphs.add(new Paragraph("Para 4"));
			paragraphs.add(new Paragraph("Para 5"));
			paragraphs.add(new Paragraph("Another Para"));
			page.setParagraphs(paragraphs);
			
			ocm.insert(page);

			page = new Page();
			page.setPath("/test/node2/page2");
			page.setTitle("Page 4 Title");
			
			paragraphs = new ArrayList();
			
			paragraphs.add(new Paragraph("Para 6"));
			paragraphs.add(new Paragraph("Para 7"));
			paragraphs.add(new Paragraph("Para 8"));
			paragraphs.add(new Paragraph("Another Para"));
			page.setParagraphs(paragraphs);
			
			ocm.insert(page);
			
			ocm.save();
			

			
		}
		catch (RepositoryException e)
		{
			
			e.printStackTrace();
		}


    }
}