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

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;

/**
 * Tests if the required repository descriptors are available.
 *
 */
public class RepositoryDescriptorTest extends AbstractJCRTest {

    private static final Set<String> requiredDescriptorKeys = new HashSet<String>();

    static {
        requiredDescriptorKeys.add(Repository.IDENTIFIER_STABILITY);
        requiredDescriptorKeys.add(Repository.LEVEL_1_SUPPORTED);
        requiredDescriptorKeys.add(Repository.LEVEL_2_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_NODE_TYPE_MANAGEMENT_SUPPORTED);
        requiredDescriptorKeys.add(Repository.NODE_TYPE_MANAGEMENT_AUTOCREATED_DEFINITIONS_SUPPORTED);
        requiredDescriptorKeys.add(Repository.NODE_TYPE_MANAGEMENT_INHERITANCE);
        requiredDescriptorKeys.add(Repository.NODE_TYPE_MANAGEMENT_MULTIPLE_BINARY_PROPERTIES_SUPPORTED);
        requiredDescriptorKeys.add(Repository.NODE_TYPE_MANAGEMENT_MULTIVALUED_PROPERTIES_SUPPORTED);
        requiredDescriptorKeys.add(Repository.NODE_TYPE_MANAGEMENT_ORDERABLE_CHILD_NODES_SUPPORTED);
        requiredDescriptorKeys.add(Repository.NODE_TYPE_MANAGEMENT_OVERRIDES_SUPPORTED);
        requiredDescriptorKeys.add(Repository.NODE_TYPE_MANAGEMENT_PRIMARY_ITEM_NAME_SUPPORTED);
        requiredDescriptorKeys.add(Repository.NODE_TYPE_MANAGEMENT_PROPERTY_TYPES);
        requiredDescriptorKeys.add(Repository.NODE_TYPE_MANAGEMENT_RESIDUAL_DEFINITIONS_SUPPORTED);
        requiredDescriptorKeys.add(Repository.NODE_TYPE_MANAGEMENT_SAME_NAME_SIBLINGS_SUPPORTED);
        requiredDescriptorKeys.add(Repository.NODE_TYPE_MANAGEMENT_VALUE_CONSTRAINTS_SUPPORTED);
        requiredDescriptorKeys.add(Repository.NODE_TYPE_MANAGEMENT_UPDATE_IN_USE_SUPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_ACCESS_CONTROL_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_JOURNALED_OBSERVATION_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_LIFECYCLE_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_LOCKING_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_OBSERVATION_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_NODE_AND_PROPERTY_WITH_SAME_NAME_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_QUERY_SQL_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_RETENTION_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_SHAREABLE_NODES_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_SIMPLE_VERSIONING_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_TRANSACTIONS_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_UNFILED_CONTENT_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_UPDATE_MIXIN_NODE_TYPES_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_UPDATE_PRIMARY_NODE_TYPE_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_VERSIONING_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_WORKSPACE_MANAGEMENT_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_XML_EXPORT_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_XML_IMPORT_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_ACTIVITIES_SUPPORTED);
        requiredDescriptorKeys.add(Repository.OPTION_BASELINES_SUPPORTED);
        
        requiredDescriptorKeys.add(Repository.QUERY_FULL_TEXT_SEARCH_SUPPORTED);
        requiredDescriptorKeys.add(Repository.QUERY_JOINS);
        requiredDescriptorKeys.add(Repository.QUERY_LANGUAGES);
        requiredDescriptorKeys.add(Repository.QUERY_STORED_QUERIES_SUPPORTED);
        requiredDescriptorKeys.add(Repository.QUERY_XPATH_DOC_ORDER);
        requiredDescriptorKeys.add(Repository.QUERY_XPATH_POS_INDEX);
        requiredDescriptorKeys.add(Repository.REP_NAME_DESC);
        requiredDescriptorKeys.add(Repository.REP_VENDOR_DESC);
        requiredDescriptorKeys.add(Repository.REP_VENDOR_URL_DESC);
        requiredDescriptorKeys.add(Repository.SPEC_NAME_DESC);
        requiredDescriptorKeys.add(Repository.SPEC_VERSION_DESC);
        requiredDescriptorKeys.add(Repository.WRITE_SUPPORTED);
    }

    /** The session for the tests */
    private Session session;

    /**
     * Sets up the fixture for this test.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();
        session = getHelper().getReadOnlySession();
    }

    /**
     * Releases the session aquired in {@link #setUp}.
     */
    protected void tearDown() throws Exception {
        if (session != null) {
            session.logout();
            session = null;
        }
        super.tearDown();
    }

    /**
     * Tests that the required repository descriptors are available.
     */
    public void testRequiredDescriptors() {
        Repository rep = session.getRepository();
        for (Iterator<String> it = requiredDescriptorKeys.iterator(); it.hasNext();) {
            String descName = it.next();
            assertTrue(descName + " is a standard descriptor", rep.isStandardDescriptor(descName));
            if (rep.isSingleValueDescriptor(descName)) {
                Value val = rep.getDescriptorValue(descName);
                assertNotNull("Required descriptor is missing: " + descName,
                        val);
            } else {
                Value[] vals = rep.getDescriptorValues(descName);
                assertNotNull("Required descriptor is missing: " + descName,
                        vals);
            }
        }
    }

    /**
     * Tests if {@link Repository#getDescriptorKeys()} returns all required
     * descriptors keys.
     */
    public void testGetDescriptorKeys() {
        List<String> keys = Arrays.asList(session.getRepository().getDescriptorKeys());
        for (Iterator<String> it = requiredDescriptorKeys.iterator(); it.hasNext();) {
            String key = it.next();
            assertTrue("Required descriptor is missing: " + key,
                    keys.contains(key));
        }
    }

    /**
     * Tests whether {@link Repository#getDescriptorValues(String)} returns an
     * Value[] of size 1 for single valued descriptors.
     */
    public void testGetDescriptorValues() {
        Repository rep = session.getRepository();
        // "option.node.type.management.supported" denotes a single-valued BOOLEAN descriptor
        String descName = Repository.OPTION_NODE_TYPE_MANAGEMENT_SUPPORTED;
        assertTrue(rep.isSingleValueDescriptor(descName));
        Value[] vals = rep.getDescriptorValues(descName);
        assertNotNull("Required descriptor is missing: " + descName, vals);
        assertEquals(1, vals.length);
        assertEquals(PropertyType.BOOLEAN, vals[0].getType());
        try {
            // getDescriptorValue(key).getString() is equivalent to getDescriptor(key)
            assertEquals(vals[0].getString(), rep.getDescriptor(descName));
        } catch (RepositoryException e) {
            fail(e.getMessage());
        }

        // "option.node.type.management.supported" denotes a single-valued BOOLEAN descriptor
        descName = Repository.QUERY_LANGUAGES;
        assertFalse(rep.isSingleValueDescriptor(descName));
        Value val = rep.getDescriptorValue(descName);
        assertNull(descName + " is a multi-value descriptor, getDescriptorValue() should return null", val);
    }
}
