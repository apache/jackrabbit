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

import javax.jcr.lock.Lock;

import junit.framework.TestCase;

import org.apache.jackrabbit.rmi.client.ClientLock;
import org.apache.jackrabbit.rmi.remote.RemoteLock;
import org.apache.jackrabbit.rmi.server.ServerLock;
import org.easymock.MockControl;

public class RemoteLockTest extends TestCase {

    private MockControl control;

    private Lock mock;
    
    private Lock lock;
    
    protected void setUp() throws Exception {
        control = MockControl.createControl(Lock.class);
        mock = (Lock) control.getMock();
        
        RemoteLock remote = new ServerLock(mock);
        lock = new ClientLock(null, remote);
    }
    
    public void testRemoteMethods() throws Exception {
        RemoteTestHelper helper = new RemoteTestHelper(Lock.class);
        helper.ignoreMethod("getNode"); // implemented locally
        helper.testRemoteMethods(lock, mock, control);
    }

}
