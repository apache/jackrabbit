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
package org.apache.jackrabbit.core.persistence;

import java.io.File;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import junit.framework.TestCase;

import org.apache.derby.drda.NetworkServerControl;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.test.config.PersistenceManagerConf;
import org.apache.jackrabbit.test.config.RepositoryConf;


/**
 * <code>DatabaseConnectionFailureTest</code> tests various situations in
 * which the connection of a persistence manager or file system to its database
 * is lost. If the server restarts properly, Jackrabbit should re-connect
 * automatically and in the best case there is no data lost.
 */
public class DatabaseConnectionFailureTest extends TestCase {

    private static final String DRIVER = "org.apache.derby.jdbc.ClientDriver";

    private static final String USER = "cloud";

    private static final String PASSWORD = "scape";

    private static final int SLEEP = 1000; // ms

    private File directory;

    private NetworkServerControl server;

    private RepositoryImpl repository;

    protected void setUp() throws Exception {
        directory = File.createTempFile("jackrabbit", "test");
        directory.delete();
        directory.mkdir();

        server = new NetworkServerControl();
        startDerby();

        RepositoryConf conf = new RepositoryConf();

        // set jdbc urls on PMs for external derby
        // workspaces
        PersistenceManagerConf pmc = conf.getWorkspaceConfTemplate().getPersistenceManagerConf();
        pmc.setParameter("url", "jdbc:derby://localhost/${wsp.home}/version/db/itemState;create=true");
        pmc.setParameter("driver", DRIVER);
        pmc.setParameter("user", USER);
        pmc.setParameter("password", PASSWORD);
        // false is the default value anyway, but we want to make sure, the code does not block forever
        pmc.setParameter("blockOnConnectionLoss", "false");

        // versioning
        pmc = conf.getVersioningConf().getPersistenceManagerConf();
        pmc.setParameter("url", "jdbc:derby://localhost/${rep.home}/db/itemState;create=true");
        pmc.setParameter("driver", DRIVER);
        pmc.setParameter("user", USER);
        pmc.setParameter("password", PASSWORD);
        // false is the default value anyway, but we want to make sure, the code does not block forever
        pmc.setParameter("blockOnConnectionLoss", "false");

        RepositoryConfig config = conf.createConfig(directory.getPath());
        config.init();
        repository = RepositoryImpl.create(config);
    }

    protected void tearDown() throws Exception {
        repository.shutdown();
        stopDerby();
        delete(directory);
    }

    private void delete(File file) {
        File[] files = file.listFiles();
        for (int i = 0; files != null && i < files.length; i++) {
            delete(files[i]);
        }
        file.delete();
    }

    private void startDerby() throws Exception {
        server.start(null);

        // Make sure that the server has started
        Thread.sleep(SLEEP);
        server.ping();
    }

    private void stopDerby() throws Exception {
        server.shutdown();

        // Make sure that the server has stopped
        Thread.sleep(SLEEP);
    }

    public void testConnectionBrokenAndReconnect() throws Exception {
        Session session = repository.login(
                new SimpleCredentials("admin", "admin".toCharArray()));
        try {
            // do something jcr-like
            session.getRootNode().addNode("test1");
            session.save();

            // RESTART derby
            stopDerby();
            startDerby();

            // do something jcr-like => works again, maybe data corrupted
            session.getRootNode().addNode("test2");
            try {
                // an exception here means that the PM does not properly re-connect to the database
                session.save();
            } catch (RepositoryException e) {
                fail("JCR-940: add db connection autoConnect for BundleDbPersistenceManager");
            }
        } finally {
            session.logout();
        }
    }

    // this test takes about 2 mins and is not very important (it verifies
    // that an RepositoryException is thrown if the database server behind
    // the persistence manager is killed)
/*
        // external derby process
        public void testConnectionBroken() throws Exception {
                startExternalDerby();
                
                startJackrabbitWithExternalDerby();
                Session session = helper.getSuperuserSession();
                
                // do something jcr-like
                jcrWorkA(session);
                session.save();
                
                killExternalDerby();
                
                // do something jcr-like => expect RepositoryException
                jcrWorkB(session);
                
                long start = System.currentTimeMillis();
                try {
                        // with the auto-reconnect feature in Bundle PMs, this save will trigger
                        // a loop of connection trials that will all fail, because we killed
                        // the server. this typically takes about 2 mins before finally a
                        // RepositoryException is thrown.
                        session.save();
                        
                        assertTrue("RepositoryException was expected (waiting some time is normal)", false);
                } catch (RepositoryException e) {
                        // fine, exception is expected
                }
                long end = System.currentTimeMillis();
                logger.debug("time taken: " + (end - start));
        }
*/

        // The following test cases are just ideas for testing an embedded derby
/*      
        // embedded derby
        public void testConnectionClosed() throws Exception {
                // start derby
                // start jackrabbit + derby pm/file system
                // do something jcr-like
                // SHUTDOWN derby
                // do something jcr-like => expect RepositoryException
        }

        // embedded derby
        public void testConnectionClosedAndReconnect() throws Exception {
                // start derby
                // start jackrabbit + derby pm/file system
                // do something jcr-like
                // SHUTDOWN derby
                // RESTART derby
                // do something jcr-like => everything should work normally
        }
*/
}
