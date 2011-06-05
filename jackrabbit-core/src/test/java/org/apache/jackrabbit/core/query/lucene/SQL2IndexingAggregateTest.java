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

import static javax.jcr.query.Query.JCR_SQL2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Calendar;

import javax.jcr.Node;

import org.apache.commons.io.input.NullInputStream;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.core.query.AbstractIndexingTest;

/**
 * <code>IndexingAggregateTest</code> checks if the nt:file nt:resource
 * aggregate defined in workspace indexing-test works properly.
 */
public class SQL2IndexingAggregateTest extends AbstractIndexingTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        for (Node c : JcrUtils.getChildNodes(testRootNode)) {
            testRootNode.getSession().removeItem(c.getPath());
        }
        testRootNode.getSession().save();
    }

    public void testNtFileAggregate() throws Exception {

        String sqlBase = "SELECT * FROM [nt:file] as f"
                + " WHERE ISCHILDNODE([" + testRoot + "])";
        String sqlCat = sqlBase + " AND CONTAINS (f.*, 'cat')";
        String sqlDog = sqlBase + " AND CONTAINS (f.*, 'dog')";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(out, "UTF-8");
        writer.write("the quick brown fox jumps over the lazy dog.");
        writer.flush();

        Node file = testRootNode.addNode("myFile", "nt:file");
        Node resource = file.addNode("jcr:content", "nt:resource");
        resource.setProperty("jcr:lastModified", Calendar.getInstance());
        resource.setProperty("jcr:encoding", "UTF-8");
        resource.setProperty("jcr:mimeType", "text/plain");
        resource.setProperty("jcr:data", session.getValueFactory()
                .createBinary(new ByteArrayInputStream(out.toByteArray())));
        testRootNode.getSession().save();
        executeSQL2Query(sqlDog, new Node[] { file });

        // update jcr:data
        out.reset();
        writer.write("the quick brown fox jumps over the lazy cat.");
        writer.flush();
        resource.setProperty("jcr:data", session.getValueFactory()
                .createBinary(new ByteArrayInputStream(out.toByteArray())));
        testRootNode.getSession().save();
        executeSQL2Query(sqlCat, new Node[] { file });

        // replace jcr:content with unstructured
        resource.remove();
        Node unstrContent = file.addNode("jcr:content", "nt:unstructured");
        Node foo = unstrContent.addNode("foo");
        foo.setProperty("text", "the quick brown fox jumps over the lazy dog.");
        testRootNode.getSession().save();
        executeSQL2Query(sqlDog, new Node[] { file });

        // remove foo
        foo.remove();
        testRootNode.getSession().save();

        executeSQL2Query(sqlDog, new Node[] {});

        // replace jcr:content again with resource
        unstrContent.remove();
        resource = file.addNode("jcr:content", "nt:resource");
        resource.setProperty("jcr:lastModified", Calendar.getInstance());
        resource.setProperty("jcr:encoding", "UTF-8");
        resource.setProperty("jcr:mimeType", "text/plain");
        resource.setProperty("jcr:data", session.getValueFactory()
                .createBinary(new ByteArrayInputStream(out.toByteArray())));
        testRootNode.getSession().save();
        executeSQL2Query(sqlCat, new Node[] { file });

    }

    /**
     * checks that while text extraction runs in the background, the node is
     * available for query on any of its properties
     * 
     * see <a href="https://issues.apache.org/jira/browse/JCR-2980">JCR-2980</a>
     * 
     */
    public void testAsyncIndexQuery() throws Exception {

        Node n = testRootNode.addNode("justnode", JcrConstants.NT_UNSTRUCTURED);
        n.setProperty("type", "testnode");
        n.setProperty("jcr:encoding", "UTF-8");
        n.setProperty("jcr:mimeType", "text/plain");
        n.setProperty(
                "jcr:data",
                session.getValueFactory().createBinary(
                        new NullInputStream(1024 * 40)));
        testRootNode.getSession().save();

        String sql = "SELECT * FROM [nt:unstructured] as f "
                + " WHERE ISCHILDNODE([" + testRoot
                + "]) and type = 'testnode' ";
        checkResult(qm.createQuery(sql, JCR_SQL2).execute(), 1);
    }
}
