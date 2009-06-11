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
package org.apache.jackrabbit.test.api.query;

import javax.jcr.query.Query;
import javax.jcr.RepositoryException;

/**
 * <code>SetOffsetTest</code> contains test cases for the method Query.setOffset().
 */
public class SetOffsetTest extends AbstractQueryTest {

    public void testSetOffset() throws RepositoryException {
        testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.addNode(nodeName2, testNodeType);
        testRootNode.addNode(nodeName3, testNodeType);
        superuser.save();
        for (int i = 0; i < 5; i++) {
            Query query = qf.createQuery(
                    qf.selector(testNodeType, "s"),
                    qf.descendantNode("s", testRoot),
                    null,
                    null
            );
            query.setOffset(i);
            long expected = Math.max(3 - i, 0);
            assertEquals("Wrong numer of results", expected,
                    getSize(query.execute().getNodes()));
        }
    }
}
