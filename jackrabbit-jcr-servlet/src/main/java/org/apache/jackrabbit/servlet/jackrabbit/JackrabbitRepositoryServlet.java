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
package org.apache.jackrabbit.servlet.jackrabbit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;

import org.apache.jackrabbit.core.RepositoryContext;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.servlet.AbstractRepositoryServlet;

/**
 * Servlet that makes a Jackrabbit repository available as a servlet context
 * attribute. The repository is started during servlet initialization and
 * shut down when the servlet is destroyed.
 * <p>
 * The supported initialization parameters of this servlet are:
 * <dl>
 *   <dt>org.apache.jackrabbit.core.RepositoryContext</dt>
 *   <dd>
 *     Name of the servlet context attribute to put the internal
 *     component context of the repository in. The default value is
 *     "<code>org.apache.jackrabbit.core.RepositoryContext</code>".
 *   </dd>
 *   <dt>javax.jcr.Repository</dt>
 *   <dd>
 *     Name of the servlet context attribute to put the repository in.
 *     The default value is "<code>javax.jcr.Repository</code>".
 *   </dd>
 *   <dt>repository.home</dt>
 *   <dd>
 *     Path of the repository home directory. The default value is
 *     "<code>jackrabbit-repository</code>". The home directory is
 *     automatically created during servlet initialization if it does
 *     not already exist.
 *   </dd>
 *   <dt>repository.config</dt>
 *   <dd>
 *     Path of the repository configuration file. The default value is
 *     "<code>repository.xml</code>" within the configured repository home
 *     directory. A standard configuration file is automatically copied to
 *     the configured location during servlet initialization if the file
 *     does not already exist.
 *   </dd>
 * </dl>
 * <p>
 * The repository servlet can also be mapped to the URL space. See
 * {@link AbstractRepositoryServlet} for the details.
 *
 * @since 1.4
 */
public class JackrabbitRepositoryServlet extends AbstractRepositoryServlet {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 7102770011290708450L;

    /**
     * Repository instance.
     */
    private RepositoryContext context;

    /**
     * Starts the repository instance and makes it available in the
     * servlet context.
     *
     * @throws ServletException if the repository can not be started
     */
    public void init() throws ServletException {
        try {
            File home = new File(getInitParameter(
                    "repository.home", "jackrabbit-repository"));
            if (!home.exists()) {
                log("Creating repository home directory: " + home);
                home.mkdirs();
            }

            File config = new File(getInitParameter(
                    "repository.config",
                    new File(home, "repository.xml").getPath()));
            if (!config.exists()) {
                log("Creating default repository configuration: " + config);
                createDefaultConfiguration(config);
            }

            context = RepositoryContext.create(RepositoryConfig.create(
                    config.toURI(), home.getPath()));

            String name = RepositoryContext.class.getName();
            getServletContext().setAttribute(
                    getInitParameter(name, name), context);
        } catch (RepositoryException e) {
            throw new ServletException("Failed to start Jackrabbit", e);
        }

        super.init();
    }

    /**
     * Removes the repository from the servlet context and shuts it down.
     */
    public void destroy() {
        super.destroy();
        context.getRepository().shutdown();
    }

    /**
     * Returns the configured repository instance.
     *
     * @return repository
     */
    @Override
    protected Repository getRepository() {
        return context.getRepository();
    }

    /**
     * Copies the default repository configuration file to the given location.
     *
     * @param config path of the configuration file
     * @throws ServletException if the configuration file could not be copied
     */
    private void createDefaultConfiguration(File config)
            throws ServletException {
        try {
            OutputStream output = new FileOutputStream(config);
            try {
                InputStream input =
                    RepositoryImpl.class.getResourceAsStream("repository.xml");
                try {
                    byte[] buffer = new byte[8192];
                    int n = input.read(buffer);
                    while (n != -1) {
                        output.write(buffer, 0, n);
                        n = input.read(buffer);
                    }
                } finally {
                    input.close();
                }
            } finally {
                output.close();
            }
        } catch (IOException e) {
            throw new ServletException(
                    "Failed to copy default configuration: " + config, e);
        }
    }

}
