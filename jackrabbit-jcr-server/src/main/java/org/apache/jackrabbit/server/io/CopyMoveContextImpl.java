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
package org.apache.jackrabbit.server.io;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

/**
 * <code>CopyMoveContextImpl</code>...
 */
public class CopyMoveContextImpl implements CopyMoveContext {

    private final boolean isShallow;
    private final Session session;

    public CopyMoveContextImpl(Session session) {
        this(session, false);
    }

    public CopyMoveContextImpl(Session session, boolean isShallowCopy) {
        this.isShallow = isShallowCopy;
        this.session = session;
    }

    //----------------------------------------------------< CopyMoveContext >---    
    /**
     * @see org.apache.jackrabbit.server.io.CopyMoveContext#isShallowCopy()
     */
    public boolean isShallowCopy() {
        return isShallow;
    }

    /**
     * @see org.apache.jackrabbit.server.io.CopyMoveContext#getSession() 
     */
    public Session getSession() {
        return session;
    }

    /**
     * @see org.apache.jackrabbit.server.io.CopyMoveContext#getWorkspace() 
     */
    public Workspace getWorkspace() throws RepositoryException {
        return session.getWorkspace();
    }
}