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
package org.apache.jackrabbit.webdav.jcr;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.jcr.lock.LockTokenMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.HashSet;

/**
 * <code>JcrDavSession</code> specific base implementation of the
 * <code>DavSession</code> interface, which simply wraps a {@link Session}
 * object. This implementation adds a utility method that allows to
 * {@link #getRepositorySession() unwrap} the underlying repository session.
 * <br>
 * Note, that in this basic implementation the following methods are simply
 * forwarded to the corresponding call on <code>Session</code>:
 * <ul>
 * <li>{@link #getLockTokens()}         =&gt; {@link Session#getLockTokens()}</li>
 * <li>{@link #addLockToken(String)}    =&gt; {@link Session#addLockToken(String)}</li>
 * <li>{@link #removeLockToken(String)} =&gt; {@link Session#removeLockToken(String)}</li>
 * </ul>
 * Subclasses may overwrite or extend this behaviour.
 */
public abstract class JcrDavSession implements DavSession {

    private static Logger log = LoggerFactory.getLogger(JcrDavSession.class);

    /** the underlying jcr session */
    private final Session session;

    /** the lock tokens of this session */
    private final HashSet<String> lockTokens = new HashSet<String>();

    /**
     *
     * @param session
     */
    protected JcrDavSession(Session session) {
        this.session = session;
    }

    /**
     *
     * @param davSession
     * @throws DavException
     */
    public static void checkImplementation(DavSession davSession) throws DavException {
        if (!(davSession instanceof JcrDavSession)) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, "JCR specific DavSession expected. Found: " + davSession);
        }
    }

    /**
     *
     * @param davSession
     * @return
     * @throws DavException
     */
    public static Session getRepositorySession(DavSession davSession) throws DavException {
        checkImplementation(davSession);
        return ((JcrDavSession)davSession).getRepositorySession();
    }

    /**
     * Unwrap the {@link Session repository session} object.
     *
     * @return the session object wrapped by this <code>DavSession</code>
     */
    public Session getRepositorySession() {
        return session;
    }

    //---------------------------------------------------------< DavSession >---
    /**
     *
     * @param token
     * @see DavSession#addLockToken(String)
     */
    @Override
    public void addLockToken(String token) {
        if (!LockTokenMapper.isForSessionScopedLock(token)) {
            try {
                session.getWorkspace().getLockManager().addLockToken(LockTokenMapper.getJcrLockToken(token));
            }
            catch (RepositoryException ex) {
                log.debug("trying to add lock token " + token + " to session", ex);
            }
        }
        lockTokens.add(token);
    }

    /**
     *
     * @return
     * @see DavSession#getLockTokens()
     */
    @Override
    public String[] getLockTokens() {
        return lockTokens.toArray(new String[lockTokens.size()]);
    }

    /**
     *
     * @param token
     * @see DavSession#removeLockToken(String)
     */
    @Override
    public void removeLockToken(String token) {
        if (!LockTokenMapper.isForSessionScopedLock(token)) {
            try {
                session.getWorkspace().getLockManager().removeLockToken(LockTokenMapper.getJcrLockToken(token));
            }
            catch (RepositoryException ex) {
                log.debug("trying to remove lock token " + token + " to session", ex);
            }
        }
        lockTokens.remove(token);
    }
}
