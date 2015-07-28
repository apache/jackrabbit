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
package org.apache.jackrabbit.core;

import java.io.File;
import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

import javax.jcr.AccessDeniedException;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.security.authentication.AuthContext;
import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Integration tests for the Session implementation in Jackrabbit core.
 */
public class RepositoryImplTest extends AbstractJCRTest {
	
	boolean logoutCalled = false;

    /**
     * Checks if the logout method is called after garbage collection if the connection was not cleanly closed.
     * 
     * @throws RepositoryException error creating session
     * @throws LoginException wrong credentials
     * @throws InterruptedException got interrupt
     * @throws IOException unable to create repository directory
     */
    public void testLogoutAfterGC() throws LoginException, RepositoryException, InterruptedException, IOException {
    	// setup session
    	File dir = new File("target", "RepositoryImplTest");
    	if (dir.exists()) {
    		FileUtils.deleteDirectory(dir);
    	}
    	RepositoryConfig conf = RepositoryConfig.install(dir);
    	RepositoryImpl repo = new RepositoryImplLogoutCheck(conf);
    	SimpleCredentials credentials =
                new SimpleCredentials("admin", "admin".toCharArray());
            credentials.setAttribute("test", "attribute");
        Session session = repo.login(credentials);
        Assert.assertTrue(session.isLive());
        // remove reference to allow garbage collection
        session = null;
        repo = null;
        conf = null;
        credentials = null;
        // run garbage collection
        final long time = 1000;
        final long cycles = 60;
        for (int i = 0; i < cycles; i++) {
            System.gc();
            if (logoutCalled) {
            	break;
            }
            Thread.sleep(time);        	
        }
        // verify that logout was called after garbage collection
        Assert.assertTrue(logoutCalled);
    }
    
    /**
     * Checks if the logout method is not called after garbage collection if the connection was cleanly closed.
     * 
     * @throws RepositoryException error creating session
     * @throws LoginException wrong credentials
     * @throws InterruptedException got interrupt
     * @throws IOException unable to create repository directory
     */
    @SuppressWarnings("unchecked")
	public void testNoDoubleLogoutAfterGC() throws LoginException, RepositoryException, InterruptedException, IOException {
    	// setup session
    	File dir = new File("target", "RepositoryImplTest2");
    	if (dir.exists()) {
    		FileUtils.deleteDirectory(dir);
    	}
    	RepositoryConfig conf = RepositoryConfig.install(dir);
    	RepositoryImplLogoutCheck repo = new RepositoryImplLogoutCheck(conf);
    	SimpleCredentials credentials =
                new SimpleCredentials("admin", "admin".toCharArray());
            credentials.setAttribute("test", "attribute");
        Session session = repo.login(credentials);
        Assert.assertTrue(session.isLive());
        ReferenceQueue<Session> queue = new ReferenceQueue<Session>();
        PhantomReference<Session> ref = new PhantomReference<Session>(session, queue);
        // logout
        session.logout();
        // remove reference to allow garbage collection
        session = null;
        conf = null;
        credentials = null;
        // run garbage collection
        final long time = 1000;
        final long cycles = 60;
        boolean collected = false;
        for (int i = 0; i < cycles; i++) {
            System.gc();
            boolean foundRef = false;
            PhantomReference<Session> r = (PhantomReference<Session>) queue.poll();
            while (r != null) {
            	if (r == ref) {
            		collected = true;
            		r.clear();
            		foundRef = true;
            		repo.cleanupPhantomSessions();
            		break;
            	}
            	r = (PhantomReference<Session>) queue.poll();
            }
        	if (foundRef) {
        		break;
        	}
            Thread.sleep(time);        	
        }
        // verify that logout was called after garbage collection
        Assert.assertTrue(collected);
        Assert.assertFalse(logoutCalled);
    }
    
    
    /**
     * Test class to override and track logout method.
     * 
     * @author Roland Gruber
     */
    private class XASessionImplLogoutCheck extends XASessionImpl {
    	
		protected XASessionImplLogoutCheck(RepositoryContext repositoryContext,
				Subject subject, WorkspaceConfig wspConfig)
				throws AccessDeniedException, RepositoryException {
			super(repositoryContext, subject, wspConfig);
		}

		public XASessionImplLogoutCheck(RepositoryContext context,
				AuthContext loginContext, WorkspaceConfig wspConfig) throws AccessDeniedException, RepositoryException {
			super(context, loginContext, wspConfig);
		}

		@Override
		public void cleanup() {
			super.cleanup();
			// notify test about successful logout
			logoutCalled = true;
		}

    }
    
    /**
     * Test class to create sessions of type SessionImplLogoutCheck.
     * 
     * @author Roland Gruber
     */
    private class RepositoryImplLogoutCheck extends RepositoryImpl {

		protected RepositoryImplLogoutCheck(RepositoryConfig repConfig)
				throws RepositoryException {
			super(repConfig);
			// speed up test
			PHANTOM_PERIOD = 1;
		}

		@Override
		protected SessionImpl createSessionInstance(AuthContext loginContext,
				WorkspaceConfig wspConfig) throws AccessDeniedException,
				RepositoryException {
			// use our test session class
			return new XASessionImplLogoutCheck(context, loginContext, wspConfig);
		}

		@Override
		protected SessionImpl createSessionInstance(Subject subject,
				WorkspaceConfig wspConfig) throws AccessDeniedException,
				RepositoryException {
			// use our test session class
			return new XASessionImplLogoutCheck(context, subject, wspConfig);
		}

    	
    }
    
}
