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
package org.apache.jackrabbit.spi.commons.namespace;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * helper class that exposes the <code>NamespaceResolver</code>
 * interface on a <code>Session</code>.
 */
public class SessionNamespaceResolver implements NamespaceResolver {

    /**
     * the session for the namespace lookups
     */
    private final Session session;

    /**
     * Creates a new namespace resolver based on a session
     * @param session
     */
    public SessionNamespaceResolver(Session session) {
        this.session = session;
    }

    /**
     * {@inheritDoc}
     */
    public String getPrefix(String uri) throws NamespaceException {
        try {
            return session.getNamespacePrefix(uri);
        } catch (RepositoryException e) {
            // should never get here...
            throw new NamespaceException("internal error: failed to resolve namespace uri", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getURI(String prefix) throws NamespaceException {
        try {
            return session.getNamespaceURI(prefix);
        } catch (RepositoryException e) {
            // should never get here...
            throw new NamespaceException("internal error: failed to resolve namespace prefix", e);
        }
    }
}
