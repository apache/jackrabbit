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

import javax.jcr.Session;

import junit.framework.TestCase;

import org.apache.jackrabbit.rmi.client.ClientAdapterFactory;
import org.apache.jackrabbit.rmi.client.ClientSession;
import org.apache.jackrabbit.rmi.remote.RemoteSession;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerSession;
import org.easymock.MockControl;

public class RemoteSessionTest extends TestCase {

    private MockControl control;

    private Session mock;
    
    private Session session;
    
    protected void setUp() throws Exception {
        control = MockControl.createControl(Session.class);
        mock = (Session) control.getMock();
        
        RemoteSession remote = 
            new ServerSession(mock, new ServerAdapterFactory());
        session = new ClientSession(null, remote, new ClientAdapterFactory());
    }
    
    public void testRemoteMethods() throws Exception {
        RemoteTestHelper helper = new RemoteTestHelper(Session.class);
        helper.ignoreMethod("getRepository");           // implemented locally
        helper.ignoreMethod("importXML");               // wrapped stream
        helper.ignoreMethod("getImportContentHandler"); // implemented locally
        helper.ignoreMethod("exportSysView");           // multiple methods
        helper.ignoreMethod("exportDocView");           // multiple method
        helper.testRemoteMethods(session, mock, control);
    }

}
