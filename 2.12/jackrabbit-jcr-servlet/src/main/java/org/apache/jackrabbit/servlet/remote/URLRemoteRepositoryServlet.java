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
import java.net.URL;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.repository.URLRemoteRepositoryFactory;
import org.apache.jackrabbit.servlet.AbstractRepositoryServlet;

/**
 * Servlet that makes a remote repository from a ULR available as an attribute
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
 *   <dt>url</dt>
 *   <dd>
 *     URL of the remote repository.
 *   </dd>
 * </dl>
 * <p>
 * This servlet can also be mapped to the URL space. See
 * {@link AbstractRepositoryServlet} for the details.
 *
 * @since 1.4
 */
public class URLRemoteRepositoryServlet extends RemoteRepositoryServlet {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 6144781813459102448L;

    /**
     * Returns the remote repository at the given URL.
     *
     * @return repository
     * @throws RepositoryException if the repository could not be accessed
     */
    @Override
    protected Repository getRepository() throws RepositoryException {
        String url = getInitParameter("url");
        if (url == null) {
            throw new RepositoryException("Missing init parameter: url");
        }

        try {
            return new URLRemoteRepositoryFactory(
                        getLocalAdapterFactory(), new URL(url)).getRepository();
        } catch (MalformedURLException e) {
            throw new RepositoryException("Invalid repository URL: " + url, e);
        }
    }

}
