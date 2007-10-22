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

import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;
import org.apache.jackrabbit.test.RepositoryStubException;
import org.apache.log4j.PropertyConfigurator;

import javax.jcr.Repository;
import java.util.Properties;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.net.MalformedURLException;

/**
 * <code>RMIRepositoryStub</code> implements a repository stub that initializes
 * a Jackrabbit RMI client.
 */
public class RMIRepositoryStub extends DefaultRepositoryStub {

    /**
     * Property for the repository url
     */
    public static final String PROP_REPOSITORY_URI = "org.apache.jackrabbit.rmi.repository.uri";

    static {
        PropertyConfigurator.configure(RMIRepositoryStub.class.getClassLoader().getResource("log4j.properties"));
    }

    /**
     * The Jackrabbit repository.
     */
    private Repository repo;

    /**
     * Constructor required by TCK.
     *
     * @param env the environment.
     */
    public RMIRepositoryStub(Properties env) {
        super(env);
    }

    /**
     * @return the repository instance to test.
     * @throws RepositoryStubException if an error occurs while starting up the
     *                                 repository.
     */
    public Repository getRepository() throws RepositoryStubException {
        if (repo == null) {
            repo = getRepositoryFromRMI();
        }
        return repo;
    }

    private Repository getRepositoryFromRMI() throws RepositoryStubException {
        try {
            Class clazz = Class.forName(getServerFactoryDelegaterClass());
            ClientFactoryDelegater cfd = (ClientFactoryDelegater) clazz.newInstance();

            String repositoryURI = environment.getProperty(PROP_REPOSITORY_URI);
            Repository r = cfd.getRepository(repositoryURI);
            //log.info("Acquired repository via RMI.");
            return r;
        } catch (Exception e) {
            //log.error("Error while retrieving repository using RMI: " + e);
            throw new RepositoryStubException(e.getMessage());
        }
    }

    /**
     * Return the fully qualified name of the class providing the client
     * repository. The class whose name is returned must implement the
     * {@link ClientFactoryDelegater} interface.
     *
     * @return the qfn of the factory class.
     */
    protected String getServerFactoryDelegaterClass() {
        return getClass().getName() + "$RMIClientFactoryDelegater";
    }

    /**
     * optional class for RMI, will only be used, if RMI client is present
     */
    protected static abstract class ClientFactoryDelegater {

        public abstract Repository getRepository(String uri)
                throws RemoteException, MalformedURLException, NotBoundException;
    }

    /**
     * optional class for RMI, will only be used, if RMI server is present
     */
    protected static class RMIClientFactoryDelegater extends ClientFactoryDelegater {

        // only used to enforce linking upon Class.forName()
        static String FactoryClassName = ClientRepositoryFactory.class.getName();

        public Repository getRepository(String uri) throws MalformedURLException, NotBoundException, RemoteException {
            System.setProperty("java.rmi.server.useCodebaseOnly", "true");
            return new ClientRepositoryFactory().getRepository(uri);
        }
    }
}
