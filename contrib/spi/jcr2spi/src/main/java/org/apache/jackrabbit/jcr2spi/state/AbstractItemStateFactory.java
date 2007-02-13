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
package org.apache.jackrabbit.jcr2spi.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.HashSet;

/**
 * <code>AbstractItemStateFactory</code>...
 */
public abstract class AbstractItemStateFactory implements ItemStateFactory {

    private static Logger log = LoggerFactory.getLogger(AbstractItemStateFactory.class);

    private final Set creationListeners = new HashSet();

    //---------------------------------------------------< ItemStateFactory >---
    /**
     * @inheritDoc
     * @see ItemStateFactory#addCreationListener(ItemStateCreationListener)
     */
    public void addCreationListener(ItemStateCreationListener listener) {
        synchronized (creationListeners) {
            creationListeners.add(listener);
        }
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#removeCreationListener(ItemStateCreationListener)
     */
    public void removeCreationListener(ItemStateCreationListener listener) {
        synchronized (creationListeners) {
            creationListeners.remove(listener);
        }
    }

    //------------------------------------------------< private | protected >---
    /**
     *
     * @return
     */
    private ItemStateCreationListener[] getListeners() {
        synchronized (creationListeners) {
            return (ItemStateCreationListener[]) creationListeners.toArray(new ItemStateCreationListener[creationListeners.size()]);
        }
    }

    /**
     * 
     * @param createdState
     */
    void notifyCreated(ItemState createdState) {
        ItemStateCreationListener[] listeners = getListeners();
        for (int i = 0; i < listeners.length; i++) {
            // notify listeners when this item state is saved or invalidated
            createdState.addListener(listeners[i]);
            // now inform about creation
            listeners[i].created(createdState);
        }
    }
}