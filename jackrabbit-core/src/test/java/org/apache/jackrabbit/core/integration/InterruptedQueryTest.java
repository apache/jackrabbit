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
package org.apache.jackrabbit.core.integration;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.lucene.util.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @see <a href="https://issues.apache.org/jira/browse/JCR-3469">JCR-3469</a>
 */
public class InterruptedQueryTest {

    private RepositoryImpl repo;

    private Session session;
    
    @Before
    public void setUp() throws Exception {
        if (Constants.WINDOWS) {
            return;
        }
        deleteAll();
        FileUtils.copyInputStreamToFile(
                getClass().getResourceAsStream("repository-with-SimpleFSDirectory.xml"),
                new File(getTestDir(), "repository.xml"));
        repo = RepositoryImpl.create(RepositoryConfig.create(getTestDir()));
        session = repo.login(new SimpleCredentials("admin", "admin".toCharArray()));
    }

    @After
    public void tearDown() throws Exception {
        if (session != null) {
            session.logout();
        }
        if (repo != null) {
            repo.shutdown();
        }
        deleteAll();
    }

    @Test
    public void testQuery() throws Exception {
        if (Constants.WINDOWS) {
            return;
        }
        for (int i = 0; i < 100; i++) {
            session.getRootNode().addNode("node" + i, "nt:unstructured");
        }
        session.save();
        final QueryManager qm = session.getWorkspace().getQueryManager();
        final AtomicBoolean stop = new AtomicBoolean(false);
        final List<Exception> exceptions = Collections.synchronizedList(
                new ArrayList<Exception>());
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!stop.get() && exceptions.isEmpty()) {
                    try {
                        // execute query
                        String stmt = "//*[@jcr:primaryType='nt:unstructured']";
                        qm.createQuery(stmt, Query.XPATH).execute();
                    } catch (RepositoryException e) {
                        if (Constants.SUN_OS) {
                            // on Solaris it's OK when the root cause
                            // of the exception is an InterruptedIOException
                            // the underlying file is not closed
                            Throwable t = e;
                            while (t.getCause() != null) {
                                t = t.getCause();
                            }
                            if (!(t instanceof InterruptedIOException)) {
                                exceptions.add(e);
                            }
                        } else {
                            exceptions.add(e);
                        }
                    }
                }
            }
        });
        t.start();
        for (int i = 0; i < 200 && t.isAlive(); i++) {
            t.interrupt();
            Thread.sleep((long) (100.0 * Math.random())); 
        }
        stop.set(true);
        t.join();
        if (!exceptions.isEmpty()) {
            throw exceptions.get(0);
        }
    }

    private static void deleteAll() throws IOException {
        FileUtils.deleteDirectory(getTestDir());
    }

    private static File getTestDir() throws IOException {
        return new File("target",
                InterruptedQueryTest.class.getSimpleName());
    }
}
