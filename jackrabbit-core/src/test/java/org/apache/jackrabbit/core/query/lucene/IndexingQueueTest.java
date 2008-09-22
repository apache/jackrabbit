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

import org.apache.jackrabbit.extractor.TextExtractor;
import org.apache.jackrabbit.core.query.AbstractIndexingTest;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.Query;
import java.io.Reader;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.util.Calendar;

/**
 * <code>IndexingQueueTest</code> checks if the indexing queue properly indexes
 * nodes in a background thread when text extraction takes more than 100 ms.
 */
public class IndexingQueueTest extends AbstractIndexingTest {

    private static final String CONTENT_TYPE = "application/indexing-queue-test";

    private static final String ENCODING = "UTF-8";

    public void testQueue() throws Exception {
        SearchIndex index = (SearchIndex) getQueryHandler();
        IndexingQueue queue = index.getIndex().getIndexingQueue();

        assertEquals(0, queue.getNumPendingDocuments());

        String text = "the quick brown fox jumps over the lazy dog.";
        InputStream in = new ByteArrayInputStream(text.getBytes(ENCODING));
        Node resource = testRootNode.addNode(nodeName1, "nt:resource");
        resource.setProperty("jcr:data", in);
        resource.setProperty("jcr:lastModified", Calendar.getInstance());
        resource.setProperty("jcr:mimeType", CONTENT_TYPE);
        resource.setProperty("jcr:encoding", ENCODING);
        session.save();

        assertEquals(1, queue.getNumPendingDocuments());

        Query q = qm.createQuery(testPath + "/*[jcr:contains(., 'fox')]", Query.XPATH);
        NodeIterator nodes = q.execute().getNodes();
        assertFalse(nodes.hasNext());

        while (queue.getNumPendingDocuments() > 0) {
            Thread.sleep(50);
        }

        q = qm.createQuery(testPath + "/*[jcr:contains(., 'fox')]", Query.XPATH);
        nodes = q.execute().getNodes();
        assertTrue(nodes.hasNext());
    }

    public static final class Extractor implements TextExtractor {

        public String[] getContentTypes() {
            return new String[]{CONTENT_TYPE};
        }

        public Reader extractText(InputStream stream, String type, String encoding)
        throws IOException {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new IOException();
            }
            return new InputStreamReader(stream, encoding);
        }
    }
}
