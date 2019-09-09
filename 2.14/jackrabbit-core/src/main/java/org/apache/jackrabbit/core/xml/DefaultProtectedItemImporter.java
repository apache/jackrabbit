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

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.util.ReferenceChangeTracker;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

import javax.jcr.RepositoryException;
import java.util.List;

/**
 * <code>DefaultProtectedItemImporter</code>...
 */
public class DefaultProtectedItemImporter implements ProtectedPropertyImporter, ProtectedNodeImporter {

    protected JackrabbitSession session;
    protected NamePathResolver resolver;
    protected boolean isWorkspaceImport;
    protected int uuidBehavior;
    protected ReferenceChangeTracker referenceTracker;

    public DefaultProtectedItemImporter() {
    }

    //----------------------------------------------< ProtectedItemImporter >---
    /**
     * @see ProtectedItemImporter#init(org.apache.jackrabbit.api.JackrabbitSession, org.apache.jackrabbit.spi.commons.conversion.NamePathResolver, boolean, int, org.apache.jackrabbit.core.util.ReferenceChangeTracker)
     */
    public boolean init(JackrabbitSession session, NamePathResolver resolver, boolean isWorkspaceImport, int uuidBehavior, ReferenceChangeTracker referenceTracker) {
        this.session = session;
        this.resolver = resolver;
        this.isWorkspaceImport = isWorkspaceImport;
        this.uuidBehavior = uuidBehavior;
        this.referenceTracker = referenceTracker;
        return true;
    }

    //----------------------------------------------< ProtectedNodeImporter >---
    /**
     * Always returns <code>false</code>.
     *
     * @see ProtectedNodeImporter#start(org.apache.jackrabbit.core.NodeImpl)
     */
    public boolean start(NodeImpl protectedParent) throws RepositoryException {
        return false;
    }

    /**
     * Always returns <code>false</code>.
     *
     * @see ProtectedNodeImporter#start(org.apache.jackrabbit.core.state.NodeState)
     */
    public boolean start(NodeState protectedParent) throws RepositoryException {
        return false;
    }

    /**
     * Does nothing.
     *
     * @see ProtectedNodeImporter#end(NodeImpl)
     */
    public void end(NodeImpl protectedParent) throws RepositoryException {
    }

    /**
     * Does nothing.
     *
     * @see ProtectedNodeImporter#end(NodeState)
     */
    public void end(NodeState protectedParent) throws RepositoryException {
    }

    /**
     * Does nothing.
     *
     * @see ProtectedNodeImporter#startChildInfo(NodeInfo, java.util.List)
     */
    public void startChildInfo(NodeInfo childInfo, List<PropInfo> propInfos) throws RepositoryException {
    }

    /**
     * Does nothing.
     *
     * @see ProtectedNodeImporter#endChildInfo()
     */
    public void endChildInfo() throws RepositoryException {
    }



    //------------------------------------------< ProtectedPropertyImporter >---
    /**
     * Always returns <code>false</code>.
     *
     * @see ProtectedPropertyImporter#handlePropInfo(org.apache.jackrabbit.core.NodeImpl, PropInfo, org.apache.jackrabbit.spi.QPropertyDefinition)
     */
    public boolean handlePropInfo(NodeImpl parent, PropInfo protectedPropInfo, QPropertyDefinition def) throws RepositoryException {
        return false;
    }

    /**
     * Always returns <code>false</code>.
     *
     * @see ProtectedPropertyImporter#handlePropInfo(org.apache.jackrabbit.core.state.NodeState, PropInfo, QPropertyDefinition)
     */
    public boolean handlePropInfo(NodeState parent, PropInfo protectedPropInfo, QPropertyDefinition def) throws RepositoryException {
        return false;
    }

    /**
     * Always returns <code>false</code>.
     *
     * @see ProtectedPropertyImporter#processReferences()
     */
    public void processReferences() throws RepositoryException {
    }
}