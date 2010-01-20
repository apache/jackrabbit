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
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;

/**
 * Servlet that writes the remote reference of a repository in the servlet
 * context to the configured URL.
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
 *     URL where to store the remote repository reference.
 *   </dd>
 * </dl>
 *
 * @since 1.4
 */
public class URLRemoteBindingServlet extends RemoteBindingServlet {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 3187755583290121129L;

    /**
     * Writes the remote reference of a repository in the servlet context
     * to the configured URL.
     *
     * @throws ServletException if the URL could not be written to
     */
    public void init() throws ServletException {
        String url = getInitParameter("url");
        if (url == null) {
            throw new ServletException("Missing init parameter: url");
        }
        try {
            ObjectOutputStream output = new ObjectOutputStream(
                    new URL(url).openConnection().getOutputStream());
            try {
                output.writeObject(getRemoteRepository());
            } finally {
                output.close();
            }
        } catch (MalformedURLException e) {
            throw new ServletException("Malformed URL: " + url, e);
        } catch (IOException e) {
            throw new ServletException("Failed to write to URL: " + url, e);
        }
    }

}
