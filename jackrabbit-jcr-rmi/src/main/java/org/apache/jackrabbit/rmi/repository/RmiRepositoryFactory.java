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

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URI;
import java.rmi.Naming;
import java.util.Hashtable;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.naming.InitialContext;

import org.apache.jackrabbit.rmi.client.ClientAdapterFactory;
import org.apache.jackrabbit.rmi.remote.RemoteRepository;

public class RmiRepositoryFactory implements RepositoryFactory {

    private static final String REPOSITORY_URI =
        "org.apache.jackrabbit.repository.uri";

    @SuppressWarnings("unchecked")
    public Repository getRepository(Map parameters) throws RepositoryException {
        if (parameters != null && parameters.containsKey(REPOSITORY_URI)) {
            Object parameter = parameters.get(REPOSITORY_URI);
            try {
                URI uri = new URI(parameter.toString().trim());
                String scheme = uri.getScheme();
                if ("rmi".equalsIgnoreCase(scheme)) {
                    return getRepository((RemoteRepository) Naming.lookup(
                            uri.getSchemeSpecificPart()));
                } else if ("jndi".equalsIgnoreCase(scheme)) {
                    Hashtable environment = new Hashtable(parameters);
                    environment.remove(REPOSITORY_URI);
                    Object value = new InitialContext(environment).lookup(
                            uri.getSchemeSpecificPart());
                    if (value instanceof RemoteRepository) {
                        return getRepository((RemoteRepository) value);
                    } else {
                        return null;
                    }
                } else {
                    InputStream stream = uri.toURL().openStream();
                    try {
                        Object remote =
                            new ObjectInputStream(stream).readObject();
                        if (remote instanceof RemoteRepository) {
                            return getRepository((RemoteRepository) remote);
                        } else {
                            return null;
                        }
                    } finally {
                        stream.close();
                    }
                }
            } catch (Exception e) {
                return null;
            }
        } else {
            return null;
        }
    }

    private Repository getRepository(RemoteRepository remote) {
        return new ClientAdapterFactory().getRepository(remote);
    }

}
