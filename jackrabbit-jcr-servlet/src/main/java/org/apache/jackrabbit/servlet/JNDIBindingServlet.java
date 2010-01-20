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
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 * Servlet that binds a repository from a servlet context attribute in JNDI.
 * <p>
 * The initialization parameters of this servlet are:
 * <dl>
 *   <dt>javax.jcr.Repository</dt>
 *   <dd>
 *     Name of the servlet context attribute that contains the repository.
 *     The default value is "<code>javax.jcr.Repository</code>".
 *   </dd>
 *   <dt>location</dt>
 *   <dd>
 *     Location where to bind the repository in the JNDI directory.
 *     The default value is "<code>javax/jcr/Repository</code>".
 *   </dd>
 *   <dt>*</dt>
 *   <dd>
 *     All other init parameters are used as the JNDI environment when
 *     instantiating {@link InitialContext} for binding up the repository.
 *   </dd>
 * </dl>
 *
 * @since 1.4
 */
public class JNDIBindingServlet extends HttpServlet {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -9033906248473370936L;

    /**
     * JNDI context to which to bind the repository.
     */
    private Context context;

    /**
     * Location of the repository within the JNDI context.
     */
    private String location = Repository.class.getName().replace('.', '/');

    /**
     * Binds a repository from the servlet context in the configured
     * JNDI location.
     *
     * @throws ServletException if the repository could not be bound in JNDI
     */
    public void init() throws ServletException {
        try {
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
            context =  new InitialContext(environment);
            context.bind(location, new ServletRepository(this));
        } catch (NamingException e) {
            throw new ServletException(
                    "Failed to bind repository to JNDI: " + location, e);
        }
    }

    /**
     * Unbinds the repository from JNDI.
     */
    public void destroy() {
        try {
            context.unbind(location);
        } catch (NamingException e) {
            log("Failed to unbind repository from JNDI: " + location, e);
        }
    }

}
