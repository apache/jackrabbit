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
package org.apache.jackrabbit.spi2davex;

import org.apache.jackrabbit.spi.AbstractSPITest;
import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.commons.namespace.AbstractNamespaceResolver;

import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * <code>ConnectionTest</code>...
 */
public class BatchTest extends AbstractSPITest {

    private final String testPath = "/test";
    private NamePathResolver resolver;
    private RepositoryService rs;
    private SessionInfo si;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        rs = helper.getRepositoryService();
        si = helper.getAdminSessionInfo();
        NamespaceResolver nsResolver = new AbstractNamespaceResolver() {
            public String getURI(String prefix) {
                return ("jcr".equals(prefix)) ? "http://www.jcp.org/jcr/1.0" : prefix;
            }
            public String getPrefix(String uri) {
                return ("http://www.jcp.org/jcr/1.0".equals(uri)) ? "jcr" : uri;
            }
        };
        resolver = new DefaultNamePathResolver(nsResolver);
        
        try {
            rs.getNodeInfo(si, getNodeId(testPath));
        } catch (RepositoryException e) {
            Batch b = rs.createBatch(si, getNodeId("/"));
            b.addNode(getNodeId("/"), resolver.getQName("test"), NameConstants.NT_UNSTRUCTURED, null);
            rs.submit(b);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            Batch b = rs.createBatch(si, getNodeId("/"));
            b.remove(getNodeId(testPath));
            rs.submit(b);
        } finally {
            rs.dispose(si);
            super.tearDown();
        }
    }

    public void testAddNode() throws RepositoryException {
        NodeId nid = getNodeId(testPath);
        Batch b = rs.createBatch(si, nid);

        b.addNode(nid, resolver.getQName("aNode"), NameConstants.NT_UNSTRUCTURED, null);
        b.addProperty(nid, resolver.getQName("aString"), rs.getQValueFactory().create("ba", PropertyType.STRING));
        b.addProperty(nid, resolver.getQName("aName"), new QValue[] {rs.getQValueFactory().create(NameConstants.JCR_ENCODING), rs.getQValueFactory().create(NameConstants.JCR_DATA)});
        b.addProperty(nid, resolver.getQName("aBinary"), rs.getQValueFactory().create(new byte[] { 'a', 'b', 'c'}));

        rs.submit(b);

        NodeId id = rs.getIdFactory().createNodeId(nid, resolver.getQPath("aNode"));
        Iterator<? extends ItemInfo> it = rs.getItemInfos(si, id);
        while (it.hasNext()) {
            ItemInfo info = it.next();
            if (info.denotesNode()) {
                NodeInfo nInfo = (NodeInfo) info;
                assertEquals(NameConstants.NT_UNSTRUCTURED, nInfo.getNodetype());
                Iterator<ChildInfo> childIt = nInfo.getChildInfos();
                assertTrue(childIt == null || !childIt.hasNext());
                assertEquals(id, nInfo.getId());
            }
        }

        b = rs.createBatch(si, nid);
        b.remove(id);
        rs.submit(b);
    }

    public void testImport() throws RepositoryException {
        NodeId nid = getNodeId(testPath);
        Batch b = rs.createBatch(si, nid);

        String uuid = UUID.randomUUID().toString();
        b.addNode(nid, resolver.getQName("testUUIDNode"), NameConstants.NT_UNSTRUCTURED, uuid);

        NodeId id = getNodeId(testPath + "/testUUIDNode");
        b.setMixins(id, new Name[] {NameConstants.MIX_REFERENCEABLE});

        rs.submit(b);

        NodeInfo nInfo = rs.getNodeInfo(si, id);
        assertEquals(uuid, nInfo.getId().getUniqueID());
        Name[] mixins = nInfo.getMixins();
        assertEquals(1, mixins.length);
        assertEquals(NameConstants.MIX_REFERENCEABLE, mixins[0]);

        b = rs.createBatch(si, nid);
        b.remove(rs.getIdFactory().createNodeId(uuid));
        rs.submit(b);

        try {
            rs.getItemInfos(si, id);
            fail();
        } catch (RepositoryException e) {
            // success
        }
        try {
            rs.getItemInfos(si, rs.getIdFactory().createNodeId(uuid));
            fail();
        } catch (RepositoryException e) {
            // success
        }
    }

    public void testSetMixin() throws RepositoryException {
        NodeId nid = getNodeId(testPath);

        Batch b = rs.createBatch(si, nid);
        b.addNode(nid, resolver.getQName("anyNode"), NameConstants.NT_UNSTRUCTURED, null);
        NodeId id = getNodeId(testPath + "/anyNode");
        b.setMixins(id, new Name[] {NameConstants.MIX_LOCKABLE});
        rs.submit(b);

        b = rs.createBatch(si, id);
        b.setMixins(id, new Name[0]);
        rs.submit(b);

        NodeInfo nInfo = rs.getNodeInfo(si, id);
        assertEquals(0, nInfo.getMixins().length);
    }

    public void testMove() throws RepositoryException {
        NodeId nid = getNodeId(testPath);

        Batch b = rs.createBatch(si, nid);
        b.addNode(nid, resolver.getQName("anyNode"), NameConstants.NT_UNSTRUCTURED, null);
        rs.submit(b);

        NodeId id = getNodeId(testPath + "/anyNode");

        b = rs.createBatch(si, nid);
        b.move(id, nid, resolver.getQName("moved"));
        rs.submit(b);

        try {
            rs.getItemInfos(si, id);
            fail();
        } catch (RepositoryException e) {
            // ok
        }

        rs.getNodeInfo(si, getNodeId(testPath + "/moved"));
    }

    public void testReorder() throws RepositoryException {
        NodeId nid = getNodeId(testPath);

        Batch b = rs.createBatch(si, nid);
        b.addNode(nid, resolver.getQName("1"), NameConstants.NT_UNSTRUCTURED, null);
        b.addNode(nid, resolver.getQName("3"), NameConstants.NT_UNSTRUCTURED, null);
        b.addNode(nid, resolver.getQName("2"), NameConstants.NT_UNSTRUCTURED, null);
        rs.submit(b);

        b = rs.createBatch(si, nid);
        b.reorderNodes(nid, getNodeId(testPath + "/3"), null);
        rs.submit(b);

        Iterator<ChildInfo> it = rs.getChildInfos(si, nid);
        int i = 1;
        while (it.hasNext()) {
            ChildInfo ci = it.next();
            assertEquals(i, Integer.parseInt(ci.getName().getLocalName()));
            i++;
        }
    }

    public void testReorder1() throws RepositoryException {
        NodeId nid = getNodeId(testPath);

        Batch b = rs.createBatch(si, nid);
        b.addNode(nid, resolver.getQName("2"), NameConstants.NT_UNSTRUCTURED, null);
        b.addNode(nid, resolver.getQName("3"), NameConstants.NT_UNSTRUCTURED, null);
        b.addNode(nid, resolver.getQName("1"), NameConstants.NT_UNSTRUCTURED, null);
        rs.submit(b);

        b = rs.createBatch(si, nid);
        b.reorderNodes(nid, getNodeId(testPath + "/1"), getNodeId(testPath + "/2"));
        rs.submit(b);

        Iterator<ChildInfo> it = rs.getChildInfos(si, nid);
        int i = 1;
        while (it.hasNext()) {
            ChildInfo ci = it.next();
            assertEquals(i, Integer.parseInt(ci.getName().getLocalName()));
            i++;
        }
    }

    public void testRemove() throws RepositoryException {
        NodeId nid = getNodeId(testPath);
        Batch b = rs.createBatch(si, nid);

        NodeId id = getNodeId(testPath + "/aTestNode");
        b.addNode(nid, resolver.getQName("aTestNode"), NameConstants.NT_UNSTRUCTURED, null);
        b.addProperty(id, resolver.getQName("aString"), rs.getQValueFactory().create("ba", PropertyType.STRING));
        rs.submit(b);

        PropertyId pid = getPropertyId(id, resolver.getQName("aString"));
        b = rs.createBatch(si, nid);
        b.remove(pid);
        rs.submit(b);

        try {
            rs.getPropertyInfo(si, pid);
            fail();
        } catch (RepositoryException e) {
            // success
        }
    }

    public void testEmptyValueArray() throws RepositoryException {
        NodeId nid = getNodeId(testPath);
        Name propName = resolver.getQName("mvProperty");

        Batch b = rs.createBatch(si, nid);
        b.addProperty(nid, propName, new QValue[0]);
        rs.submit(b);

        PropertyId pid = getPropertyId(nid, propName);
        PropertyInfo pi = rs.getPropertyInfo(si, pid);
        assertTrue(pi.isMultiValued());
        assertEquals(Arrays.asList(new QValue[0]), Arrays.asList(pi.getValues()));
        assertFalse(pi.getType() == PropertyType.UNDEFINED);

        Iterator<? extends ItemInfo> it = rs.getItemInfos(si, nid);
        while (it.hasNext()) {
            ItemInfo info = it.next();
            if (!info.denotesNode()) {
                PropertyInfo pInfo = (PropertyInfo) info;
                if (propName.equals((pInfo.getId().getName()))) {
                    assertTrue(pi.isMultiValued());
                    assertEquals(Arrays.asList(new QValue[0]), Arrays.asList(pi.getValues()));
                    assertFalse(pi.getType() == PropertyType.UNDEFINED);
                    break;
                }
            }
        }
    }

    public void testEmptyValueArray2() throws RepositoryException {
        NodeId nid = getNodeId(testPath);
        Name propName = resolver.getQName("mvProperty");

        Batch b = rs.createBatch(si, nid);
        b.addProperty(nid, propName, new QValue[] { rs.getQValueFactory().create(true)});
        rs.submit(b);

        PropertyId pid = getPropertyId(nid, propName);
        b = rs.createBatch(si, pid);
        b.setValue(pid, new QValue[0]);
        rs.submit(b);

        PropertyInfo pi = rs.getPropertyInfo(si, pid);
        assertTrue(pi.isMultiValued());
        assertEquals(Arrays.asList(new QValue[0]), Arrays.asList(pi.getValues()));
    }

    public void testMultiValuedProperty() throws RepositoryException {
        NodeId nid = getNodeId(testPath);
        Name propName = resolver.getQName("mvProperty2");
        QValue[] vs = new QValue[] {rs.getQValueFactory().create(111), rs.getQValueFactory().create(222), rs.getQValueFactory().create(333)};

        Batch b = rs.createBatch(si, nid);
        b.addProperty(nid, propName, vs);
        rs.submit(b);

        PropertyInfo pi = rs.getPropertyInfo(si, getPropertyId(nid, propName));
        assertTrue(pi.isMultiValued());
        assertEquals(Arrays.asList(vs), Arrays.asList(pi.getValues()));
        assertEquals(PropertyType.LONG, pi.getType());
    }

    public void testSetBinaryValue() throws RepositoryException, IOException {
        NodeId nid = getNodeId(testPath);
        Name propName = resolver.getQName("binProp");

        Batch b = rs.createBatch(si, nid);
        b.addProperty(nid, propName, rs.getQValueFactory().create(new byte[] {'a', 'b', 'c'}));
        rs.submit(b);

        PropertyInfo pi = rs.getPropertyInfo(si, getPropertyId(nid, propName));
        assertFalse(pi.isMultiValued());
        assertEquals("abc", pi.getValues()[0].getString());
        assertEquals(PropertyType.BINARY, pi.getType());
    }

    public void testSetEmptyBinaryValue() throws RepositoryException, IOException {
        NodeId nid = getNodeId(testPath);
        Name propName = resolver.getQName("binProp");

        Batch b = rs.createBatch(si, nid);
        b.addProperty(nid, propName, rs.getQValueFactory().create(new byte[0]));
        rs.submit(b);

        PropertyInfo pi = rs.getPropertyInfo(si, getPropertyId(nid, propName));
        assertFalse(pi.isMultiValued());
        InputStream in = pi.getValues()[0].getStream();
        assertTrue(in.read() == -1);
        assertEquals("", pi.getValues()[0].getString());
        assertEquals(PropertyType.BINARY, pi.getType());

        pi = getPropertyInfo(nid, propName);
        assertFalse(pi.isMultiValued());
        in = pi.getValues()[0].getStream();
        assertTrue(in.read() == -1);
        assertEquals("", pi.getValues()[0].getString());
        assertEquals(PropertyType.BINARY, pi.getType());
    }

    public void testSetBinaryValues() throws RepositoryException, IOException {
        NodeId nid = getNodeId(testPath);
        Name propName = resolver.getQName("binPropMV");

        Batch b = rs.createBatch(si, nid);
        QValue[] vs = new QValue[] {
                rs.getQValueFactory().create(new byte[] {'a', 'b', 'c'}),
                rs.getQValueFactory().create(new byte[] {'d', 'e', 'f'}),
                rs.getQValueFactory().create(new byte[] {'g', 'h', 'i'})
        };
        b.addProperty(nid, propName, vs);
        rs.submit(b);

        PropertyInfo pi = rs.getPropertyInfo(si, getPropertyId(nid, propName));
        assertTrue(pi.isMultiValued());
        vs = pi.getValues();
        assertEquals("abc", vs[0].getString());
        assertEquals("def", vs[1].getString());
        assertEquals("ghi", vs[2].getString());
        assertEquals(PropertyType.BINARY, pi.getType());

        pi = getPropertyInfo(nid, propName);
        vs = pi.getValues();
        assertEquals("abc", vs[0].getString());
        assertEquals("def", vs[1].getString());
        assertEquals("ghi", vs[2].getString());
        assertEquals(PropertyType.BINARY, pi.getType());
    }

    public void testSetMixedBinaryValues() throws RepositoryException, IOException {
        NodeId nid = getNodeId(testPath);
        Name propName = resolver.getQName("binPropMV");

        Batch b = rs.createBatch(si, nid);
        QValue[] vs = new QValue[] {
                rs.getQValueFactory().create(new byte[] {'a', 'b', 'c'}),
                rs.getQValueFactory().create(new byte[0]),
                rs.getQValueFactory().create(new byte[] {'g', 'h', 'i'})
        };
        b.addProperty(nid, propName, vs);
        rs.submit(b);

        PropertyInfo pi = rs.getPropertyInfo(si, getPropertyId(nid, propName));
        assertTrue(pi.isMultiValued());
        vs = pi.getValues();
        assertEquals("abc", vs[0].getString());
        assertEquals("", vs[1].getString());
        assertEquals("ghi", vs[2].getString());
        assertEquals(PropertyType.BINARY, pi.getType());

        pi = getPropertyInfo(nid, propName);
        vs = pi.getValues();
        assertEquals("abc", vs[0].getString());
        assertEquals("", vs[1].getString());
        assertEquals("ghi", vs[2].getString());
        assertEquals(PropertyType.BINARY, pi.getType());
    }

    public void testSetEmptyBinaryValues() throws RepositoryException, IOException {
        NodeId nid = getNodeId(testPath);
        Name propName = resolver.getQName("binPropMV");

        Batch b = rs.createBatch(si, nid);
        QValue[] vs = new QValue[] {
                rs.getQValueFactory().create(new byte[0]),
                rs.getQValueFactory().create(new byte[0]),
                rs.getQValueFactory().create(new byte[0])
        };
        b.addProperty(nid, propName, vs);
        rs.submit(b);

        PropertyInfo pi = rs.getPropertyInfo(si, getPropertyId(nid, propName));
        assertTrue(pi.isMultiValued());
        vs = pi.getValues();
        assertEquals("", vs[0].getString());
        assertEquals("", vs[1].getString());
        assertEquals("", vs[2].getString());
        assertEquals(PropertyType.BINARY, pi.getType());

        pi = getPropertyInfo(nid, propName);
        vs = pi.getValues();
        assertEquals("", vs[0].getString());
        assertEquals("", vs[1].getString());
        assertEquals("", vs[2].getString());
        assertEquals(PropertyType.BINARY, pi.getType());
    }

    public void testBinary() throws RepositoryException, IOException {
        NodeId nid = getNodeId(testPath);
        Name propName = resolver.getQName("binProp");

        Batch b = rs.createBatch(si, nid);
        ClassLoader loader = getClass().getClassLoader();
        InputStream in = loader.getResourceAsStream("org/apache/jackrabbit/spi/spi2davex/image.bmp");
        if (in != null) {
            try {
                QValue v = rs.getQValueFactory().create(in);
                b.addProperty(nid, propName, v);
                rs.submit(b);

                PropertyInfo pi = rs.getPropertyInfo(si, getPropertyId(nid, propName));
                String str1 = pi.getValues()[0].getString();

                pi = getPropertyInfo(nid, propName);
                String str2 = pi.getValues()[0].getString();
                assertEquals(str1, str2);
            } finally {
                in.close();
            }
        }
    }

    public void testSetDoubleValue() throws RepositoryException {
        NodeId nid = getNodeId(testPath);
        Name propName = resolver.getQName("doubleProp");

        QValue v = rs.getQValueFactory().create((double) 12);

        Batch b = rs.createBatch(si, nid);
        b.addProperty(nid, propName, v);
        rs.submit(b);

        PropertyInfo pi = rs.getPropertyInfo(si, getPropertyId(nid, propName));
        assertFalse(pi.isMultiValued());
        assertEquals(v, pi.getValues()[0]);
        assertEquals(v.getString(), pi.getValues()[0].getString());
        assertEquals(PropertyType.DOUBLE, pi.getType());

        pi = getPropertyInfo(nid, propName);
        assertEquals(v, pi.getValues()[0]);
        assertEquals(v.getString(), pi.getValues()[0].getString());
        assertEquals(PropertyType.DOUBLE, pi.getType());
    }

    public void testSetLongValue() throws RepositoryException {
        NodeId nid = getNodeId(testPath);
        Name propName = resolver.getQName("doubleProp");

        QValue v = rs.getQValueFactory().create(234567);

        Batch b = rs.createBatch(si, nid);
        b.addProperty(nid, propName, v);
        rs.submit(b);

        PropertyInfo pi = rs.getPropertyInfo(si, getPropertyId(nid, propName));
        assertFalse(pi.isMultiValued());
        assertEquals(v, pi.getValues()[0]);
        assertEquals(v.getString(), pi.getValues()[0].getString());
        assertEquals(PropertyType.LONG, pi.getType());

        pi = getPropertyInfo(nid, propName);
        assertEquals(v, pi.getValues()[0]);
        assertEquals(v.getString(), pi.getValues()[0].getString());
        assertEquals(PropertyType.LONG, pi.getType());
    }

    public void testSetDateValue() throws RepositoryException {
        NodeId nid = getNodeId(testPath);
        Name propName = resolver.getQName("dateProp");

        QValue v = rs.getQValueFactory().create(Calendar.getInstance());

        Batch b = rs.createBatch(si, nid);
        b.addProperty(nid, propName, v);
        rs.submit(b);

        PropertyInfo pi = rs.getPropertyInfo(si, getPropertyId(nid, propName));
        assertFalse(pi.isMultiValued());
        assertEquals(v, pi.getValues()[0]);
        assertEquals(v.getString(), pi.getValues()[0].getString());
        assertEquals(PropertyType.DATE, pi.getType());

        pi = getPropertyInfo(nid, propName);
        assertEquals(v, pi.getValues()[0]);
        assertEquals(v.getString(), pi.getValues()[0].getString());
        assertEquals(PropertyType.DATE, pi.getType());
    }

    public void testSetStringValue() throws RepositoryException {
        NodeId nid = getNodeId(testPath);
        Name propName = resolver.getQName("stringProp");
        QValueFactory vf = rs.getQValueFactory();

        List<String> l = new ArrayList<String>();
        l.add("String value containing \"double quotes\" and \'single\' and \"undeterminated quote.");
        l.add("String value \ncontaining \n\rline \r\nseparators and \t tab.");
        l.add("String value containing \r\n\r\r\n\r\n multiple \r\n\r\n line separators in sequence.");
        l.add("String value containing >diff -char +act ^ters.");
        l.add("String value containing \n>line sep \r+and \r\n-diff\n\r^chars.");
        l.add("String value containing \u0633\u0634 unicode chars.");

        for (String val : l) {
            QValue v = vf.create(val, PropertyType.STRING);
            Batch b = rs.createBatch(si, nid);
            b.addProperty(nid, propName, v);
            rs.submit(b);

            PropertyInfo pi = getPropertyInfo(nid, propName);
            assertEquals(v, pi.getValues()[0]);
            assertEquals(v.getString(), pi.getValues()[0].getString());
            assertEquals(PropertyType.STRING, pi.getType());

            pi = rs.getPropertyInfo(si, getPropertyId(nid, propName));
            assertEquals(v, pi.getValues()[0]);
            assertEquals(v.getString(), pi.getValues()[0].getString());
            assertEquals(PropertyType.STRING, pi.getType());
        }
    }

    public void testSetNameValue() throws RepositoryException {
        NodeId nid = getNodeId(testPath);
        Name propName = resolver.getQName("nameProp");

        QValue[] vs = new QValue[] {
                rs.getQValueFactory().create(NameConstants.JCR_BASEVERSION),
                rs.getQValueFactory().create(NameConstants.JCR_DEFAULTPRIMARYTYPE),
                rs.getQValueFactory().create(NameConstants.MIX_LOCKABLE),
                rs.getQValueFactory().create(NameConstants.JCR_PRIMARYTYPE),
                rs.getQValueFactory().create(NameConstants.NT_VERSION)
        };
        Batch b = rs.createBatch(si, nid);
        b.addProperty(nid, propName, vs);
        rs.submit(b);

        PropertyInfo pi = rs.getPropertyInfo(si, getPropertyId(nid, propName));
        assertTrue(pi.isMultiValued());
        assertEquals(Arrays.asList(vs), Arrays.asList(pi.getValues()));
        assertEquals(PropertyType.NAME, pi.getType());

        pi = getPropertyInfo(nid, propName);
        assertEquals(vs[0].getName(), pi.getValues()[0].getName());
        assertEquals(Arrays.asList(vs), Arrays.asList(pi.getValues()));
        assertEquals(PropertyType.NAME, pi.getType());
    }

    public void testSetPathValue() throws RepositoryException {
        NodeId nid = getNodeId(testPath);
        Name propName = resolver.getQName("pathProp");

        QValue v = rs.getQValueFactory().create(resolver.getQPath(testPath));
        Batch b = rs.createBatch(si, nid);
        b.addProperty(nid, propName, v);
        rs.submit(b);

        PropertyInfo pi = rs.getPropertyInfo(si, getPropertyId(nid, propName));
        assertFalse(pi.isMultiValued());
        assertEquals(v, pi.getValues()[0]);
        assertEquals(PropertyType.PATH, pi.getType());

        pi = getPropertyInfo(nid, propName);
        assertEquals(v.getPath(), pi.getValues()[0].getPath());
        assertEquals(v, pi.getValues()[0]);
        assertEquals(PropertyType.PATH, pi.getType());
    }

    public void testSetBooleanValue() throws RepositoryException {
        NodeId nid = getNodeId(testPath);
        Name propName = resolver.getQName("booleanProp");

        QValue v = rs.getQValueFactory().create(false);
        Batch b = rs.createBatch(si, nid);
        b.addProperty(nid, propName, v);
        rs.submit(b);

        PropertyInfo pi = rs.getPropertyInfo(si, getPropertyId(nid, propName));
        assertFalse(pi.isMultiValued());
        assertFalse(pi.getValues()[0].getBoolean());
        assertEquals(PropertyType.BOOLEAN, pi.getType());

        pi = getPropertyInfo(nid, propName);
        assertFalse(pi.getValues()[0].getBoolean());
        assertEquals(PropertyType.BOOLEAN, pi.getType());
    }

    public void testSetReferenceValue() throws RepositoryException {
        NodeId nid = getNodeId(testPath);
        NodeInfo nInfo = rs.getNodeInfo(si, nid);
        if (!Arrays.asList(nInfo.getMixins()).contains(NameConstants.MIX_REFERENCEABLE)) {
            Batch b = rs.createBatch(si, nid);
            b.setMixins(nid, new Name[] {NameConstants.MIX_REFERENCEABLE});
            rs.submit(b);
        }

        String ref = rs.getNodeInfo(si, nid).getId().getUniqueID();
        Name propName = resolver.getQName("refProp");
        QValue v = rs.getQValueFactory().create(ref, PropertyType.REFERENCE);

        Batch b = rs.createBatch(si, nid);
        b.addProperty(nid, propName, v);
        rs.submit(b);

        PropertyInfo pi = rs.getPropertyInfo(si, getPropertyId(nid, propName));
        assertFalse(pi.isMultiValued());
        assertEquals(v, pi.getValues()[0]);
        assertEquals(PropertyType.REFERENCE, pi.getType());

        pi = getPropertyInfo(nid, propName);
        assertEquals(v, pi.getValues()[0]);
        assertEquals(PropertyType.REFERENCE, pi.getType());
    }

    public void testSetWeakReferenceValue() throws RepositoryException {
        NodeId nid = getNodeId(testPath);
        NodeInfo nInfo = rs.getNodeInfo(si, nid);
        if (!Arrays.asList(nInfo.getMixins()).contains(NameConstants.MIX_REFERENCEABLE)) {
            Batch b = rs.createBatch(si, nid);
            b.setMixins(nid, new Name[] {NameConstants.MIX_REFERENCEABLE});
            rs.submit(b);
        }

        String ref = rs.getNodeInfo(si, nid).getId().getUniqueID();
        Name propName = resolver.getQName("weakRefProp");
        QValue v = rs.getQValueFactory().create(ref, PropertyType.WEAKREFERENCE);

        Batch b = rs.createBatch(si, nid);
        b.addProperty(nid, propName, v);
        rs.submit(b);

        PropertyInfo pi = rs.getPropertyInfo(si, getPropertyId(nid, propName));
        assertFalse(pi.isMultiValued());
        assertEquals(v, pi.getValues()[0]);
        assertEquals(PropertyType.WEAKREFERENCE, pi.getType());

        pi = getPropertyInfo(nid, propName);
        assertEquals(v, pi.getValues()[0]);
        assertEquals(PropertyType.WEAKREFERENCE, pi.getType());
    }

    public void testSetPropertyTwice() throws RepositoryException {
        NodeId nid = getNodeId(testPath);
        Name propName = resolver.getQName("nameProp");
        PropertyId pid = getPropertyId(nid, propName);

        QValue v = rs.getQValueFactory().create(NameConstants.JCR_AUTOCREATED);
        QValue v2 = rs.getQValueFactory().create(NameConstants.JCR_BASEVERSION);
        QValue v3 = rs.getQValueFactory().create(NameConstants.JCR_CONTENT);

        Batch b = rs.createBatch(si, nid);
        b.addProperty(nid, propName, v);
        b.setValue(pid, v2);
        b.setValue(pid, v3);
        rs.submit(b);

        PropertyInfo pi = rs.getPropertyInfo(si, getPropertyId(nid, propName));
        assertFalse(pi.isMultiValued());
        assertEquals(1, pi.getValues().length);
        assertEquals(v3, pi.getValues()[0]);
        assertEquals(PropertyType.NAME, pi.getType());

        pi = getPropertyInfo(nid, propName);
        assertFalse(pi.isMultiValued());
        assertEquals(1, pi.getValues().length);
        assertEquals(v3, pi.getValues()[0]);
        assertEquals(PropertyType.NAME, pi.getType());
    }

    public void testUseConsumedBatch() throws RepositoryException {
        NodeId nid = getNodeId(testPath);
        Batch b = rs.createBatch(si, nid);
        b.addProperty(nid, resolver.getQName("any"), rs.getQValueFactory().create(1.34));
        rs.submit(b);

        try {
            b.remove(nid);
            rs.submit(b);
            fail();
        } catch (IllegalStateException e) {
            // success
        }
    }
    //--------------------------------------------------------------------------
    private NodeId getNodeId(String path) throws NamespaceException, RepositoryException {
        return rs.getIdFactory().createNodeId((String) null, resolver.getQPath(path));
    }

    private PropertyId getPropertyId(NodeId nId, Name propName) throws RepositoryException {
        return rs.getIdFactory().createPropertyId(nId, propName);
    }

    private PropertyInfo getPropertyInfo(NodeId parentId, Name propName) throws RepositoryException {
        Iterator<? extends ItemInfo> it = rs.getItemInfos(si, parentId);
        while (it.hasNext()) {
            ItemInfo info = it.next();
            if (!info.denotesNode()) {
                PropertyInfo pInfo = (PropertyInfo) info;
                if (propName.equals((pInfo.getId().getName()))) {
                    return pInfo;
                }
            }
        }
        throw new ItemNotFoundException();
    }
}
