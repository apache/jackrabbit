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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.DigesterTestBase;
import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.apache.jackrabbit.ocm.testmodel.Page;
import org.apache.jackrabbit.ocm.testmodel.Paragraph;


/**
 * Test QueryManagerImpl Query methods
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 */
public class DigesterSimpleQueryTest extends DigesterTestBase
{
    private final static Log log = LogFactory.getLog(DigesterSimpleQueryTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public DigesterSimpleQueryTest(String testName)  throws Exception
    {
        super(testName);
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(
                new TestSuite(DigesterSimpleQueryTest.class));
    }

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        importData();
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    public void tearDown() throws Exception
    {
    	cleanUpRepisotory();
    	
        super.tearDown();
    }
    
    /**
     * Test equalTo
     *
     */
    public void testGetObjectEqualsTo()
    {

    	try
    	{
    		    		      		 
    		  // Build the Query Object
    	      QueryManager queryManager = this.getQueryManager();
    	      Filter filter = queryManager.createFilter(Paragraph.class);
    	      filter.addEqualTo("text", "Para 1");    	          	     
    	      
    	      
    	      Query query = queryManager.createQuery(filter);
    	      
    	      ObjectContentManager ocm = this.getObjectContentManager();
    	      Paragraph paragraph = (Paragraph) ocm.getObject(query);
    	      assertNotNull("Object is null", paragraph);    	      
    	      assertTrue("Invalid paragraph found" , paragraph.getText().equals("Para 1"));
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }
        
    }    
    
    /**
     * Test equalTo
     *
     */
    public void testGetObjectsEqualsTo()
    {

    	try
    	{
    		    		      		 
    		  // Build the Query Object
    	      QueryManager queryManager = this.getQueryManager();
    	      Filter filter = queryManager.createFilter(Paragraph.class);
    	      filter.addEqualTo("text", "Para 1");    	          	     
    	      filter.setScope("/test/");
    	      
    	      Query query = queryManager.createQuery(filter);
    	      
    	      ObjectContentManager ocm = this.getObjectContentManager();
    	      Collection result = ocm.getObjects(query);
    	      assertEquals("Invalid number of objects - should be = 1", 1, result.size());
    	      Paragraph paragraph = (Paragraph) result.iterator().next();
    	      assertTrue("Invalid paragraph found" , paragraph.getText().equals("Para 1"));
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }
        
    }

    /**
     * Test the like "like" expression
     */
    public void testGetObjectsLike()
    {

    	try
    	{
    		
    		  // Build the Query Object
    	      QueryManager queryManager = this.getQueryManager();
    	      Filter filter = queryManager.createFilter(Paragraph.class);
    	      filter.addLike("text", "Para%");    	          	     
    	      filter.setScope("/test/");
    	      
    	      Query query = queryManager.createQuery(filter);
    	      
    	      ObjectContentManager ocm = this.getObjectContentManager();
    	      Collection result = ocm.getObjects(query);
    	      assertEquals("Invalid number of objects - should be = 3", 3, result.size());

    	      Paragraph[] paragraphs = (Paragraph[]) result.toArray(new Paragraph[result.size()]);
    	      assertTrue("Invalid paragraph found", paragraphs[0].getText().equals("Para 1"));
    	      assertTrue("Invalid paragraph found", paragraphs[1].getText().equals("Para 2"));
    	      assertTrue("Invalid paragraph found", paragraphs[2].getText().equals("Para 3"));
    	      

        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }
        
    }
    
    
    /**
     * Build an or expression between 2 filters
     *
     */
    public void testGetObjectsOr()
    {

    	try
    	{
    		
    		  // Build the Query Object
    	      QueryManager queryManager = this.getQueryManager();
    	      Filter filter1 = queryManager.createFilter(Paragraph.class);
    	      filter1.addEqualTo("text", "Para 1");    	     
    	      filter1.setScope("/test/");

    	      Filter filter2 = queryManager.createFilter(Paragraph.class);
    	      filter2.addEqualTo("text", "Para 2");    	     
    	      
              filter1.addOrFilter(filter2);
    	      
    	      Query query = queryManager.createQuery(filter1);
    	      
    	      ObjectContentManager ocm = this.getObjectContentManager();
    	      Collection result = ocm.getObjects(query);
    	      assertEquals("Invalid number of objects - should be = 2", 2, result.size());
    	      
    	      Paragraph[] paragraphs = (Paragraph[]) result.toArray(new Paragraph[result.size()]);
    	      assertTrue("Invalid paragraph found", paragraphs[0].getText().equals("Para 1"));
    	      assertTrue("Invalid paragraph found", paragraphs[1].getText().equals("Para 2"));
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }
        
    }
    
    public void testGetObjectOrderBy()
    {

    	try
    	{
    		
    		  // Build the Query Object
    	      QueryManager queryManager = this.getQueryManager();
    	      Filter filter = queryManager.createFilter(Paragraph.class);
    	      filter.addLike("text", "Para%");    	          	     
    	      filter.setScope("/test/");
    	      
    	      Query query = queryManager.createQuery(filter);
    	      query.addOrderByDescending("text");
    	      
    	      ObjectContentManager ocm = this.getObjectContentManager();
    	      Collection result = ocm.getObjects(query);
    	      assertEquals("Invalid number of objects - should be = 3", 3, result.size());

    	      Paragraph[] paragraphs = (Paragraph[]) result.toArray(new Paragraph[result.size()]);
    	      assertTrue("Invalid paragraph found", paragraphs[0].getText().equals("Para 3"));
    	      assertTrue("Invalid paragraph found", paragraphs[1].getText().equals("Para 2"));
    	      assertTrue("Invalid paragraph found", paragraphs[2].getText().equals("Para 1"));
    	      

        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }
        
    }
    
    private void importData() throws JcrMappingException 
    {
    	ObjectContentManager ocm = getObjectContentManager();

        Page page = new Page();
        page.setPath("/test");
        page.setTitle("Page Title");
        
        ArrayList paragraphs = new ArrayList();
        
        paragraphs.add(new Paragraph("Para 1"));
        paragraphs.add(new Paragraph("Para 2"));
        paragraphs.add(new Paragraph("Para 3"));
        paragraphs.add(new Paragraph("Another Para "));
        page.setParagraphs(paragraphs);
        
        ocm.insert(page);  
        ocm.save();
                

    }
}