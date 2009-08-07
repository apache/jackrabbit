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
package org.apache.jackrabbit.core.data;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeIdIterator;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.persistence.bundle.AbstractBundlePersistenceManager;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.uuid.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;

public class PersistenceManagerIteratorTest extends AbstractJCRTest {
    /** logger instance */
    private static final Logger LOG = LoggerFactory.getLogger(PersistenceManagerIteratorTest.class);

    private void log(String s) {
        // System.out.println(s);
        LOG.info(s);
    }

    public void testGetAllNodeIds() throws Exception {
        Node root = testRootNode;
        Session session = root.getSession();
        Repository rep = session.getRepository();

        if (!(rep instanceof RepositoryImpl)) {
            log("Test skipped. Required repository class: "
                    + RepositoryImpl.class + " got: " + rep.getClass());
            return;
        }

        RepositoryImpl r = (RepositoryImpl) rep;
        RepositoryConfig conf = r.getConfig();
        Collection coll = conf.getWorkspaceConfigs();
        String[] names = new String[coll.size()];
        Iterator wspIt = coll.iterator();
        for(int i = 0; wspIt.hasNext(); i++) {
            WorkspaceConfig wsc = (WorkspaceConfig) wspIt.next();
            names[i] = wsc.getName();
        }

        for (int i = 0; i < names.length; i++) {
            Session s = helper.getSuperuserSession(names[i]);
            try {
                Method m = r.getClass().getDeclaredMethod("getWorkspaceInfo", new Class[] { String.class });
                m.setAccessible(true);
                Object info = m.invoke(r, new String[] { names[i] });
                m = info.getClass().getDeclaredMethod("getPersistenceManager", new Class[0]);
                m.setAccessible(true);
                PersistenceManager pm = (PersistenceManager) m.invoke(info, new Object[0]);
                if (!(pm instanceof AbstractBundlePersistenceManager)) {
                    log("PM skipped: " + pm.getClass());
                    continue;
                }
                AbstractBundlePersistenceManager apm = (AbstractBundlePersistenceManager) pm;
                log("PM: " + pm.getClass().getName());

                log("All nodes in one step");
                NodeIdIterator it = apm.getAllNodeIds(null, 0);
                NodeId after = null;
                NodeId first = null;
                while (it.hasNext()) {
                    NodeId id = it.nextNodeId();
                    log("  " + id.toString());
                    if (first == null) {
                        // initialize first node id
                        first = id;
                    }
                    if (after != null) {
                        assertFalse(id.getUUID().compareTo(after.getUUID()) == 0);
                    }
                    after = id;
                }

                // start with first
                after = first;
                log("All nodes using batches");
                while (true) {
                    log(" bigger than: " + after);
                    it = apm.getAllNodeIds(after, 2);
                    if (!it.hasNext()) {
                        break;
                    }
                    while (it.hasNext()) {
                        NodeId id = it.nextNodeId();
                        log("    " + id.toString());
                        assertFalse(id.getUUID().compareTo(after.getUUID()) == 0);
                        after = id;
                    }
                }

                log("Random access");
                for (int j = 0; j < 50; j++) {
                    after = new NodeId(UUID.randomUUID());
                    log(" bigger than: " + after);
                    it = apm.getAllNodeIds(after, 2);
                    while (it.hasNext()) {
                        NodeId id = it.nextNodeId();
                        log("    " + id.toString());
                        assertFalse(id.getUUID().compareTo(after.getUUID()) == 0);
                        after = id;
                    }
                }
            } finally {
                s.logout();
            }
        }
    }

}
