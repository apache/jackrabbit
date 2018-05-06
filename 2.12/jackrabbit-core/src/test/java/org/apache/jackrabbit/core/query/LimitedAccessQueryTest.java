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

import java.security.Principal;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;

import org.apache.jackrabbit.core.NodeImplTest;

/**
 * <code>LimitedAccessQueryTest</code> tests queries that include nodes that are
 * outside their access.
 */
public class LimitedAccessQueryTest extends AbstractQueryTest {

    private Session readOnly;
    private Principal principal;

    private Node a;
    private Node b;

    protected void setUp() throws Exception {
        super.setUp();

        a = testRootNode.addNode("a", "nt:unstructured");
        a.setProperty("p", 1);
        b = testRootNode.addNode("b", "nt:unstructured");
        b.setProperty("p", 1);
        superuser.save();

        principal = NodeImplTest.getReadOnlyPrincipal(getHelper());
        NodeImplTest.changeReadPermission(principal, a, false);
        superuser.save();

        readOnly = getHelper().getReadOnlySession();

        // preliminary tests
        try {
            readOnly.getNodeByIdentifier(a.getIdentifier());
            fail("Access to the node '" + a.getPath() + "' has to be denied.");
        } catch (ItemNotFoundException e) {
            // good acl
        }

        try {
            readOnly.getNodeByIdentifier(b.getIdentifier());
        } catch (ItemNotFoundException e) {
            fail(e.getMessage());
        }

    }

    protected void tearDown() throws Exception {
        readOnly.logout();
        NodeImplTest.changeReadPermission(principal, a, true);
        super.tearDown();
    }

    /**
     * this test is for the DescendantSelfAxisQuery class.
     * 
     * see <a href="https://issues.apache.org/jira/browse/JCR-3001">JCR-3001</a>
     * 
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    public void testDescendantSelfAxisQuery() throws Exception {
        String xpath = "/" + testRootNode.getPath() + "//*";
        checkResult(
                readOnly.getWorkspace().getQueryManager()
                        .createQuery(xpath, Query.XPATH).execute(),
                new Node[] { b });
    }
}
