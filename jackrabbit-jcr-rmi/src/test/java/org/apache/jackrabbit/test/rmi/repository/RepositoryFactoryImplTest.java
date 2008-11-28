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
package org.apache.jackrabbit.test.rmi.repository;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.jcr.Repository;
import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.jcr.RepositoryException;
import javax.imageio.spi.ServiceRegistry;

import org.apache.jackrabbit.api.jsr283.RepositoryFactory;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryFactoryImpl;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;
import org.apache.jackrabbit.rmi.jackrabbit.JackrabbitServerAdapterFactory;

import junit.framework.TestCase;

/**
 * <code>RepositoryFactoryImplTest</code>...
 */
public class RepositoryFactoryImplTest extends TestCase {

    private static final Credentials CREDENTIALS
            = new SimpleCredentials("user", "pass".toCharArray());

    private static final File TARGET = new File("target");

    private static final File REPO_HOME = new File(TARGET, "repository");

    private static final File REPO_CONF = new File(REPO_HOME, "repository.xml");

    private static final String RMI_URL = "rmi://localhost/repository";

    static {
        REPO_HOME.mkdirs();
        if (!REPO_CONF.exists()) {
            try {
                // get default configuration from jackrabbit-core
                InputStream in = RepositoryImpl.class.getResourceAsStream("repository.xml");
                try {
                    OutputStream out = new FileOutputStream(REPO_CONF);
                    try {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = in.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                        }
                    } finally {
                        out.close();
                    }
                } finally {
                    in.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // try to create a registry
        try {
            LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        } catch (RemoteException e) {
            // ignore
        }
    }

    private Repository repository;

    protected void setUp() throws Exception {
        super.setUp();
        // get a local repository
        Map params = new HashMap();
        params.put(RepositoryFactoryImpl.REPOSITORY_CONF, REPO_CONF.getAbsolutePath());
        params.put(RepositoryFactoryImpl.REPOSITORY_HOME, REPO_HOME.getAbsolutePath());
        repository = RepositoryManager.getRepository(params);
        
    }

    protected void tearDown() throws Exception {
        // shutdown local repository
        if (repository instanceof JackrabbitRepository) {
            ((JackrabbitRepository) repository).shutdown();
        }
        super.tearDown();
    }

    public void testConnect() throws Exception {
        // setup remote repository
        RemoteAdapterFactory raf = new JackrabbitServerAdapterFactory();
        Naming.bind(RMI_URL, raf.getRemoteRepository(repository));

        Map params = new HashMap();
        params.put(org.apache.jackrabbit.rmi.repository.RepositoryFactoryImpl.REPOSITORY_RMI_URL, RMI_URL);
        Repository r = RepositoryManager.getRepository(params);
        r.login(CREDENTIALS).logout();
    }

    private static final class RepositoryManager {

        private static Repository getRepository(Map parameters)
                throws RepositoryException {
            Repository repo = null;
            Iterator factories = ServiceRegistry.lookupProviders(RepositoryFactory.class);
            while (factories.hasNext()) {
                RepositoryFactory factory = (RepositoryFactory) factories.next();
                repo = factory.getRepository(parameters);
                if (repo != null) {
                    break;
                }
            }
            return repo;
        }
    }
}
