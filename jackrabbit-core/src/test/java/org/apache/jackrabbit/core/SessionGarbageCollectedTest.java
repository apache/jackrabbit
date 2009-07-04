/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Test if sessions get garbage collected.
 */
public class SessionGarbageCollectedTest extends AbstractJCRTest {

    public void testSessionsGetGarbageCollected() throws RepositoryException {
        ArrayList<WeakReference<Session>> list = new ArrayList<WeakReference<Session>>();
        ReferenceQueue<Session> detect = new ReferenceQueue<Session>();
        Error error = null;
        try {
            for (int i = 0;; i++) {
                Session s = getHelper().getReadWriteSession();
                // eat  a lot of memory so it gets garbage collected quickly
                // (or quickly runs out of memory)
                Node n = s.getRootNode().addNode("n" + i);
                n.setProperty("x", new String(new char[1000000]));
                list.add(new WeakReference<Session>(s, detect));
                if (detect.poll() != null) {
                    break;
                }
            }
        } catch (OutOfMemoryError e) {
            error = e;
        }
        for (int i = 0; i < list.size(); i++) {
            Reference<Session> ref = list.get(i);
            Session s = ref.get();
            if (s != null) {
                s.logout();
            }
        }
        if (error != null) {
            throw error;
        }
    }

}
