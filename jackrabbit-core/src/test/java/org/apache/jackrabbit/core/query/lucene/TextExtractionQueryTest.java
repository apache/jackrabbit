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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;

import org.apache.jackrabbit.core.query.AbstractIndexingTest;
import org.apache.jackrabbit.core.query.FulltextQueryTest;

public class TextExtractionQueryTest extends AbstractIndexingTest {

    public void testFileContains() throws Exception {
        assertFileContains("test.txt", "text/plain",
                "AE502DBEA2C411DEBD340AD156D89593");
        assertFileContains("test.rtf", "application/rtf", "quick brown fox");
    }

    public void testNtFile() throws RepositoryException, IOException {
        Node file = testRootNode.addNode(nodeName1, "nt:file");
        Node resource = file.addNode("jcr:content", "nt:resource");
        resource.setProperty("jcr:encoding", "UTF-8");
        resource.setProperty("jcr:mimeType", "text/plain");
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(data, "UTF-8");
        writer.write("The quick brown fox jumps over the lazy dog.");
        writer.close();
        resource.setProperty("jcr:data", new ByteArrayInputStream(data.toByteArray()));
        resource.setProperty("jcr:lastModified", Calendar.getInstance());

        testRootNode.save();
        String xpath = testPath + "/*[jcr:contains(jcr:content, 'lazy')]";
        executeXPathQuery(xpath, new Node[]{file});
    }

    private void assertFileContains(String name, String type,
            String... statements) throws Exception {
        while (testRootNode.hasNode(nodeName1)) {
            testRootNode.getNode(nodeName1).remove();
        }
        Node resource = testRootNode.addNode(nodeName1, NodeType.NT_RESOURCE);
        resource.setProperty("jcr:mimeType", type);
        InputStream stream = FulltextQueryTest.class.getResourceAsStream(name);
        try {
            resource.setProperty("jcr:data", stream);
        } finally {
            stream.close();
        }
        testRootNode.save();
        flushSearchIndex();
        for (String statement : statements) {
            assertContainsQuery(statement, true);
        }
    }

    private void assertContainsQuery(String statement, boolean match)
            throws InvalidQueryException, RepositoryException {
        StringBuffer stmt = new StringBuffer();
        stmt.append("/jcr:root").append(testRoot).append("/*");
        stmt.append("[jcr:contains(., '").append(statement);
        stmt.append("')]");

        Query q = qm.createQuery(stmt.toString(), Query.XPATH);
        checkResult(q.execute(), match ? 1 : 0);

        stmt = new StringBuffer();
        stmt.append("SELECT * FROM nt:base ");
        stmt.append("WHERE jcr:path LIKE '").append(testRoot).append("/%' ");
        stmt.append("AND CONTAINS(., '").append(statement).append("')");

        q = qm.createQuery(stmt.toString(), Query.SQL);
        checkResult(q.execute(), match ? 1 : 0);
    }

}
