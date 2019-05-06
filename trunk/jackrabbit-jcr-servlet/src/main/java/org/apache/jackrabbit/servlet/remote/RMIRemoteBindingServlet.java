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

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.servlet.ServletException;

/**
 * Servlet that binds a repository from a servlet context attribute in RMI.
 * <p>
 * The initialization parameters of this servlet are:
 * <dl>
 *   <dt>javax.jcr.Repository</dt>
 *   <dd>
 *     Name of the servlet context attribute that contains the repository.
 *     The default value is "<code>javax.jcr.Repository</code>".
 *   </dd>
 *   <dt>org.apache.jackrabbit.rmi.server.RemoteAdapterFactory</dt>
 *   <dd>
 *     Name of the remote adapter factory class used to create the remote
 *     repository reference. The configured class should have public
 *     constructor that takes no arguments.
 *   </dd>
 *   <dt>url</dt>
 *   <dd>
 *     RMI URL where to bind the repository in. The default value is
 *     "<code>//localhost/javax/jcr/Repository</code>".
 *   </dd>
 * </dl>
 *
 * @since 1.4
 */
public class RMIRemoteBindingServlet extends RemoteBindingServlet {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1627580747678104906L;

    /**
     * Location of the repository within the JNDI context.
     */
    private String url;

    /**
     * Binds a repository from the servlet context in the configured RMI URL.
     *
     * @throws ServletException if the repository could not be bound in RMI
     */
    public void init() throws ServletException {
        url = getInitParameter("url");
        if (url == null) {
            url = "//localhost/javax/jcr/Repository";
        }
        try {
            Naming.rebind(url, getRemoteRepository());
        } catch (MalformedURLException e) {
            log("Invalid RMI URL: " + url, e);
        } catch (RemoteException e) {
            log("Failed to bind repository to RMI: " + url, e);
        }
    }

    /**
     * Unbinds the repository from RMI.
     */
    public void destroy() {
        try {
            Naming.unbind(url);
        } catch (NotBoundException e) {
            // Ignore, perhaps the reference was already manually removed
        } catch (MalformedURLException e) {
            // Ignore, we already logged a warning about this during init()
        } catch (RemoteException e) {
            log("Failed to unbind repository from RMI: " + url, e);
        }
    }

}
