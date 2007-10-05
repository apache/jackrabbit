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
package org.apache.jackrabbit.spi2jcr;

import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.name.AbstractNamespaceResolver;
import org.apache.jackrabbit.name.NamespaceResolver;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Session;
import javax.jcr.RepositoryException;
import javax.jcr.Credentials;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;

/**
 * <code>SessionInfoImpl</code> implements a session info based on a JCR
 * {@link Session}.
 */
class SessionInfoImpl implements SessionInfo {

    /**
     * The underlying JCR session.
     */
    private final Session session;

    /**
     * The namespace resolver for this session info.
     */
    private final NamespaceResolver resolver;

    /**
     * A copy of the credentials that were used to obtain the JCR session.
     */
    private Credentials credentials;

    /**
     * Creates a new session info based on the given <code>session</code>.
     *
     * @param session     the JCR session.
     * @param credentials a copy of the credentials that were used to obtain the
     *                    JCR session.
     * @throws RepositoryException 
     */
    SessionInfoImpl(Session session, Credentials credentials) throws RepositoryException {
        this.session = session;
        this.credentials = credentials;
        
        final NamespaceRegistry nsReg = session.getWorkspace().getNamespaceRegistry();
       
        this.resolver = new AbstractNamespaceResolver() {
            public String getPrefix(String uri) throws NamespaceException {
                try {
                    return nsReg.getPrefix(uri);
                }
                catch (RepositoryException e) {
                    // should never get here...
                    throw new NamespaceException("internal error: failed to resolve namespace uri", e);
                }
            }

            public String getURI(String prefix) throws NamespaceException {
                try {
                    return nsReg.getURI(prefix);
                }
                catch (RepositoryException e) {
                    // should never get here...
                    throw new NamespaceException("internal error: failed to resolve namespace prefix", e);
                }
            }
        };
        
    }

    /**
     * @return the underlying session.
     */
    Session getSession() {
        return session;
    }

    /**
     * @return the namespace resolver for this session info.
     */
    NamespaceResolver getNamespaceResolver() {
        return resolver;
    }

    /**
     * @return credentials that were used to obtain this session info.
     * @throws RepositoryException if the credentials cannot be duplicated.
     */
    Credentials getCredentials() throws RepositoryException {
        // return a duplicate
        return duplicateCredentials(credentials);
    }

    /**
     * Returns a duplicate of the passed credentials
     *
     * @param credentials the credentials to duplicate.
     * @return a duplicate of the passed credentials.
     * @throws RepositoryException if an error occurs while duplicating the
     *                             credentials.
     */
    public static Credentials duplicateCredentials(Credentials credentials)
            throws RepositoryException {
        if (credentials == null) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream oOut = new ObjectOutputStream(out);
            oOut.writeObject(credentials);
            oOut.close();

            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            ObjectInputStream oIn = new ObjectInputStream(in);
            return (Credentials) oIn.readObject();
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    //--------------------------------------------------------< SessionInfo >---

    /**
     * @inheritDoc
     */
    public String getUserID() {
        return session.getUserID();
    }

    /**
     * @inheritDoc
     */
    public String getWorkspaceName() {
        return session.getWorkspace().getName();
    }

    /**
     * @inheritDoc
     */
    public String[] getLockTokens() {
        return session.getLockTokens();
    }

    /**
     * @inheritDoc
     */
    public void addLockToken(String lockToken) {
        session.addLockToken(lockToken);
    }

    /**
     * @inheritDoc
     */
    public void removeLockToken(String lockToken) {
        session.removeLockToken(lockToken);
    }
}
