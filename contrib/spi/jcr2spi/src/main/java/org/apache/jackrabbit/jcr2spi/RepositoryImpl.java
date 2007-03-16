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
package org.apache.jackrabbit.jcr2spi;

import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.XASessionInfo;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import java.util.Map;

/**
 * <code>RepositoryImpl</code>...
 */
public class RepositoryImpl implements Repository {

    private static Logger log = LoggerFactory.getLogger(RepositoryImpl.class);

    // configuration of the repository
    private final RepositoryConfig config;
    private final Map descriptors;

    private RepositoryImpl(RepositoryConfig config) throws RepositoryException {
        this.config = config;
        descriptors = config.getRepositoryService().getRepositoryDescriptors();
    }

    public static Repository create(RepositoryConfig config) throws RepositoryException {
        return new RepositoryImpl(config);
    }

    //---------------------------------------------------------< Repository >---
    /**
     * @see Repository#getDescriptorKeys()
     */
    public String[] getDescriptorKeys() {
        String[] keys = (String[]) descriptors.keySet().toArray(new String[descriptors.keySet().size()]);
        return keys;
    }

    /**
     * @see Repository#getDescriptor(String)
     */
    public String getDescriptor(String descriptorKey) {
        return (String) descriptors.get(descriptorKey);
    }

    /**
     * @see Repository#login(javax.jcr.Credentials, String)
     */
    public Session login(Credentials credentials, String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        String wspName = (workspaceName == null) ? config.getDefaultWorkspaceName() : workspaceName;
        SessionInfo info = config.getRepositoryService().obtain(credentials, wspName);
        try {
            if (info instanceof XASessionInfo) {
                return new XASessionImpl((XASessionInfo) info, this, config);
            } else {
                return new SessionImpl(info, this, config);
            }
        } catch (RepositoryException ex) {
            config.getRepositoryService().dispose(info);
            throw ex;
        }
    }

    /**
     * @see Repository#login(javax.jcr.Credentials)
     */
    public Session login(Credentials credentials) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login(credentials, null);
    }

    /**
     * @see Repository#login(String)
     */
    public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login(null, workspaceName);
    }

    /**
     * @see Repository#login()
     */
    public Session login() throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login(null, null);
    }
}