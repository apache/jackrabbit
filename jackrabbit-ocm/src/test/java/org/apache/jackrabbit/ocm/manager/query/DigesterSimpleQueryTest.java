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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.DigesterTestBase;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.apache.jackrabbit.ocm.query.impl.QueryImpl;
import org.apache.jackrabbit.ocm.testmodel.Page;
import org.apache.jackrabbit.ocm.testmodel.Paragraph;

/**
 * Test QueryManagerImpl Query methods
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe
 *         Lombart</a>
 */
public class DigesterSimpleQueryTest extends DigesterTestBase {
    private final static Log log = LogFactory.getLog(DigesterSimpleQueryTest.class);

    /**
     * <p>
     * Defines the test case name for junit.
     * </p>
     *
     * @param testName
     *            The test case name.
     */
    public DigesterSimpleQueryTest(String testName) throws Exception {
        super(testName);
    }

    public static Test suite() {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(new TestSuite(DigesterSimpleQueryTest.class));
    }

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        importData();
    }

    /**
     * Test equalTo
     *
     */
    public void testGetObjectEqualsTo() {

        try {

            // Build the Query Object
            QueryManager queryManager = this.getQueryManager();
            Filter filter = queryManager.createFilter(Paragraph.class);
            filter.addEqualTo("text", "Para 1");

            Query query = queryManager.createQuery(filter);
            

            ObjectContentManager ocm = this.getObjectContentManager();
            Paragraph paragraph = (Paragraph) ocm.getObject(query);
            assertNotNull("Object is null", paragraph);
            assertTrue("Invalid paragraph found", paragraph.getText().equals("Para 1"));

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }

    }

    /**
     * Test equalTo
     *
     */
    public void testGetObjectsEqualsTo() {

        try {

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
            assertTrue("Invalid paragraph found", paragraph.getText().equals("Para 1"));

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }

    }

    /**
     * Test the like "like" expression
     */
    public void testGetObjectsLike() {

        try {

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
            assertTrue("Invalid paragraph found", this.containsText(paragraphs,"Para 1"));
            assertTrue("Invalid paragraph found", this.containsText(paragraphs,"Para 2"));
            assertTrue("Invalid paragraph found", this.containsText(paragraphs,"Para 3"));

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }

    }

    /**
     * Build an or expression between 2 filters
     *
     */
    public void testGetObjectsOr() {

        try {

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
            assertTrue("Invalid paragraph found", this.containsText(paragraphs,"Para 1"));
            assertTrue("Invalid paragraph found", this.containsText(paragraphs,"Para 2"));

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }

    }

    /**
     * Build an or expression within a single filter
     * @author Shrirang Edgaonkar
     */
    public void testGetObjectsOrForSingleFilter() {

        try {

            // Build the Query Object
            QueryManager queryManager = this.getQueryManager();
            Filter filter1 = queryManager.createFilter(Paragraph.class);
            filter1.addOrFilter("text", new String[]{"Para 1","Para 2"});
            filter1.setScope("/test/");

            Query query = queryManager.createQuery(filter1);

            ObjectContentManager ocm = this.getObjectContentManager();
            Collection result = ocm.getObjects(query);
            assertEquals("Invalid number of objects - should be = 2", 2, result.size());

            Paragraph[] paragraphs = (Paragraph[]) result.toArray(new Paragraph[result.size()]);
            assertTrue("Invalid paragraph found", this.containsText(paragraphs,"Para 1"));
            assertTrue("Invalid paragraph found", this.containsText(paragraphs,"Para 2"));

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }

    }

    /**
     * Build an or expression within a single filter
     * @author Shrirang Edgaonkar
     */
    public void testGetObjectsOrWithAndForSingleFilter() {

        try {

            // Build the Query Object
            QueryManager queryManager = this.getQueryManager();
            Filter filter1 = queryManager.createFilter(Paragraph.class);
            filter1.addOrFilter("text", new String[]{"Para 1","Another Para "}).addLike("text", "Para%");
            filter1.setScope("/test/");

            Query query = queryManager.createQuery(filter1);

            ObjectContentManager ocm = this.getObjectContentManager();
            Collection result = ocm.getObjects(query);
            assertEquals("Invalid number of objects - should be = 1", 1, result.size());

            Paragraph[] paragraphs = (Paragraph[]) result.toArray(new Paragraph[result.size()]);
            assertTrue("Invalid paragraph found", this.containsText(paragraphs,"Para 1"));
            //assertTrue("Invalid paragraph found", this.containsText(paragraphs,"Para 2"));

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }

    }
   
    
    public void testGetObjectOrderBy() {

        try {

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
            assertTrue("Invalid paragraph found", this.containsText(paragraphs,"Para 1"));
            assertTrue("Invalid paragraph found", this.containsText(paragraphs,"Para 2"));
            assertTrue("Invalid paragraph found", this.containsText(paragraphs,"Para 3"));

	        } catch (Exception e) {
	            e.printStackTrace();
	            fail("Exception occurs during the unit test : " + e);
	        }

    	}

        
        
        public void testGetObjectOrderByWithUpdatableJCRExpression() {

            try {

                // Build the Query Object
                QueryManager queryManager = this.getQueryManager();
                Filter filter = queryManager.createFilter(Paragraph.class);
                filter.addLike("text", "Para%");
                filter.setScope("/test/");

                Query query = queryManager.createQuery(filter);
                query.addOrderByDescending("text");

                String strQueryBuilderStringWithDescending = ((QueryImpl)query).getOrderByExpression();
                
                ObjectContentManager ocm = this.getObjectContentManager();
                Collection result = ocm.getObjects(query);
                assertEquals("Invalid number of objects - should be = 3", 3, result.size());

                //Text is Descending
                Paragraph[] paragraphs = (Paragraph[]) result.toArray(new Paragraph[result.size()]);
                Iterator iterator = result.iterator();
                Paragraph para = (Paragraph)iterator.next();
                assertEquals("Para 3",para.getText());
                para = (Paragraph)iterator.next();
                assertEquals("Para 2",para.getText());
                para = (Paragraph)iterator.next();
                assertEquals("Para 1",para.getText());

                //Text is Ascending
                query = queryManager.createQuery(filter);
                query.addOrderByAscending("text");

                ocm = this.getObjectContentManager();
                result = ocm.getObjects(query);
                assertEquals("Invalid number of objects - should be = 3", 3, result.size());
                iterator = result.iterator();
                para = (Paragraph)iterator.next();
                assertEquals("Para 1",para.getText());
                para = (Paragraph)iterator.next();
                assertEquals("Para 2",para.getText());
                para = (Paragraph)iterator.next();
                assertEquals("Para 3",para.getText());

                //Text is Descending
                query = queryManager.createQuery(filter);
                ((QueryImpl)query).addJCRExpression(strQueryBuilderStringWithDescending);
                ocm = this.getObjectContentManager();
                result = ocm.getObjects(query);
                assertEquals("Invalid number of objects - should be = 3", 3, result.size());
                iterator = result.iterator();
                para = (Paragraph)iterator.next();
                assertEquals("Para 3",para.getText());
                para = (Paragraph)iterator.next();
                assertEquals("Para 2",para.getText());
                para = (Paragraph)iterator.next();
                assertEquals("Para 1",para.getText());
                
            
            } catch (Exception e) {
                e.printStackTrace();
                fail("Exception occurs during the unit test : " + e);
            }
        
        
    }

    public void testGetObjectsByClassNameAndPath() {
        try {
            ObjectContentManager ocm = this.getObjectContentManager();
            Collection result = ocm.getObjects(Page.class, "/test");
            assertEquals("Invalid number of objects", 1, result.size());

            Page[] pages = (Page[]) result.toArray(new Page[result.size()]);
            assertTrue("Invalid Page found", pages[0].getTitle().equals("Page Title"));

            result = ocm.getObjects(Page.class, "/folder/test");
            assertEquals("Invalid number of objects", 4, result.size());

            result = ocm.getObjects(Page.class, "/folder");
            assertEquals("Invalid number of objects", 0, result.size());

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }
    }

    private void importData() throws JcrMappingException
    {
        try {
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
			
			//add an extra node with other page objects
			Node root = ocm.getSession().getRootNode();
			root.addNode("folder");
			page = new Page();
			page.setPath("/folder/test");
			page.setTitle("Page Title");
			
			paragraphs = new ArrayList();
			
			paragraphs.add(new Paragraph("Para 1.1"));
			paragraphs.add(new Paragraph("Para 1.2"));
			paragraphs.add(new Paragraph("Para 1.3"));
			paragraphs.add(new Paragraph("1.Another Para "));
			page.setParagraphs(paragraphs);
			
			ocm.insert(page);

			page = new Page();
			page.setPath("/folder/test");
			page.setTitle("Page Title");
			
			paragraphs = new ArrayList();
			
			paragraphs.add(new Paragraph("Para 2.1"));
			paragraphs.add(new Paragraph("Para 2.2"));
			paragraphs.add(new Paragraph("Para 2.3"));
			paragraphs.add(new Paragraph("2.Another Para "));
			page.setParagraphs(paragraphs);
			
			ocm.insert(page);

			page = new Page();
			page.setPath("/folder/test");
			page.setTitle("Page Title");
			
			paragraphs = new ArrayList();
			
			paragraphs.add(new Paragraph("Para 3.1"));
			paragraphs.add(new Paragraph("Para 3.2"));
			paragraphs.add(new Paragraph("Para 3.3"));
			paragraphs.add(new Paragraph("3.Another Para "));
			page.setParagraphs(paragraphs);
			
			ocm.insert(page);

			page = new Page();
			page.setPath("/folder/test");
			page.setTitle("Page Title");
			
			paragraphs = new ArrayList();
			
			paragraphs.add(new Paragraph("Para 4.1"));
			paragraphs.add(new Paragraph("Para 4.2"));
			paragraphs.add(new Paragraph("Para 4.3"));
			paragraphs.add(new Paragraph("4.Another Para "));
			page.setParagraphs(paragraphs);
			
			ocm.insert(page);
			
			ocm.save();
		}
        catch (Exception e)
        {
        	fail("Impossible to create the data " + e);
        }
    }
    
    private boolean containsText(Paragraph[] paragraphs, String text)
    {
    	
    	for (int i = 0; i < paragraphs.length; i++) 
    	{
    		if (paragraphs[i].getText().equals(text))
			{
				return true;
			}	
		}
    	return false; 
    	
    }    
}