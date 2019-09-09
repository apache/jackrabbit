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
package org.apache.jackrabbit.webdav.simple;

import java.io.ByteArrayOutputStream;
import java.io.File;

import javax.jcr.Repository;
import javax.jcr.Session;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.util.Text;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LitmusTest extends TestCase {

    /**
     * Logger instance.
     */
    private static final Logger log = LoggerFactory.getLogger(LitmusTest.class);

    public void testLitmus() throws Exception {
        File dir = new File("target", "litmus");
        String litmus = System.getProperty("litmus", "litmus");

        if (Boolean.getBoolean("jackrabbit.test.integration")
                && isLitmusAvailable(litmus)) {
            final Repository repository = JcrUtils.getRepository(
                    "jcr-jackrabbit://" + Text.escapePath(dir.getCanonicalPath()));
            Session session = repository.login(); // for the TransientRepository
            try {
                Server server = new Server();

                ServerConnector connector = new ServerConnector(server);
                connector.setHost("localhost");
                connector.setPort(Integer.getInteger("litmus.port", 0));

                server.addConnector(connector);

                ServletHolder holder = new ServletHolder(
                        new SimpleWebdavServlet() {
                            @Override
                            public Repository getRepository() {
                                return repository;
                            }
                        });
                holder.setInitParameter("resource-config", "/config.xml");

                ServletContextHandler schandler = new ServletContextHandler(server, "/");
                schandler.addServlet(holder, "/*");

                server.start();
                try {
                    int port = connector.getLocalPort();
                    String url = "http://localhost:" + port + "/default";

                    ProcessBuilder builder =
                        new ProcessBuilder(litmus, url, "admin", "admin");
                    builder.directory(dir);
                    builder.redirectErrorStream();

                    assertLitmus(builder, "basic", 0);

                    assertLitmus(builder, "http", 0);

                    assertLitmus(builder, "props", 0);

                    // FIXME: JCR-2637: WebDAV shallow copy test failure
                    assertLitmus(builder, "copymove", 1);

                    // FIXME: JCR-2638: Litmus locks test failures
                    assertLitmus(builder, "locks", 1);
                } finally {
                    server.stop();
                }
            } finally {
                session.logout();
            }
        }
    }

    private void assertLitmus(
            ProcessBuilder builder, String tests, int exit) throws Exception {
        builder.environment().put("TESTS", tests);
        Process process = builder.start();
        IOUtils.copy(process.getInputStream(), System.out);
        assertEquals(exit, process.waitFor());
    }

    private static boolean isLitmusAvailable(String litmus) {
        try {
            ProcessBuilder builder = new ProcessBuilder(litmus, "--version");
            builder.redirectErrorStream();
            Process process = builder.start();

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            IOUtils.copy(process.getInputStream(), buffer);
            int rv = process.waitFor();
            log.info("litmus version: {}", buffer.toString("US-ASCII").trim());

            return rv == 0;
        } catch (Exception e) {
            log.warn("litmus is not available: " + litmus, e);
            return false;
        }
    }

}
