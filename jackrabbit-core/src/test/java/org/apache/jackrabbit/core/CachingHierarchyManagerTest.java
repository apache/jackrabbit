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

import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.uuid.UUID;

import junit.framework.TestCase;

public class CachingHierarchyManagerTest extends TestCase {

    volatile Exception exception;
    volatile boolean stop;
    CachingHierarchyManager cache;

    public void testResolveNodePath() throws Exception {
        NodeId rootNodeId = new NodeId(UUID.randomUUID());
        ItemStateManager provider = new MyItemStateManager();
        cache = new CachingHierarchyManager(rootNodeId, provider, null);
        PathFactory factory = PathFactoryImpl.getInstance();
        final Path path = factory.create("{}\t{}");
        for(int i=0; i<3; i++) {
            new Thread(new Runnable() {
                public void run() {
                    while(!stop) {
                        try {
                            cache.resolveNodePath(path);
                        } catch(Exception e) {
                            exception = e;
                        }
                    }
                }
            }).start();
        }
        Thread.sleep(1000);
        stop = true;
        if(exception != null) {
            throw exception;
        }
    }

    static class MyItemStateManager implements ItemStateManager {

        public ItemState getItemState(ItemId id) throws NoSuchItemStateException, ItemStateException {
            Name name = NameFactoryImpl.getInstance().create("", "");
            NodeState ns = new NodeState((NodeId)id, name, null, NodeState.STATUS_NEW, false);
            ns.setDefinitionId(NodeDefId.valueOf("1"));
            return ns;
        }

        public NodeReferences getNodeReferences(NodeReferencesId id) throws NoSuchItemStateException, ItemStateException {
            return null;
        }

        public boolean hasItemState(ItemId id) {
            return false;
        }

        public boolean hasNodeReferences(NodeReferencesId id) {
            return false;
        }

    };

}
