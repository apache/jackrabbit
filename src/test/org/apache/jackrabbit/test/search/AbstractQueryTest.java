/*
 * Copyright 2004 The Apache Software Foundation.
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

import org.apache.jackrabbit.test.AbstractTest;

import javax.jcr.query.QueryResult;
import javax.jcr.*;

/**
 * Abstract base class for query test cases.
 *
 * @author Marcel Reutegger
 * @version $Revision:  $, $Date:  $
 */
public class AbstractQueryTest extends AbstractTest {

    /**
     * Checks if the <code>result</code> contains a number of <code>hits</code>.
     * @param result the <code>QueryResult</code>.
     * @param hits the number of expected hits.
     * @throws RepositoryException if an error occurs while iterating over
     *  the result nodes.
     */
    protected void checkResult(QueryResult result, int hits)
	    throws RepositoryException {
	int count = 0;
	log.info("Nodes:");
	for (NodeIterator nodes = result.getNodes(); nodes.hasNext(); count++ ) {
	    Node n = nodes.nextNode();
	    log.info(" " + n.getPath());
	}
	if (count == 0) {
	    log.info(" NONE");
	}
	assertEquals("Wrong hit count.", hits, count);
    }

    /**
     * Checks if the <code>result</code> contains a number of <code>hits</code>
     * and <code>properties</code>.
     * @param result the <code>QueryResult</code>.
     * @param hits the number of expected hits.
     * @param properties the number of expected properties.
     * @throws RepositoryException if an error occurs while iterating over
     *  the result nodes.
     */
    protected void checkResult(QueryResult result, int hits, int properties)
	    throws RepositoryException {
	checkResult(result, hits);
	// now check property count
	int count = 0;
	log.info("Properties:");
	for (PropertyIterator it = result.getProperties(); it.hasNext(); count++ ) {
	    StringBuffer msg = new StringBuffer();
	    Property p = it.nextProperty();
	    msg.append("  ").append(p.getName()).append(": ");
	    Value[] values = null;
	    if (p.getDefinition().isMultiple()) {
		values = p.getValues();
	    } else {
		values = new Value[] { p.getValue() };
	    }
	    String sep = "";
	    for (int i = 0; i < values.length; i++) {
		msg.append(sep);
		msg.append(values[i].getString());
		sep = " | ";
	    }
	    log.info(msg);
	}
	if (count == 0) {
	    log.info("  NONE");
	}
	assertEquals("Wrong property count.", properties, count);
    }

}
