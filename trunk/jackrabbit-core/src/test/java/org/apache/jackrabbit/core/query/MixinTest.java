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
package org.apache.jackrabbit.core.query;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.commons.cnd.CndImporter;

/**
 * Tests if mixin types are queried correctly when using element test: element()
 */
public class MixinTest extends AbstractQueryTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        NodeTypeManager manager = superuser.getWorkspace().getNodeTypeManager();
        if (!manager.hasNodeType("test:mimeType")) {
            String cnd =
                "<test='http://www.apache.org/jackrabbit/test'>\n"
                + "[test:mimeType] > mix:mimeType mixin";

            Reader cndReader = new InputStreamReader(new ByteArrayInputStream(cnd.getBytes()));
            CndImporter.registerNodeTypes(cndReader, superuser);
        }
    }

    public void testBuiltInMixin() throws RepositoryException {
        // nt:resource is mix:mimeType by its node type definition
        Node n1 = testRootNode.addNode("n1", "nt:resource");
        n1.setProperty("jcr:data", new ByteArrayInputStream("hello world".getBytes()));
        n1.setProperty("jcr:lastModified", Calendar.getInstance());
        n1.setProperty("jcr:mimeType", "application/octet-stream");

        // assign mix:referenceable to arbitrary node
        Node n2 = testRootNode.addNode("n2");
        n2.addMixin("mix:mimeType");

        // make node referenceable using a mixin that extends from mix:mimeType
        Node n3 = testRootNode.addNode("n3");
        n3.addMixin("test:mimeType");

        testRootNode.save();

        String query = testPath + "//element(*, mix:mimeType)";
        executeXPathQuery(query, new Node[]{n1, n2, n3});
    }

}
