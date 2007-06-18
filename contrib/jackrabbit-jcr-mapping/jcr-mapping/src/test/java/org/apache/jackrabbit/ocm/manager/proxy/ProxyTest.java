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
package org.apache.jackrabbit.ocm.manager.proxy;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.TestBase;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.testmodel.proxy.Detail;
import org.apache.jackrabbit.ocm.testmodel.proxy.Main;

/**
 * Test inheritance with node type per concrete class (without  discreminator field)
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 */
public class ProxyTest extends TestBase {
	private final static Log log = LogFactory.getLog(ProxyTest.class);

	/**
	 * <p>Defines the test case name for junit.</p>
	 * @param testName The test case name.
	 */
	public ProxyTest(String testName) throws Exception {
		super(testName);

	}

	public static Test suite() {
		// All methods starting with "test" will be executed in the test suite.
		return new RepositoryLifecycleTestSetup(new TestSuite(
				ProxyTest.class));
	}

	public void tearDown() throws Exception {

		cleanUpRepisotory();
		super.tearDown();
		
	}

	public void testBeanProxy() {

		try {
			ObjectContentManager ocm = this.getObjectContentManager();

			Detail detail = new Detail();
			detail.setField("FieldValue");			
			
			Detail proxyDetail = new Detail();
			proxyDetail.setField("ProxyFieldValue");
			
			Main main = new Main();
			main.setPath("/test");
			main.setDetail(detail);
			main.setProxyDetail(proxyDetail);
							
            ocm.insert(main);
			ocm.save();
			
			
			//---------------------------------------------------------------------------------------------------------
			// Retrieve the main object
			//---------------------------------------------------------------------------------------------------------						
			main = (Main) ocm.getObject( "/test");
			assertNotNull("detail is null", main.getDetail());
			assertTrue("Invalid detail bean", main.getDetail().getField().equals("FieldValue"));

			assertNotNull("proxydetail is null", main.getProxyDetail());
			Object proxyObject = main.getProxyDetail();
			assertTrue("Invalid class specify for the proxy bean", proxyObject  instanceof Detail);
			assertTrue("Invalid proxy detail bean",proxyDetail .getField().equals("ProxyFieldValue"));
			
			Detail nullDetail = main.getNullDetail();
			assertNull("nulldetail is not  null",nullDetail );

			
			//---------------------------------------------------------------------------------------------------------
			// Update  
			//---------------------------------------------------------------------------------------------------------						
			 detail = new Detail();
			detail.setField("AnotherFieldValue");			
			
			proxyDetail = new Detail();
			proxyDetail.setField("AnotherProxyFieldValue");
			
			main.setDetail(detail);
			main.setProxyDetail(proxyDetail);
			
			ocm.update(main);
			ocm.save();

			//---------------------------------------------------------------------------------------------------------
			// Retrieve the main object
			//---------------------------------------------------------------------------------------------------------						

			main = (Main) ocm.getObject( "/test");
			assertNotNull("detail is null", main.getDetail());
			assertTrue("Invalid detail bean", main.getDetail().getField().equals("AnotherFieldValue"));

			assertNotNull("proxydetail is null", main.getProxyDetail());
			proxyObject = main.getProxyDetail();
			assertTrue("Invalid class specify for the proxy bean", proxyObject  instanceof Detail);
			assertTrue("Invalid proxy detail bean",proxyDetail .getField().equals("AnotherProxyFieldValue"));
						
			assertNull("nulldetail is not  null",main.getNullDetail());
				
	
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		
	}
	
	public void testCollectionProxy() {

		try {
			ObjectContentManager ocm = this.getObjectContentManager();

			ArrayList  details= new ArrayList();
			for(int i=1; i<=100;i++)
			{
				Detail detail = new Detail();
				detail.setField("field" + i);				
				details.add(detail);
			}
			
			Main main = new Main();
			main.setProxyCollection(details);
			main.setPath("/test");							
            ocm.insert(main);
			ocm.save();
			
			
			//---------------------------------------------------------------------------------------------------------
			// Retrieve the main object
			//---------------------------------------------------------------------------------------------------------						
			main = (Main) ocm.getObject( "/test");
			assertNotNull("main is null", main);

            Collection result = main.getProxyCollection();
            assertEquals("Invalide size", result.size(), 100);
            assertNull("nullcollectionproxy  is not null", main.getNullProxyCollection());
			
			//---------------------------------------------------------------------------------------------------------
			// Update  
			//---------------------------------------------------------------------------------------------------------
            
            Detail detail = new Detail();
			detail.setField("newFieldValue");			
			result.add(detail);
			main.setProxyCollection(result);
			ocm.update(main);
			ocm.save();

			//---------------------------------------------------------------------------------------------------------
			// Retrieve the main object
			//---------------------------------------------------------------------------------------------------------						
			main = (Main) ocm.getObject("/test");
			assertNotNull("main  is null", main);
            assertEquals("Invalide size",main.getProxyCollection().size(), 101);
            assertNull("nullcollectionproxy  is not null", main.getNullProxyCollection());
            
	
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		
	}



	    
}