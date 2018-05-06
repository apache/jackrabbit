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
package org.apache.jackrabbit.jca;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.naming.Reference;
import javax.resource.Referenceable;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;

import org.apache.jackrabbit.commons.repository.ProxyRepository;
import org.apache.jackrabbit.commons.repository.RepositoryFactory;

import java.io.Serializable;

/**
 * This class implements the JCA implementation of repository.
 */
public final class JCARepositoryHandle extends ProxyRepository
        implements Referenceable, Serializable {

	private static final long serialVersionUID = 1235867375647927916L;

	/**
     * Managed connection factory.
     */
    private final JCAManagedConnectionFactory mcf;

    /**
     * Connection manager.
     */
    private final ConnectionManager cm;

    /**
     * Reference.
     */
    private Reference reference;

    /**
     * Construct the repository.
     */
    public JCARepositoryHandle(
            JCAManagedConnectionFactory mcf, ConnectionManager cm) {
        super(new JCARepositoryFactory(mcf));
        this.mcf = mcf;
        this.cm = cm;
    }

    /**
     * Creates a new session.
     */
    @SuppressWarnings("deprecation")
    public Session login(Credentials creds, String workspace)
            throws RepositoryException {
        try {
            return (Session) cm.allocateConnection(
                    mcf, new JCAConnectionRequestInfo(creds, workspace));
        } catch (ResourceException e) {
            Throwable cause = e.getCause();
            if (cause == null) {
                cause = e.getLinkedException();
            }
            if (cause instanceof LoginException) {
                throw (LoginException) cause;
            } else if (cause instanceof NoSuchWorkspaceException) {
                throw (NoSuchWorkspaceException) cause;
            } else if (cause instanceof RepositoryException) {
                throw (RepositoryException) cause;
            } else if (cause != null) {
                throw new RepositoryException(cause);
            } else {
                throw new RepositoryException(e);
            }
        }
    }

    /**
     * Return the reference.
     */
    public Reference getReference() {
        return reference;
    }

    /**
     * Set the reference.
     */
    public void setReference(Reference reference) {
        this.reference = reference;
    }

    private static class JCARepositoryFactory
            implements RepositoryFactory, Serializable {

		private static final long serialVersionUID = 5364039431121341634L;
		
		private final JCAManagedConnectionFactory mcf;

        public JCARepositoryFactory(JCAManagedConnectionFactory mcf) {
            this.mcf = mcf;
        }

        public Repository getRepository() throws RepositoryException {
            return mcf.getRepository();
        }

    }

}
