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
package org.apache.jackrabbit.rmi.client;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.commons.repository.RepositoryFactory;

/**
 * Factory that looks up a repository from JNDI.
 *
 * @since 1.4
 */
public class RMIRepositoryFactory implements RepositoryFactory {

    /**
     * Local adapter factory.
     */
    private final LocalAdapterFactory factory;

    /**
     * RMI URL of the repository.
     */
    private final String url;

    /**
     * Creates a factory for looking up a repository from the given RMI URL.
     *
     * @param factory local adapter factory
     * @param url RMI URL of the repository
     */
    public RMIRepositoryFactory(LocalAdapterFactory factory, String url) {
        this.factory = factory;
        this.url = url;
    }

    /**
     * Looks up and returns a repository from the configured RMI URL.
     *
     * @return local adapter for the remote repository
     * @throws RepositoryException if the repository could not be accessed
     */
    public Repository getRepository() throws RepositoryException {
        try {
            return new ClientRepositoryFactory(factory).getRepository(url);
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
