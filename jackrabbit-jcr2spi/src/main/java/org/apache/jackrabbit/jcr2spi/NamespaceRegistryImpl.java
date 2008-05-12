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
package org.apache.jackrabbit.jcr2spi;

import java.util.Collection;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;

/**
 * <code>NamespaceRegistryImpl</code> implements the JCR client facing
 * NamespaceRegistry.
 */
public class NamespaceRegistryImpl implements NamespaceRegistry {

    /**
     * Repository service.
     */
    private final RepositoryService service;

    /**
     * Session info.
     */
    private final SessionInfo info;

    /**
     * Create a new <code>NamespaceRegistryImpl</code>.
     *
     * @param service repository service
     * @param info session info
     */
    public NamespaceRegistryImpl(RepositoryService service, SessionInfo info) {
        this.service = service;
        this.info = info;
    }

    //--------------------------------------------------< NamespaceRegistry >---

    /**
     * @see NamespaceRegistry#registerNamespace(String, String)
     */
    public void registerNamespace(String prefix, String uri)
            throws RepositoryException {
        service.registerNamespace(info, prefix, uri);
    }

    /**
     * @see NamespaceRegistry#unregisterNamespace(String)
     */
    public void unregisterNamespace(String prefix) throws RepositoryException {
        service.unregisterNamespace(info, getURI(prefix));
    }

    /**
     * @see javax.jcr.NamespaceRegistry#getPrefixes()
     */
    public String[] getPrefixes() throws RepositoryException {
        Collection prefixes = service.getRegisteredNamespaces(info).keySet();
        return (String[]) prefixes.toArray(new String[prefixes.size()]);
    }

    /**
     * @see javax.jcr.NamespaceRegistry#getURIs()
     */
    public String[] getURIs() throws RepositoryException {
        Collection uris = service.getRegisteredNamespaces(info).values();
        return (String[]) uris.toArray(new String[uris.size()]);
    }

    /**
     * @see javax.jcr.NamespaceRegistry#getURI(String)
     */
    public String getURI(String prefix) throws RepositoryException {
        return service.getNamespaceURI(info, prefix);
    }

    /**
     * @see javax.jcr.NamespaceRegistry#getPrefix(String)
     */
    public String getPrefix(String uri) throws RepositoryException {
        return service.getNamespacePrefix(info, uri);
    }

}
