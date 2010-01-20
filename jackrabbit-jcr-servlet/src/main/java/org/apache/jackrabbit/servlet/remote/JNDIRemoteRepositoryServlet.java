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

import java.util.Enumeration;
import java.util.Hashtable;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;

import org.apache.jackrabbit.commons.repository.RepositoryFactory;
import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;
import org.apache.jackrabbit.rmi.remote.RemoteRepository;
import org.apache.jackrabbit.rmi.repository.JNDIRemoteRepositoryFactory;
import org.apache.jackrabbit.servlet.AbstractRepositoryServlet;

/**
 * Servlet that makes a remote repository from JNDI available as an attribute
 * in the servlet context.
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
 *   <dt>location</dt>
 *   <dd>
 *     Location of the remote repository in the JNDI directory.
 *     The default value is
 *     "<code>org/apache/jackrabbit/rmi/remote/RemoteRepository</code>".
 *   </dd>
 *   <dt>*</dt>
 *   <dd>
 *     All other init parameters are used as the JNDI environment when
 *     instantiating {@link InitialContext} for looking up the repository.
 *   </dd>
 * </dl>
 * <p>
 * This servlet can also be mapped to the URL space. See
 * {@link AbstractRepositoryServlet} for the details.
 *
 * @since 1.4
 */
public class JNDIRemoteRepositoryServlet extends RemoteRepositoryServlet {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 9029928193416404478L;

    /**
     * Returns the remote repository in the configured JNDI location.
     *
     * @return repository
     * @throws RepositoryException if the repository could not be accessed
     */
    @Override
    protected Repository getRepository() throws RepositoryException {
        String location =
            "//localhost/" + RemoteRepository.class.getName().replace('.', '/');
        try {
            Hashtable environment = new Hashtable();
            Enumeration names = getInitParameterNames();
            while (names.hasMoreElements()) {
                String name = (String) names.nextElement();
                if (name.equals("location")) {
                    location = getInitParameter(name);
                } else if (!name.equals(Repository.class.getName())
                        && !name.equals(LocalAdapterFactory.class.getName())) {
                    environment.put(name, getInitParameter(name));
                }
            }
            return new JNDIRemoteRepositoryFactory(
                    getLocalAdapterFactory(),
                    new InitialContext(environment), location).getRepository();
        } catch (NamingException e) {
            throw new RepositoryException(
                    "Repository not found: Invalid JNDI context", e);
        }
    }

}
