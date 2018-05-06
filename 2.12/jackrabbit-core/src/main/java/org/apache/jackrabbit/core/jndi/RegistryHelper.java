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
package org.apache.jackrabbit.core.jndi;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import org.apache.jackrabbit.api.JackrabbitRepository;

/**
 * JNDI helper functionality. This class contains static utility
 * methods for binding and unbinding Jackrabbit repositories to and
 * from a JNDI context.
 */
public class RegistryHelper {

    /**
     * hidden constructor
     */
    private RegistryHelper() {
    }

    /**
     * Binds a configured repository to the given JNDI context.
     * This method creates a {@link BindableRepository BindableRepository}
     * instance using the given configuration information, and binds
     * it to the given JNDI context.
     *
     * @param ctx            context where the repository should be registered (i.e. bound)
     * @param name           the name to register the repository with
     * @param configFilePath path to the configuration file of the repository
     * @param repHomeDir     repository home directory
     * @param overwrite      if <code>true</code>, any existing binding with the given
     *                       name will be overwritten; otherwise a <code>NamingException</code> will
     *                       be thrown if the name is already bound
     * @throws RepositoryException if the repository cannot be created
     * @throws NamingException if the repository cannot be registered in JNDI
     */
    public static void registerRepository(Context ctx, String name,
                                          String configFilePath,
                                          String repHomeDir,
                                          boolean overwrite)
            throws NamingException, RepositoryException {
        Reference reference = new Reference(
                Repository.class.getName(),
                BindableRepositoryFactory.class.getName(),
                null); // no classpath defined
        reference.add(new StringRefAddr(
                BindableRepository.CONFIGFILEPATH_ADDRTYPE, configFilePath));
        reference.add(new StringRefAddr(
                BindableRepository.REPHOMEDIR_ADDRTYPE, repHomeDir));

        // always create instance by using BindableRepositoryFactory
        // which maintains an instance cache;
        // see http://issues.apache.org/jira/browse/JCR-411 for details
        Object obj = new BindableRepositoryFactory().getObjectInstance(
                reference, null, null, null);
        if (overwrite) {
            ctx.rebind(name, obj);
        } else {
            ctx.bind(name, obj);
        }
    }

    /**
     * This method shutdowns a {@link BindableRepository BindableRepository}
     * instance using the given configuration information, and unbinds
     * it from the given JNDI context.
     *
     * @param ctx  context where the repository should be unregistered (i.e. unbound)
     * @param name the name of the repository to unregister
     * @throws NamingException on JNDI errors
     */
    public static void unregisterRepository(Context ctx, String name)
            throws NamingException {
        ((JackrabbitRepository) ctx.lookup(name)).shutdown();
        ctx.unbind(name);
    }

}
