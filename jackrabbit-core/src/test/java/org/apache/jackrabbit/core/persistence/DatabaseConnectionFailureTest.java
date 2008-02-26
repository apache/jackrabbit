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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.core.persistence.ext.ExternalDerbyTest;

/**
 * <code>DatabaseConnectionFailureTest</code> tests various situations in
 * which the connection of a persistence manager or file system to its database
 * is lost. If the server restarts properly, Jackrabbit should re-connect
 * automatically and in the best case there is no data lost.
 */
public class DatabaseConnectionFailureTest extends ExternalDerbyTest {

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
	
	// external derby process
	public void testConnectionBrokenAndReconnect() throws Exception {
		startExternalDerby();
		
		startJackrabbitWithExternalDerby();
		Session session = helper.getSuperuserSession();
		
		// do something jcr-like
		jcrWorkA(session);
		session.save();
		
		killExternalDerby();
		
		// RESTART derby
		startExternalDerby();
		
		// do something jcr-like => works again, maybe data corrupted
		jcrWorkB(session);
		
		// an exception here means that the PM does not properly re-connect to the database
		session.save();
	}

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
