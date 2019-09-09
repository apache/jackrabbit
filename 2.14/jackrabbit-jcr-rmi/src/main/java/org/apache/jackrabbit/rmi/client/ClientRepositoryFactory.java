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
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Hashtable;

import javax.jcr.Repository;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.apache.jackrabbit.rmi.remote.RemoteRepository;

/**
 * Object factory for JCR-RMI clients. This factory can be used either
 * directly or as a JNDI object factory.
 *
 * @see ClientRepository
 */
public class ClientRepositoryFactory implements ObjectFactory {

    /**
     * The JNDI parameter name for configuring the RMI URL of
     * a remote repository.
     */
    public static final String URL_PARAMETER = "url";

    /**
     * Local adapter factory.
     */
    private LocalAdapterFactory factory;

    /**
     * Creates a JCR-RMI client factory with the default adapter factory.
     */
    public ClientRepositoryFactory() {
        this(new ClientAdapterFactory());
    }

    /**
     * Creates a JCR-RMI client factory with the given adapter factory.
     *
     * @param factory local adapter factory
     */
    public ClientRepositoryFactory(LocalAdapterFactory factory) {
        this.factory = factory;
    }

    /**
     * Returns a client wrapper for a remote content repository. The remote
     * repository is looked up from the RMI registry using the given URL by
     * the returned {@link SafeClientRepository} instance.
     * <p>
     * The current implementation of this method will not throw any of the
     * declared exceptions (because of the {@link SafeClientRepository} being
     * used), but the throws clauses are kept for backwards compatibility and
     * potential future use. Clients should be prepared to handle exceptions
     * from this method.
     *
     * @param url the RMI URL of the remote repository
     * @return repository client
     * @throws MalformedURLException if the given URL is malfored
     * @throws NotBoundException if the given URL points to nothing
     * @throws ClassCastException if the given URL points to something unknown
     * @throws RemoteException if the remote repository can not be accessed
     */
    public Repository getRepository(final String url)
            throws MalformedURLException, NotBoundException,
            ClassCastException, RemoteException {
        return new SafeClientRepository(factory) {

            protected RemoteRepository getRemoteRepository()
                    throws RemoteException {
                try {
                    return (RemoteRepository) Naming.lookup(url);
                } catch (MalformedURLException e) {
                    throw new RemoteException("Malformed URL: " + url, e);
                } catch (NotBoundException e) {
                    throw new RemoteException("No target found: " + url, e);
                } catch (ClassCastException e) {
                    throw new RemoteException("Unknown target: " + url, e);
                }
            }

        };
    }

    /**
     * JNDI factory method for creating JCR-RMI clients. Creates a lazy
     * client repository instance that uses the reference parameter "url"
     * as the RMI URL where the remote repository is looked up when accessed.
     *
     * @param object      reference parameters
     * @param name        unused
     * @param context     unused
     * @param environment unused
     * @return repository client
     */
    public Object getObjectInstance(
            Object object, Name name, Context context, Hashtable environment) {
        if (object instanceof Reference) {
            Reference reference = (Reference) object;
            RefAddr url = reference.get(URL_PARAMETER);
            if (url != null && url.getContent() != null) {
                try {
                    return getRepository(url.getContent().toString());
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

}
