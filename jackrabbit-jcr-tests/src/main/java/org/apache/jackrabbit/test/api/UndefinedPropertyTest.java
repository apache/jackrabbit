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
package org.apache.jackrabbit.test.api;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Property;
import javax.jcr.PropertyType;

/**
 * Tests if no property in the workspace is of type
 * {@link javax.jcr.PropertyType#UNDEFINED}.
 *
 * @test
 * @sources UndefinedPropertyTest.java
 * @executeClass org.apache.jackrabbit.test.api.UndefinedPropertyTest
 * @keywords level1
 */
public class UndefinedPropertyTest extends AbstractJCRTest {

    /**
     * Sets up the fixture for this test.
     */
    protected void setUp() throws NotExecutableException, Exception {
        isReadOnly = true;
        super.setUp();
    }

    /**
     * Returns "does not matter" (<code>null</code>).
     * @return <code>null</code>.
     */
    protected Boolean getPropertyIsMultivalued() {
        return null;
    }

    /**
     * Tests that no actual property with type Undefined exists.
     */
    public void testUndefinedProperty() throws RepositoryException {
        Session session = helper.getReadOnlySession();
        try {
            Property prop = PropertyUtil.searchProp(session, session.getRootNode().getNode(testPath), PropertyType.UNDEFINED, null);
            assertNull("Property with type Undefined found.", prop);
        } finally {
            session.logout();
        }
    }
}