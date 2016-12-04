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

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.query.Query;

import org.apache.jackrabbit.core.query.AbstractIndexingTest;

import java.io.ByteArrayOutputStream;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;


/**
 * <code>IndexingAggregateTest</code> checks if the nt:file nt:resource
 * aggregate defined in workspace indexing-test works properly.
 */
public class IndexingAggregateTest extends AbstractIndexingTest {

    public void testNtFileAggregate() throws Exception {
        String sqlBase = "SELECT * FROM nt:file"
                + " WHERE jcr:path LIKE '" + testRoot + "/%"
                + "' AND CONTAINS";
        String sqlCat = sqlBase + "(., 'cat')";
        String sqlDog = sqlBase + "(., 'dog')";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(out, "UTF-8");
        writer.write("the quick brown fox jumps over the lazy dog.");
        writer.flush();

        Node file = testRootNode.addNode("myFile", "nt:file");
        Node resource = file.addNode("jcr:content", "nt:resource");
        resource.setProperty("jcr:lastModified", Calendar.getInstance());
        resource.setProperty("jcr:encoding", "UTF-8");
        resource.setProperty("jcr:mimeType", "text/plain");
        resource.setProperty("jcr:data", new ByteArrayInputStream(out.toByteArray()));

        testRootNode.save();

        executeSQLQuery(sqlDog, new Node[]{file});

        // update jcr:data
        out.reset();
        writer.write("the quick brown fox jumps over the lazy cat.");
        writer.flush();
        resource.setProperty("jcr:data", new ByteArrayInputStream(out.toByteArray()));
        testRootNode.save();

        executeSQLQuery(sqlCat, new Node[]{file});

        // replace jcr:content with unstructured
        resource.remove();
        Node unstrContent = file.addNode("jcr:content", "nt:unstructured");
        Node foo = unstrContent.addNode("foo");
        foo.setProperty("text", "the quick brown fox jumps over the lazy dog.");
        testRootNode.save();

        executeSQLQuery(sqlDog, new Node[]{file});

        // remove foo
        foo.remove();
        testRootNode.save();

        executeSQLQuery(sqlDog, new Node[]{});

        // replace jcr:content again with resource
        unstrContent.remove();
        resource = file.addNode("jcr:content", "nt:resource");
        resource.setProperty("jcr:lastModified", Calendar.getInstance());
        resource.setProperty("jcr:encoding", "UTF-8");
        resource.setProperty("jcr:mimeType", "text/plain");
        resource.setProperty("jcr:data", new ByteArrayInputStream(out.toByteArray()));
        testRootNode.save();

        executeSQLQuery(sqlCat, new Node[]{file});
    }

    public void testContentLastModified() throws RepositoryException {
        List expected = new ArrayList();
        long time = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            expected.add(addFile(testRootNode, "file" + i, time));
            time += 1000;
        }
        testRootNode.save();

        String stmt = testPath + "/* order by jcr:content/@jcr:lastModified";
        Query q = qm.createQuery(stmt, Query.XPATH);
        checkResultSequence(q.execute().getRows(), (Node[]) expected.toArray(new Node[expected.size()]));

        // descending
        stmt = testPath + "/* order by jcr:content/@jcr:lastModified descending";
        q = qm.createQuery(stmt, Query.XPATH);
        Collections.reverse(expected);
        checkResultSequence(q.execute().getRows(), (Node[]) expected.toArray(new Node[expected.size()]));

        // reverse order in content
        for (Iterator it = expected.iterator(); it.hasNext(); ) {
            Node file = (Node) it.next();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(time);
            file.getNode("jcr:content").setProperty("jcr:lastModified", cal);
            time -= 1000;
        }
        testRootNode.save();

        stmt = testPath + "/* order by jcr:content/@jcr:lastModified descending";
        q = qm.createQuery(stmt, Query.XPATH);
        checkResultSequence(q.execute().getRows(), (Node[]) expected.toArray(new Node[expected.size()]));
    }

    public void disabled_testPerformance() throws RepositoryException {
        createNodes(testRootNode, 10, 4, 0, new NodeCreationCallback() {
            public void nodeCreated(Node node, int count) throws
                    RepositoryException {
                node.addNode("child").setProperty("property", "value" + count);
                // save once in a while
                if (count % 1000 == 0) {
                    session.save();
                    System.out.println("added " + count + " nodes so far.");
                }
            }
        });
        session.save();

        String xpath = testPath + "//*[child/@property] order by child/@property";
        for (int i = 0; i < 3; i++) {
            long time = System.currentTimeMillis();
            Query query = qm.createQuery(xpath, Query.XPATH);
            query.setLimit(20);
            query.execute().getNodes().getSize();
            time = System.currentTimeMillis() - time;
            System.out.println("executed query in " + time + " ms.");
        }
    }

    private static Node addFile(Node folder, String name, long lastModified)
            throws RepositoryException {
        Node file = folder.addNode(name, "nt:file");
        Node resource = file.addNode("jcr:content", "nt:resource");
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(lastModified);
        resource.setProperty("jcr:lastModified", cal);
        resource.setProperty("jcr:encoding", "UTF-8");
        resource.setProperty("jcr:mimeType", "text/plain");
        resource.setProperty("jcr:data", new ByteArrayInputStream("test".getBytes()));
        return file;
    }

    private int createNodes(Node n, int nodesPerLevel, int levels,
                            int count, NodeCreationCallback callback)
            throws RepositoryException {
        levels--;
        for (int i = 0; i < nodesPerLevel; i++) {
            Node child = n.addNode("node" + i);
            count++;
            callback.nodeCreated(child, count);
            if (levels > 0) {
                count = createNodes(child, nodesPerLevel, levels, count, callback);
            }
        }
        return count;
    }

    private static interface NodeCreationCallback {

        public void nodeCreated(Node node, int count) throws RepositoryException;
    }
}
