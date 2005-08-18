/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.apache.jackrabbit.chain.test;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.jcr.Repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.chain.CtxHelper;
import org.apache.jackrabbit.chain.command.ClearWorkspace;
import org.apache.jackrabbit.chain.command.ConnectToRmiServer;
import org.apache.jackrabbit.chain.command.Login;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.rmi.remote.RemoteRepository;
import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;

/**
 * Commands tests
 */
public class RemoteCommandsTest extends CommandsTest
{
    private static Log log = LogFactory.getLog(RemoteCommandsTest.class);

    /** rmi url */
    private static String URL = "jcr/repository";

    /** registry */
    private static Registry REGISTRY;

    /** clear workspace */
    private ClearWorkspace cw = new ClearWorkspace();

    /**
     * override setup. creates a connection to a remote repository.
     */
    protected void setUp() throws Exception
    {
        // create registry & bind jcr server
        if (REGISTRY == null)
        {
            RepositoryConfig conf = RepositoryConfig.create(CONFIG, HOME);
            Repository repo = RepositoryImpl.create(conf);
            RemoteAdapterFactory factory = new ServerAdapterFactory();
            RemoteRepository remote = factory.getRemoteRepository(repo);
            REGISTRY = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
            REGISTRY.bind(URL, remote);
            // give jackrabbit some time to start
            Thread.sleep(5000);
            log.info("rmi server started");
        }
        ConnectToRmiServer cmd = new ConnectToRmiServer();
        cmd.setUrl(URL);
        cmd.execute(ctx);
        assertTrue(CtxHelper.getRepository(ctx) != null);
        
        // Login
        Login loginCmd = new Login();
        loginCmd.setUser("user");
        loginCmd.setPassword("password");
        loginCmd.execute(ctx);
        assertTrue(CtxHelper.getSession(ctx) != null);
        assertTrue(CtxHelper.getCurrentNode(ctx).getPath().equals("/"));
        
        // clear workspace
        cw.execute(ctx);
    }

}
