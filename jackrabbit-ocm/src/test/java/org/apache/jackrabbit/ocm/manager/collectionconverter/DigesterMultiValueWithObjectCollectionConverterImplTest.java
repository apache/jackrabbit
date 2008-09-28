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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.DigesterTestBase;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.testmodel.MultiValueWithObjectCollection;

/**
 * Test NTCollectionConverterImpl
 *
 * @author : <a href="mailto:boni.g@bioimagene.com">Boni Gopalan</a>
 */
public class DigesterMultiValueWithObjectCollectionConverterImplTest extends DigesterTestBase
{
    private final static Log log = LogFactory.getLog(DigesterMultiValueWithObjectCollectionConverterImplTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public DigesterMultiValueWithObjectCollectionConverterImplTest(String testName)  throws Exception
    {
        super(testName);
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(new TestSuite(DigesterMultiValueWithObjectCollectionConverterImplTest.class));
    }

    public void testMultiValue(){
    	checkMultiValue(new String [] {"Value1", "Value2", "Value3", "Value4", "Value5"}, "/test-string", String.class);
    	checkMultiValue(new Long [] {1L, 2L, 3L, 4L, 5L}, "/test-long", Long.class);
    }

    public void checkMultiValue(Object [] testData, String nodeName, Class klazz )
    {
        try
        {
        	ObjectContentManager ocm = getObjectContentManager();

            // --------------------------------------------------------------------------------
            // Create and store an object graph in the repository
            // --------------------------------------------------------------------------------

            MultiValueWithObjectCollection multiValue = new MultiValueWithObjectCollection();
            multiValue.setPath(nodeName);

            ArrayList values = new ArrayList();
            values.add(testData[0]);
            values.add(testData[1]);
            multiValue.setMultiValues(values);

            ocm.insert(multiValue);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------
            multiValue = (MultiValueWithObjectCollection) ocm.getObject( nodeName);
            assertNotNull("Object is null", multiValue);
            assertNull("nullMultiValues field is not null", multiValue.getNullMultiValues());
            assertTrue("Incorrect number of values", multiValue.getMultiValues().size() == 2);
            Iterator anIterator = multiValue.getMultiValues().iterator();
            assertEquals(testData[0], klazz.cast(anIterator.next()));
            assertEquals(testData[1], klazz.cast(anIterator.next()));

            // --------------------------------------------------------------------------------
            // Update the object
            // --------------------------------------------------------------------------------
            ArrayList values1 = new ArrayList();
            values1.add(testData[2]);
            values1.add(testData[3]);
            values1.add(testData[4]);
            multiValue.setMultiValues(values1);

            ocm.update(multiValue);
            ocm.save();

            // --------------------------------------------------------------------------------
            // Get the object
            // --------------------------------------------------------------------------------

            multiValue = (MultiValueWithObjectCollection) ocm.getObject(nodeName);
            assertNotNull("Object is null", multiValue);
            assertNull("nullMultiValues field is not null", multiValue.getNullMultiValues());
            assertTrue("Incorrect number of values", multiValue.getMultiValues().size() == 3);
            assertEquals(testData[2], klazz.cast(multiValue.getMultiValues().iterator().next()));

        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }

    }


}