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

import javax.jcr.Item;

import junit.framework.TestCase;

import org.apache.jackrabbit.rmi.client.ClientAdapterFactory;
import org.apache.jackrabbit.rmi.client.ClientItem;
import org.apache.jackrabbit.rmi.remote.RemoteItem;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerItem;
import org.easymock.MockControl;

public class RemoteItemTest extends TestCase {

    private MockControl control;

    private Item mock;
    
    private Item item;
    
    protected void setUp() throws Exception {
        control = MockControl.createControl(Item.class);
        mock = (Item) control.getMock();
        
        RemoteItem remote = new ServerItem(mock, new ServerAdapterFactory());
        item = new ClientItem(null, remote, new ClientAdapterFactory());
    }
    
    public void testRemoteMethods() throws Exception {
        RemoteTestHelper helper = new RemoteTestHelper(Item.class);
        helper.ignoreMethod("accept");     // implemented in subclasses
        helper.ignoreMethod("getSession"); // implemented locally
        helper.ignoreMethod("isNode");     // implemented in subclasses
        helper.ignoreMethod("isSame");     // implemented locally
        helper.testRemoteMethods(item, mock, control);
    }

}
