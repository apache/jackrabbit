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

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.Session;
import java.io.ByteArrayOutputStream;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Calendar;

/**
 * <code>IndexingAggregateTest</code> checks if the nt:file nt:resource
 * aggregate defined in workspace indexing-test works properly.
 */
public class IndexingAggregateTest extends AbstractQueryTest {

    private Session session;

    private Node testRootNode;

    protected void setUp() throws Exception {
        super.setUp();
        session = helper.getSuperuserSession("indexing-test");
        testRootNode = cleanUpTestRoot(session);
        // overwrite query manager
        qm = session.getWorkspace().getQueryManager();
    }

    protected void tearDown() throws Exception {
        cleanUpTestRoot(session);
        session = null;
        testRootNode = null;
        super.tearDown();
    }

    public void testNtFileAggregate() throws RepositoryException, IOException {
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
}
