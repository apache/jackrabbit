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

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;
import org.apache.jackrabbit.rmi.remote.RemoteRepository;

/**
 * Factory that looks up a remote repository from an RMI registry.
 *
 * @since 1.4
 */
public class RMIRemoteRepositoryFactory
        extends AbstractRemoteRepositoryFactory {

    /**
     * RMI URL of the remote repository.
     */
    private final String url;

    /**
     * Creates a factory for looking up a remote repository from
     * an RMI registry.
     *
     * @param factory local adapter factory
     * @param url RMI URL of the repository
     */
    public RMIRemoteRepositoryFactory(LocalAdapterFactory factory, String url) {
        super(factory);
        this.url = url;
    }

    /**
     * Looks up a remote repository from the RMI registry.
     *
     * @return remote repository reference
     * @throws RepositoryException if the remote repository is not available
     */
    protected RemoteRepository getRemoteRepository()
            throws RepositoryException {
        try {
            return (RemoteRepository) Naming.lookup(url);
        } catch (MalformedURLException e) {
            throw new RepositoryException("Invalid repository URL: " + url, e);
        } catch (NotBoundException e) {
            throw new RepositoryException("Repository not found: " + url, e);
        } catch (ClassCastException e) {
            throw new RepositoryException("Invalid repository: " + url, e);
        } catch (RemoteException e) {
            throw new RepositoryException("Repository access error: " + url, e);
        }
    }

}
