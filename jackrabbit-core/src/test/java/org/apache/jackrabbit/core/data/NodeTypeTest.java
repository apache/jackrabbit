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
package org.apache.jackrabbit.core.data;

import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.name.NameFactoryImpl;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

public class NodeTypeTest extends AbstractJCRTest {
    
    /**
     * Test a node type with a binary default value
     * @throws RepositoryException 
     */
    public void testNodeTypesWithBinaryDefaultValue() throws RepositoryException {
        Session session = helper.getReadWriteSession();
        NamespaceRegistry reg = session.getWorkspace().getNamespaceRegistry();
        reg.registerNamespace("ns", "http://namespace.com/ns");
        
        doTestNodeTypesWithBinaryDefaultValue(0);
        doTestNodeTypesWithBinaryDefaultValue(10);
        doTestNodeTypesWithBinaryDefaultValue(10000);
    }
    
    public void doTestNodeTypesWithBinaryDefaultValue(int len) {
        try {
            Session session = helper.getReadWriteSession();
            Workspace ws = session.getWorkspace();
            String d = new String(new char[len]).replace('\0', 'a');
            Reader reader = new StringReader(
                    "<ns = 'http://namespace.com/ns'>\n"
                    + "[ns:foo"+len+"] \n" 
                    + "- ns:bar(binary) = '" + d + "' m a");
            CompactNodeTypeDefReader cndReader = new CompactNodeTypeDefReader(
                    reader, "test");
            List ntdList = cndReader.getNodeTypeDefs();
            NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) ws.getNodeTypeManager();
            NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();
            if (!ntreg.isRegistered(NameFactoryImpl.getInstance().create("http://namespace.com/ns", "foo" + len))) {
                ntreg.registerNodeTypes(ntdList);
            }
            Node root = session.getRootNode();
            Node f = root.addNode("testfoo" + len, "ns:foo" + len);
            InputStream in = f.getProperty("ns:bar").getValue().getStream();
            StringBuffer buff = new StringBuffer();
            while (true) {
                int x = in.read();
                if (x < 0) {
                    break;
                }
                buff.append((char) x);
            }
            String d2 = buff.toString();
            assertEquals(d, d2);
        } catch (Exception e) {
            e.printStackTrace();
            assertFalse(e.getMessage(), true);
        }
    }

}
