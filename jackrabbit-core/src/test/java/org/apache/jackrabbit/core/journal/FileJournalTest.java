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
package org.apache.jackrabbit.core.journal;

import java.io.File;

import javax.jcr.RepositoryException;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.cluster.ClusterNode;
import org.apache.jackrabbit.core.cluster.SimpleClusterContext;
import org.apache.jackrabbit.core.config.ClusterConfig;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.test.JUnitTest;

/**
 * Test cases for file journal.
 */
public class FileJournalTest extends JUnitTest {

    /**
     * Repository home value.
     */
    private static final String REPOSITORY_HOME = "target/repository_for_test";

    /**
     * Default cluster node id.
     */
    private static final String CLUSTER_NODE_ID = "node";

    /**
     * Default sync delay: 5 seconds.
     */
    private static final long SYNC_DELAY = 5000;

    /**
     * Repository home.
     */
    private File repositoryHome;

    /**
     * Journal directory.
     */
    private File journalDirectory;

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        repositoryHome = new File(REPOSITORY_HOME);
        repositoryHome.mkdirs();
        FileUtils.cleanDirectory(repositoryHome);
        journalDirectory = new File(repositoryHome, "journal");

        super.setUp();
    }

    /**
     * {@inheritDoc}
     */
    protected void tearDown() throws Exception {
        if (repositoryHome != null) {
            FileUtils.deleteDirectory(repositoryHome);
        }
        super.tearDown();
    }

    /**
     * Create a journal with no revision file name. Verify that the journal
     * is created nonetheless, with a revision file in the repository home.
     *
     * @throws Exception
     * @see <a href="http://issues.apache.org/jira/browse/JCR-904">JCR-904</a>
     */
    public void testRevisionIsOptional() throws Exception {
        final FileJournal journal = new FileJournal();
        journal.setDirectory(journalDirectory.getPath());
        JournalFactory jf = new JournalFactory() {
            public Journal getJournal(NamespaceResolver resolver) {
                return journal;
            }
        };
        ClusterConfig cc = new ClusterConfig(CLUSTER_NODE_ID, SYNC_DELAY, jf);
        SimpleClusterContext context = new SimpleClusterContext(cc, repositoryHome);

        journal.setRepositoryHome(repositoryHome);
        journal.init(CLUSTER_NODE_ID, context.getNamespaceResolver());

        ClusterNode clusterNode = new ClusterNode();
        clusterNode.init(context);

        try {
            File revisionFile =
                new File(repositoryHome, FileJournal.DEFAULT_INSTANCE_FILE_NAME);
            assertTrue(revisionFile.exists());
        } finally {
            clusterNode.stop();
        }
    }

    /**
     * Verify that <code>ClusterNode.stop</code> can be invoked even when
     * <code>ClusterNode.init</code> throws because of a bad journal class.
     *
     * @throws Exception
     */
    public void testClusterInitIncompleteBadJournalClass() throws Exception {
        JournalFactory jf = new JournalFactory() {
            public Journal getJournal(NamespaceResolver resolver)
                    throws RepositoryException {
                throw new RepositoryException("Journal not available");
            }
        };
        ClusterConfig cc = new ClusterConfig(CLUSTER_NODE_ID, SYNC_DELAY, jf);
        SimpleClusterContext context = new SimpleClusterContext(cc);

        ClusterNode clusterNode = new ClusterNode();

        try {
            clusterNode.init(context);
            fail("Bad cluster configuration.");
        } catch (Exception e) {
        }

        clusterNode.stop();
    }

    /**
     * Verify that <code>ClusterNode.stop</code> can be invoked even when
     * <code>ClusterNode.init</code> throws because the journal can not
     * be initialized. Note: this is done by omitting the required argument
     * <code>directory</code>.
     *
     * @throws Exception
     */
    public void testClusterInitIncompleteMissingParam() throws Exception {
        JournalFactory jf = new JournalFactory() {
            public Journal getJournal(NamespaceResolver resolver)
                    throws RepositoryException {
                try {
                    FileJournal journal = new FileJournal();
                    // no setDirectory() call here
                    journal.init(CLUSTER_NODE_ID, resolver);
                    return journal;
                } catch (JournalException e) {
                    throw new RepositoryException("Expected failure", e);
                }
            }
        };
        ClusterConfig cc = new ClusterConfig(CLUSTER_NODE_ID, SYNC_DELAY, jf);
        SimpleClusterContext context = new SimpleClusterContext(cc);

        ClusterNode clusterNode = new ClusterNode();
        try {
            clusterNode.init(context);
            fail("Bad cluster configuration.");
        } catch (Exception e) {
        }

        clusterNode.stop();
    }
}
