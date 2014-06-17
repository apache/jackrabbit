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

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;

import org.apache.jackrabbit.commons.JcrUtils;

/**
 * Implements the JCA ManagedConnectionFactory contract.
 */
public class JCAManagedConnectionFactory
        implements ManagedConnectionFactory {

	private static final long serialVersionUID = 4775747072955577131L;

	/**
     * Repository parameters.
     */
    private final Map<String, String> parameters = new HashMap<String, String>();

    /**
     * Key for the repository home
     */
    final static String HOMEDIR_KEY = "org.apache.jackrabbit.repository.home";
    /**
     * Key for the repository config file
     */
    final static String CONFIGFILE_KEY = "org.apache.jackrabbit.repository.conf";
    
    /**
     * Flag indicating whether the session should be bound to the
     * transaction lifecycle.
     * In other words, if this flag is true the handle
     * will be closed when the transaction ends.
     */
    private Boolean bindSessionToTransaction = Boolean.TRUE;

    /**
     * Flag indicating whether the Repository should start
     * immediately or lazy on first access. Per default true so the Repository will
     * start immediately if this JCAManagedConnectionFactory will be initialized.
     */
    private Boolean startRepositoryImmediately = Boolean.TRUE;

    /**
     * Repository.
     */
    private transient Repository repository;

    /**
     * Log writer.
     */
    private transient PrintWriter logWriter;

    /**
     * Return the repository URI.
     */
    public String getRepositoryURI() {
        return parameters.get(JcrUtils.REPOSITORY_URI);
    }

    /**
     * Set the repository URI.
     */
    public void setRepositoryURI(String uri) {
        parameters.put(JcrUtils.REPOSITORY_URI, uri);
    }

    /**
     * Return the repository home directory.
     */
    public String getHomeDir() {
        return parameters.get(HOMEDIR_KEY);
    }

    /**
     * Set the repository home directory.
     */
    public void setHomeDir(String home) {
        parameters.put(HOMEDIR_KEY, home);
    }

    /**
     * Return the repository configuration file.
     */
    public String getConfigFile() {
        return parameters.get(CONFIGFILE_KEY);
    }

    /**
     * Set the repository configuration file.
     */
    public void setConfigFile(String conf) {
        parameters.put(CONFIGFILE_KEY, conf);
    }

    /**
     * Get the log writer.
     */
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    /**
     * Set the log writer.
     */
    public void setLogWriter(PrintWriter logWriter)
            throws ResourceException {
        this.logWriter = logWriter;
    }

    /**
     * Creates a Connection Factory instance.
     */
    public Object createConnectionFactory()
            throws ResourceException {
        return createConnectionFactory(new JCAConnectionManager());
    }

    /**
     * Creates a Connection Factory instance.
     */
    public Object createConnectionFactory(ConnectionManager cm)
            throws ResourceException {
    	if (startRepositoryImmediately) {
    		createRepository();
    	}
        JCARepositoryHandle handle = new JCARepositoryHandle(this, cm);
        log("Created repository handle (" + handle + ")");
        return handle;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Creates a new physical connection to the underlying EIS resource manager.
     * <p>
     * WebSphere 5.1.1 will try to recover an XA resource on startup, regardless
     * whether it was committed or rolled back. On this occasion, <code>cri</code>
     * will be <code>null</code>. In order to be interoperable, we return an
     * anonymous connection, whose XA resource is recoverable-only.
     */
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cri)
            throws ResourceException {

        if (cri == null) {
            return new AnonymousConnection();
        }
        return createManagedConnection((JCAConnectionRequestInfo) cri);
    }

    /**
     * Creates a new physical connection to the underlying EIS resource manager.
     */
    private ManagedConnection createManagedConnection(JCAConnectionRequestInfo cri)
            throws ResourceException {
        return new JCAManagedConnection(this, cri);
    }

    /**
     * Returns a matched connection from the candidate set of connections.
     */
    @SuppressWarnings("rawtypes")
	public ManagedConnection matchManagedConnections(
            Set set, Subject subject, ConnectionRequestInfo cri)
            throws ResourceException {
        for (Object connection : set) {
            if (connection instanceof JCAManagedConnection) {
                JCAManagedConnection mc = (JCAManagedConnection) connection;
                if (equals(mc.getManagedConnectionFactory())) {
                    JCAConnectionRequestInfo otherCri = mc.getConnectionRequestInfo();
                    if (cri == otherCri || (cri != null && cri.equals(otherCri))) {
                        return mc;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Create/startup the repository.
     */
    @SuppressWarnings("deprecation")
    private void createRepository() throws ResourceException {
        if (repository == null) {
            try {
                JCARepositoryManager mgr = JCARepositoryManager.getInstance();
                repository = mgr.createRepository(parameters);
                log("Created repository (" + repository + ")");
            } catch (RepositoryException e) {
                log("Failed to create repository", e);
                ResourceException exception = new ResourceException(
                        "Failed to create repository: " + e.getMessage());
                exception.setLinkedException(e);
                throw exception;
            }
        }
    }

    /**
     * Return the repository, automatically creating it if needed.
     */
    @SuppressWarnings("deprecation")
	public Repository getRepository() throws RepositoryException {
    	if (repository == null) {
    		synchronized (this) {
    			try {
    				createRepository();
    			} catch (ResourceException e) {
    				throw (RepositoryException) e.getLinkedException();
    			}
			}
    	}
        return repository;
    }
    
    /**
     * Log a message.
     */
    public void log(String message) {
        log(message, null);
    }

    /**
     * Log a message.
     */
    public void log(String message, Throwable exception) {
        if (logWriter != null) {
            logWriter.println(message);

            if (exception != null) {
                exception.printStackTrace(logWriter);
            }
        }
    }

    /**
     * Return the hash code.
     */
    public int hashCode() {
        return parameters.hashCode();
    }

    /**
     * Return true if equals.
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof JCAManagedConnectionFactory) {
            return equals((JCAManagedConnectionFactory) o);
        } else {
            return false;
        }
    }

    /**
     * Return true if equals.
     */
    private boolean equals(JCAManagedConnectionFactory o) {
        return parameters.equals(o.parameters);
    }

    /**
     * Shutdown the repository.
     */
    protected void finalize() {
        JCARepositoryManager mgr = JCARepositoryManager.getInstance();
        mgr.autoShutdownRepository(parameters);
    }

    public Boolean getBindSessionToTransaction() {
        return bindSessionToTransaction;
    }

    public void setBindSessionToTransaction(Boolean bindSessionToTransaction) {
        this.bindSessionToTransaction = bindSessionToTransaction;
    }

    public Boolean getStartRepositoryImmediately() {
        return startRepositoryImmediately;
    }

    public void setStartRepositoryImmediately(Boolean startRepositoryImmediately) {
        this.startRepositoryImmediately = startRepositoryImmediately;
    }

}
