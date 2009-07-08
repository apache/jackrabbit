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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.NodeIterator;
import javax.jcr.query.QueryManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.Version;

/**
 * Tests the jcr:deref() function.
 */
public class DerefTest extends AbstractQueryTest {

    /**
     * Test nodes.
     */
    private Node andrew, bill, carl, daren, eric, frank;

    /**
     * Test nodes.
     */
    private Node sun, microsoft, ibm;

    /**
     * Sets up the following structure:
     * <pre>
     *   + people
     *      + andrew (worksfor -> company/sun)
     *         + bill (worksfor -> company/ibm)
     *      + carl (worksfor -> company/microsoft)
     *         + daren (worksfor -> company/ibm)
     *            + eric (worksfor -> company/sun)
     *      + frank (worksfor -> company/microsoft)
     *   + company
     *      + sun (eotm -> andrew)
     *      + microsoft (eotm -> carl)
     *      + ibm (eotm -> daren)
     * </pre>
     */
    protected void setUp() throws Exception {
        super.setUp();

        Node people = testRootNode.addNode("people");
        Node company = testRootNode.addNode("company");

        sun = company.addNode("sun");
        sun.addMixin(mixReferenceable);
        sun.setProperty("ceo", "McNealy");
        microsoft = company.addNode("microsoft");
        microsoft.addMixin(mixReferenceable);
        microsoft.setProperty("ceo", "Ballmer");
        ibm = company.addNode("ibm");
        ibm.addMixin(mixReferenceable);
        ibm.setProperty("ceo", "Palmisano");

        andrew = people.addNode("andrew");
        andrew.addMixin(mixReferenceable);
        andrew.setProperty("worksfor", sun);
        bill = andrew.addNode("bill");
        bill.setProperty("worksfor", ibm);
        carl = people.addNode("carl");
        carl.addMixin(mixReferenceable);
        carl.setProperty("worksfor", microsoft);
        daren = carl.addNode("daren");
        daren.addMixin(mixReferenceable);
        daren.setProperty("worksfor", ibm);
        eric = daren.addNode("eric");
        eric.setProperty("worksfor", sun);
        frank = people.addNode("frank");
        frank.setProperty("worksfor", microsoft);

        // Employees of the month
        sun.setProperty("eotm", andrew);
        microsoft.setProperty("eotm", carl);
        ibm.setProperty("eotm", daren);

        testRootNode.save();
    }

    protected void tearDown() throws Exception {
        andrew = null;
        bill = null;
        carl = null;
        daren = null;
        eric = null;
        frank = null;
        sun = null;
        microsoft = null;
        ibm = null;
        super.tearDown();
    }

    /**
     * Tests various XPath queries with jcr:deref() function.
     */
    public void testDeref() throws RepositoryException {
        executeXPathQuery(testPath + "/people/jcr:deref(@worksfor, '*')",
                new Node[]{});

        executeXPathQuery(testPath + "/people/*/jcr:deref(@worksfor, '*')",
                new Node[]{sun, microsoft});

        executeXPathQuery(testPath + "/people/*/*/jcr:deref(@worksfor, '*')",
                new Node[]{ibm});

        executeXPathQuery(testPath + "/people//jcr:deref(@worksfor, '*')",
                new Node[]{sun, ibm, microsoft});

        executeXPathQuery(testPath + "/people/carl//jcr:deref(@worksfor, '*')",
                new Node[]{sun, ibm});

        executeXPathQuery(testPath + "/people//jcr:deref(@worksfor, 'sun')",
                new Node[]{sun});

        executeXPathQuery(testPath + "/people//jcr:deref(@worksfor, '*')[@ceo = 'McNealy']",
                new Node[]{sun});

        executeXPathQuery(testPath + "/people/*/jcr:deref(@worksfor, '*')[jcr:contains(.,'ballmer')]",
                new Node[]{microsoft});
    }

    public void testDerefInPredicate() throws RepositoryException {
        executeXPathQuery(testPath + "/people//*[jcr:deref(@worksfor, '*')/@ceo='McNealy']",
                new Node[]{andrew, eric});

        executeXPathQuery("//*[people/jcr:deref(@worksfor, '*')/@ceo='McNealy']",
                new Node[]{testRootNode});

//        executeXPathQuery("//*[jcr:contains(people/jcr:deref(@worksfor, '*'),'ballmer')]",
//                new Node[]{testRootNode});
    }

    public void testRewrite() throws RepositoryException {
        executeXPathQuery("//*[people/jcr:deref(@worksfor, '*')/@foo=1]",
                new Node[]{});
    }

    /**
     * Checks if jcr:deref works when dereferencing into the version storage.
     */
    public void testDerefToVersionNode() throws RepositoryException {
        Node referenced = testRootNode.addNode(nodeName1);
        referenced.addMixin(mixVersionable);
        testRootNode.save();

        Version version = referenced.checkin();
        Node referencedVersionNode = version.getNode(jcrFrozenNode);
        Node referencer = testRootNode.addNode(nodeName2);
        referencer.setProperty(propertyName1, referencedVersionNode);
        testRootNode.save();

        String query = "/" + testRoot + "/*[@" + propertyName1 +
                "]/jcr:deref(@" + propertyName1 + ",'*')";
        QueryManager qm = superuser.getWorkspace().getQueryManager();
        Query q = qm.createQuery(query, Query.XPATH);
        QueryResult qr = q.execute();
        NodeIterator ni = qr.getNodes();
        assertEquals("Must find one result in query", 1, ni.getSize());
        while (ni.hasNext()) {
            Node node = (Node) ni.next();
            assertTrue(node.getProperty("jcr:frozenUuid").getString().equals(referenced.getUUID()));
        }
    }

    /**
     * Tests various XPath queries with multiple jcr:deref() function.
     */
    public void testMultipleDeref() throws RepositoryException {
        executeXPathQuery(testPath + "/people/frank/jcr:deref(@worksfor, '*')/jcr:deref(@eotm, '*')",
                new Node[]{carl});
        executeXPathQuery(testPath + "/people/frank/jcr:deref(@worksfor, '*')/jcr:deref(@eotm, '*')[@jcr:uuid]",
                new Node[]{carl});
        executeXPathQuery(testPath + "/people/frank/jcr:deref(@worksfor, '*')[@jcr:uuid]/jcr:deref(@eotm, '*')[@jcr:uuid]",
                new Node[]{carl});
        executeXPathQuery(testPath + "/people/frank/jcr:deref(@worksfor, '*')[@jcr:uuid]/jcr:deref(@eotm, '*')",
                new Node[]{carl});

        executeXPathQuery(testPath + "/people//jcr:deref(@worksfor, '*')/jcr:deref(@eotm, '*')",
                new Node[]{andrew, carl, daren});
        executeXPathQuery(testPath + "/people//jcr:deref(@worksfor, '*')/jcr:deref(@eotm, '*')[@jcr:uuid]",
                new Node[]{andrew, carl, daren});
        executeXPathQuery(testPath + "/people//jcr:deref(@worksfor, '*')[@jcr:uuid]/jcr:deref(@eotm, '*')[@jcr:uuid]",
                new Node[]{andrew, carl, daren});
        executeXPathQuery(testPath + "/people//jcr:deref(@worksfor, '*')[@jcr:uuid]/jcr:deref(@eotm, '*')",
                new Node[]{andrew, carl, daren});
    }
}
