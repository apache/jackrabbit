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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.TestHelper;
import org.apache.jackrabbit.core.fs.local.FileUtil;
import org.apache.jackrabbit.core.query.AbstractIndexingTest;

/**
 * <code>IndexingQueueTest</code> checks if the indexing queue properly indexes
 * nodes in a background thread when text extraction takes more than 10 ms. See
 * the workspace.xml file for the indexing-test workspace.
 */
public class IndexingQueueTest extends AbstractIndexingTest {

    private static final File TEMP_DIR =
        new File(System.getProperty("java.io.tmpdir"));

    private static final String TESTCONTENT = "<?xml version='1.0'?>\n<blocked>The quick brown fox jumps over the lazy dog.</blocked>";
    
    public void testQueue() throws Exception {
        SearchIndex index = getSearchIndex();
        IndexingQueue queue = index.getIndex().getIndexingQueue();

        BlockingParser.block();
        assertEquals(0, queue.getNumPendingDocuments());

        Node resource = testRootNode.addNode(nodeName1, "nt:resource");
        resource.setProperty("jcr:data", TESTCONTENT, PropertyType.BINARY);
        resource.setProperty("jcr:lastModified", Calendar.getInstance());
        resource.setProperty("jcr:mimeType", BlockingParser.TYPE.toString());
        session.save();

        index.getIndex().getVolatileIndex().commit();
        assertEquals(1, queue.getNumPendingDocuments());

        Query q = qm.createQuery(testPath + "/*[jcr:contains(., 'fox')]", Query.XPATH);
        NodeIterator nodes = q.execute().getNodes();
        assertFalse(nodes.hasNext());

        BlockingParser.unblock();
        waitForTextExtractionTasksToFinish();
        assertEquals(0, queue.getNumPendingDocuments());

        q = qm.createQuery(testPath + "/*[jcr:contains(., 'fox')]", Query.XPATH);
        nodes = q.execute().getNodes();
        assertTrue(nodes.hasNext());
    }

    public void testInitialIndex() throws Exception {
        BlockingParser.block();
        File indexDir = new File(getSearchIndex().getPath());

        // fill workspace
        Node testFolder = testRootNode.addNode("folder", "nt:folder");
        int num = createFiles(testFolder, 10, 2, 0);
        session.save();

        // shutdown workspace
        RepositoryImpl repo = (RepositoryImpl) session.getRepository();
        session.logout();
        session = null;
        superuser.logout();
        superuser = null;
        TestHelper.shutdownWorkspace(getWorkspaceName(), repo);

        // delete index
        try {
            FileUtil.delete(indexDir);
        } catch (IOException e) {
            fail("Unable to delete index directory");
        }

        int initialNumExtractorFiles = getNumExtractorFiles();

        BlockingParser.unblock();
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    session = getHelper().getSuperuserSession(getWorkspaceName());
                } catch (RepositoryException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        t.start();

        while (t.isAlive()) {
            // there must not be more than 20 extractor files, because:
            // - initial index creation checks indexing queue every 10 nodes
            // - there is an aggregate definition on the workspace that causes
            //   2 extractor jobs per nt:resource
            // => 2 * 10 = 20
            int numFiles = getNumExtractorFiles() - initialNumExtractorFiles;
            assertTrue(numFiles <= 20);
            Thread.sleep(50);
        }

        qm = session.getWorkspace().getQueryManager();
        waitForTextExtractionTasksToFinish();

        String stmt = testPath + "//element(*, nt:resource)[jcr:contains(., 'fox')] order by @jcr:score descending";
        Query q = qm.createQuery(stmt, Query.XPATH);
        assertEquals(num, q.execute().getNodes().getSize());
    }

    /*
     * Test case for JCR-2082
     */
    public void testReaderUpToDate() throws Exception {
        BlockingParser.block();
        SearchIndex index = getSearchIndex();
        File indexDir = new File(index.getPath());

        // shutdown workspace
        RepositoryImpl repo = (RepositoryImpl) session.getRepository();
        session.logout();
        session = null;
        superuser.logout();
        superuser = null;
        TestHelper.shutdownWorkspace(getWorkspaceName(), repo);

        // delete index
        try {
            FileUtil.delete(indexDir);
        } catch (IOException e) {
            fail("Unable to delete index directory");
        }

        BlockingParser.unblock();
        // start workspace again by getting a session
        session = getHelper().getSuperuserSession(getWorkspaceName());

        qm = session.getWorkspace().getQueryManager();

        Query q = qm.createQuery(testPath, Query.XPATH);
        assertEquals(1, getSize(q.execute().getNodes()));
    }

    private int createFiles(
            Node folder, int filesPerLevel, int levels, int count)
            throws RepositoryException {
        levels--;
        for (int i = 0; i < filesPerLevel; i++) {
            // create files
            Node file = folder.addNode("file" + i, "nt:file");
            Node resource = file.addNode("jcr:content", "nt:resource");
            resource.setProperty("jcr:data", TESTCONTENT, PropertyType.BINARY);
            resource.setProperty("jcr:lastModified", Calendar.getInstance());
            resource.setProperty("jcr:mimeType", BlockingParser.TYPE.toString());
            count++;
        }
        if (levels > 0) {
            for (int i = 0; i < filesPerLevel; i++) {
                // create files
                Node subFolder = folder.addNode("folder" + i, "nt:folder");
                count = createFiles(subFolder, filesPerLevel, levels, count);
            }
        }
        return count;
    }

    private int getNumExtractorFiles() throws IOException {
        return TEMP_DIR.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("extractor");
            }
        }).length;
    }

}
