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
package org.apache.jackrabbit.core.persistence.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;

import javax.jcr.PropertyType;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle.PropertyEntry;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

import junit.framework.TestCase;

public class BundleBindingTest extends TestCase {

    private BundleBinding binding;

    protected void setUp() throws Exception {
        binding = new BundleBinding(
                null, null, new HashMapIndex(), new HashMapIndex(), null);
    }

    public void testEmptyBundle() throws Exception {
        NodePropBundle bundle = new NodePropBundle(new NodeId());
        bundle.setParentId(new NodeId(1, 2));
        bundle.setNodeTypeName(NameConstants.NT_UNSTRUCTURED);
        bundle.setMixinTypeNames(Collections.<Name>emptySet());
        bundle.setSharedSet(Collections.<NodeId>emptySet());

        assertBundleRoundtrip(bundle);

        assertBundleSerialization(bundle, new byte[] {
                2, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0,
                0, 0, 0, 0, 0, 2, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1,
                0, 0, 0, 0, 0 });
    }

    public void testComplexBundle() throws Exception {
        NodeId id = new NodeId(1, 2);
        NodePropBundle bundle = new NodePropBundle(id);
        bundle.setParentId(new NodeId(3, 4));
        bundle.setNodeTypeName(NameConstants.NT_UNSTRUCTURED);
        bundle.setMixinTypeNames(Collections.singleton(
                NameConstants.MIX_CREATED));
        bundle.setReferenceable(true);
        bundle.setSharedSet(new HashSet<NodeId>(Arrays.asList(
                new NodeId(5, 6), new NodeId(7, 8), new NodeId(9, 10))));

        PropertyEntry created = new PropertyEntry(
                new PropertyId(id, NameConstants.JCR_CREATED));
        created.setType(PropertyType.DATE);
        created.setMultiValued(false);
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(1234567890);
        created.setValues(new InternalValue[] { InternalValue.create(date) });
        bundle.addProperty(created);

        PropertyEntry createdby = new PropertyEntry(
                new PropertyId(id, NameConstants.JCR_CREATEDBY));
        createdby.setType(PropertyType.STRING);
        createdby.setMultiValued(false);
        createdby.setValues(new InternalValue[] {
                InternalValue.create("test") });
        bundle.addProperty(createdby);

        bundle.addChildNodeEntry(
                NameConstants.JCR_SYSTEM, new NodeId(11, 12));
        bundle.addChildNodeEntry(
                NameConstants.JCR_VERSIONSTORAGE, new NodeId(13, 14));

        assertBundleRoundtrip(bundle);

        assertBundleSerialization(bundle, new byte[] {
                2, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0,
                0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, -1, -1, -1, -1,
                0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0,
                0, 0, 4, 116, 101, 115, 116, 0, 0, 0, 2, 0, 0, 0, 1, 0, 0, 0,
                5, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 29, 49, 57, 55, 48, 45, 48,
                49, 45, 49, 53, 84, 48, 55, 58, 53, 54, 58, 48, 55, 46, 56,
                57, 48, 43, 48, 49, 58, 48, 48, -1, -1, -1, -1, 1, 1, 0, 0,
                0, 0, 0, 0, 0, 11, 0, 0, 0, 0, 0, 0, 0, 12, 0, 0, 0, 2, 0, 6,
                115, 121, 115, 116, 101, 109, 1, 0, 0, 0, 0, 0, 0, 0, 13, 0,
                0, 0, 0, 0, 0, 0, 14, 0, 0, 0, 2, 0, 14, 118, 101, 114, 115,
                105, 111, 110, 83, 116, 111, 114, 97, 103, 101, 0, 0, 0, 1,
                0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 8, 1, 0, 0, 0,
                0, 0, 0, 0, 9, 0, 0, 0, 0, 0, 0, 0, 10, 1, 0, 0, 0, 0, 0, 0,
                0, 5, 0, 0, 0, 0, 0, 0, 0, 6, 0 });
    }

    private void assertBundleRoundtrip(NodePropBundle bundle)
            throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        binding.writeBundle(buffer, bundle);
        byte[] bytes = buffer.toByteArray();

        assertEquals(bundle, binding.readBundle(
                new ByteArrayInputStream(bytes), bundle.getId()));
    }

    private void assertBundleSerialization(NodePropBundle bundle, byte[] data)
            throws Exception {
        assertEquals(bundle, binding.readBundle(
                new ByteArrayInputStream(data), bundle.getId()));
    }

}
