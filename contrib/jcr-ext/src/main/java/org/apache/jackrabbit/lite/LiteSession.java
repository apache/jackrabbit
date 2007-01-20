/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.lite;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.apache.jackrabbit.base.BaseSession;

/**
 * TODO
 */
public class LiteSession extends BaseSession implements Session {

    /** The content repository to which this session belongs. */
    private Repository repository;

    /** The workspace associated with this session. */
    private Workspace workspace;

    private final Map attributes = new HashMap();

    private String userID;

    private boolean live = true;

    /** Lock tokens held by this session. */
    private final Set lockTokens = new HashSet();

    /**
     * Returns the content repository to which this session belongs.
     * Subclasses can use the
     * {@link #setRepository(Repository) setRepository(Repository)} method
     * to set the content repository during session construction.
     *
     * @return content repository
     * @see Session#getRepository()
     */
    public Repository getRepository() {
        return repository;
    }

    /**
     * Sets the content repository to which this session belongs.
     *
     * @param repository content repository
     * @see getRepository()
     */
    protected void setRepository(Repository repository) {
        this.repository = repository;
    }

    /**
     * Returns the workspace associated with this session. Subclasses can use
     * the {@link #setWorkspace(Workspace) setWorkspace(Workspace)} method
     * to set the workspace during session construction.
     *
     * @return workspace
     * @see Session#getWorkspace()
     */
    public Workspace getWorkspace() {
        return workspace;
    }

    /**
     * Sets the workspace associated with this session.
     *
     * @param workspace workspace
     * @see #getWorkspace()
     */
    protected void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    /** {@inheritDoc} */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }
    
    protected void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    /** {@inheritDoc} */
    public String[] getAttributeNames() {
        return (String[])
            attributes.keySet().toArray(new String[attributes.size()]);
    }

    /** {@inheritDoc} */
    public String getUserID() {
        return userID;
    }

    protected void setUserID(String userID) {
        this.userID = userID;
    }

    /**
     * Checks whether the session is still active.
     *
     * @return <code>true</code> if the close() method has <em>not</em>
     *         been called, <code>false</code> if it has
     * @see Session#isLive()
     */
    public boolean isLive() {
        return live;
    }

    /**
     * Marks the session as closed.
     *
     * @see Session#logout()
     */
    public void logout() {
        live = false;
    }

    /**
     * Adds the given lock token.
     *
     * @param lt lock token
     * @see Session#addLockToken(String)
     */
    public void addLockToken(String lt) {
        lockTokens.add(lt);
    }

    /**
     * Returns the lock tokens held by this session.
     *
     * @return lock tokens
     * @see Session#getLockTokens()
     */
    public String[] getLockTokens() {
        return (String[]) lockTokens.toArray(new String[lockTokens.size()]);
    }

    /**
     * Removes the given lock token.
     *
     * @param lt lock token
     * @see Session#removeLockToken(String)
     */
    public void removeLockToken(String lt) {
        lockTokens.remove(lt);
    }

}
