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

package org.apache.jackrabbit.util;

import junit.framework.TestCase;
import org.apache.jackrabbit.commons.JcrUtils;

import javax.jcr.PropertyType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * <code>JcrUtilsTest</code>...
 */
public class JcrUtilsTest extends TestCase {

    public void testGetPropertyTypeNames() {
        String[] names = JcrUtils.getPropertyTypeNames(true);
        assertEquals(13, names.length);
        Set<String> nameSet = new HashSet<String>(Arrays.asList(names));
        assertEquals(13, nameSet.size());
        assertTrue(nameSet.contains(PropertyType.TYPENAME_UNDEFINED));

        names = JcrUtils.getPropertyTypeNames(false);
        assertEquals(12, names.length);
        nameSet = new HashSet<String>(Arrays.asList(names));
        assertEquals(12, nameSet.size());
        assertFalse(nameSet.contains(PropertyType.TYPENAME_UNDEFINED));
    }

}