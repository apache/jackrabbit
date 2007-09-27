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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.DigesterTestBase;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.testmodel.A;
import org.apache.jackrabbit.ocm.testmodel.C;
import org.apache.jackrabbit.ocm.testmodel.Page;
import org.apache.jackrabbit.ocm.testmodel.Paragraph;

/**
 * Test NTCollectionConverterImpl
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 */
public class NTCollectionConverterImplTest extends DigesterTestBase
{
    private final static Log log = LogFactory.getLog(NTCollectionConverterImplTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public NTCollectionConverterImplTest(String testName)  throws Exception
    {
        super(testName);
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(
                new TestSuite(NTCollectionConverterImplTest.class));
    }
    
    
    /**
     * @see junit.framework.TestCase#tearDown()
     */
    public void tearDown() throws Exception
    {
        if (getObjectContentManager().objectExists("/test"))
        {
            getObjectContentManager().remove("/test");
            getObjectContentManager().save();
        }        
    	
        super.tearDown();
    }       

    public void testCollection()
    {
        try
        {
        	ObjectContentManager ocm = getObjectContentManager();
            
        	// --------------------------------------------------------------------------------
            // Create and store an object graph in the repository with a null collection
            // --------------------------------------------------------------------------------

            Page page = new Page();
            page.setPath("/test");
            page.setTitle("Page Title");
            
            ocm.insert(page);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------           
            page = (Page) ocm.getObject( "/test");
            assertNull("page.getParagraphs is not null", page.getParagraphs());
            assertTrue("Incorrect page title", page.getTitle().equals("Page Title"));                        
            
            // --------------------------------------------------------------------------------
            // Create and store an object graph in the repository
            // --------------------------------------------------------------------------------
            ArrayList paragraphs = new ArrayList();
            
            paragraphs.add(new Paragraph("Para 1"));
            paragraphs.add(new Paragraph("Para 2"));
            paragraphs.add(new Paragraph("Para 3"));
            page.setParagraphs(paragraphs);
            
            ocm.update(page);
            ocm.save();
            
            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------           
            page = (Page) ocm.getObject( "/test");
            assertNotNull("page.getParagraphs is null", page.getParagraphs());
            assertTrue("Incorrect page title", page.getTitle().equals("Page Title"));
            assertTrue("Incorrect page.getParagraphs size", page.getParagraphs().size() == 3);
            assertTrue("Incorrect para element", ((Paragraph) page.getParagraphs().iterator().next()).getText().equals("Para 1"));
            
            // --------------------------------------------------------------------------------
            // Update the object
            // --------------------------------------------------------------------------------
            paragraphs = new ArrayList();
            
            paragraphs.add(new Paragraph("Para 1"));
            paragraphs.add(new Paragraph("Para 2"));
            paragraphs.add(new Paragraph("Para 4"));
            paragraphs.add(new Paragraph("Para 5"));
            page.setParagraphs(paragraphs);
            
            ocm.update(page);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------           

            page = (Page) ocm.getObject( "/test");
            assertNotNull("page.getParagraphs is null", page.getParagraphs());
            assertTrue("Incorrect page title", page.getTitle().equals("Page Title"));
            assertTrue("Incorrect page.getParagraphs size", page.getParagraphs().size() == 4);
            assertTrue("Incorrect para element", ((Paragraph) page.getParagraphs().iterator().next()).getText().equals("Para 1"));
            
            // --------------------------------------------------------------------------------
            // Export to check the content
            // --------------------------------------------------------------------------------           
            this.exportDocument("target/NTCollectionExport.xml", "/test", true, false);
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }
        
    }
   
}