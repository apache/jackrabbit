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
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.apache.jackrabbit.rmi.remote.RemoteRepository;

/**
 * Object factory for JCR-RMI clients. This factory can be used either
 * directly or as a JNDI object factory.
 *
 * @author Jukka Zitting
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
     * repository is looked up from the RMI registry using the given URL and
     * wrapped into a {@link ClientRepository ClientRepository} adapter.
     *
     * @param url the RMI URL of the remote repository
     * @return repository client
     * @throws ClassCastException    if the URL points to an unknown object
     * @throws MalformedURLException if the URL is malformed
     * @throws NotBoundException     if the URL points to nowhere
     * @throws RemoteException       on RMI errors
     */
    public synchronized Repository getRepository(String url) throws
            ClassCastException, MalformedURLException,
            NotBoundException, RemoteException {
        RemoteRepository remote = (RemoteRepository) Naming.lookup(url);
        return factory.getRepository(remote);
    }

    /**
     * Utility method for looking up the URL within the given RefAddr object.
     * Feeds the content of the RefAddr object to
     * {@link #getRepository(String) getRepository(String)} and wraps all
     * errors to {@link NamingException NamingExceptions}.
     * <p>
     * Used by {@link #getObjectInstance(Object, Name, Context, Hashtable) getObjectInstance()}.
     *
     * @param url the URL reference
     * @return repository client
     * @throws NamingException on all errors
     */
    private Repository getRepository(RefAddr url) throws NamingException {
        try {
            return getRepository((String) url.getContent());
        } catch (Exception ex) {
            throw new NamingException(ex.getMessage());
        }
    }

    /**
     * JNDI factory method for creating JCR-RMI clients. Looks up a
     * remote repository using the reference parameter "url" as the RMI URL
     * and returns a client wrapper for the remote repository.
     *
     * @param object      reference parameters
     * @param name        unused
     * @param context     unused
     * @param environment unused
     * @return repository client
     * @throws NamingException on all errors
     */
    public Object getObjectInstance(
            Object object, Name name, Context context, Hashtable environment)
            throws NamingException {
        if (object instanceof Reference) {
            Reference reference = (Reference) object;
            if (Repository.class.getName().equals(reference.getClassName())) {
                RefAddr url = reference.get(URL_PARAMETER);
                if (url != null) {
                    return getRepository(url);
                }
            }
        }
        return null;
    }

}
