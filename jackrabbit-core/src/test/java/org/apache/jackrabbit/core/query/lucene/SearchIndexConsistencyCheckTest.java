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
package org.apache.jackrabbit.core.query.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.core.SearchManager;
import org.apache.jackrabbit.core.TestHelper;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.query.lucene.hits.AbstractHitCollector;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

public class SearchIndexConsistencyCheckTest extends AbstractJCRTest {

    private boolean keepRunning;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testIndexMissesNode() throws Exception {

        Session s = getHelper().getSuperuserSession();
        SearchManager searchManager = TestHelper.getSearchManager(s);
        SearchIndex searchIndex = (SearchIndex) searchManager.getQueryHandler();

        Node foo = testRootNode.addNode("foo");
        testRootNode.getSession().save();
        NodeId fooId = new NodeId(foo.getIdentifier());

        Iterator<NodeId> remove = Collections.singletonList(fooId).iterator();
        Iterator<NodeState> add = Collections.<NodeState>emptyList().iterator();

        searchIndex.updateNodes(remove, add);

        ConsistencyCheck consistencyCheck = searchIndex.runConsistencyCheck();
        List<ConsistencyCheckError> errors = consistencyCheck.getErrors();
        assertEquals("Expected 1 index consistency error", 1, errors.size());

        ConsistencyCheckError error = errors.iterator().next();
        assertEquals("Different node was reported to be missing", error.id, fooId);

        consistencyCheck.repair(false);

        assertTrue("Index was not repaired properly", searchIndexContainsNode(searchIndex, fooId));

        assertTrue("Consistency check still reports errors", searchIndex.runConsistencyCheck().getErrors().isEmpty());
    }

    public void testMissingNodeDoubleCheck() throws Exception {
        Session s = getHelper().getSuperuserSession();
        SearchManager searchManager = TestHelper.getSearchManager(s);
        SearchIndex searchIndex = (SearchIndex) searchManager.getQueryHandler();

        Node foo = testRootNode.addNode("foo");
        testRootNode.getSession().save();
        NodeId fooId = new NodeId(foo.getIdentifier());

        Iterator<NodeId> remove = Collections.singletonList(fooId).iterator();
        Iterator<NodeState> add = Collections.<NodeState>emptyList().iterator();

        searchIndex.updateNodes(remove, add);

        ConsistencyCheck consistencyCheck = searchIndex.runConsistencyCheck();
        List<ConsistencyCheckError> errors = consistencyCheck.getErrors();
        assertEquals("Expected 1 index consistency error", 1, errors.size());

        // now add foo to the index again so that double check finds a false positive
        remove = Collections.<NodeId>emptyList().iterator();
        add = Collections.singletonList(new NodeState(fooId, null, null, 1, false)).iterator();

        searchIndex.updateNodes(remove, add);

        consistencyCheck.doubleCheckErrors();

        assertTrue("Consistency double check of missing node failed", consistencyCheck.getErrors().isEmpty());

    }

    public void testIndexContainsUnknownNode() throws Exception {

        Session s = getHelper().getSuperuserSession();
        SearchManager searchManager = TestHelper.getSearchManager(s);
        SearchIndex searchIndex = (SearchIndex) searchManager.getQueryHandler();

        NodeId nodeId = new NodeId(0, 0);
        NodeState nodeState = new NodeState(nodeId, null, null, 1, false);

        Iterator<NodeId> remove = Collections.<NodeId>emptyList().iterator();
        Iterator<NodeState> add = Collections.singletonList(nodeState).iterator();

        searchIndex.updateNodes(remove, add);

        ConsistencyCheck consistencyCheck = searchIndex.runConsistencyCheck();
        List<ConsistencyCheckError> errors = consistencyCheck.getErrors();
        assertEquals("Expected 1 index consistency error", 1, errors.size());

        ConsistencyCheckError error = errors.iterator().next();
        assertEquals("Different node was reported to be unknown", error.id, nodeId);

        consistencyCheck.repair(false);

        assertFalse("Index was not repaired properly", searchIndexContainsNode(searchIndex, nodeId));

        assertTrue("Consistency check still reports errors", searchIndex.runConsistencyCheck().getErrors().isEmpty());
    }

    public void testUnknownNodeDoubleCheck() throws Exception {

        Session s = getHelper().getSuperuserSession();
        SearchManager searchManager = TestHelper.getSearchManager(s);
        SearchIndex searchIndex = (SearchIndex) searchManager.getQueryHandler();

        NodeId nodeId = new NodeId(0, 0);
        NodeState nodeState = new NodeState(nodeId, null, null, 1, false);

        Iterator<NodeId> remove = Collections.<NodeId>emptyList().iterator();
        Iterator<NodeState> add = Collections.singletonList(nodeState).iterator();

        searchIndex.updateNodes(remove, add);

        ConsistencyCheck consistencyCheck = searchIndex.runConsistencyCheck();
        List<ConsistencyCheckError> errors = consistencyCheck.getErrors();
        assertEquals("Expected 1 index consistency error", 1, errors.size());

        // now remove the unknown node from the index again so that double check finds a false positive
        remove = Collections.singletonList(nodeId).iterator();
        add = Collections.<NodeState>emptyList().iterator();

        searchIndex.updateNodes(remove, add);

        consistencyCheck.doubleCheckErrors();

        assertTrue("Consistency double check of deleted node failed", consistencyCheck.getErrors().isEmpty());
    }

    public void testIndexMissesAncestor() throws Exception {
        Session s = getHelper().getSuperuserSession();
        SearchManager searchManager = TestHelper.getSearchManager(s);
        SearchIndex searchIndex = (SearchIndex) searchManager.getQueryHandler();

        Node foo = testRootNode.addNode("foo");
        Node bar = foo.addNode("bar");
        testRootNode.getSession().save();
        NodeId fooId = new NodeId(foo.getIdentifier());
        NodeId barId = new NodeId(bar.getIdentifier());

        Iterator<NodeId> remove = Collections.singletonList(fooId).iterator();
        Iterator<NodeState> add = Collections.<NodeState>emptyList().iterator();

        searchIndex.updateNodes(remove, add);

        ConsistencyCheck consistencyCheck = searchIndex.runConsistencyCheck();
        List<ConsistencyCheckError> errors = consistencyCheck.getErrors();

        assertEquals("Expected 2 index consistency errors", 2, errors.size());

        assertEquals("Different node was reported to have missing parent", errors.get(0).id, barId);
        assertEquals("Different node was reported to be missing", errors.get(1).id, fooId);

        consistencyCheck.repair(false);

        assertTrue("Index was not repaired properly", searchIndexContainsNode(searchIndex, fooId));

        assertTrue("Consistency check still reports errors", searchIndex.runConsistencyCheck().getErrors().isEmpty());
    }

    public void testMissingAncestorDoubleCheck() throws Exception {

        Session s = getHelper().getSuperuserSession();
        SearchManager searchManager = TestHelper.getSearchManager(s);
        SearchIndex searchIndex = (SearchIndex) searchManager.getQueryHandler();

        Node foo = testRootNode.addNode("foo");
        foo.addNode("bar");
        testRootNode.getSession().save();
        NodeId fooId = new NodeId(foo.getIdentifier());

        Iterator<NodeId> remove = Collections.singletonList(fooId).iterator();
        Iterator<NodeState> add = Collections.<NodeState>emptyList().iterator();

        searchIndex.updateNodes(remove, add);

        ConsistencyCheck consistencyCheck = searchIndex.runConsistencyCheck();
        List<ConsistencyCheckError> errors = consistencyCheck.getErrors();

        assertEquals("Expected 2 index consistency errors", 2, errors.size());

        remove = Collections.<NodeId>emptyList().iterator();
        add = Collections.singletonList(new NodeState(fooId, null, null, 1, true)).iterator();

        searchIndex.updateNodes(remove, add);

        consistencyCheck.doubleCheckErrors();

        assertTrue("Consistency double check of missing ancestor failed", consistencyCheck.getErrors().isEmpty());
    }

    public void testIndexContainsMultipleEntries() throws Exception {
        Session s = getHelper().getSuperuserSession();
        SearchManager searchManager = TestHelper.getSearchManager(s);
        SearchIndex searchIndex = (SearchIndex) searchManager.getQueryHandler();

        Node foo = testRootNode.addNode("foo");
        testRootNode.getSession().save();
        NodeId fooId = new NodeId(foo.getIdentifier());

        NodeState nodeState = new NodeState(fooId, null, null, 1, false);
        Iterator<NodeId> remove = Collections.<NodeId>emptyList().iterator();
        Iterator<NodeState> add = Arrays.asList(nodeState).iterator();

        searchIndex.updateNodes(remove, add);

        searchIndex.flush();

        remove = Collections.<NodeId>emptyList().iterator();
        add = Arrays.asList(nodeState).iterator();

        searchIndex.updateNodes(remove, add);

        ConsistencyCheck consistencyCheck = searchIndex.runConsistencyCheck();
        List<ConsistencyCheckError> errors = consistencyCheck.getErrors();

        assertEquals("Expected 1 index consistency error", 1, errors.size());
        assertEquals("Different node was reported to be duplicate", errors.get(0).id, fooId);

        consistencyCheck.doubleCheckErrors();
        errors = consistencyCheck.getErrors();

        assertEquals("Expected 1 index consistency error after double check", 1, errors.size());
        assertEquals("Different node was reported to be duplicate after double check", errors.get(0).id, fooId);

        consistencyCheck.repair(false);

        assertTrue("Index was not repaired properly", searchIndexContainsNode(searchIndex, fooId));

        consistencyCheck.doubleCheckErrors();
        errors = consistencyCheck.getErrors();

        assertTrue("Consistency double check of multiple entries failed", errors.isEmpty());
        assertTrue("Consistency check still finds errors", searchIndex.runConsistencyCheck().getErrors().isEmpty());

    }

    /**
     * Stress test on the double check mechanism
     */
    public void testDoubleCheckStressTest() throws Exception {
        Thread t = new Thread(new Runnable() {
            private Session s = getHelper().getReadWriteSession();
            @Override
            public void run() {
                while (keepRunning) {
                    try {
                        Node foo = s.getRootNode().getNode(testPath).addNode("foo");
                        s.save();
                        foo.remove();
                        s.save();
                    } catch (RepositoryException e) {
                        System.out.println(e);
                    }
                }
            }
        });

        Session s = getHelper().getSuperuserSession();
        SearchManager searchManager = TestHelper.getSearchManager(s);
        SearchIndex searchIndex = (SearchIndex) searchManager.getQueryHandler();

        keepRunning = true;
        try {
            t.start();
            Thread.sleep(100);
            for (int i = 100; i > 0; i--) {
                final ConsistencyCheck consistencyCheck = searchIndex.runConsistencyCheck();
                consistencyCheck.doubleCheckErrors();
                final List<ConsistencyCheckError> errors = consistencyCheck.getErrors();
                assertTrue(errors.isEmpty());
            }
        } finally {
            keepRunning = false;
        }

    }

    private boolean searchIndexContainsNode(SearchIndex searchIndex, NodeId nodeId) throws IOException {
        final List<Integer> docs = new ArrayList<Integer>(1);
        final IndexReader reader = searchIndex.getIndexReader();
        try {
            IndexSearcher searcher = new IndexSearcher(reader);
            try {
                Query q = new TermQuery(new Term(FieldNames.UUID, nodeId.toString()));
                searcher.search(q, new AbstractHitCollector() {
                    @Override
                    protected void collect(final int doc, final float score) {
                        docs.add(doc);
                    }
                });
            } finally {
                searcher.close();
            }
        } finally {
            Util.closeOrRelease(reader);
        }
        return !docs.isEmpty();

    }

}
