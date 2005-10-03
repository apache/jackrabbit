/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import javax.jcr.Session;

import org.apache.jackrabbit.webdav.DavSession;

import java.util.HashSet;

/**
 * Simple implementation of the {@link DavSession} interface. Stores
 * lock tokens but does not yet store references.
 */
public class DavSessionImpl implements DavSession {

    /** the underlaying jcr session */
    private final Session session;

    /** the lock tokens of this session */
    private final HashSet lockTokens = new HashSet();

    /**
     * Creates a new DavSession based on a jcr session
     * @param session
     */
    public DavSessionImpl(Session session) {
        this.session = session;
    }

    /**
     * @see DavSession#addReference(Object)
     */
    public void addReference(Object reference) {
        throw new UnsupportedOperationException("No yet implemented.");
    }

    /**
     * @see DavSession#removeReference(Object)
     */
    public void removeReference(Object reference) {
        throw new UnsupportedOperationException("No yet implemented.");
    }

    /**
     * @see DavSession#getRepositorySession()
     */
    public Session getRepositorySession() {
        return session;
    }

    /**
     * @see DavSession#addLockToken(String)
     */
    public void addLockToken(String token) {
        lockTokens.add(token);
        session.addLockToken(token);
    }

    /**
     * @see DavSession#getLockTokens()
     */
    public String[] getLockTokens() {
        return (String[]) lockTokens.toArray(new String[lockTokens.size()]);
    }

    /**
     * @see DavSession#removeLockToken(String)
     */
    public void removeLockToken(String token) {
        lockTokens.remove(token);
        session.removeLockToken(token);
    }
}
