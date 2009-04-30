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
package org.apache.jackrabbit.api.jsr283.query.qom;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.ValueFactory;
import javax.jcr.Value;
import javax.jcr.query.Query;
import java.util.Calendar;

/**
 * <code>BindVariableValueTest</code>...
 */
public class BindVariableValueTest extends AbstractQOMTest {

    private static final String STRING_VALUE = "JSR";

    private static final long LONG_VALUE = 283;

    private static final double DOUBLE_VALUE = Math.PI;

    private static final boolean BOOLEAN_VALUE = true;

    private static final Calendar DATE_VALUE = Calendar.getInstance();

    private Query query;

    private ValueFactory vf;

    protected void setUp() throws Exception {
        super.setUp();
        vf = superuser.getValueFactory();
        query = qomFactory.createQuery(
                qomFactory.selector(testNodeType, "s"),
                qomFactory.and(
                        qomFactory.childNode("s", testRoot),
                        qomFactory.comparison(
                                qomFactory.propertyValue(propertyName1),
                                OPERATOR_EQUAL_TO,
                                qomFactory.bindVariable("v")
                        )
                ), null, null);
    }

    protected void tearDown() throws Exception {
        vf = null;
        query = null;
        super.tearDown();
    }

    public void testIllegalArgumentException() throws RepositoryException {
        try {
            bindVariableValue(query, "x", vf.createValue(STRING_VALUE));
            fail("Query.bindValue() must throw IllegalArgumentException for unknown variable name");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testString() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.setProperty(propertyName1, STRING_VALUE);
        testRootNode.save();

        bindVariableValue(query, "v", vf.createValue(STRING_VALUE));
        checkResult(query.execute(), new Node[]{n});
    }

    public void testDate() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.setProperty(propertyName1, DATE_VALUE);
        testRootNode.save();

        bindVariableValue(query, "v", vf.createValue(DATE_VALUE));
        checkResult(query.execute(), new Node[]{n});
    }

    public void testLong() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.setProperty(propertyName1, LONG_VALUE);
        testRootNode.save();

        bindVariableValue(query, "v", vf.createValue(LONG_VALUE));
        checkResult(query.execute(), new Node[]{n});
    }

    public void testDouble() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.setProperty(propertyName1, DOUBLE_VALUE);
        testRootNode.save();

        bindVariableValue(query, "v", vf.createValue(DOUBLE_VALUE));
        checkResult(query.execute(), new Node[]{n});
    }

    public void testBoolean() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.setProperty(propertyName1, BOOLEAN_VALUE);
        testRootNode.save();

        bindVariableValue(query, "v", vf.createValue(BOOLEAN_VALUE));
        checkResult(query.execute(), new Node[]{n});
    }

    public void testName() throws RepositoryException {
        Value name = vf.createValue(STRING_VALUE);
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.setProperty(propertyName1, name);
        testRootNode.save();

        bindVariableValue(query, "v", name);
        checkResult(query.execute(), new Node[]{n});
    }

    public void testPath() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        Value path = vf.createValue(n.getPath());
        n.setProperty(propertyName1, path);
        testRootNode.save();

        bindVariableValue(query, "v", path);
        checkResult(query.execute(), new Node[]{n});
    }

    public void testReference() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();

        if (!n.isNodeType(mixReferenceable)) {
            n.addMixin(mixReferenceable);
            n.save();
        }
        n.setProperty(propertyName1, n);
        n.save();


        bindVariableValue(query, "v", vf.createValue(n));
        checkResult(query.execute(), new Node[]{n});
    }

    public void testWeakReference() throws RepositoryException {
        // TODO
    }

    public void testURI() throws RepositoryException {
        // TODO
    }

    public void testDecimal() throws RepositoryException {
        // TODO
    }
}
