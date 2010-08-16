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
package org.apache.jackrabbit.performance;

import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Calendar;

/** <code>AbstractBenchmarkTest</code>... */
abstract class AbstractBenchmarkTest extends AbstractJCRTest {

    protected static final int MEMBERS = 500;

    protected static final int MEMBERSIZE = 1024;

    protected static final String MIMETYPE = "application/octet-stream";

    protected static final int MINTIME = 1000;

    protected static final int MINCOUNT = 5;

    protected void setUp() throws Exception {
        super.setUp();
        Session session = testRootNode.getSession();
        Node folder = null;
        try {
            folder = testRootNode.getNode(getCollectionName());
        }
        catch (RepositoryException ex) {
            // nothing to do
        }

        // delete when needed
        if (folder != null) {
            folder.remove();
            session.save();
        }

        folder = testRootNode.addNode(getCollectionName(), "nt:folder");
        createContent(folder);
    }

    protected void tearDown() throws Exception {
        try {
            Node folder = testRootNode.getNode(getCollectionName());
            folder.remove();
            folder.getSession().save();
        }
        catch (RepositoryException ex) {
            // nothing to do
        }
        super.tearDown();
    }

    protected void createContent(Node folder) throws RepositoryException {
        long cnt = 0;
        while (cnt < MEMBERS) {
            InputStream is = new BufferedInputStream(new ContentGenerator(MEMBERSIZE), MEMBERSIZE);
            Node l_new = folder.addNode("tst" + cnt, "nt:file");
            Node l_cnew = l_new.addNode("jcr:content", "nt:resource");
            l_cnew.setProperty("jcr:data", is);
            l_cnew.setProperty("jcr:mimeType", MIMETYPE);
            l_cnew.setProperty("jcr:lastModified", Calendar.getInstance());
            cnt += 1;
        }
        folder.getSession().save();
    }

    protected abstract String getCollectionName();

    /**
     * Generator for test content of a specific length.
     */
    protected static class ContentGenerator extends InputStream {

        private long length;
        private long position;

        public ContentGenerator(long length) {
            this.length = length;
            this.position = 0;
        }

        public int read() {
            if (this.position++ < this.length) {
                return 0;
            }
            else {
                return -1;
            }
        }
    }
}