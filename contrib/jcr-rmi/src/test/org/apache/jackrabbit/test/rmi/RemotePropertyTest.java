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

import javax.jcr.Property;

import junit.framework.TestCase;

import org.apache.jackrabbit.rmi.client.ClientAdapterFactory;
import org.apache.jackrabbit.rmi.client.ClientProperty;
import org.apache.jackrabbit.rmi.remote.RemoteProperty;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerProperty;
import org.easymock.MockControl;

public class RemotePropertyTest extends TestCase {

    private MockControl control;

    private Property mock;
    
    private Property property;
    
    protected void setUp() throws Exception {
        control = MockControl.createControl(Property.class);
        mock = (Property) control.getMock();
        
        RemoteProperty remote =
            new ServerProperty(mock, new ServerAdapterFactory());
        property = new ClientProperty(null, remote, new ClientAdapterFactory());
    }
    
    public void testRemoteMethods() throws Exception {
        RemoteTestHelper helper = new RemoteTestHelper(Property.class);
        helper.ignoreMethod("getBoolean");  // implemented locally
        helper.ignoreMethod("getLong");     // implemented locally
        helper.ignoreMethod("getDouble");   // implemented locally
        helper.ignoreMethod("getDate");     // implemented locally
        helper.ignoreMethod("getString");   // implemented locally
        helper.ignoreMethod("getStream");   // implemented locally
        helper.ignoreMethod("getNode");     // implemented locally
        helper.ignoreMethod("setValue");    // multiple methods
        helper.testRemoteMethods(property, mock, control);
    }

}
