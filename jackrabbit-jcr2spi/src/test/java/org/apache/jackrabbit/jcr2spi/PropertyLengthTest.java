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
package org.apache.jackrabbit.jcr2spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.api.PropertyUtil;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import java.util.Calendar;

/**
 * <code>PropertyLengthTest</code>...
 */
public class PropertyLengthTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(PropertyLengthTest.class);

    private static long getValueLength(Value val) throws RepositoryException {
        long valLength;
        if (val.getType() == PropertyType.BINARY) {
            valLength = PropertyUtil.countBytes(val);
        } else {
            valLength = val.getString().length();
        }
        return valLength;
    }

    private Property getProperty(int propertyType) throws RepositoryException, NotExecutableException {
        Property p = PropertyUtil.searchProp(testRootNode.getSession(), testRootNode, propertyType, null);
        if (p == null) {
            try {
                Value val;
                ValueFactory factory = testRootNode.getSession().getValueFactory();
                switch (propertyType) {
                    case PropertyType.BINARY:
                        val = factory.createValue("binaryValue", PropertyType.BINARY);
                        break;
                    case PropertyType.BOOLEAN:
                        val = factory.createValue(true);
                        break;
                    case PropertyType.DATE:
                        val = factory.createValue(Calendar.getInstance());
                        break;
                    case PropertyType.DOUBLE:
                        val = factory.createValue(new Double(134).doubleValue());
                        break;
                    case PropertyType.LONG:
                        val = factory.createValue(new Long(134).longValue());
                        break;
                    case PropertyType.NAME:
                        val = factory.createValue(ntBase, PropertyType.NAME);
                        break;
                    case PropertyType.PATH:
                        val = factory.createValue(testRootNode.getPath(), PropertyType.PATH);
                        break;
                    case PropertyType.REFERENCE:
                        Node refNode = testRootNode.addNode(nodeName1);
                        if (refNode.canAddMixin(mixReferenceable)) {
                            testRootNode.addMixin(mixReferenceable);
                        }
                        testRootNode.save();
                        val = factory.createValue(refNode);
                        break;
                    case PropertyType.STRING:
                        val = factory.createValue("StringValue");
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid property value type" + propertyType);
                }
                p = testRootNode.setProperty(propertyName1, val);
            } catch (RepositoryException e) {
                log.error("Unable to create Property of type " + propertyType);
                throw new NotExecutableException();
            }
        }
        return p;
    }

    private static void checkLength(Property p) throws RepositoryException {
        if (p.isMultiple()) {
            Value[] vals = p.getValues();
            long[] lengths = p.getLengths();
            for (int i = 0; i < lengths.length; i++) {
                assertTrue("Wrong property length", lengths[i] == getValueLength(vals[i]));
            }
        } else {
            assertTrue("Wrong property length", p.getLength() == getValueLength(p.getValue()));
        }
    }

    public void testLengthOfBinary() throws RepositoryException, NotExecutableException {
        Property p = getProperty(PropertyType.BINARY);
        checkLength(p);
    }

    public void testLengthOfBoolean() throws RepositoryException, NotExecutableException {
        Property p = getProperty(PropertyType.BOOLEAN);
        checkLength(p);
    }
    public void testLengthOfDate() throws RepositoryException, NotExecutableException {
        Property p = getProperty(PropertyType.DATE);
        checkLength(p);
    }
    public void testLengthOfDouble() throws RepositoryException, NotExecutableException {
        Property p = getProperty(PropertyType.DOUBLE);
        checkLength(p);
    }
    public void testLengthOfLong() throws RepositoryException, NotExecutableException {
        Property p = getProperty(PropertyType.LONG);
        checkLength(p);
    }
    public void testLengthOfName() throws RepositoryException, NotExecutableException {
        Property p = getProperty(PropertyType.NAME);
        checkLength(p);
    }
    public void testLengthOfPath() throws RepositoryException, NotExecutableException {
        Property p = getProperty(PropertyType.PATH);
        checkLength(p);
    }
    public void testLengthOfReference() throws RepositoryException, NotExecutableException {
        Property p = getProperty(PropertyType.REFERENCE);
        checkLength(p);
    }
    public void testLengthOfString() throws RepositoryException, NotExecutableException {
        Property p = getProperty(PropertyType.STRING);
        checkLength(p);
    }
}