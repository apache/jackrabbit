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
package org.apache.jackrabbit.test.rmi;

import javax.jcr.Node;

import junit.framework.TestCase;

import org.apache.jackrabbit.rmi.client.ClientAdapterFactory;
import org.apache.jackrabbit.rmi.client.ClientNode;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerNode;
import org.easymock.MockControl;

public class RemoteNodeTest extends TestCase {

    private MockControl control;

    private Node mock;
    
    private Node node;
    
    protected void setUp() throws Exception {
        control = MockControl.createControl(Node.class);
        mock = (Node) control.getMock();
        
        RemoteNode remote = new ServerNode(mock, new ServerAdapterFactory());
        node = new ClientNode(null, remote, new ClientAdapterFactory());
    }
    
    public void testRemoteMethods() throws Exception {
        RemoteTestHelper helper = new RemoteTestHelper(Node.class);
        helper.ignoreMethod("cancelMerge");              // TODO
        helper.ignoreMethod("doneMerge");                // TODO
        helper.ignoreMethod("checkin");                  // TODO
        helper.ignoreMethod("restore");                  // multiple methods
        helper.ignoreMethod("getVersionHistory");        // TODO
        helper.ignoreMethod("getBaseVersion");           // TODO
        helper.ignoreMethod("setProperty");              // multiple methods
        helper.testRemoteMethods(node, mock, control);
    }

}
