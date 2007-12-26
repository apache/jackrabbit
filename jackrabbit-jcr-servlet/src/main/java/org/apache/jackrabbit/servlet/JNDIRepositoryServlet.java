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
package org.apache.jackrabbit.servlet;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.jcr.Repository;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;

import org.apache.jackrabbit.commons.repository.JNDIRepositoryFactory;
import org.apache.jackrabbit.commons.repository.RepositoryFactory;

/**
 * Servlet that makes a repository from JNDI available as an attribute
 * in the servlet context.
 * <p>
 * The supported initialization parameters of this servlet are:
 * <dl>
 *   <dt>javax.jcr.Repository</dt>
 *   <dd>
 *     Name of the servlet context attribute to put the repository in.
 *     The default value is "<code>javax.jcr.Repository</code>".
 *   </dd>
 *   <dt>location</dt>
 *   <dd>
 *     Location of the repository in the JNDI directory.
 *     The default value is "<code>javax/jcr/Repository</code>".
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
public class JNDIRepositoryServlet extends AbstractRepositoryServlet {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 8952525573562952560L;

    /**
     * Creates and returns a JNDI repository factory based on the configured
     * init parameters.
     *
     * @return JNDI repository factory
     */
    protected RepositoryFactory getRepositoryFactory() throws ServletException {
        try {
            String location = Repository.class.getName().replace('.', '/');
            Hashtable environment = new Hashtable();
            Enumeration names = getInitParameterNames();
            while (names.hasMoreElements()) {
                String name = (String) names.nextElement();
                if (name.equals("location")) {
                    location = getInitParameter(name);
                } else if (!name.equals(Repository.class.getName())) {
                    environment.put(name, getInitParameter(name));
                }
            }
            return new JNDIRepositoryFactory(
                    new InitialContext(environment), location);
        } catch (NamingException e) {
            throw new ServletException(
                    "Repository not found: Invalid JNDI context", e);
        }
    }

}
