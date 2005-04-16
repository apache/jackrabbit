/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.trace;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.decorator.DecoratorFactory;
import org.apache.jackrabbit.decorator.RepositoryDecorator;

/**
 * TODO
 */
public class TraceRepositoryDecorator extends RepositoryDecorator implements
        Repository {

    private TraceLogger logger;
    
    public TraceRepositoryDecorator(
            DecoratorFactory factory, Repository repository, TraceLogger log) {
        super(factory, repository);
        this.logger = log;
    }
    
    /**
     * Logs the method call and forwards it to the underlying repository.
     * {@inheritDoc}
     */
    public String getDescriptor(String key) {
        logger.trace("getDescriptor", key);
        return super.getDescriptor(key);
    }
    
    /**
     * Logs the method call and forwards it to the underlying repository.
     * {@inheritDoc}
     */
    public String[] getDescriptorKeys() {
        logger.trace("getDescriptorKeys");
        return super.getDescriptorKeys();
    }
    
    /**
     * Logs the method call and forwards it to the underlying repository.
     * {@inheritDoc}
     */
    public Session login() throws LoginException, NoSuchWorkspaceException,
            RepositoryException {
        logger.trace("login");
        return super.login();
    }
    
    /**
     * Logs the method call and forwards it to the underlying repository.
     * {@inheritDoc}
     */
    public Session login(Credentials credentials, String workspaceName)
            throws LoginException, NoSuchWorkspaceException,
            RepositoryException {
        logger.trace("login", credentials, workspaceName);
        return super.login(credentials, workspaceName);
    }
    
    /**
     * Logs the method call and forwards it to the underlying repository.
     * {@inheritDoc}
     */
    public Session login(Credentials credentials) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        logger.trace("login", credentials);
        return super.login(credentials);
    }
    
    /**
     * Logs the method call and forwards it to the underlying repository.
     * {@inheritDoc}
     */
    public Session login(String workspaceName) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        logger.trace("login", workspaceName);
        return super.login(workspaceName);
    }
    
}
