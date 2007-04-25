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
package org.apache.portals.graffito.jcr.spring;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.apache.portals.graffito.jcr.repository.RepositoryUtil;
import org.springmodules.jcr.JcrSessionFactory;

/**
 *  JCR session factory specific to Jaclrabbit for Graffito. Until now, Jackrabbit cannot unregister a namespace. 
 *  So, the JcrSessionFactory provided by the spring module is not usefull when namespace management are needed. 
 *  This class extends the JcrSessionFactory in order to add the namespace graffito
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 */
public class JackrabbitSessionFactory extends JcrSessionFactory 
{

	/**
	 * Register the namespaces.
	 * 
	 * @param session
	 * @throws RepositoryException
	 */
    protected void registerNamespaces() throws RepositoryException {
        NamespaceRegistry registry =
            getSession().getWorkspace().getNamespaceRegistry();

        // Keep trying until the Graffito namespace has been registered
        int n = 0;
        String prefix = null;
        while (prefix == null) {
            try {
                // Is the Graffito namespace registered?
                prefix = registry.getPrefix(RepositoryUtil.GRAFFITO_NAMESPACE);
            } catch (NamespaceException e1) {
                // No, try to register it with the default prefix
                prefix = RepositoryUtil.GRAFFITO_NAMESPACE_PREFIX;
                // ... and a sequence number if the first attempt failed 
                if (n++ > 0) {
                    prefix = prefix + n;
                }
                try {
                    // Is this prefix registered to the Graffito namespace?
                    if (!RepositoryUtil.GRAFFITO_NAMESPACE.equals(
                            registry.getURI(prefix))) {
                        // No, but it *is* registered. Try the next prefix...
                        prefix = null;
                    }
                } catch (NamespaceException e2) {
                    try {
                        // No, and it's not registered. Try registering it:
                        registry.registerNamespace(
                                prefix, RepositoryUtil.GRAFFITO_NAMESPACE);
                    } catch (NamespaceException e3) {
                        // Registration failed. Try the next prefix...
                        prefix = null;
                    }
                }
            }
        }

        super.registerNamespaces();
    }

}
