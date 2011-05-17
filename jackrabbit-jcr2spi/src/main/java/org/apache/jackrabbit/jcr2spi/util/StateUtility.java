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

import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.jcr2spi.state.Status;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

import javax.jcr.RepositoryException;

/**
 * <code>StateUtility</code>...
 */
public final class StateUtility {

    /**
     * Avoid instantiation
     */
    private StateUtility() {}

    /**
     *
     * @param ps
     * @return
     * @throws IllegalArgumentException if the name of the PropertyState is NOT
     * {@link NameConstants#JCR_MIXINTYPES}
     */
    public static Name[] getMixinNames(PropertyState ps) {
        if (!NameConstants.JCR_MIXINTYPES.equals(ps.getName())) {
            throw new IllegalArgumentException();
        }
        if (ps.getStatus() == Status.REMOVED) {
            return Name.EMPTY_ARRAY;
        } else {
            QValue[] values = ps.getValues();
            Name[] newMixins = new Name[values.length];
            for (int i = 0; i < values.length; i++) {
                try {
                    newMixins[i] = values[i].getName();
                } catch (RepositoryException e) {
                    // ignore: should never occur.
                }
            }
            return newMixins;
        }
    }

    public static Name getPrimaryTypeName(PropertyState ps) throws RepositoryException {
        if (!NameConstants.JCR_PRIMARYTYPE.equals(ps.getName())) {
            throw new IllegalArgumentException();
        }
        QValue[] values = ps.getValues();
        return values[0].getName();
    }

    public static boolean isUuidOrMixin(Name propName) {
        return NameConstants.JCR_UUID.equals(propName) || NameConstants.JCR_MIXINTYPES.equals(propName);
    }

    public static boolean isMovedState(NodeState state) {
        if (state.isRoot()) {
            // the root state cannot be moved
            return false;
        } else {
            NodeEntry ne = state.getNodeEntry();
            return ne.isTransientlyMoved();
        }
    }
}