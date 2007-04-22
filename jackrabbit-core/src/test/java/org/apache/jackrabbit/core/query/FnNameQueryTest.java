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

import javax.jcr.RepositoryException;
import javax.jcr.Node;

/**
 * <code>FnNameQueryTest</code> tests queries with fn:name() functions.
 */
public class FnNameQueryTest extends AbstractQueryTest {

    public void testFnName() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1);
        n1.setProperty(propertyName1, 1);
        Node n2 = testRootNode.addNode(nodeName2);
        n2.setProperty(propertyName1, 2);
        Node n3 = testRootNode.addNode(nodeName3);
        n3.setProperty(propertyName1, 3);

        testRootNode.save();

        String base = testPath + "/*[@" + propertyName1;
        executeXPathQuery(base + " = 1 and fn:name() = '" + nodeName1 + "']",
                new Node[]{n1});
        executeXPathQuery(base + " = 1 and fn:name() = '" + nodeName2 + "']",
                new Node[]{});
        executeXPathQuery(base + " > 0 and fn:name() = '" + nodeName2 + "']",
                new Node[]{n2});
        executeXPathQuery(base + " > 0 and (fn:name() = '" + nodeName1 +
                "' or fn:name() = '" + nodeName2 + "')]", new Node[]{n1, n2});
        executeXPathQuery(base + " > 0 and not(fn:name() = '" + nodeName1 + "')]",
                new Node[]{n2, n3});
    }

    public void testFnNameWithSpace() throws RepositoryException {
        Node n1 = testRootNode.addNode("My Documents");
        n1.setProperty(propertyName1, 1);

        testRootNode.save();

        String base = testPath + "/*[@" + propertyName1;
        executeXPathQuery(base + " = 1 and fn:name() = 'My Documents']",
                new Node[]{});
        executeXPathQuery(base + " = 1 and fn:name() = 'My_x0020_Documents']",
                new Node[]{n1});
    }
}
