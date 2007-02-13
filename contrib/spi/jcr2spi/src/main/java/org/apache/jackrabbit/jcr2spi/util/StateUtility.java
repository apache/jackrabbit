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
package org.apache.jackrabbit.jcr2spi.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.jcr2spi.state.Status;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.spi.QValue;

import javax.jcr.RepositoryException;

/**
 * <code>StateUtility</code>...
 */
public class StateUtility {

    private static Logger log = LoggerFactory.getLogger(StateUtility.class);

    /**
     *
     * @param ps
     * @return
     * @throws IllegalArgumentException if the name of the PropertyState is NOT
     * {@link QName#JCR_MIXINTYPES}
     */
    public static QName[] getMixinNames(PropertyState ps) {
        if (!QName.JCR_MIXINTYPES.equals(ps.getQName())) {
            throw new IllegalArgumentException();
        }
        if (ps.getStatus() == Status.REMOVED) {
            return QName.EMPTY_ARRAY;
        } else {
            QValue[] values = ps.getValues();
            QName[] newMixins = new QName[values.length];
            for (int i = 0; i < values.length; i++) {
                try {
                    newMixins[i] = QName.valueOf(values[i].getString());
                } catch (RepositoryException e) {
                    // ignore: should never occur.
                }
            }
            return newMixins;
        }
    }


    public static boolean isUuidOrMixin(QName propName) {
        return QName.JCR_UUID.equals(propName) || QName.JCR_MIXINTYPES.equals(propName);
    }

    public static boolean isMovedState(NodeState state) {
        state.checkIsSessionState();
        if (state.isRoot()) {
            // the root state cannot be moved
            return false;
        } else {
            // a session-state is moved, if its NodeEntry is not the same as
            // the NodeEntry of its overlayed state. If no overlayedState
            // exists the state not moved anyway.
            ItemState overlayed = state.getWorkspaceState();
            if (overlayed == null) {
                return false;
            } else {
                return state.getHierarchyEntry() != overlayed.getHierarchyEntry();
                //return modState.overlayedState.getParent() != modState.getParent().overlayedState;
            }
        }
    }
}