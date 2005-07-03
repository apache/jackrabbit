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

import javax.jcr.Repository;

import org.apache.commons.chain.Context;
import org.apache.commons.chain.impl.ContextBase;
import org.apache.jackrabbit.chain.ContextHelper;
import org.apache.jackrabbit.chain.command.AddNode;
import org.apache.jackrabbit.chain.command.CurrentNode;
import org.apache.jackrabbit.chain.command.Login;
import org.apache.jackrabbit.chain.command.Logout;
import org.apache.jackrabbit.chain.command.RemoveNode;
import org.apache.jackrabbit.chain.command.Save;
import org.apache.jackrabbit.chain.command.StopJackrabbit;
import org.apache.jackrabbit.chain.command.StartOrGetJackrabbitSingleton;

import junit.framework.TestCase;

/**
 * Chain testing
 */
public class JcrChainTest extends TestCase
{
    private static String CONFIG = "applications/test/repository.xml";

    private static String HOME = "applications/test";

    public void testChain() throws Exception
    {
        Context ctx = new ContextBase();

        // Start
        StartOrGetJackrabbitSingleton startCmd = new StartOrGetJackrabbitSingleton();
        startCmd.setConfig(CONFIG);
        startCmd.setHome(HOME);
        startCmd.execute(ctx);
        assertTrue(ContextHelper.getRepository(ctx) instanceof Repository);

        // Login
        Login loginCmd = new Login();
        loginCmd.setUser("user");
        loginCmd.setPassword("password");
        loginCmd.execute(ctx);
        assertTrue(ContextHelper.getSession(ctx) != null);
        assertTrue(ContextHelper.getCurrentNode(ctx).getPath().equals("/"));

        String testNodeStr = "test";

        // Add node
        AddNode addNodeCmd = new AddNode();
        addNodeCmd.setName(testNodeStr);
        addNodeCmd.execute(ctx);
        assertTrue(ContextHelper.getCurrentNode(ctx).hasNode(testNodeStr));

        // Current node
        CurrentNode cnCmd = new CurrentNode();
        cnCmd.setPath("/" + testNodeStr);
        cnCmd.execute(ctx);
        assertTrue(ContextHelper.getCurrentNode(ctx).getPath().equals(
            "/" + testNodeStr));

        // Save changes
        Save saveCmd = new Save() ;
        saveCmd.execute(ctx);
        
        // Logout
        Logout logoutCmd = new Logout() ;
        logoutCmd.execute(ctx);
        
        // See persisted changes
        loginCmd = new Login();
        loginCmd.setUser("user2");
        loginCmd.setPassword("password");
        loginCmd.execute(ctx);
        assertTrue(ContextHelper.getCurrentNode(ctx).hasNode(testNodeStr));

        // Change current node 
        cnCmd.execute(ctx);
        
        // Remove node
        RemoveNode removeNodeCmd = new RemoveNode();
        removeNodeCmd.execute(ctx);
        assertFalse(ContextHelper.getCurrentNode(ctx).hasNode(testNodeStr));
        
        logoutCmd = new Logout() ;
        logoutCmd.execute(ctx);

        // Stop
        StopJackrabbit stopCmd = new StopJackrabbit();
        stopCmd.execute(ctx);
    }

}
