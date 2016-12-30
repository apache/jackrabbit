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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.jackrabbit.rmi.client.ClientAdapterFactory;
import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;

/**
 * Proxy for a remote repository bound in JNDI. The configured repository is
 * looked up from JNDI lazily during each method call. Thus the JNDI entry
 * does not need to exist when this class is instantiated. The JNDI entry
 * can also be replaced with another repository during the lifetime of an
 * instance of this class.
 *
 * @since 1.4
 */
public class JNDIRemoteRepository extends ProxyRepository {

    /**
     * Creates a proxy for a remote repository in JNDI.
     *
     * @param factory local adapter factory
     * @param context JNDI context
     * @param location JNDI location
     */
    public JNDIRemoteRepository(
            LocalAdapterFactory factory, Context context, String location) {
        super(new JNDIRemoteRepositoryFactory(factory, context, location));
    }

    /**
     * Creates a proxy for the remote repository in JNDI.
     * Uses {@link ClientAdapterFactory} as the default
     * local adapter factory.
     *
     * @param context JNDI context
     * @param location JNDI location
     */
    public JNDIRemoteRepository(Context context, String location) {
        this(new ClientAdapterFactory(), context, location);
    }

    /**
     * Creates a proxy for the remote repository in JNDI.
     * Uses {@link ClientAdapterFactory} as the default
     * local adapter factory.
     *
     * @param location JNDI location in default context
     * @throws NamingException if the default JNDI context is not available
     */
    public JNDIRemoteRepository(String location) throws NamingException {
        this(new InitialContext(), location);
    }

}
