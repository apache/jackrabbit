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
package org.apache.jackrabbit.servlet.remote;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.client.ClientAdapterFactory;
import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;
import org.apache.jackrabbit.servlet.AbstractRepositoryServlet;

/**
 * Abstract base class for servlets that make a remote repository available
 * locally in the servlet context.
 * <p>
 * The supported initialization parameters of this servlet are:
 * <dl>
 *   <dt>javax.jcr.Repository</dt>
 *   <dd>
 *     Name of the servlet context attribute to put the repository in.
 *     The default value is "<code>javax.jcr.Repository</code>".
 *   </dd>
 *   <dt>org.apache.jackrabbit.rmi.client.LocalAdapterFactory</dt>
 *   <dd>
 *     Name of the local adapter factory class used to create the local
 *     adapter for the remote repository. The configured class should have
 *     public constructor that takes no arguments.
 *   </dd>
 * </dl>
 * <p>
 * This servlet can also be mapped to the URL space. See
 * {@link AbstractRepositoryServlet} for the details.
 *
 * @since 1.4
 */
public abstract class RemoteRepositoryServlet
        extends AbstractRepositoryServlet {

    /**
     * Instantiates and returns the configured local adapter factory.
     *
     * @return local adapter factory
     * @throws RepositoryException if the factory could not be instantiated
     */
    protected LocalAdapterFactory getLocalAdapterFactory()
            throws RepositoryException {
        String name = getInitParameter(
                LocalAdapterFactory.class.getName(),
                ClientAdapterFactory.class.getName());
        try {
            Class factoryClass = Class.forName(name);
            return (LocalAdapterFactory) factoryClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new RepositoryException(
                    "Local adapter factory class not found: " + name, e);
        } catch (InstantiationException e) {
            throw new RepositoryException(
                    "Failed to instantiate the adapter factory: " + name, e);
        } catch (IllegalAccessException e) {
            throw new RepositoryException(
                    "Adapter factory constructor is not public: " + name, e);
        } catch (ClassCastException e) {
            throw new RepositoryException(
                    "Invalid local adapter factory class: " + name, e);
        }
    }

}
