/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.base;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * Repository base class.
 */
public class BaseRepository implements Repository {

    /** Protected constructor. This class is only useful when extended. */
    protected BaseRepository() {
    }

    /** Not implemented. {@inheritDoc} */
    public String[] getDescriptorKeys() {
        throw new UnsupportedOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public String getDescriptor(String key) {
        throw new UnsupportedOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public Session login(Credentials credentials, String workspaceName)
            throws LoginException, NoSuchWorkspaceException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Implemented by calling
     * <code>login(credentials, null)</code>
     * as suggested by the JCR specification.
     * {@inheritDoc}
     */
    public Session login(Credentials credentials) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        return login(credentials, null);
    }

    /**
     * Implemented by calling
     * <code>login(null, workspaceName)</code>
     * as suggested by the JCR specification.
     * {@inheritDoc}
     */
    public Session login(String workspaceName) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        return login(null, workspaceName);
    }

    /**
     * Implemented by calling
     * <code>login(null, null)</code>
     * as suggested by the JCR specification.
     * {@inheritDoc}
     */
    public Session login() throws LoginException, NoSuchWorkspaceException,
            RepositoryException {
        return login(null, null);
    }

}
