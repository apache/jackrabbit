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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.jackrabbit.rmi.client.ClientAdapterFactory;
import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;
import org.apache.jackrabbit.rmi.client.SafeClientRepository;
import org.apache.jackrabbit.rmi.remote.RemoteRepository;

public class RmiRepositoryFactory implements RepositoryFactory {

    private static final String REPOSITORY_URI =
        "org.apache.jackrabbit.repository.uri";

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Repository getRepository(Map parameters) throws RepositoryException {
        if (parameters != null && parameters.containsKey(REPOSITORY_URI)) {
            URI uri;
            try {
                uri = new URI(parameters.get(REPOSITORY_URI).toString().trim());
            } catch (URISyntaxException e) {
                return null;
            }

            String scheme = uri.getScheme();
            if ("rmi".equalsIgnoreCase(scheme)) {
                return getRmiRepository(uri.getSchemeSpecificPart());
            } else if ("jndi".equalsIgnoreCase(scheme)) {
                Hashtable environment = new Hashtable(parameters);
                environment.remove(REPOSITORY_URI);
                return getJndiRepository(
                        uri.getSchemeSpecificPart(), environment);
            } else {
                try {
                    return getUrlRepository(uri.toURL());
                } catch (MalformedURLException e) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    private Repository getUrlRepository(final URL url)
            throws RepositoryException {
        return new RmiSafeClientRepository(new ClientAdapterFactory()) {

            @Override
            protected RemoteRepository getRemoteRepository() throws RemoteException {
                try {
                    InputStream stream = url.openStream();
                    try {
                        Object remote = new ObjectInputStream(stream).readObject();
                        if (remote instanceof RemoteRepository) {
                            return (RemoteRepository) remote;
                        } else {
                            throw new RemoteException("The resource at URL " + url
                                    + " is not a remote repository stub: " + remote);
                        }
                    } finally {
                        if (stream != null) {
                            stream.close();
                        }
                    }
                } catch (ClassNotFoundException e) {
                    throw new RemoteException("The resource at URL " + url
                            + " requires a class that is not available", e);
                } catch (IOException e) {
                    throw new RemoteException("Failed to read the resource at URL "
                            + url, e);
                }
            }
        };
    }

    @SuppressWarnings("rawtypes")
    private Repository getJndiRepository(final String name,
            final Hashtable environment) throws RepositoryException {
        return new RmiSafeClientRepository(new ClientAdapterFactory()) {

            @Override
            protected RemoteRepository getRemoteRepository() throws RemoteException {
                try {
                    Object value = new InitialContext(environment).lookup(name);
                    if (value instanceof RemoteRepository) {
                        return (RemoteRepository) value;
                    } else {
                        throw new RemoteException("The JNDI resource " + name
                                + " is not a remote repository stub: " + value);
                    }
                } catch (NamingException e) {
                    throw new RemoteException(
                            "Failed to look up the JNDI resource " + name, e);
                }
            }
        };
    }

    private Repository getRmiRepository(final String name)
            throws RepositoryException {
        return new RmiSafeClientRepository(new ClientAdapterFactory()) {

            @Override
            protected RemoteRepository getRemoteRepository() throws RemoteException {
                try {
                    Object value = Naming.lookup(name);
                    if (value instanceof RemoteRepository) {
                        return (RemoteRepository) value;
                    } else {
                        throw new RemoteException("The RMI resource " + name
                                + " is not a remote repository stub: " + value);
                    }
                } catch (NotBoundException e) {
                    throw new RemoteException(
                            "RMI resource " + name + " not found", e);
                } catch (MalformedURLException e) {
                    throw new RemoteException("Invalid RMI name: " + name, e);
                } catch (RemoteException e) {
                    throw new RemoteException("Failed to look up the RMI resource "
                            + name, e);
                }
            }
        };
    }

    /**
     * Basic SafeClientRepository for the different lookup types of a rmi repository
     */
    private static class RmiSafeClientRepository extends SafeClientRepository {

        public RmiSafeClientRepository(LocalAdapterFactory factory) {
            super(factory);
        }

        @Override
        protected RemoteRepository getRemoteRepository() throws RemoteException {
            return null;
        }

    }
}
