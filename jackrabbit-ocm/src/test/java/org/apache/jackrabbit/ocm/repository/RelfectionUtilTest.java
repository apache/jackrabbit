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
package org.apache.jackrabbit.ocm.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;
import org.apache.jackrabbit.ocm.testmodel.collection.CustomList;

/** Testcase for ReflectionUtils.
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class RelfectionUtilTest extends TestCase
{
    private final static Log log = LogFactory.getLog(RelfectionUtilTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public RelfectionUtilTest(String testName)
    {
        super(testName);
    }

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    public void tearDown() throws Exception
    {
        super.tearDown();
    }


    /**
     * Test for getRepository() and login
     *
     */
    public void testimplementInterface()
    {
    	assertTrue(ReflectionUtils.implementsInterface(List.class, Collection.class));
    	assertTrue(ReflectionUtils.implementsInterface(new ArrayList<String>().getClass(), Collection.class));
    	assertFalse(ReflectionUtils.implementsInterface(Map.class, Collection.class));
    	assertTrue(ReflectionUtils.implementsInterface(LinkedList.class, Collection.class));

    	assertTrue(ReflectionUtils.implementsInterface(HashMap.class, Map.class));
    	assertTrue(ReflectionUtils.implementsInterface(SortedMap.class, Map.class));
    	assertTrue(ReflectionUtils.implementsInterface(CustomList.class, Collection.class));
    }

}