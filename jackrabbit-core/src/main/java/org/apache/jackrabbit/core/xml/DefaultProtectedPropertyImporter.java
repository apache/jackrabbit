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

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.util.ReferenceChangeTracker;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.QPropertyDefinition;

/**
 * Default implementation that isn't able to handle any protected properties.
 */
public class DefaultProtectedPropertyImporter implements ProtectedPropertyImporter {

    protected JackrabbitSession session;

    protected NamePathResolver resolver;

    protected boolean isWorkspaceImport;

    protected int uuidBehavior;

    protected ReferenceChangeTracker referenceTracker;

    public DefaultProtectedPropertyImporter() {
    }

    public boolean init(JackrabbitSession session, NamePathResolver resolver,
                        boolean isWorkspaceImport,
                        int uuidBehavior, ReferenceChangeTracker referenceTracker) {
        this.session = session;
        this.resolver = resolver;
        this.isWorkspaceImport = isWorkspaceImport;
        this.uuidBehavior = uuidBehavior;
        this.referenceTracker = referenceTracker;
        return true;
    }

    /**
     * Always returns <code>false</code>.
     *
     * @see ProtectedPropertyImporter#handlePropInfo(org.apache.jackrabbit.core.NodeImpl, PropInfo, QPropertyDefinition)
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