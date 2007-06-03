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
package org.apache.jackrabbit.rmi.repository;

import javax.jcr.RepositoryException;
import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;
import org.apache.jackrabbit.rmi.remote.RemoteRepository;

/**
 * Factory that looks up a remote repository from JNDI.
 *
 * @since 1.4
 */
public class JNDIRemoteRepositoryFactory
        extends AbstractRemoteRepositoryFactory {

    /**
     * JNDI context of the remote repository.
     */
    private final Context context;

    /**
     * JNDI location of the remote repository.
     */
    private final String location;

    /**
     * Creates a factory for looking up a remote repository from JNDI.
     *
     * @param factory local adapter factory
     * @param context JNDI context
     * @param location JNDI location
     */
    public JNDIRemoteRepositoryFactory(
            LocalAdapterFactory factory, Context context, String location) {
        super(factory);
        this.context = context;
        this.location = location;
    }

    /**
     * Looks up a remote repository from JNDI.
     *
     * @return remote repository reference
     * @throws RepositoryException if the remote repository is not available
     */
    protected RemoteRepository getRemoteRepository()
            throws RepositoryException {
        try {
            Object remote = context.lookup(location);
            if (remote instanceof RemoteRepository) {
                return (RemoteRepository) remote;
            } else if (remote == null) {
                throw new RepositoryException(
                        "Remote repository not found: The JNDI entry "
                        + location + " is null");
            } else {
                throw new RepositoryException(
                        "Invalid remote repository: The JNDI entry "
                        + location + " is an instance of "
                        + remote.getClass().getName());
            }
        } catch (NamingException e) {
            throw new RepositoryException(
                    "Remote repository not found: The JNDI entry " + location
                    + " could not be looked up", e);
        }
    }

}
