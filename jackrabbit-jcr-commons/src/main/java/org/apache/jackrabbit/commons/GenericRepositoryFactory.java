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
package org.apache.jackrabbit.commons;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Generic JCR repository factory. This factory knows how to handle
 * parameter maps containing the following keys:
 * <dl>
 *   <dt>{@link #JNDI_NAME org.apache.jackrabbit.repository.jndi.name}</dt>
 *   <dd>
 *     The repository instance is looked up from JNDI.
 *     An {@link InitialContext initial JNDI context} is constructed using
 *     all the other parameters, and a repository at the given name is
 *     looked up from the instantiated context.
 *   </dd>
 *   <dt>{@link #URI org.apache.jackrabbit.repository.uri}</dt>
 *   <dd>
 *     Connects to the given repository URI.
 *     See {@link JcrUtils#getRepository(String)} for the supported URI types.
 *     Note that this class does not directly implement all these connection
 *     mechanisms. Instead it maps the given repository URI to known
 *     {@link RepositoryFactory} parameters and uses the Java Service
 *     Provider mechanism to recursively ask all the available repository
 *     factories to handle the resulting connection parameters. All the other
 *     parameters except the repository URI from the original invocation are
 *     also passed on to these recursive calls.
 *   </dd>
 * </dl>
 * Clients should normally only use this class through the Java Service
 * Provider mechanism. See the getRepository utility methods in
 * {@link JcrUtils} for an easy way to do that.
 *
 * @since Apache Jackrabbit 2.0
 */
@SuppressWarnings("unchecked")
public class GenericRepositoryFactory implements RepositoryFactory {

    /**
     * The repository URI parameter name.
     */
    public static final String URI =
        "org.apache.jackrabbit.repository.uri";

    /**
     * The JNDI name parameter name.
     */
    public static final String JNDI_NAME =
        "org.apache.jackrabbit.repository.jndi.name";

    /**
     * Handles the generic repository parameters mentioned in the
     * description of this class. Returns <code>null</code> if none of
     * the described parameters are given or if the given parameter map is
     * <code>null</code>.
     */
    public Repository getRepository(Map parameters)
            throws RepositoryException {
        if (parameters == null) {
            return null;
        } else if (parameters.containsKey(URI)) {
            return getUriRepository(parameters);
        } else if (parameters.containsKey(JNDI_NAME)) {
            return getJndiRepository(parameters);
        } else {
            return null;
        }
    }

    /**
     * Implements the repository URI connection as documented in the
     * description of this class.
     *
     * @param original original repository parameters, including {@link #URI}
     * @return repository instance
     * @throws RepositoryException if the repository can not be accessed,
     *                             or if the URI is invalid or unknown
     */
    private Repository getUriRepository(Map original)
            throws RepositoryException {
        Map parameters = new HashMap(original);
        String parameter = parameters.remove(URI).toString().trim();

        try {
            URI uri = new URI(parameter);
            String scheme = uri.getScheme();
            if ("jndi".equalsIgnoreCase(scheme)) {
                parameters.put(JNDI_NAME, uri.getSchemeSpecificPart());
            } else if ("file".equalsIgnoreCase(scheme)) {
                File file = new File(uri);
                String home, conf;
                if (file.isFile()) {
                    home = file.getParentFile().getPath();
                    conf = file.getPath();
                } else {
                    home = file.getPath();
                    conf = new File(file, "repository.xml").getPath();
                }
                parameters.put("org.apache.jackrabbit.repository.home", home);
                parameters.put("org.apache.jackrabbit.repository.conf", conf);
            } else {
                return null;
            }
        } catch (URISyntaxException e) {
            return null;
        }

        return JcrUtils.getRepository(parameters);
    }

    /**
     * Implements the JNDI lookup as documented in the description of
     * this class.
     *
     * @param parameters repository parameters, including {@link #JNDI_NAME}
     * @return the repository instance from JNDI
     * @throws RepositoryException if the name is not found in JNDI
     *                             or does not point to a repository instance
     */
    private Repository getJndiRepository(Map parameters)
            throws RepositoryException {
        Hashtable environment = new Hashtable(parameters);
        String name = environment.remove(JNDI_NAME).toString();

        try {
            Object value = new InitialContext(environment).lookup(name);
            if (value instanceof Repository) {
                return (Repository) value;
            } else {
                throw new RepositoryException(
                        "Invalid repository object " + value
                        + " found at " + name + " in JNDI environment "
                        + environment);
            }
        } catch (NamingException e) {
            throw new RepositoryException(
                    "Failed to look up " + name
                    + " from JNDI environment " + environment, e);
        }
    }

}
