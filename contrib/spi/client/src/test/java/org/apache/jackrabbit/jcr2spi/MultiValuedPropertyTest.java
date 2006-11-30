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
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.api.PropertyUtil;

import javax.jcr.RepositoryException;
import javax.jcr.Property;
import javax.jcr.ValueFormatException;

/**
 * <code>MultiValuedPropertyTest</code>...
 */
public class MultiValuedPropertyTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(MultiValuedPropertyTest.class);

    /**
     * Tests if Property.getStream() fails with ValueFormatException for
     * multivalued properties.
     *
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testGetStreamFromMultivalued() throws RepositoryException, NotExecutableException {
        Property prop = PropertyUtil.searchMultivalProp(testRootNode);
        if (prop == null) {
            throw new NotExecutableException("No multivalued property found.");
        }
        else {
            try {
                prop.getStream();
                fail("Property.getStream() must fail with ValueFormatException for any multivalued property.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        }
    }

    /**
     * Tests if Property.getString() fails with ValueFormatException for
     * multivalued properties.
     *
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testGetStringFromMultivalued() throws RepositoryException, NotExecutableException {
        Property prop = PropertyUtil.searchMultivalProp(testRootNode);
        if (prop == null) {
            throw new NotExecutableException("No multivalued property found.");
        }
        else {
            try {
                prop.getString();
                fail("Property.getString() must fail with ValueFormatException for any multivalued property.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        }
    }

    /**
     * Tests if Property.getDate() fails multivalued properties.
     *
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testGetDateFromMultivalued() throws RepositoryException, NotExecutableException {
        Property prop = PropertyUtil.searchMultivalProp(testRootNode);
        if (prop == null) {
            throw new NotExecutableException("No multivalued property found.");
        }
        else {
            try {
                prop.getDate();
                fail("Property.getDate() must fail with ValueFormatException for any multivalued property.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        }
    }

    /**
     * Tests if Property.getDouble() fails for multivalued properties.
     *
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testGetDoubleFromMultivalued() throws RepositoryException, NotExecutableException {
        Property prop = PropertyUtil.searchMultivalProp(testRootNode);
        if (prop == null) {
            throw new NotExecutableException("No multivalued property found.");
        }
        else {
            try {
                prop.getDouble();
                fail("Property.getDouble() must fail with ValueFormatException for any multivalued property.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        }
    }

    /**
     * Tests if Property.getLong() fails for multivalued properties.
     *
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testGetLongFromMultivalued() throws RepositoryException, NotExecutableException {
        Property prop = PropertyUtil.searchMultivalProp(testRootNode);
        if (prop == null) {
            throw new NotExecutableException("No multivalued property found.");
        }
        else {
            try {
                prop.getLong();
                fail("Property.getLong() must fail with ValueFormatException for any multivalued property.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        }
    }

    /**
     * Tests if Property.getBoolean() fails for multivalued properties.
     *
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testGetBooleanFromMultivalued() throws RepositoryException, NotExecutableException {
        Property prop = PropertyUtil.searchMultivalProp(testRootNode);
        if (prop == null) {
            throw new NotExecutableException("No multivalued property found.");
        }
        else {
            try {
                prop.getBoolean();
                fail("Property.getLong() must fail with ValueFormatException for any multivalued property.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        }
    }

    /**
     * Tests if Property.getLength() fails for multivalued property.
     */
    public void testGetLengthFromMultivalued() throws RepositoryException, NotExecutableException {
        Property prop = PropertyUtil.searchMultivalProp(testRootNode);
        if (prop == null) {
            throw new NotExecutableException("No multivalued property found.");
        }
        try {
            prop.getLength();
            fail("Property.getLength() called on a multivalue property must fail (ValueFormatException).");
        } catch (ValueFormatException vfe) {
            // ok
        }
    }
}