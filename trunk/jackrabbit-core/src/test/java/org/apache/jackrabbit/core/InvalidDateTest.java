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
package org.apache.jackrabbit.core;

import java.util.Calendar;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.ValueFormatException;
import javax.jcr.PropertyType;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * <code>InvalidDateTest</code> contains tests for JCR-1996.
 */
public class InvalidDateTest extends AbstractJCRTest {

    public void testDateRange() throws RepositoryException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 20000);
        Node n = testRootNode.addNode(nodeName1);
        try {
            superuser.getValueFactory().createValue(cal);
            fail("must throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            n.setProperty(propertyName1, cal);
            fail("must throw ValueFormatException");
        } catch (ValueFormatException e) {
            // expected
        }
        String calString = superuser.getValueFactory().createValue(
                Calendar.getInstance()).getString();
        calString = "1" + calString;
        try {
            n.setProperty(propertyName1, calString, PropertyType.DATE);
            fail("must throw ValueFormatException");
        } catch (ValueFormatException e) {
            // expected
        }
    }
}
