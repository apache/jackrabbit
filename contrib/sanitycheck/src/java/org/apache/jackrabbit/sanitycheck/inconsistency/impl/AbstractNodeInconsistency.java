/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.sanitycheck.inconsistency.impl;

import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PersistenceManager;
import org.apache.jackrabbit.sanitycheck.inconsistency.NodeInconsistency;

/**
 * Superclass for all Inconsistency implementors.
 */
public abstract class AbstractNodeInconsistency implements NodeInconsistency
{
    /** Node */
    protected NodeState node;

    /** Persistence Manager */
    protected PersistenceManager persistenceManager;

    /** Persistence Manager name */
    protected String persistenceManagerName;

    public PersistenceManager getPersistenceManager()
    {
        return persistenceManager;
    }

    public void setPersistenceManager(PersistenceManager persistenceManager)
    {
        this.persistenceManager = persistenceManager;
    }

    public NodeState getNode()
    {
        return node;
    }

    public void setNode(NodeState target)
    {
        this.node = target;
    }

    public String getPersistenceManagerName()
    {
        return persistenceManagerName;
    }

    public void setPersistenceManagerName(String persistenceManagerName)
    {
        this.persistenceManagerName = persistenceManagerName;
    }
    
}
