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
package org.apache.jackrabbit.base;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Repository base class. The dummy repository implemented by this class
 * contains no descriptors and forwards all login requests to the
 * canonical {@link #login(Credentials, String)} method.
 */
public class BaseRepository implements Repository {

    /**
     * Returns an empty to indicate that no descriptor keys are available.
     * Subclasses should override this method to return the available
     * descriptor keys.
     *
     * @return empty array
     * @see Repository#getDescriptorKeys()
     */
    public String[] getDescriptorKeys() {
        return new String[0];
    }

    /**
     * Returns <code>null</code> to indicate that the requested descriptor
     * does not exist. Subclasses should override this method to return the
     * actual repository descriptor values.
     *
     * @param key descriptor key
     * @return always <code>null</code>
     * @see Repository#getDescriptor(String)
     */
    public String getDescriptor(String key) {
        return null;
    }

    /**
     * Throws a {@link LoginException} to indicate that logins are
     * not available. Subclasses should override this method to allow
     * repository logins. Note that by default the other login methods
     * invoke this method, so all the login methods can be made to work
     * by overriding just this method. 
     *
     * @param credentials login credentials
     * @param workspaceName workspace name
     * @return nothing (throws a LoginException)
     * @see Repository#login(Credentials, String)
     */
    public Session login(Credentials credentials, String workspaceName)
            throws RepositoryException {
        throw new LoginException();
    }

    /**
     * Implemented by calling {@link #login(Credentials, String)} with a
     * <code>null</code> workspace name. This default implementation
     * follows the JCR specification, so there should be little
     * reason for subclasses to override this method.
     *
     * @param credentials login credentials
     * @return repository session
     * @see Repository#login(Credentials)
     */
    public Session login(Credentials credentials) throws RepositoryException {
        return login(credentials, null);
    }

    /**
     * Implemented by calling {@link #login(Credentials, String)} with
     * <code>null</code> credentials. This default implementation
     * follows the JCR specification, so there should be little
     * reason for subclasses to override this method.
     *
     * @param workspaceName workspace name
     * @return repository session
     * @see Repository#login(String)
     */
    public Session login(String workspaceName) throws RepositoryException {
        return login(null, workspaceName);
    }

    /**
     * Implemented by calling {@link #login(Credentials, String)} with a
     * <code>null</code> workspace name and <code>null</code> credentials.
     * This default implementation follows the JCR specification, so there
     * should be little reason for subclasses to override this method.
     *
     * @return repository session
     * @see Repository#login()
     */
    public Session login() throws RepositoryException {
        return login(null, null);
    }

}
