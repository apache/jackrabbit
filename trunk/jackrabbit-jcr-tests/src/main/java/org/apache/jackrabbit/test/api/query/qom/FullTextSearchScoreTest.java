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
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.RowIterator;
import javax.jcr.query.Query;

/**
 * <code>FullTextSearchScoreTest</code> contains fulltext search score tests.
 */
public class FullTextSearchScoreTest extends AbstractQOMTest {

    private static final String TEXT = "the quick brown fox jumps over the lazy dog.";

    protected void setUp() throws Exception {
        super.setUp();
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.setProperty(propertyName1, TEXT);
        Node n2 = testRootNode.addNode(nodeName2, testNodeType);
        n2.setProperty(propertyName1, TEXT);
        n2.setProperty(propertyName2, TEXT);
        superuser.save();
    }

    public void testOrdering() throws RepositoryException {
        QueryObjectModel qom = qf.createQuery(
                qf.selector(testNodeType, "s"),
                qf.and(
                        qf.fullTextSearch("s", null, qf.literal(vf.createValue("fox"))),
                        qf.descendantNode("s", testRootNode.getPath())
                ),
                new Ordering[]{qf.ascending(qf.fullTextSearchScore("s"))},
                null
        );
        forQOMandSQL2(qom, new Callable() {
            public Object call(Query query) throws RepositoryException {
                RowIterator rows = query.execute().getRows();
                double previousScore = Double.NaN;
                while (rows.hasNext()) {
                    double score = rows.nextRow().getScore("s");
                    if (!Double.isNaN(previousScore)) {
                        assertTrue("wrong order", previousScore <= score);
                    }
                    previousScore = score;
                }
                return null;
            }
        });
    }

    public void testConstraint() throws RepositoryException {
        QueryObjectModel qom = qf.createQuery(
                qf.selector(testNodeType, "s"),
                qf.and(
                        qf.and(
                                qf.fullTextSearch("s", null, qf.literal(vf.createValue("fox"))),
                                qf.comparison(
                                        qf.fullTextSearchScore("s"),
                                        QueryObjectModelFactory.JCR_OPERATOR_GREATER_THAN,
                                        qf.literal(vf.createValue(Double.MIN_VALUE))
                                )
                        ),
                        qf.descendantNode("s", testRootNode.getPath())
                ),
                new Ordering[]{qf.descending(qf.fullTextSearchScore("s"))},
                null
        );
        forQOMandSQL2(qom, new Callable() {
            public Object call(Query query) throws RepositoryException {
                RowIterator rows = query.execute().getRows();
                while (rows.hasNext()) {
                    double score = rows.nextRow().getScore("s");
                    if (!Double.isNaN(score)) {
                        assertTrue("wrong full text search score", Double.MIN_VALUE < score);
                    }
                }
                return null;
            }
        });
    }
}
