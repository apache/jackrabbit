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
package org.apache.jackrabbit.test.api.query.qom;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.QueryObjectModel;

/**
 * <code>PropertyValueTest</code> performs a test with property value
 * comparision.
 */
public class PropertyValueTest extends AbstractQOMTest {

    private static final String TEXT = "abc";

    public void testPropertyExistence() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.setProperty(propertyName1, TEXT);
        Node n2 = testRootNode.addNode(nodeName2, testNodeType);
        n2.setProperty(propertyName2, TEXT);
        superuser.save();

        QueryObjectModel qom = qf.createQuery(
                qf.selector(testNodeType, "s"),
                qf.and(
                        qf.childNode("s", testRoot),
                        qf.comparison(
                                qf.propertyValue("s", propertyName1),
                                QueryObjectModelFactory.JCR_OPERATOR_EQUAL_TO,
                                qf.literal(vf.createValue(TEXT))
                        )
                ), null, null);
        checkQOM(qom, new Node[]{n1});

        qom = qf.createQuery(
                qf.selector(testNodeType, "s"),
                qf.and(
                        qf.childNode("s", testRoot),
                        qf.comparison(
                                qf.propertyValue("s", propertyName2),
                                QueryObjectModelFactory.JCR_OPERATOR_EQUAL_TO,
                                qf.literal(vf.createValue(TEXT))
                        )
                ), null, null);
        checkQOM(qom, new Node[]{n2});
    }
}
