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
package org.apache.jackrabbit.sanitycheck.check;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.chain.impl.ContextBase;
import org.apache.jackrabbit.core.state.PersistenceManager;
import org.apache.jackrabbit.sanitycheck.inconsistency.NodeInconsistency;

/**
 * Sanity Checks Context
 */
public class SanityCheckContext extends ContextBase
{
    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3691035482775564595L;

    /** Inconsistencies key */
    private Collection inconsistencies = new ArrayList();

    /** workspace pm key */
    private PersistenceManager persistenceManager;

    /** versioning pm key */
    public PersistenceManager versioningPM;

    /** Root node uuid */
    private String rootUUID;

    /** Persistence Manager Name */
    private String persistenceManagerName;

    /**
     * Adds an inconsistency to the context
     * 
     * @param inc
     */
    public void addInconsistency(NodeInconsistency inc)
    {
        this.getInconsistencies().add(inc);
    }

    /**
     * Set versioning PM
     * 
     * @param pm
     */
    public void setVersioningPM(PersistenceManager pm)
    {
        this.versioningPM = pm;
    }

    /**
     * Set workspace PM
     * 
     * @param pm
     */
    public void setPersistenceManager(PersistenceManager pm)
    {
        this.persistenceManager = pm;
    }

    /**
     * @return inconsistencies
     */
    public Collection getInconsistencies()
    {
        return inconsistencies;
    }

    /**
     * Gets the persistence manager to check
     * 
     * @return
     */
    public PersistenceManager getPersistenceManager()
    {
        return persistenceManager;
    }

    /**
     * Gets the persistence manager to check
     * 
     * @return
     */
    public PersistenceManager getVersioningPersistenceManager()
    {
        return versioningPM;
    }

    /**
     * Get the root node uuid
     * 
     * @return
     */
    public String getRootUUID()
    {
        return rootUUID;
    }

    /**
     * Sets
     * 
     * @return
     */
    public void setRootUUID(String rootUUID)
    {
        this.rootUUID = rootUUID;
    }

    /**
     * @return PersistenceManager name
     */
    public String getPersistenceManagerName()
    {
        return persistenceManagerName;
    }

    /**
     * Sets the PersistenceManager name
     * @param persistenceManagerName
     */
    public void setPersistenceManagerName(String persistenceManagerName)
    {
        this.persistenceManagerName = persistenceManagerName;
    }
}
