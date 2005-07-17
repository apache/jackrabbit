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

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.Repository;

import junit.framework.TestCase;

import org.apache.commons.chain.Context;
import org.apache.commons.chain.impl.ContextBase;
import org.apache.jackrabbit.chain.ContextHelper;
import org.apache.jackrabbit.chain.command.AddNode;
import org.apache.jackrabbit.chain.command.ClearWorkspace;
import org.apache.jackrabbit.chain.command.CollectChildren;
import org.apache.jackrabbit.chain.command.CurrentNode;
import org.apache.jackrabbit.chain.command.Login;
import org.apache.jackrabbit.chain.command.Logout;
import org.apache.jackrabbit.chain.command.RemoveNode;
import org.apache.jackrabbit.chain.command.SaveSession;
import org.apache.jackrabbit.chain.command.StartOrGetJackrabbitSingleton;

/**
 * Chain testing
 */
public class JcrChainTest extends TestCase
{
    private static String CONFIG = "applications/test/repository.xml";

    private static String HOME = "applications/test/repository";

    Context ctx = new ContextBase();

    public void testChain() throws Exception
    {

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
        SaveSession saveCmd = new SaveSession() ;
        saveCmd.execute(ctx);
        
        // Logout
        Logout logoutCmd = new Logout() ;
        logoutCmd.execute(ctx);
        
        // See persisted changes
        Login loginCmd = new Login();
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
        
        saveCmd.execute(ctx);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
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
        
    }
    
    protected void tearDown() throws Exception
    {
        super.tearDown();
        ClearWorkspace cw = new ClearWorkspace() ;
        cw.execute(ctx) ;
        
        // Logout
        Logout logoutCmd = new Logout() ;
        logoutCmd.execute(ctx);
        
    }
    
    public void testTraverse() throws Exception
    {
        String target = "iterator" ;
        
        CollectChildren traverse = new CollectChildren() ;
        traverse.setDepth(-1) ;
        traverse.setTarget(target);
        traverse.execute(ctx) ;
        
        Iterator iter = (Iterator) ctx.get(target) ;
        while(iter.hasNext()) {
            Node node = (Node) iter.next() ;
            System.out.println(node.getPath()) ;
        }
        System.out.println("------------------ ") ;
        traverse = new CollectChildren() ;
        traverse.setDepth(2) ;
        traverse.setTarget(target);
        traverse.execute(ctx) ;
        
        iter = (Iterator) ctx.get(target) ;
        while(iter.hasNext()) {
            Node node = (Node) iter.next() ;
            System.out.println(node.getPath()) ;
        }
        
    }
    
}
