/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
     *      + sun
     *      + microsoft
     *      + ibm
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
        andrew.setProperty("worksfor", sun);
        bill = andrew.addNode("bill");
        bill.setProperty("worksfor", ibm);
        carl = people.addNode("carl");
        carl.setProperty("worksfor", microsoft);
        daren = carl.addNode("daren");
        daren.setProperty("worksfor", ibm);
        eric = daren.addNode("eric");
        eric.setProperty("worksfor", sun);
        frank = people.addNode("frank");
        frank.setProperty("worksfor", microsoft);

        testRootNode.save();
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
}
