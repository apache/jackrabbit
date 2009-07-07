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
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

/**
 * <code>QueryResultTest</code> tests various methods on the
 * <code>NodeIterator</code> returned by a <code>QueryResult</code>.
 */
public class ParentNodeTest extends AbstractQueryTest {

    protected void setUp() throws Exception {
        super.setUp();
        
        // creates the following test structure:
        // + base (foo1=bar1)
        //    + child (foo2=bar2)
        //    + child2 (foo2=bar2)
        //    + child3 (foo2=bar2)
        Node base = testRootNode.addNode("base");
        base.setProperty("foo1", "bar1");

        base.addNode("child").setProperty("foo2", "bar2");
        base.addNode("child2").setProperty("foo2", "bar2");
        base.addNode("child3").setProperty("foo2", "bar2");
        
        superuser.save();
    }

    public void testParentInPath() throws RepositoryException {
        String stmt = testPath + "//child/..[@foo1]";
        QueryResult result = qm.createQuery(stmt, Query.XPATH).execute();
        assertEquals("Wrong size of NodeIterator in result",
                1, result.getNodes().getSize());

        assertEquals("base", result.getNodes().nextNode().getName());
    }

    public void testParentInAttribute1() throws RepositoryException {
        String stmt = testPath + "//child[../@foo1]";
        QueryResult result = qm.createQuery(stmt, Query.XPATH).execute();
        assertTrue("Wrong size of NodeIterator in result", result.getNodes().getSize() > 0);

        assertEquals("child", result.getNodes().nextNode().getName());
    }
    
    public void testParentInAttribute2() throws RepositoryException {
        String stmt = testPath + "//child[../child/@foo2]";
        QueryResult result = qm.createQuery(stmt, Query.XPATH).execute();
        assertTrue("Wrong size of NodeIterator in result", result.getNodes().getSize() > 0);

        assertEquals("child", result.getNodes().nextNode().getName());
    }

    public void testParentInAttribute3() throws RepositoryException {
        String stmt = testPath + "//child[../../base/@foo1]";
        QueryResult result = qm.createQuery(stmt, Query.XPATH).execute();
        assertTrue("Wrong size of NodeIterator in result", result.getNodes().getSize() > 0);
        
        assertEquals("child", result.getNodes().nextNode().getName());
    }

    public void testParentInAttribute4() throws RepositoryException {
        String stmt = testPath + "//child[../@foo1 = 'bar1']";
        QueryResult result = qm.createQuery(stmt, Query.XPATH).execute();
        assertTrue("Wrong size of NodeIterator in result", result.getNodes().getSize() > 0);

        assertEquals("child", result.getNodes().nextNode().getName());
    }

    public void testParentAttributeWithDescendant() throws RepositoryException {
        String stmt = testPath + "//base[../base/@foo1 = 'bar1']/child";
        QueryResult result = qm.createQuery(stmt, Query.XPATH).execute();
        assertTrue("Wrong size of NodeIterator in result", result.getNodes().getSize() > 0);

        assertEquals("child", result.getNodes().nextNode().getName());
    }
    
    public void testParentWithAnd() throws RepositoryException {
        String stmt = testPath + "//child[../@foo1 = 'bar1 and @foo2']";
        QueryResult result = qm.createQuery(stmt, Query.XPATH).execute();
        assertEquals("Wrong size of NodeIterator in result", 0, result.getNodes().getSize());
    }
}
