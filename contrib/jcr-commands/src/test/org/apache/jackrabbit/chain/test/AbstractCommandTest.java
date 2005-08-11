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

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import junit.framework.TestCase;

import org.apache.commons.chain.Catalog;
import org.apache.commons.chain.Context;
import org.apache.commons.chain.config.ConfigParser;
import org.apache.commons.chain.impl.CatalogFactoryBase;
import org.apache.commons.chain.impl.ContextBase;
import org.apache.jackrabbit.chain.CtxHelper;
import org.apache.jackrabbit.chain.JcrCommandException;
import org.apache.jackrabbit.chain.command.ClearWorkspace;
import org.apache.jackrabbit.chain.command.Login;
import org.apache.jackrabbit.chain.command.Logout;
import org.apache.jackrabbit.chain.command.StartOrGetJackrabbitSingleton;

/**
 * Commands Test superclass
 */
public abstract class AbstractCommandTest extends TestCase
{
    /** config */
    private static String CONFIG = "applications/test/repository.xml";

    /** home */
    private static String HOME = "applications/test/repository";

    static
    {
        try
        {
            ConfigParser parser = new ConfigParser();
            parser.parse(AbstractCommandTest.class.getResource("chains.xml"));
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /** clear workspace */
    private ClearWorkspace cw = new ClearWorkspace();

    /** Context */
    protected Context ctx = new ContextBase();

    /** catalog */
    protected Catalog catalog = CatalogFactoryBase.getInstance().getCatalog("test");

    protected void setUp() throws Exception
    {
        super.setUp();

        // Start
        StartOrGetJackrabbitSingleton startCmd = new StartOrGetJackrabbitSingleton();
        startCmd.setConfig(CONFIG);
        startCmd.setHome(HOME);
        startCmd.execute(ctx);
        assertTrue(CtxHelper.getRepository(ctx) instanceof Repository);

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
    

    protected void addTestNode() throws Exception
    {
        catalog.getCommand("addTestNode").execute(ctx);
    }    

    protected void tearDown() throws Exception
    {
        super.tearDown();

        cw.execute(ctx);

        // Logout
        Logout logoutCmd = new Logout();
        logoutCmd.execute(ctx);

        ctx.clear();
    }
    
    protected Node getRoot() throws PathNotFoundException, JcrCommandException, RepositoryException {
        return CtxHelper.getNode(ctx, "/") ;
    }
    
    

}
