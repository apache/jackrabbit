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
import java.util.Collections;

import org.apache.jackrabbit.core.id.NodeId;
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
        bundle.setParentId(new NodeId());
        bundle.setNodeTypeName(NameConstants.NT_UNSTRUCTURED);
        bundle.setMixinTypeNames(Collections.<Name>emptySet());
        bundle.setSharedSet(Collections.<NodeId>emptySet());
        assertBundleRoundtrip(bundle);
    }

    private void assertBundleRoundtrip(NodePropBundle bundle)
            throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        binding.writeBundle(buffer, bundle);
        byte[] bytes = buffer.toByteArray();

        assertTrue(binding.checkBundle(new ByteArrayInputStream(bytes)));

        assertEquals(bundle, binding.readBundle(
                new ByteArrayInputStream(bytes), bundle.getId()));
    }
}
