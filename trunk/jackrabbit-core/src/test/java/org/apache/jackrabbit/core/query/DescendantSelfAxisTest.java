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

import static org.apache.jackrabbit.JcrConstants.*;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class DescendantSelfAxisTest extends AbstractQueryTest {

    /**
     * JCR-3407 CaseTermQuery #rewrite behavior changes
     */
    public void testCaseTermQueryNPE() throws RepositoryException {
        String xpathNPE = "//element(*,nt:unstructured)[fn:lower-case(@jcr:language)='en']//element(*,nt:unstructured)[@jcr:message]/(@jcr:key|@jcr:message)";
        executeXPathQuery(xpathNPE, new Node[] {});
    }

    /**
     * JCR-3401 Wrong results when querying with a DescendantSelfAxisQuery
     */
    public void testNodeName() throws RepositoryException {
        String name = "testNodeName" + System.currentTimeMillis();

        Node foo = testRootNode.addNode("foo", NT_UNSTRUCTURED);
        foo.addNode("branch1", NT_FOLDER).addNode(name, NT_FOLDER);
        foo.addNode("branch2", NT_FOLDER).addNode(name, NT_FOLDER);
        Node bar = testRootNode.addNode(name, NT_UNSTRUCTURED);

        testRootNode.getSession().save();

        executeXPathQuery("//element(*, nt:unstructured)[fn:name() = '" + name
                + "']", new Node[] { bar });
        executeXPathQuery("//element(" + name + ", nt:unstructured)",
                new Node[] { bar });
    }
}
