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
package org.apache.jackrabbit.ocm.manager.enumeration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.AnnotationTestBase;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.DigesterTestBase;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.testmodel.A;
import org.apache.jackrabbit.ocm.testmodel.B;
import org.apache.jackrabbit.ocm.testmodel.C;
import org.apache.jackrabbit.ocm.testmodel.Discriminator;
import org.apache.jackrabbit.ocm.testmodel.enumeration.Odyssey;
import org.apache.jackrabbit.ocm.testmodel.enumeration.Planet;

/**
 * Test Simple Enumeration mappings
 *
 * @author <a href="mailto:boni.g@bioimagene.com">Boni Gopalan</a>
 */
public class AnnotationSimpleEnumerationTest extends AnnotationTestBase
{
    private final static Log logger = LogFactory.getLog(AnnotationSimpleEnumerationTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public AnnotationSimpleEnumerationTest(String testName)  throws Exception
    {
        super(testName);
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(
                new TestSuite(AnnotationSimpleEnumerationTest.class));
    }

    public void testMapSimpleEnumeration()
    {
    	try {
    		new SimpleEnumerationTestBase(getObjectContentManager()).testMapSimpleEnumeration();
		} catch (Throwable e) {
			e.printStackTrace();
			fail("Tests resulted in exception");
		}
    }

}
