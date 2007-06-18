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
package org.apache.jackrabbit.ocm.manager.beanconverter;



import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.TestBase;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.testmodel.A;
import org.apache.jackrabbit.ocm.testmodel.B;
import org.apache.jackrabbit.ocm.testmodel.D;
import org.apache.jackrabbit.ocm.testmodel.DFull;
import org.apache.jackrabbit.ocm.testmodel.E;
import org.apache.jackrabbit.ocm.testmodel.Page;
import org.apache.jackrabbit.ocm.testmodel.Paragraph;

/**
 * ObjectConverter test for bean-descriptor with inner bean inlined and inner bean with
 * custom converter.
 * 
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class BeanDescriptorTest extends TestBase {

    
    public BeanDescriptorTest(String testname) {
        super(testname);
    }

    public static Test suite() {

        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(new TestSuite(BeanDescriptorTest.class));
    }
    
    
    /**
     * @see org.apache.jackrabbit.ocm.TestBase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        clean();
    }

    
    /**
     * @see org.apache.jackrabbit.ocm.TestBase#tearDown()
     */
    public void tearDown() throws Exception {
        clean();
        super.tearDown();
    }

    private void clean() throws Exception {
        if(getSession().itemExists("/someD")) 
        {
            getSession().getItem("/someD").remove();
            getSession().save();
        }
        
        if (getPersistenceManager().objectExists("/test"))
        {
            getPersistenceManager().remove("/test");
            getPersistenceManager().save();
        }           

    }
    
    public void testBasic() throws Exception 
    {
    	
    	try 
    	{
    		// ------------------------------------------------------------------------
    		// Create a main object (a) with a null attribute (A.b)
    		// ------------------------------------------------------------------------    		
			A a = new A();
			a.setPath("/test");
			a.setA1("a1");
			persistenceManager.insert(a);
			persistenceManager.save();
			
    		// ------------------------------------------------------------------------
    		// Retrieve 
    		// ------------------------------------------------------------------------
			a = (A) persistenceManager.getObject("/test");
			assertNotNull("Object is null", a);
			assertNull("attribute is not null", a.getB());
			
			B b = new B();
			b.setB1("b1");
			b.setB2("b2");
			a.setB(b);
			
			persistenceManager.update(a);
			persistenceManager.save();

    		// ------------------------------------------------------------------------
    		// Retrieve 
    		// ------------------------------------------------------------------------
			a = (A) persistenceManager.getObject("/test");
			assertNotNull("Object is null", a);
			assertNotNull("attribute is null", a.getB());
			
    		// ------------------------------------------------------------------------
			// Remove object
    		// ------------------------------------------------------------------------			
			persistenceManager.remove("/test");
			persistenceManager.save();
		} 
    	catch (RuntimeException e) 
    	{
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);    		
		}
    	
    	
    }
    public void testInlined() throws Exception {
        
        B expB = new B();
        expB.setB1("b1value");
        expB.setB2("b2value");
        D expD = new D();
        expD.setPath("/someD");
        expD.setD1("d1value");
        expD.setB1(expB);
        
       persistenceManager.insert( expD);
       persistenceManager.save();
        
        D actD = (D) persistenceManager.getObject( "/someD");
        
        assertEquals(expD.getD1(), actD.getD1());
        assertEquals(expB.getB1(), actD.getB1().getB1());
        assertEquals(expB.getB2(), actD.getB1().getB2());
        
        DFull actDFull = (DFull) persistenceManager.getObject( DFull.class,  "/someD");
        
        assertEquals(expD.getD1(), actDFull.getD1());
        assertEquals(expB.getB1(), actDFull.getB1());
        assertEquals(expB.getB2(), actDFull.getB2());
        
        expB.setB1("updatedvalue1");
        
        persistenceManager.update( expD);
        getSession().save();
        
        actD = (D) persistenceManager.getObject( "/someD");
        
        assertEquals(expD.getD1(), actD.getD1());
        assertEquals(expB.getB1(), actD.getB1().getB1());
        assertEquals(expB.getB2(), actD.getB1().getB2());
        
        actDFull = (DFull) persistenceManager.getObject( DFull.class,  "/someD");
        
        assertEquals(expD.getD1(), actDFull.getD1());
        assertEquals(expB.getB1(), actDFull.getB1());
        assertEquals(expB.getB2(), actDFull.getB2());
        
            
        expD.setB1(null);
        persistenceManager.update( expD);
        getSession().save();
        
        actD = (D) persistenceManager.getObject(  "/someD");
        
        assertEquals(expD.getD1(), actD.getD1());
        assertNull("b1 was not  removed", actD.getB1());
        
        actDFull = (DFull) persistenceManager.getObject( DFull.class,  "/someD");
        assertEquals(expD.getD1(), actDFull.getD1());
        assertNull("b1 was not  removed", actDFull.getB1());
        assertNull("b2 wan not remove", actDFull.getB2());

    }
    
    
    public void testBeanDescriptorConverter() throws Exception 
    {
        
        B expB = new B();
        expB.setB1("b1value");
        expB.setB2("b2value");
        E expE = new E();
        expE.setPath("/someD");
        expE.setD1("d1value");
        expE.setB1(expB);
        
        
        persistenceManager.insert( expE);
        persistenceManager.save();
       
        E actE = (E) persistenceManager.getObject( "/someD");
       
        assertEquals(expE.getD1(), actE.getD1());
        
        expE.setD1("updatedvalueD1");
        expB.setB1("updatedvalue1");
        
        persistenceManager.update( expE);
        persistenceManager.save();
               
        actE = (E) persistenceManager.getObject(  "/someD");
        
        assertEquals(expE.getD1(), actE.getD1());
                        
        expE.setB1(null);
        persistenceManager.update( expE);
        persistenceManager.save();
        
        actE = (E) persistenceManager.getObject(  "/someD");
        
        assertEquals(expE.getD1(), actE.getD1());        
        
   
        List messages = FakeBeanConverter.getLog();
        assertEquals(6, messages.size());
        assertEquals("insert at path /someD", messages.get(0));
        assertEquals("get from path /someD", messages.get(1));
        assertEquals("update at path /someD", messages.get(2));
        assertEquals("get from path /someD", messages.get(3));
        assertEquals("remove from path /someD", messages.get(4));
        assertEquals("get from path /someD", messages.get(5));

    }
    
    public void testParentBeanConverter() throws Exception
    {
        try
        {
        	ObjectContentManager persistenceManager = getPersistenceManager();

            // --------------------------------------------------------------------------------
            // Create and store an object graph in the repository
            // --------------------------------------------------------------------------------

            Page page = new Page();
            page.setPath("/test");
            page.setTitle("Page Title");
            
            Collection paragraphs = new ArrayList();
            
            paragraphs.add(new Paragraph("Para 1"));
            paragraphs.add(new Paragraph("Para 2"));
            paragraphs.add(new Paragraph("Para 3"));
            page.setParagraphs(paragraphs);
            
            persistenceManager.insert(page);
            persistenceManager.save();
            
            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------           
            page = (Page) persistenceManager.getObject("/test");
            paragraphs = page.getParagraphs();
            for (Iterator iter = paragraphs.iterator(); iter.hasNext();) {
				Paragraph paragraph = (Paragraph) iter.next();
				System.out.println("Paragraph path : " + paragraph.getPath());				
			}            
            Paragraph p1 = (Paragraph) persistenceManager.getObject("/test/collection-element[2]");
            Page paraPage = p1.getPage();
            assertNotNull("Parent page is null", paraPage);
            assertTrue("Invalid parent page", paraPage.getPath().equals("/test"));
            
            // --------------------------------------------------------------------------------
            // Remove the object
            // --------------------------------------------------------------------------------           
            persistenceManager.remove("/test");
            persistenceManager.save();
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }
    	
    }
    
}
