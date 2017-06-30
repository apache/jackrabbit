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

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayInputStream;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Property;
import javax.jcr.Binary;
import javax.jcr.ValueFormatException;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * <code>BinaryTest</code>...
 */
public class BinaryTest extends AbstractJCRTest {

    private static ByteArrayInputStream generateValue() {
        byte[] data = new byte[1024 * 1024];
        new Random().nextBytes(data);

        return new ByteArrayInputStream(data);
    }

    private static QValue getQValue(Property p) throws ValueFormatException {
        return ((PropertyState) ((PropertyImpl) p).getItemState()).getValue();
    }

    private static void assertDisposed(QValue v) {
        try {
            v.getStream();
            fail("Value should have been disposed.");
        } catch (Exception e) {
            // success (interpret this as value was disposed)
        }
    }

    public void testStreamBinary() throws Exception {
        Node test = testRootNode.addNode("test");
        Property p = test.setProperty("prop", generateValue());
        // check before save
        checkBinary(p);
        superuser.save();
        // check after save
        checkBinary(p);

        // check from other session
        Session s = getHelper().getReadOnlySession();
        try {
            p = s.getNode(testRoot).getNode("test").getProperty("prop");
            checkBinary(p);
        } finally {
            s.logout();
        }
    }

    public void testStreamBinary2() throws Exception {
        Node test = testRootNode.addNode("test");
        Property p = test.setProperty("prop", generateValue());
        // check before save
        checkBinary(p);
        superuser.save();
        // check after save
        checkBinary(p);

        // check from other session
        Session s = getHelper().getReadOnlySession();
        try {
            p = s.getProperty(testRoot + "/test/prop");
            checkBinary(p);
        } finally {
            s.logout();
        }
    }

    public void testBinaryTwiceNewProperty() throws Exception {
        Node test = testRootNode.addNode("test");
        Property p = test.setProperty("prop", generateValue());
        QValue qv1 = getQValue(p);
        test.setProperty("prop", generateValue());
        QValue qv2 = getQValue(p);

        assertFalse(qv1.equals(qv2));

        superuser.save();

        assertEquals(qv2, getQValue(p));
        assertDisposed(qv1);
    }

    public void testBinaryTwiceModifiedProperty() throws Exception {
        Node test = testRootNode.addNode("test");
        Property p = test.setProperty("prop", generateValue());
        superuser.save();

        // modify twice
        test.setProperty("prop", generateValue());
        QValue qv1 = getQValue(p);
        test.setProperty("prop", generateValue());
        QValue qv2 = getQValue(p);

        assertFalse(qv1.equals(qv2));

        superuser.save();

        assertEquals(qv2, getQValue(p));
        assertDisposed(qv1);
    }

    public void testBinaryTwiceIntermediateSave() throws Exception {
        Node test = testRootNode.addNode("test");
        Property p = test.setProperty("prop", generateValue());
        QValue qv1 = getQValue(p);
        superuser.save();

        test.setProperty("prop", generateValue());
        QValue qv2 = getQValue(p);

        assertFalse(qv1.equals(qv2));

        superuser.save();

        assertEquals(qv2, getQValue(p));
        assertDisposed(qv1);
    }

    public void testRevertSettingExistingBinary() throws Exception {
        Node test = testRootNode.addNode("test");

        Binary b = superuser.getValueFactory().createBinary(generateValue());
        Property p = test.setProperty("prop", b);
        QValue qv1 = getQValue(p);
        superuser.save();

        Binary b2 = superuser.getValueFactory().createBinary(generateValue());
        test.setProperty("prop", b2);
        QValue qv2 = getQValue(p);

        assertFalse(qv1.equals(qv2));

        superuser.refresh(false);

        assertEquals(qv1, getQValue(p));
        assertSame(qv1, getQValue(p));

        assertFalse(qv2.equals(getQValue(p)));
    }

    public void testStreamIntegrity() throws Exception {
        Node test = testRootNode.addNode("test");
        byte bytes[] = new byte[256];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte)i;
        }
        ByteArrayInputStream testData = new ByteArrayInputStream(bytes);
        Property p = test.setProperty("prop", superuser.getValueFactory().createBinary(testData));
        superuser.save();

        // check from other session
        Session s = getHelper().getReadOnlySession();
        try {
            p = s.getNode(testRoot).getNode("test").getProperty("prop");

            // check the binaries are indeed the same (JCR-4154)
            byte[] result = new byte[bytes.length];
            IOUtils.readFully(p.getBinary().getStream(), result);
            assertArrayEquals(bytes, result);
        } finally {
            s.logout();
        }
    }

    protected void checkBinary(Property p) throws Exception {
        for (int i = 0; i < 3; i++) {
            Binary bin = p.getBinary();
            try {
                //System.out.println(bin.getClass() + "@" + System.identityHashCode(bin));
                bin.read(new byte[1], 0);
            } finally {
                bin.dispose();
            }
        }
    }
}
