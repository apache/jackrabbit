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
package org.apache.jackrabbit.test.search;

import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.*;
import javax.jcr.query.QueryResult;

/**
 * Abstract base class for query test cases.
 */
public class AbstractQueryTest extends AbstractJCRTest {

    /**
     * Overwrites the <code>setUp</code> method from 
     * {@link org.apache.jackrabbit.test.AbstractJCRTest} and adds additional
     * setup code.
     * @throws Exception if an error occurs during setup.
     */
    protected void setUp() throws Exception {
        super.setUp();
        // make sure root node is indexed
        superuser.getRootNode().setProperty("dummy", new String[]{"dummy"});
        superuser.getRootNode().save();
    }

    /**
     * Checks if the <code>result</code> contains a number of <code>hits</code>.
     *
     * @param result the <code>QueryResult</code>.
     * @param hits   the number of expected hits.
     * @throws RepositoryException if an error occurs while iterating over
     *                             the result nodes.
     */
    protected void checkResult(QueryResult result, int hits)
            throws RepositoryException {
        int count = 0;
        log.println("Nodes:");
        for (NodeIterator nodes = result.getNodes(); nodes.hasNext(); count++) {
            Node n = nodes.nextNode();
            log.println(" " + n.getPath());
        }
        if (count == 0) {
            log.println(" NONE");
        }
        assertEquals("Wrong hit count.", hits, count);
    }

    /**
     * Checks if the <code>result</code> contains a number of <code>hits</code>
     * and <code>properties</code>.
     *
     * @param result     the <code>QueryResult</code>.
     * @param hits       the number of expected hits.
     * @param properties the number of expected properties.
     * @throws RepositoryException if an error occurs while iterating over
     *                             the result nodes.
     */
    protected void checkResult(QueryResult result, int hits, int properties)
            throws RepositoryException {
/*
        checkResult(result, hits);
        // now check property count
        int count = 0;
        log.println("Properties:");
        for (PropertyIterator it = result.getProperties(); it.hasNext(); count++) {
            StringBuffer msg = new StringBuffer();
            Property p = it.nextProperty();
            msg.append("  ").append(p.getName()).append(": ");
            Value[] values = null;
            if (p.getDefinition().isMultiple()) {
                values = p.getValues();
            } else {
                values = new Value[]{p.getValue()};
            }
            String sep = "";
            for (int i = 0; i < values.length; i++) {
                msg.append(sep);
                msg.append(values[i].getString());
                sep = " | ";
            }
            log.println(msg);
        }
        if (count == 0) {
            log.println("  NONE");
        }
        assertEquals("Wrong property count.", properties, count);
*/
        //@todo rewrite test case
        fail("need to rewrite test case according to interface change");
    }

}
