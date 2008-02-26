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

package org.apache.jackrabbit.core.persistence.ext;

import java.io.File;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.test.JUnitTest;
import org.apache.jackrabbit.test.RepositoryHelper;
import org.apache.jackrabbit.test.config.DynamicRepositoryHelper;
import org.apache.jackrabbit.test.config.PersistenceManagerConf;
import org.apache.jackrabbit.test.config.RepositoryConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ExternalDerbyTest</code> is an abstract base class for jackrabbit
 * persistence manager tests that need to connect to an external derby
 * server for testing connection problems, data loss etc., which can
 * be easily done by manipulating the external process.
 * 
 * This test class does not use {@link #setUp()} to setup the repository, but
 * instead it is required to call {@link #startExternalDerby()} and
 * {@link #startJackrabbitWithExternalDerby()}.
 */
public abstract class ExternalDerbyTest extends JUnitTest {
	
    /**
     * Helper object to access repository transparently
     */
    public static RepositoryHelper helper = new RepositoryHelper();
    
	private static final String REPO_HOME = "target/repository";

    protected final Logger logger = LoggerFactory.getLogger(JUnitTest.class);
    
	protected Process derbyProcess;

	protected void setUp() {
		// do nothing here, ie. don't set up helper with a new repo
	}

	protected void tearDown() {
		killExternalDerby();
	}

	protected void startExternalDerby() throws Exception {
		logger.debug("Starting external derby server...");
		
		ExternalDerbyProcess.setLogger(logger);
		derbyProcess = ExternalDerbyProcess.start();
	
		try {
			int exitCode = derbyProcess.exitValue();
			throw new Exception("Derby server for testing did not start properly, exit code is " + exitCode);
		} catch (IllegalThreadStateException e) {
			// happens when the process is still running, which is good
		}
	}

	protected void killExternalDerby() {
		if (derbyProcess != null) {
			try {
				derbyProcess.exitValue();
				// if there is no exception, derby has quit already
				return;
			} catch (IllegalThreadStateException e) {
				// happens when the process is still running
			}
			
			logger.debug("Killing external derby server...");
			derbyProcess.destroy();
		}
	}
	
	protected RepositoryConf createRepositoryConf() {
		RepositoryConf conf = new RepositoryConf();
		
		// set jdbc urls on PMs for external derby
		// workspaces
		PersistenceManagerConf pmc = conf.getWorkspaceConfTemplate().getPersistenceManagerConf();
		pmc.setParameter("url", "jdbc:derby://localhost/${wsp.home}/version/db/itemState;create=true");
		pmc.setParameter("driver", ExternalDerbyProcess.EXTERNAL_DERBY_JDBC_DRIVER);
		pmc.setParameter("user", ExternalDerbyProcess.EXTERNAL_DERBY_USER);
		pmc.setParameter("password", ExternalDerbyProcess.EXTERNAL_DERBY_PASSWORD);
		// false is the default value anyway, but we want to make sure, the code does not block forever
		pmc.setParameter("blockOnConnectionLoss", "false");
		
		// versioning
		pmc = conf.getVersioningConf().getPersistenceManagerConf();
		pmc.setParameter("url", "jdbc:derby://localhost/${rep.home}/db/itemState;create=true");
		pmc.setParameter("driver", ExternalDerbyProcess.EXTERNAL_DERBY_JDBC_DRIVER);
		pmc.setParameter("user", ExternalDerbyProcess.EXTERNAL_DERBY_USER);
		pmc.setParameter("password", ExternalDerbyProcess.EXTERNAL_DERBY_PASSWORD);
		// false is the default value anyway, but we want to make sure, the code does not block forever
		pmc.setParameter("blockOnConnectionLoss", "false");

		return conf;
	}

	protected void startJackrabbitWithExternalDerby() throws RepositoryException {
		// clean up
		shutdownJackrabbit();
		deleteDirectory(new File(REPO_HOME));
		
		logger.debug("Starting jackrabbit...");
		helper = new DynamicRepositoryHelper(createRepositoryConf(), REPO_HOME);
	}

	protected void shutdownJackrabbit() {
		if (helper != null && helper instanceof DynamicRepositoryHelper) {
			logger.debug("Stopping jackrabbit...");
			((DynamicRepositoryHelper) helper).shutdown();
		}
	}

	protected void jcrWorkA(Session session) throws RepositoryException {
		Node rootNode = session.getRootNode();
		Node node = rootNode.addNode("test");
		node.setProperty("prop", "foobar");
	}

	protected void jcrWorkB(Session session) throws RepositoryException {
		Node rootNode = session.getRootNode();
		Node node = rootNode.addNode("test2");
		node.setProperty("prop", "foobar");
	}

	public static boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}

}
