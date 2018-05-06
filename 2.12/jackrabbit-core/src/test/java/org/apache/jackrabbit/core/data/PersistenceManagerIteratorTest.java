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

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.persistence.bundle.AbstractBundlePersistenceManager;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;

/**
 * Test AbstractBundlePersistenceManager.getAllNodeIds
 */
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
        Collection<WorkspaceConfig> coll = conf.getWorkspaceConfigs();
        String[] names = new String[coll.size()];
        Iterator<WorkspaceConfig> wspIt = coll.iterator();
        for (int i = 0; wspIt.hasNext(); i++) {
            WorkspaceConfig wsc = wspIt.next();
            names[i] = wsc.getName();
        }

        for (int i = 0; i < names.length && i < 1; i++) {
            Session s = getHelper().getSuperuserSession(names[i]);
            try {
                Method m = r.getClass().getDeclaredMethod("getWorkspaceInfo", new Class[] { String.class });
                m.setAccessible(true);
                Object info = m.invoke(r, names[i]);
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
                NodeId after = null;
                NodeId first = null;
                for (NodeId id : apm.getAllNodeIds(null, 0)) {
                    log("  " + id);
                    if (first == null) {
                        // initialize first node id
                        first = id;
                    }
                    if (after != null) {
                        assertFalse(id.compareTo(after) == 0);
                    }
                    after = id;
                }

                // start with first
                after = first;
                log("All nodes using batches");
                while (true) {
                    log(" bigger than: " + after);
                    Iterator<NodeId> it = apm.getAllNodeIds(after, 2).iterator();
                    if (!it.hasNext()) {
                        break;
                    }
                    while (it.hasNext()) {
                        NodeId id = it.next();
                        log("    " + id);
                        assertFalse(id.compareTo(after) == 0);
                        after = id;
                    }
                }

                log("Random access");
                for (int j = 0; j < 50; j++) {
                    after = NodeId.randomId();
                    log(" bigger than: " + after);
                    for (NodeId id : apm.getAllNodeIds(after, 2)) {
                        log("    " + id);
                        assertFalse(id.compareTo(after) == 0);
                        after = id;
                    }
                }
            } finally {
                s.logout();
            }
        }
    }

}
