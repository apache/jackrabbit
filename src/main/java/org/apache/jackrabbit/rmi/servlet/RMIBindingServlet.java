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
package org.apache.jackrabbit.rmi.servlet;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.jackrabbit.commons.servlet.ServletRepository;
import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;

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
 *     constructor that takes no arguments. The default value is
 *     "<code>org.apache.jackrabbit.rmi.server.ServerAdapterFactory</code>".
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
public class RMIBindingServlet extends HttpServlet {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -207084931471893942L;

    /**
     * Location of the repository within the JNDI context.
     */
    private String url;

    /**
     * The remote repository reference. Kept to avoid it from being
     * collected as garbage when no clients are connected.
     */
    private Remote remote;

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
            RemoteAdapterFactory factory = getRemoteAdapterFactory();
            remote = factory.getRemoteRepository(new ServletRepository(this));
        } catch (RemoteException e) {
            throw new ServletException(
                    "Failed to create the remote repository reference", e);
        }

        try {
            Naming.bind(url, remote);
        } catch (MalformedURLException e) {
            throw new ServletException("Invalid RMI URL: " + url, e);
        } catch (AlreadyBoundException e) {
            throw new ServletException(
                    "RMI URL is already bound: " + url, e);
        } catch (RemoteException e) {
            throw new ServletException(
                    "Failed to bind repository to RMI: " + url, e);
        }
    }

    /**
     * Instantiates and returns the configured remote adapter factory.
     *
     * @return remote adapter factory
     * @throws ServletException if the factory could not be instantiated
     */
    private RemoteAdapterFactory getRemoteAdapterFactory()
            throws ServletException {
        String name = getInitParameter(RemoteAdapterFactory.class.getName());
        if (name == null) {
            name = ServerAdapterFactory.class.getName();
        }
        try {
            Class factoryClass = Class.forName(name);
            return (RemoteAdapterFactory) factoryClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new ServletException(
                    "Remote adapter factory class not found: " + name, e);
        } catch (InstantiationException e) {
            throw new ServletException(
                    "Failed to instantiate the adapter factory: " + name, e);
        } catch (IllegalAccessException e) {
            throw new ServletException(
                    "Adapter factory constructor is not public: " + name, e);
        } catch (ClassCastException e) {
            throw new ServletException(
                    "Invalid remote adapter factory class: " + name, e);
        }
    }

    /**
     * Unbinds the repository from RMI.
     */
    public void destroy() {
        try {
            remote = null;
            Naming.unbind(url);
        } catch (MalformedURLException e) {
            log("Invalid RMI URL: " + url, e);
        } catch (NotBoundException e) {
            log("Repository not bound in RMI: " + url, e);
        } catch (RemoteException e) {
            log("Failed to unbind repository from RMI: " + url, e);
        }
    }

}
