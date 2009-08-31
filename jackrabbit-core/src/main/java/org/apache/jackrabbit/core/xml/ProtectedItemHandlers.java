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
package org.apache.jackrabbit.core.xml;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.state.NodeState;

/**
 * Provides a registry like helper class for the {@link ProtectedPropertyImporter}
 * and {@link ProtectedNodeImporter} classes.
 */
public class ProtectedItemHandlers implements ProtectedPropertyImporter {

    private final List<ProtectedNodeImporter> nodeImporters = new ArrayList<ProtectedNodeImporter>();

    private final List<ProtectedPropertyImporter> propImporters = new ArrayList<ProtectedPropertyImporter>();

    public void register(ProtectedNodeImporter ni) {
        nodeImporters.add(ni);
    }

    public void register(ProtectedPropertyImporter pi) {
        propImporters.add(pi);
    }

    /**
     * Selects the node importer that can handle the give parent by
     * invoking it's {@link ProtectedNodeImporter#start(NodeImpl)} method.

     * @param protectedParent the parent node
     * @return the importer if it handles the node
     * @throws IllegalStateException if an error occurs
     * @throws RepositoryException if an error occurs
     */
    public ProtectedNodeImporter accept(NodeImpl protectedParent)
            throws IllegalStateException, RepositoryException {
        for (ProtectedNodeImporter ni: nodeImporters) {
            if (ni.start(protectedParent)) {
                return ni;
            }
        }
        return null;
    }

    /**
     * Selects the node importer that can handle the give parent by
     * invoking it's {@link ProtectedNodeImporter#start(NodeState)} method.

     * @param protectedParent the parent node
     * @return the importer if it handles the node
     * @throws IllegalStateException if an error occurs
     * @throws RepositoryException if an error occurs
     */
    public ProtectedNodeImporter accept(NodeState protectedParent)
            throws IllegalStateException, RepositoryException {
        for (ProtectedNodeImporter ni: nodeImporters) {
            if (ni.start(protectedParent)) {
                return ni;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * Selects the property importer from the configured ones that can handle
     * the prop info.
     */
    public boolean handlePropInfo(NodeImpl parent, PropInfo protectedPropInfo,
                                  PropDef def)
            throws RepositoryException {
        for (ProtectedPropertyImporter pi: propImporters) {
            if (pi.handlePropInfo(parent, protectedPropInfo, def)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * Selects the property importer from the configured ones that can handle
     * the prop info.
     */
    public boolean handlePropInfo(NodeState parent, PropInfo protectedPropInfo,
                                  PropDef def)
            throws RepositoryException {
        for (ProtectedPropertyImporter pi: propImporters) {
            if (pi.handlePropInfo(parent, protectedPropInfo, def)) {
                return true;
            }
        }
        return false;
    }
}