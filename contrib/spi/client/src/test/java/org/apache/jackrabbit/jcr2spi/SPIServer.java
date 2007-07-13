/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.rmi.remote.RemoteRepositoryService;
import org.apache.jackrabbit.spi.rmi.server.ServerRepositoryService;
import org.apache.jackrabbit.spi2jcr.RepositoryServiceImpl;
import org.apache.jackrabbit.spi2jcr.BatchReadConfig;
import org.apache.log4j.PropertyConfigurator;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;

/**
 * <code>SPIServer</code> starts a jackrabbit repository and wraps it with
 * a SPI2JCR layer and finally makes it available over RMI.
 */
public class SPIServer {

    private static final String REPO_CONFIG = "src/test/resources/repository.xml";

    private static final String REPO_HOME = "target/repo-home";

    private final org.apache.jackrabbit.core.RepositoryImpl repo;

    private final RemoteRepositoryService remoteService;

    private final Registry reg;

    static {
        PropertyConfigurator.configure(SPIServer.class.getClassLoader().getResource("log4j.properties"));
    }

    private SPIServer() throws Exception {
        // start a jackrabbit repository
        System.out.println("Starting Jackrabbit...");
        RepositoryConfig config = RepositoryConfig.create(REPO_CONFIG, REPO_HOME);
        repo = org.apache.jackrabbit.core.RepositoryImpl.create(config);
        System.out.println("Jackrabbit started");
        // try to setup test data
        Session s = repo.login(new SimpleCredentials("user", "pass".toCharArray()));
        try {
            RepositorySetup.run(s);
        } finally {
            s.logout();
        }
        // wrap with spi2jcr
        // TODO: make BatchReadConfig configurable
        RepositoryService repoService = new RepositoryServiceImpl(repo, new BatchReadConfig());
        System.out.println("Wrapped with SPI2JCR");
        // create spi server
        remoteService = new ServerRepositoryService(repoService);
        System.out.println("Wrapped with SPI-RMI");
        // register server
        reg = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        reg.bind("spi-server", remoteService);
        System.out.println("Bound to RMI Registry");
    }

    private void shutdown() {
        System.out.println("Shutting down...");
        try {
            reg.unbind("spi-server");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
        repo.shutdown();
        System.out.println("Jackrabbit stopped");
    }

    public static void main(String[] args) {
        try {
            SPIServer server = new SPIServer();
            // wait for console input, then terminate
            while (System.in.read() != 'q') {
                // read again
            }
            server.shutdown();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
