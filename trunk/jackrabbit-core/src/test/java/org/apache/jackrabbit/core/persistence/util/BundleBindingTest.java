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
import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import javax.jcr.PropertyType;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle.PropertyEntry;
import org.apache.jackrabbit.core.util.StringIndex;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;

import junit.framework.TestCase;

public class BundleBindingTest extends TestCase {

    private static final NameFactory factory = NameFactoryImpl.getInstance();

    private BundleBinding binding;

    protected void setUp() throws Exception {
        final String[] strings = new String[] {
                "http://www.jcp.org/jcr/1.0",
                "http://www.jcp.org/jcr/nt/1.0",
                "http://www.jcp.org/jcr/mix/1.0",
                "unstructured",
                "created",
                "createdBy",
                "",
                "binary",
                "boolean",
                "date",
                "decimal",
                "double",
                "long",
                "name",
                "path",
                "reference",
                "string",
                "uri",
                "weakreference"
        };
        StringIndex index = new StringIndex() {
            public int stringToIndex(String string) {
                for (int i = 0; i < strings.length; i++) {
                    if (strings[i].equals(string)) {
                        return i;
                    }
                }
                throw new IllegalArgumentException(string);
            }
            public String indexToString(int idx) {
                if(idx> strings.length){
                    return "";
                }
                return strings[idx];
            }
        };
        binding = new BundleBinding(null, null, index, index, null);
    }

    public void testEmptyBundle() throws Exception {
        NodePropBundle bundle = new NodePropBundle(NodeId.randomId());
        bundle.setParentId(new NodeId(1, 2));
        bundle.setNodeTypeName(NameConstants.NT_UNSTRUCTURED);
        bundle.setMixinTypeNames(Collections.<Name>emptySet());
        bundle.setSharedSet(Collections.<NodeId>emptySet());

        assertBundleRoundtrip(bundle);

        assertBundleSerialization(bundle, new byte[] {
                2, 0, 0, 1, 0, 0, 0, 3, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0,
                0, 0, 0, 0, 0, 2, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1,
                0, 0, 0, 0, 0 });
    }

    /**
     * Tests serialization of a complex bundle.
     */
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

        PropertyEntry property;

        property = new PropertyEntry(
                new PropertyId(id, NameConstants.JCR_CREATED));
        property.setType(PropertyType.DATE);
        property.setMultiValued(false);
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(1234567890);
        property.setValues(new InternalValue[] { InternalValue.create(date) });
        bundle.addProperty(property);

        property = new PropertyEntry(
                new PropertyId(id, NameConstants.JCR_CREATEDBY));
        property.setType(PropertyType.STRING);
        property.setMultiValued(false);
        property.setValues(
                new InternalValue[] { InternalValue.create("test") });
        bundle.addProperty(property);

        property = new PropertyEntry(
                new PropertyId(id, factory.create("", "binary")));
        property.setType(PropertyType.BINARY);
        property.setMultiValued(false);
        property.setValues(new InternalValue[] { InternalValue.create(
                new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }) });
        bundle.addProperty(property);

        property = new PropertyEntry(
                new PropertyId(id, factory.create("", "boolean")));
        property.setType(PropertyType.BOOLEAN);
        property.setMultiValued(true);
        property.setValues(new InternalValue[] {
                InternalValue.create(true), InternalValue.create(false) });
        bundle.addProperty(property);

        property = new PropertyEntry(
                new PropertyId(id, factory.create("", "date")));
        property.setType(PropertyType.DATE);
        property.setMultiValued(false);
        property.setValues(new InternalValue[] { InternalValue.create(date) });
        bundle.addProperty(property);

        property = new PropertyEntry(
                new PropertyId(id, factory.create("", "decimal")));
        property.setType(PropertyType.DECIMAL);
        property.setMultiValued(false);
        property.setValues(new InternalValue[] {
                InternalValue.create(new BigDecimal("1234567890.0987654321")) });
        bundle.addProperty(property);

        property = new PropertyEntry(
                new PropertyId(id, factory.create("", "double")));
        property.setType(PropertyType.DOUBLE);
        property.setMultiValued(true);
        property.setValues(new InternalValue[] {
                InternalValue.create(1.0), InternalValue.create(Math.PI) });
        bundle.addProperty(property);

        property = new PropertyEntry(
                new PropertyId(id, factory.create("", "long")));
        property.setType(PropertyType.LONG);
        property.setMultiValued(false);
        property.setValues(new InternalValue[] {
                InternalValue.create(1234567890) });
        bundle.addProperty(property);

        property = new PropertyEntry(
                new PropertyId(id, factory.create("", "name")));
        property.setType(PropertyType.NAME);
        property.setMultiValued(false);
        property.setValues(new InternalValue[] {
                InternalValue.create(NameConstants.JCR_MIMETYPE) });
        bundle.addProperty(property);

        property = new PropertyEntry(
                new PropertyId(id, factory.create("", "path")));
        property.setType(PropertyType.PATH);
        property.setMultiValued(true);
        PathFactory pathFactory = PathFactoryImpl.getInstance();
        Path root = pathFactory.getRootPath();
        Path path = pathFactory.create(root, NameConstants.JCR_SYSTEM, false);
        property.setValues(new InternalValue[] {
                InternalValue.create(root), InternalValue.create(path) });
        bundle.addProperty(property);

        property = new PropertyEntry(
                new PropertyId(id, factory.create("", "reference")));
        property.setType(PropertyType.REFERENCE);
        property.setMultiValued(false);
        property.setValues(new InternalValue[] {
                InternalValue.create(new NodeId(11, 12)) });
        bundle.addProperty(property);

        property = new PropertyEntry(
                new PropertyId(id, factory.create("", "string")));
        property.setType(PropertyType.STRING);
        property.setMultiValued(false);
        property.setValues(new InternalValue[] {
                InternalValue.create("test") });
        bundle.addProperty(property);

        property = new PropertyEntry(
                new PropertyId(id, factory.create("", "uri")));
        property.setType(PropertyType.URI);
        property.setMultiValued(false);
        property.setValues(new InternalValue[] {
                InternalValue.create(new URI("http://jackrabbit.apache.org/")) });
        bundle.addProperty(property);

        property = new PropertyEntry(
                new PropertyId(id, factory.create("", "weakreference")));
        property.setType(PropertyType.WEAKREFERENCE);
        property.setMultiValued(false);
        property.setValues(new InternalValue[] {
                InternalValue.create(new NodeId(13, 14), true) });
        bundle.addProperty(property);

        bundle.addChildNodeEntry(
                NameConstants.JCR_SYSTEM, new NodeId(15, 16));
        bundle.addChildNodeEntry(
                NameConstants.JCR_VERSIONSTORAGE, new NodeId(17, 18));

        assertBundleRoundtrip(bundle);

        assertBundleSerialization(bundle, new byte[] {
                2, 0, 0, 1, 0, 0, 0, 3, 1, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0,
                0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 2, 0, 0, 0, 4, -1, -1, -1, -1,
                0, 0, 0, 6, 0, 0, 0, 12, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 1, 0,
                0, 0, 0, 73, -106, 2, -46, 0, 0, 0, 6, 0, 0, 0, 16, 0, 0, 0,
                1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 4, 116, 101, 115, 116, 0, 0,
                0, 6, 0, 0, 0, 8, 0, 0, 0, 6, 1, 0, 0, 0, 0, 0, 2, 1, 0, 0,
                0, 0, 6, 0, 0, 0, 13, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 1, 0, 0,
                0, 0, 0, 8, 109, 105, 109, 101, 84, 121, 112, 101, 0, 0, 0, 6,
                0, 0, 0, 15, 0, 0, 0, 9, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0,
                0, 0, 0, 11, 0, 0, 0, 0, 0, 0, 0, 12, 0, 0, 0, 6, 0, 0, 0, 18,
                0, 0, 0, 10, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 13,
                0, 0, 0, 0, 0, 0, 0, 14, 0, 0, 0, 6, 0, 0, 0, 9, 0, 0, 0, 5,
                0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 29, 49, 57, 55, 48, 45, 48, 49,
                45, 49, 53, 84, 48, 55, 58, 53, 54, 58, 48, 55, 46, 56, 57,
                48, 43, 48, 49, 58, 48, 48, 0, 0, 0, 6, 0, 0, 0, 7, 0, 0, 0,
                2, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 10, 0, 1, 2, 3, 4, 5, 6, 7,
                8, 9, 0, 0, 0, 6, 0, 0, 0, 17, 0, 0, 0, 11, 0, 0, 0, 0, 0, 0,
                1, 0, 0, 0, 29, 104, 116, 116, 112, 58, 47, 47, 106, 97, 99,
                107, 114, 97, 98, 98, 105, 116, 46, 97, 112, 97, 99, 104, 101,
                46, 111, 114, 103, 47, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 1, 0,
                0, 0, 0, 0, 0, 1, 0, 0, 0, 4, 116, 101, 115, 116, 0, 0, 0, 6,
                0, 0, 0, 11, 0, 0, 0, 4, 1, 0, 0, 0, 0, 0, 2, 63, -16, 0, 0,
                0, 0, 0, 0, 64, 9, 33, -5, 84, 68, 45, 24, 0, 0, 0, 0, 0, 0,
                0, 4, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 29, 49, 57,
                55, 48, 45, 48, 49, 45, 49, 53, 84, 48, 55, 58, 53, 54, 58,
                48, 55, 46, 56, 57, 48, 43, 48, 49, 58, 48, 48, 0, 0, 0, 6,
                0, 0, 0, 10, 0, 0, 0, 12, 0, 0, 0, 0, 0, 0, 1, 1, 0, 21, 49,
                50, 51, 52, 53, 54, 55, 56, 57, 48, 46, 48, 57, 56, 55, 54,
                53, 52, 51, 50, 49, 0, 0, 0, 6, 0, 0, 0, 14, 0, 0, 0, 8, 1,
                0, 0, 0, 0, 0, 2, 0, 0, 0, 2, 123, 125, 0, 0, 0, 37, 123,
                125, 9, 123, 104, 116, 116, 112, 58, 47, 47, 119, 119, 119,
                46, 106, 99, 112, 46, 111, 114, 103, 47, 106, 99, 114, 47,
                49, 46, 48, 125, 115, 121, 115, 116, 101, 109, -1, -1, -1,
                -1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 16,
                0, 0, 0, 0, 0, 6, 115, 121, 115, 116, 101, 109, 1, 0, 0, 0,
                0, 0, 0, 0, 17, 0, 0, 0, 0, 0, 0, 0, 18, 0, 0, 0, 0, 0, 14,
                118, 101, 114, 115, 105, 111, 110, 83, 116, 111, 114, 97,
                103, 101, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0,
                0, 0, 0, 8, 1, 0, 0, 0, 0, 0, 0, 0, 9, 0, 0, 0, 0, 0, 0, 0,
                10, 1, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0, 6, 0 });
    }

    /**
     * Tests serialization of custom namespaces.
     */
    public void testCustomNamespaces() throws Exception {
        NodePropBundle bundle = new NodePropBundle(NodeId.randomId());
        bundle.setParentId(NodeId.randomId());
        bundle.setNodeTypeName(NameConstants.NT_UNSTRUCTURED);
        bundle.setMixinTypeNames(Collections.<Name>emptySet());
        bundle.setSharedSet(Collections.<NodeId>emptySet());

        bundle.addChildNodeEntry(factory.create("ns1", "test"), NodeId.randomId());
        bundle.addChildNodeEntry(factory.create("ns2", "test"), NodeId.randomId());
        bundle.addChildNodeEntry(factory.create("ns3", "test"), NodeId.randomId());
        bundle.addChildNodeEntry(factory.create("ns4", "test"), NodeId.randomId());
        bundle.addChildNodeEntry(factory.create("ns5", "test"), NodeId.randomId());
        bundle.addChildNodeEntry(factory.create("ns6", "test"), NodeId.randomId());
        bundle.addChildNodeEntry(factory.create("ns7", "test"), NodeId.randomId());
        bundle.addChildNodeEntry(factory.create("ns8", "test"), NodeId.randomId());
        bundle.addChildNodeEntry(factory.create("ns1", "test"), NodeId.randomId());
        bundle.addChildNodeEntry(factory.create("ns1", "test"), NodeId.randomId());
        bundle.addChildNodeEntry(factory.create("ns2", "test"), NodeId.randomId());
        bundle.addChildNodeEntry(factory.create("ns3", "test"), NodeId.randomId());
        bundle.addChildNodeEntry(factory.create("ns1", "test"), NodeId.randomId());
        bundle.addChildNodeEntry(factory.create("ns2", "test"), NodeId.randomId());
        bundle.addChildNodeEntry(factory.create("ns3", "test"), NodeId.randomId());

        assertBundleRoundtrip(bundle);
    }

    public void testBooleanSerialization() throws Exception {
        assertValueSerialization(InternalValue.create(true));
        assertValueSerialization(InternalValue.create(false));
    }

    /**
     * Tests serialization of long values.
     */
    public void testLongSerialization() throws Exception {
        assertValueSerialization(InternalValue.create(0));
        assertValueSerialization(InternalValue.create(1));
        assertValueSerialization(InternalValue.create(-1));
        assertValueSerialization(InternalValue.create(1234567890));
        assertValueSerialization(InternalValue.create(-1234567890));
        assertValueSerialization(InternalValue.create(Long.MAX_VALUE));
        assertValueSerialization(InternalValue.create(Long.MIN_VALUE));
    }

    public void testDoubleSerialization() throws Exception {
        assertValueSerialization(InternalValue.create(0.0));
        assertValueSerialization(InternalValue.create(1.0));
        assertValueSerialization(InternalValue.create(-1.0));
        assertValueSerialization(InternalValue.create(12345.6789));
        assertValueSerialization(InternalValue.create(-12345.6789));
        assertValueSerialization(InternalValue.create(Double.MAX_VALUE));
        assertValueSerialization(InternalValue.create(Double.MIN_VALUE));
        assertValueSerialization(InternalValue.create(Double.POSITIVE_INFINITY));
        assertValueSerialization(InternalValue.create(Double.NEGATIVE_INFINITY));
        assertValueSerialization(InternalValue.create(Double.NaN));
    }

    /**
     * Tests serialization of date values.
     */
    public void testDateSerialization() throws Exception {
        assertDateSerialization("2010-10-10T10:10:10.100Z");

        // Different kinds of timezone offsets
        assertDateSerialization("2010-10-10T10:10:10.100+11:00");
        assertDateSerialization("2010-10-10T10:10:10.100-14:00");
        assertDateSerialization("2010-10-10T10:10:10.100+00:12");
        assertDateSerialization("2010-10-10T10:10:10.100-08:14");

        // Different timestamp accuracies
        assertDateSerialization("2010-10-10T10:10:00.000Z");
        assertDateSerialization("2010-10-10T10:00:00.000Z");
        assertDateSerialization("2010-10-10T00:00:00.000Z");

        // Dates far from today
        assertDateSerialization("1970-01-01T00:00:00.000Z");
        assertDateSerialization("1970-01-01T12:34:56.789-13:45");
        assertDateSerialization("2030-10-10T10:10:10.100+10:10");
        assertDateSerialization("2345-10-10T10:10:10.100Z");
        assertDateSerialization("+9876-10-10T10:10:10.100Z");
        assertDateSerialization("-9876-10-10T10:10:10.100Z");
    }

    /**
     * Tests serialization of forbidden date values.
     */
    public void testForbiddenDateSerialization() throws Exception {

        try {
            // date from really far ahead
            assertDateSerialization("20112022-11-11T19:00:00.000+01:00");
            fail("should not be able to serialize future dates");
        } catch (IllegalArgumentException e) {
            // expected error
        }
    }

    /**
     * creates a future date that is illegal in v3 using v2, and checks that the
     * error is being hadled gracefully
     * 
     * JCR-3083 Degrade gracefully when reading invalid date values
     */
    public void testCorruptedBundle() throws Exception {

        // Bundle as described by the BundleDumper:
        // version: 1
        // nodeTypeName: #13:#49
        // parentUUID: f22c788c-ab98-47cf-95ab-6c495239807a
        // definitionId: 1705077083
        // mixins: -
        // property: #1:#509
        // modcount: 1
        // type: Date
        // definitionId: 806470580
        // value: string: 20112022-11-11T19:00:00.000+01:00
        // referenceable: false
        // childId: 08026c9f-a88a-471b-bcc3-fca2bd82000b #1:linked_products
        // modCount: 3
        //
        // corrupted value: Date -> 20112022-11-11T19:00:00.000+01:00

        byte[] corrupted = new byte[] { 1, 0, 0, 13, 0, 0, 0, 49, 1, -14, 44,
                120, -116, -85, -104, 71, -49, -107, -85, 108, 73, 82, 57,
                -128, 122, 0, 10, 49, 55, 48, 53, 48, 55, 55, 48, 56, 51, -1,
                -1, -1, -1,  0,
                0, 0, 1, 0, 0, 1, -3, 0, 1, 0, 5, 0, 0, 9, 56, 48, 54, 52, 55,
                48, 53, 56, 48, 0, 0, 0, 1, 0, 0, 0, 33, 50, 48, 49, 49, 50,
                48, 50, 50, 45, 49, 49, 45, 49, 49, 84, 49, 57, 58, 48, 48, 58,
                48, 48, 46, 48, 48, 48, 43, 48, 49, 58, 48, 48, -1, -1, -1, -1,
                0, 1, 8, 2, 108, -97, -88, -118, 71, 27, -68, -61, -4, -94,
                -67, -126, 0, 11, 0, 0, 0, 1, 0, 15, 108, 105, 110, 107, 101,
                100, 95, 112, 114, 111, 100, 117, 99, 116, 115, 0, 3, 0, 0, 0,
                0, 70, -53, 75, -124 };

        NodePropBundle result = binding.readBundle(new ByteArrayInputStream(
                corrupted), NodeId.randomId());
        
        Iterator<PropertyEntry> iterator = result.getPropertyEntries()
                .iterator();
        PropertyEntry pe = iterator.next();
        InternalValue iv = pe.getValues()[0];
        assertEquals(PropertyType.DATE, pe.getType());
        assertEquals(PropertyType.DATE, iv.getType());
        assertEquals("20112022-11-11T19:00:00.000+01:00", iv.getString());

        try {
            iv.getDate();
            fail("should not be able to read the property as a DATE");
        } catch (Exception e) {
            // expected
        }
    }

    private void assertDateSerialization(String date) throws Exception {
        assertValueSerialization(
                InternalValue.valueOf(date, PropertyType.DATE));
    }

    private void assertValueSerialization(InternalValue value)
            throws Exception {
        NodePropBundle bundle = new NodePropBundle(NodeId.randomId());
        bundle.setParentId(NodeId.randomId());
        bundle.setNodeTypeName(NameConstants.NT_UNSTRUCTURED);
        bundle.setMixinTypeNames(Collections.<Name>emptySet());
        bundle.setSharedSet(Collections.<NodeId>emptySet());

        Name name = factory.create("", "test");

        PropertyEntry property =
            new PropertyEntry(new PropertyId(bundle.getId(), name));
        property.setType(value.getType());
        property.setMultiValued(false);
        property.setValues(new InternalValue[] { value });
        bundle.addProperty(property);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        binding.writeBundle(buffer, bundle);
        byte[] bytes = buffer.toByteArray();
        NodePropBundle result =
            binding.readBundle(new ByteArrayInputStream(bytes), bundle.getId());

        assertEquals(value, result.getPropertyEntry(name).getValues()[0]);
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
