/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import javax.naming.NamingException;
import javax.naming.Context;
import javax.jcr.RepositoryException;

/**
 * <code>RegistryHelper</code> ...
 */
public class RegistryHelper {

    /**
     * hidden constructor
     */
    private RegistryHelper() {
    }

    /**
     *
     * @param ctx context where the repository should be registered (i.e. bound)
     * @param name the name to register the repository with
     * @param configFilePath path to the configuration file of the repository
     * @param repHomeDir repository home directory
     * @param overwrite if <code>true</code>, any existing binding with the given
     * name will be overwritten; otherwise a <code>NamingException</code> will
     * be thrown if the name is already bound
     * @throws NamingException
     * @throws RepositoryException
     */
    public static void registerRepository(Context ctx, String name,
                                         String configFilePath,
                                         String repHomeDir,
                                         boolean overwrite)
            throws NamingException, RepositoryException {
        Object obj = BindableRepository.create(configFilePath, repHomeDir);
        if (overwrite) {
            ctx.rebind(name, obj);
        } else {
            ctx.bind(name, obj);
        }
    }

    /**
     *
     * @param ctx context where the repository should be unregistered (i.e. unbound)
     * @param name the name of the repository to unregister
     * @throws NamingException
     */
    public static void unregisterRepository(Context ctx, String name)
            throws NamingException {
        ctx.unbind(name);
    }
}
