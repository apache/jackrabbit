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
import java.io.ByteArrayInputStream;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Property;
import javax.jcr.Binary;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * <code>BinaryTest</code>...
 */
public class BinaryTest extends AbstractJCRTest {

    public void testStreamBinary() throws Exception {
        byte[] data = new byte[1024 * 1024];
        new Random().nextBytes(data);
        Node test = testRootNode.addNode("test");
        Property p = test.setProperty("prop", new ByteArrayInputStream(data));
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
