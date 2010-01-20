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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.rmi.remote.RemoteRepository;
import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.apache.jackrabbit.servlet.ServletRepository;

/**
 * Servlet that makes a repository in servlet context available as a remote
 * repository reference. By default this servlet makes the serialized
 * reference available through HTTP GET, but subclasses can extend this
 * behavior to bind the remote reference to various locations like JNDI
 * or the RMI registry.
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
 * </dl>
 *
 * @since 1.4
 */
public class RemoteBindingServlet extends HttpServlet {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -162482284843619248L;

    /**
     * Remote repository.
     */
    private RemoteRepository remote;

    /**
     * Returns the configured remote repository reference. The remote
     * repository is instantiated and memorized during the first call to
     * this method.
     *
     * @return remote repository
     * @throws ServletException if the repository could not be instantiated
     */
    protected RemoteRepository getRemoteRepository() throws ServletException {
        if (remote == null) {
            try {
                RemoteAdapterFactory factory = getRemoteAdapterFactory();
                remote = factory.getRemoteRepository(new ServletRepository(this));
            } catch (RemoteException e) {
                throw new ServletException(
                        "Failed to create the remote repository reference", e);
            }
        }
        return remote;
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
            return new ServerAdapterFactory();
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
     * Outputs the remote repository reference as a serialized stream.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @throws ServletException if the remote reference is not available
     * @throws IOException on IO errors
     */
    protected void doGet(
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/octet-stream");
        ObjectOutputStream output =
            new ObjectOutputStream(response.getOutputStream());
        output.writeObject(RemoteObject.toStub(getRemoteRepository()));
        output.flush();
    }

}
