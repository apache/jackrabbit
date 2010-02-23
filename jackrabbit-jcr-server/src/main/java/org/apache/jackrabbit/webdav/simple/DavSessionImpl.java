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

import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.jcr.JcrDavSession;

import javax.jcr.Session;
import java.util.HashSet;

/**
 * Simple implementation of the {@link DavSession} interface. Stores
 * lock tokens but does not yet store references.
 */
public class DavSessionImpl extends JcrDavSession {

    /** the lock tokens of this session */
    private final HashSet<String> lockTokens = new HashSet<String>();

    /**
     * Creates a new DavSession based on a jcr session
     * @param session
     */
    public DavSessionImpl(Session session) {
        super(session);
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
     * @see DavSession#addLockToken(String)
     */
    @Override
    public void addLockToken(String token) {
        super.addLockToken(token);
        lockTokens.add(token);
    }

    /**
     * @see DavSession#getLockTokens()
     */
    @Override
    public String[] getLockTokens() {
        return lockTokens.toArray(new String[lockTokens.size()]);
    }

    /**
     * @see DavSession#removeLockToken(String)
     */
    @Override
    public void removeLockToken(String token) {
        super.removeLockToken(token);
        lockTokens.remove(token);
    }
}
