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
package org.apache.jackrabbit.rmi.server.jmx;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.naming.InitialContext;

import org.apache.jackrabbit.rmi.remote.RemoteRepository;
import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;

/**
 * MBean that registers a JCR RMI server through JNDI.
 */
public class JCRServer implements JCRServerMBean {

    /**
     * local repository address
     */
    private String localAddress;

    /**
     * remote repository address
     */
    private String remoteAddress;

    /**
     * Optional local JNDI environment properties
     */
    private String localEnvironment;

    /**
     * Optional remote JNDI environment properties
     */
    private String remoteEnvironment;

    /**
     * Remote repository instance
     */
    RemoteRepository remote;

    /**
     * Local repository instance
     */
    private Repository localRepository;

    public void start() throws Exception {

        if (this.localAddress == null) {
            throw new IllegalStateException("local repository address is null");
        }

        if (this.remoteAddress == null) {
            throw new IllegalStateException("remote repository address is null");
        }

        // local repository
        InitialContext localContext = createInitialContext(localEnvironment);
        localRepository = (Repository) localContext.lookup(this.localAddress);
        if (localRepository == null) {
            throw new IllegalArgumentException("local repository not found at "
                    + this.localAddress);
        }

        // remote repository
        InitialContext remoteContext = createInitialContext(remoteEnvironment);
        RemoteAdapterFactory factory = new ServerAdapterFactory();
        remote = factory.getRemoteRepository(localRepository);

        // bind remote server
        remoteContext.bind(this.remoteAddress, remote);
    }

    /**
     *
     * @param jndiProps
     *            jndi environment properties
     * @return an InitialContext for the given environment properties
     * @throws Exception
     *             if any error occurs
     */
    private InitialContext createInitialContext(String jndiProps)
            throws Exception {
        InitialContext initialContext = null;
        if (jndiProps != null) {
            InputStream is = new ByteArrayInputStream(jndiProps.getBytes());
            Properties props = new Properties();
            props.load(is);
            initialContext = new InitialContext(props);
        } else {
            initialContext = new InitialContext();
        }
        return initialContext;
    }

    public void stop() throws Exception {
        // unbind remote server
        InitialContext ctx = new InitialContext();
        ctx.unbind(this.remoteAddress);
        remote = null;
    }

    public void createWorkspace(
            String username, String password, String name)
            throws RepositoryException {
        Session session = localRepository.login(
                new SimpleCredentials(username, password.toCharArray()));
        try {
            session.getWorkspace().createWorkspace(name);
        } finally {
            session.logout();
        }
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(String localAddress) {
        this.localAddress = localAddress;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public String getLocalEnvironment() {
        return localEnvironment;
    }

    public void setLocalEnvironment(String localEnvironment) {
        this.localEnvironment = localEnvironment;
    }

    public String getRemoteEnvironment() {
        return remoteEnvironment;
    }

    public void setRemoteEnvironment(String remoteEnvironment) {
        this.remoteEnvironment = remoteEnvironment;
    }

}
