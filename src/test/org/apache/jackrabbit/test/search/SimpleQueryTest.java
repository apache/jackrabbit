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

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.util.Calendar;

/**
 * Performs various query test cases.
 *
 * @author Marcel Reutegger
 * @version $Revision: 1.4 $, $Date: 2004/06/28 14:28:15 $
 */
public class SimpleQueryTest extends AbstractQueryTest {


    public void testSimpleQuery1() throws Exception {
	Node foo = testRoot.addNode("foo", NT_UNSTRUCTURED);
	foo.setProperty("bla", new String[] { "bla" });

	testRoot.save();

	String jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "/foo WHERE bla=\"bla\"";
	Query q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	QueryResult result = q.execute();
	checkResult(result, 1);
    }

    public void testSimpleQuery2() throws Exception {
	Node foo = testRoot.addNode("foo", NT_UNSTRUCTURED);
	foo.setProperty("bla", new String[] { "bla" });
	Node bla = superuser.getRootNode().addNode("bla", NT_UNSTRUCTURED);
	bla.setProperty("bla", new String[] { "bla" });

	superuser.getRootNode().save();

	String jcrql = "SELECT * FROM nt:bla LOCATION /" + TEST_ROOT + "// WHERE bla=\"bla\"";
	Query q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	QueryResult result = q.execute();
	checkResult(result, 0);
    }

    public void testSimpleQuery3() throws Exception {
	Node foo = testRoot.addNode("foo", NT_UNSTRUCTURED);
	foo.setProperty("bla", new String[] { "bla" });
	Node bla = testRoot.addNode("bla", NT_UNSTRUCTURED);
	bla.setProperty("bla", new String[] { "bla" });

	testRoot.save();

	String jcrql = "SELECT * FROM nt:unstructured LOCATION /" + TEST_ROOT + "// WHERE bla=\"bla\"";
	Query q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	QueryResult result = q.execute();
	checkResult(result, 2);
    }

    public void testSimpleQuery4() throws Exception {
	Node foo = testRoot.addNode("foo", NT_UNSTRUCTURED);
	foo.setProperty("bla", new String[] { "bla" });
	Node bla = testRoot.addNode("bla", NT_UNSTRUCTURED);
	bla.setProperty("bla", new String[] { "bla" });

	testRoot.save();

	String jcrql = "SELECT * FROM nt:unstructured LOCATION /" + TEST_ROOT + "//";
	Query q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	QueryResult result = q.execute();
	checkResult(result, 2);
    }

    public void testDateField1() throws Exception {
	Node n = testRoot.addNode("marcel", NT_UNSTRUCTURED);
	Calendar marcel = Calendar.getInstance();
	marcel.set(1976, 4, 20, 15, 40);
	n.setProperty("birth", new Value[] { new DateValue(marcel) });

	n = testRoot.addNode("vanessa", NT_UNSTRUCTURED);
	Calendar vanessa = Calendar.getInstance();
	vanessa.set(1975, 4, 10, 13, 30);
	n.setProperty("birth", new Value[] { new DateValue(vanessa) });

	testRoot.save();

	String jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE birth > 1976-01-01T00:00:00.000+01:00";
	Query q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	QueryResult result = q.execute();
	checkResult(result, 1);

	jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE birth > 1975-01-01T00:00:00.000+01:00";
	q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	result = q.execute();
	checkResult(result, 2);
    }

    public void testDoubleField() throws Exception {
	Node n = testRoot.addNode("node1", NT_UNSTRUCTURED);
	n.setProperty("value", new Value[] { new DoubleValue(1.9928375d) });
	n = testRoot.addNode("node2", NT_UNSTRUCTURED);
	n.setProperty("value", new Value[] { new DoubleValue(0.0d) });
	n = testRoot.addNode("node3", NT_UNSTRUCTURED);
	n.setProperty("value", new Value[] { new DoubleValue(-1.42982475d) });

	testRoot.save();

	String jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value > 0.1e-0";
	Query q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	QueryResult result = q.execute();
	checkResult(result, 1);

	jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value > -0.1";
	q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	result = q.execute();
	checkResult(result, 2);

	jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value > -1.5";
	q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	result = q.execute();
	checkResult(result, 3);
    }

    public void testLongField() throws Exception {
	Node n = testRoot.addNode("node1", NT_UNSTRUCTURED);
	n.setProperty("value", new Value[] { new LongValue(1) });
	n = testRoot.addNode("node2", NT_UNSTRUCTURED);
	n.setProperty("value", new Value[] { new LongValue(0) });
	n = testRoot.addNode("node3", NT_UNSTRUCTURED);
	n.setProperty("value", new Value[] { new LongValue(-1) });

	testRoot.save();

	String jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value > 0";
	Query q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	QueryResult result = q.execute();
	checkResult(result, 1);

	jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value > -1";
	q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	result = q.execute();
	checkResult(result, 2);

	jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value > -2";
	q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	result = q.execute();
	checkResult(result, 3);
    }

    public void testLikePattern() throws Exception {
	Node n = testRoot.addNode("node1", NT_UNSTRUCTURED);
	n.setProperty("value", new String[] { "king" });
	n = testRoot.addNode("node2", NT_UNSTRUCTURED);
	n.setProperty("value", new String[] { "ping" });
	n = testRoot.addNode("node3", NT_UNSTRUCTURED);
	n.setProperty("value", new String[] { "ching" });

	testRoot.save();

	String jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value LIKE \"ping\"";
	Query q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	QueryResult result = q.execute();
	checkResult(result, 1);

	jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value LIKE \"?ing\"";
	q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	result = q.execute();
	checkResult(result, 2);

	jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value LIKE \"*ing\"";
	q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	result = q.execute();
	checkResult(result, 3);

	jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value LIKE \"_ing\"";
	q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	result = q.execute();
	checkResult(result, 2);

	jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value LIKE \"%ing\"";
	q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	result = q.execute();
	checkResult(result, 3);
    }

    public void testLikePatternBetween() throws Exception {
	Node n = testRoot.addNode("node1", NT_UNSTRUCTURED);
	n.setProperty("value", new String[] { "ping" });
	n = testRoot.addNode("node2", NT_UNSTRUCTURED);
	n.setProperty("value", new String[] { "pong" });
	n = testRoot.addNode("node3", NT_UNSTRUCTURED);
	n.setProperty("value", new String[] { "puung" });

	testRoot.save();

	String jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value LIKE \"ping\"";
	Query q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	QueryResult result = q.execute();
	checkResult(result, 1);

	jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value LIKE \"p?ng\"";
	q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	result = q.execute();
	checkResult(result, 2);

	jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value LIKE \"p*ng\"";
	q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	result = q.execute();
	checkResult(result, 3);

	jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value LIKE \"p_ng\"";
	q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	result = q.execute();
	checkResult(result, 2);

	jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value LIKE \"p%ng\"";
	q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	result = q.execute();
	checkResult(result, 3);
    }

    public void testLikePatternEnd() throws Exception {
	Node n = testRoot.addNode("node1", NT_UNSTRUCTURED);
	n.setProperty("value", new String[] { "bli" });
	n = testRoot.addNode("node2", NT_UNSTRUCTURED);
	n.setProperty("value", new String[] { "bla" });
	n = testRoot.addNode("node3", NT_UNSTRUCTURED);
	n.setProperty("value", new String[] { "blub" });

	testRoot.save();

	String jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value LIKE \"bli\"";
	Query q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	QueryResult result = q.execute();
	checkResult(result, 1);

	jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value LIKE \"bl?\"";
	q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	result = q.execute();
	checkResult(result, 2);

	jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value LIKE \"bl*\"";
	q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	result = q.execute();
	checkResult(result, 3);

	jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value LIKE \"bl_\"";
	q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	result = q.execute();
	checkResult(result, 2);

	jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value LIKE \"bl%\"";
	q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	result = q.execute();
	checkResult(result, 3);
    }

    public void testLikePatternEscaped() throws Exception {
	Node n = testRoot.addNode("node1", NT_UNSTRUCTURED);
	n.setProperty("value", new String[] { "foo\\?bar" });
	n = testRoot.addNode("node2", NT_UNSTRUCTURED);
	n.setProperty("value", new String[] { "foobar" });
	n = testRoot.addNode("node3", NT_UNSTRUCTURED);
	n.setProperty("value", new String[] { "foo?bar" });
	n = testRoot.addNode("node4", NT_UNSTRUCTURED);
	n.setProperty("value", new String[] { "foolbar" });

	testRoot.save();

	String jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value LIKE \"foo\\?bar\""; // matches node3
	Query q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	QueryResult result = q.execute();
	checkResult(result, 1);

	jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value LIKE \"foo?bar\"";    // matches node3 and node4
	q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	result = q.execute();
	checkResult(result, 2);

	jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value LIKE \"foo*bar\"";  // matches all nodes
	q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	result = q.execute();
	checkResult(result, 4);

	jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value LIKE \"foo\\\\\\?bar\"";  // matches node1
	q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	result = q.execute();
	checkResult(result, 1);

    }

    public void testNotEqual() throws Exception {
	Node n = testRoot.addNode("node1", NT_UNSTRUCTURED);
	n.setProperty("value", new String[] { "foo" });
	n = testRoot.addNode("node2", NT_UNSTRUCTURED);
	n.setProperty("value", new String[] { "bar" });
	n = testRoot.addNode("node3", NT_UNSTRUCTURED);
	n.setProperty("value", new String[] { "foobar" });

	testRoot.save();

	String jcrql = "SELECT * FROM * LOCATION /" + TEST_ROOT + "// WHERE value <> \"bar\"";
	Query q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
	QueryResult result = q.execute();
	checkResult(result, 2);

    }

}
