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
package org.apache.jackrabbit.core.value;

import java.io.ByteArrayInputStream;
import java.util.Random;

import javax.jcr.RepositoryException;
import javax.jcr.Property;
import javax.jcr.Binary;
import javax.jcr.Node;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * <code>BinaryValueTest</code> check if multiple executions of:
 * <ul>
 * <li>get Binary from property</li>
 * <li>read from Binary</li>
 * <li>dispose Binary</li>
 * </ul>
 * do not throw an exception.
 * <p>
 * See also JCR-2238.
 */
public class BinaryValueTest extends AbstractJCRTest {

    public void testDispose10() throws Exception {
        checkDispose(10, false);
    }

    public void testDispose10k() throws Exception {
        checkDispose(10 * 1024, false);
    }

    public void testDispose10Save() throws Exception {
        checkDispose(10, true);
    }

    public void testDispose10kSave() throws Exception {
        checkDispose(10 * 1024, true);
    }

    protected void checkDispose(int length, boolean save) throws Exception {
        Property prop = setProperty(testRootNode.addNode(nodeName1), length);
        if (save) {
            superuser.save();
        }
        checkProperty(prop);
    }

    protected Property setProperty(Node node, int length) throws RepositoryException {
        Random rand = new Random();
        byte[] data = new byte[length];
        rand.nextBytes(data);

        Binary b = vf.createBinary(new ByteArrayInputStream(data));
        //System.out.println(b.getClass() + ": " + System.identityHashCode(b));
        try {
            return node.setProperty(propertyName1, b);
        } finally {
            b.dispose();
        }
    }

    protected void checkProperty(Property prop) throws Exception {
        for (int i = 0; i < 3; i++) {
            Binary b = prop.getBinary();
            try {
                //System.out.println(b.getClass() + ": " + System.identityHashCode(b));
                b.read(new byte[1], 0);
            } finally {
                b.dispose();
            }
        }
    }
}
