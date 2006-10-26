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
package org.apache.jackrabbit.init;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;

import javax.jcr.RepositoryException;
import javax.jcr.NamespaceRegistry;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

/**
 * <code>NodeTypeData</code> registers a test node type if it is not yet
 * registered.
 */
public class NodeTypeData extends AbstractJCRTest {

    private static final String EXAMPLE_NAMESPACE = "http://example.org/jackrabbit/example";

    private static final String CND = "<ex = \"" + EXAMPLE_NAMESPACE + "\">\n" +
            "[ex:NodeType] > nt:unstructured";

    public void testRegisterNodeType() throws Exception {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(data);
        writer.write(CND);
        writer.close();

        NamespaceRegistry nsReg = superuser.getWorkspace().getNamespaceRegistry();
        try {
            nsReg.getPrefix(EXAMPLE_NAMESPACE);
        } catch (RepositoryException e) {
            // not yet registered
            nsReg.registerNamespace("ex", EXAMPLE_NAMESPACE);
        }
        JackrabbitNodeTypeManager ntMgr = (JackrabbitNodeTypeManager) superuser.getWorkspace().getNodeTypeManager();
        if (!ntMgr.hasNodeType("ex:NodeType")) {
            ntMgr.registerNodeTypes(new ByteArrayInputStream(data.toByteArray()), JackrabbitNodeTypeManager.TEXT_X_JCR_CND);
        }
    }
}
