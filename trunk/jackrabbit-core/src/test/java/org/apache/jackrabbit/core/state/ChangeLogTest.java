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
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.spi.NameFactory;

/**
 * <code>ChangeLogTest</code> contains the test cases for the methods
 * inside {@link org.apache.jackrabbit.core.state.ChangeLog}.
 */
public class ChangeLogTest extends AbstractJCRTest {

    private NameFactory factory;

    protected void setUp() throws Exception {
        super.setUp();
        factory = NameFactoryImpl.getInstance();
    }

    /**
     * Add an item state and then delete it. Make sure there is no
     * entry in either the added nor the removed states
     */
    public void testAddDelete() throws Exception {
        PropertyId id = new PropertyId(NodeId.randomId(), factory.create("", "a"));
        ItemState state = new PropertyState(id, ItemState.STATUS_NEW, false);

        ChangeLog log = new ChangeLog();

        log.added(state);
        log.deleted(state);

        assertFalse("State not in added collection",
                log.addedStates().iterator().hasNext());
        assertFalse("State not in deleted collection",
                log.deletedStates().iterator().hasNext());
    }

    /**
     * Add an item state and then modify it. Make sure the entry is still
     * in the added states.
     */
    public void testAddModify() throws Exception {
        PropertyId id = new PropertyId(NodeId.randomId(), factory.create("", "a"));
        ItemState state = new PropertyState(id, ItemState.STATUS_NEW, false);

        ChangeLog log = new ChangeLog();

        log.added(state);
        log.modified(state);

        assertTrue("State still in added collection",
                log.addedStates().iterator().hasNext());
        assertFalse("State not in modified collection",
                log.modifiedStates().iterator().hasNext());
    }

    /**
     * Add some item states. Retrieve them again and make sure the order is
     * preserved.
     */
    public void testPreserveOrder() throws Exception {
        ItemState[] states = new ItemState[10];
        for (int i = 0; i < states.length; i++) {
            PropertyId id = new PropertyId(NodeId.randomId(), factory.create("", "a" + i));
            states[i] = new PropertyState(id, ItemState.STATUS_NEW, false);
        }

        ChangeLog log = new ChangeLog();

        for (int i = 0; i < states.length; i++) {
            log.added(states[i]);
        }

        int i = 0;
        for (ItemState state : log.addedStates()) {
            assertTrue("Added states preserve order.",
                    state.equals(states[i++]));
        }
    }
}
