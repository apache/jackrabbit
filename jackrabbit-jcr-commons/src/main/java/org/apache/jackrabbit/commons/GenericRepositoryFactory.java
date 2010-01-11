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

import java.net.URI;
import java.net.URISyntaxException;
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
 *     Connects to the repository at the given jndi: URI.
 *     All the other parameters except the repository URI from the original
 *     invocation are also passed on to these recursive calls.
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
            return null; // no default JNDI repository
        } else {
            Hashtable environment = new Hashtable(parameters);
            if (environment.containsKey(JNDI_NAME)) {
                String name = environment.remove(JNDI_NAME).toString();
                return getRepository(name, environment);
            } else if (environment.containsKey(URI)) {
                Object parameter = environment.remove(URI);
                try {
                    URI uri = new URI(parameter.toString().trim());
                    if ("jndi".equalsIgnoreCase(uri.getScheme())) {
                        String name = uri.getSchemeSpecificPart();
                        return getRepository(name, environment);
                    } else {
                        return null; // not a jndi: URI
                    }
                } catch (URISyntaxException e) {
                    return null; // not a valid URI
                }
            } else {
                return null; // unknown parameters
            }
        }
    }

    private Repository getRepository(String name, Hashtable environment)
            throws RepositoryException {
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
