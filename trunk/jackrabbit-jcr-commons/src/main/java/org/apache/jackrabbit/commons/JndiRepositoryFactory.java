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
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * JNDI-based JCR repository factory. This factory looks up {@link Repository}
 * instances from JNDI directories based on the following parameters:
 * <dl>
 *   <dt>{@link #JNDI_NAME org.apache.jackrabbit.repository.jndi.name}</dt>
 *   <dd>
 *     The value of this parameter is used as a JNDI name for looking
 *     up the repository.
 *   </dd>
 *   <dt>{@link JcrUtils#REPOSITORY_URI org.apache.jackrabbit.repository.uri}</dt>
 *   <dd>
 *     If the URI scheme is "jndi", then the remainder of the URI is used
 *     as a JNDI name for looking up the repository.
 *   </dd>
 * </dl>
 * <p>
 * All the other repository parameters are passed as the environment of the
 * {@link InitialContext initial JNDI context}.
 * <p>
 * Clients should normally only use this class through the Java Service
 * Provider mechanism. See the getRepository utility methods in
 * {@link JcrUtils} for an easy way to do that.
 *
 * @since Apache Jackrabbit 2.0
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class JndiRepositoryFactory implements RepositoryFactory {

    /**
     * The JNDI name parameter name.
     */
    public static final String JNDI_NAME =
        "org.apache.jackrabbit.repository.jndi.name";

    public Repository getRepository(Map parameters)
            throws RepositoryException {
        if (parameters == null) {
            return null; // no default JNDI repository
        } else {
            Hashtable environment = new Hashtable(parameters);
            if (environment.containsKey(JNDI_NAME)) {
                String name = environment.remove(JNDI_NAME).toString();
                return getRepository(name, environment);
            } else if (environment.containsKey(JcrUtils.REPOSITORY_URI)) {
                Object parameter = environment.remove(JcrUtils.REPOSITORY_URI);
                try {
                    URI uri = new URI(parameter.toString().trim());
                    if ("jndi".equalsIgnoreCase(uri.getScheme())) {
                        return getRepository(uri, environment);
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

    private Repository getRepository(URI uri, Hashtable environment)
            throws RepositoryException {
        String name;
        if (uri.isOpaque()) {
            name = uri.getSchemeSpecificPart();
        } else {
            name = uri.getPath();
            if (name == null) {
                name = "";
            } else if (name.startsWith("/")) {
                name = name.substring(1);
            }
            String authority = uri.getAuthority();
            if (authority != null && authority.length() > 0) {
                environment = new Hashtable(environment);
                environment.put(Context.INITIAL_CONTEXT_FACTORY, authority);
            }
        }
        return getRepository(name, environment);
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
