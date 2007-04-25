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
package org.apache.portals.graffito.jcr.spring;

import java.util.Collection;

import org.apache.portals.graffito.jcr.query.Query;
import org.springframework.dao.DataAccessException;

/**
 * Interface that specifies a basic set of JCR mapping operations. Not often used, but 
 * a useful option to enhance testability, as it can easily be mocked or stubbed.
 * 
 * <p>
 * Provides JcrMappingTemplate's data access methods that mirror various PersistenceManager
 * methods. See the required javadocs for details on those methods.
 * 
 * @author Costin Leau
 *
 */
public interface JcrMappingOperations {

    /**
     * Execute a JcrMappingCallback.
     * 
     * @param callback callback to execute
     * @return the callback result
     * @throws DataAccessException
     */
    public Object execute(JcrMappingCallback callback) throws DataAccessException;

    /**
     * @see org.apache.portals.graffito.jcr.persistence.PersistenceManager#insert(java.lang.String, java.lang.Object)
     */
    public void insert( final java.lang.Object object);

    /**
     * @see org.apache.portals.graffito.jcr.persistence.PersistenceManager#update(java.lang.String, java.lang.Object)
     */
    public void update( final java.lang.Object object);

    /**
     * @see org.apache.portals.graffito.jcr.persistence.PersistenceManager#remove(java.lang.String)
     */
    public void remove(final java.lang.String path);

    /**
     * @see org.apache.portals.graffito.jcr.persistence.PersistenceManager#getObject(java.lang.Class, java.lang.String) 
     */
    public Object getObject( final java.lang.String path);

    /**
     * @see org.apache.portals.graffito.jcr.persistence.PersistenceManager#getObjects(org.apache.portals.graffito.jcr.query.Query)
     */
    public Collection getObjects(final Query query);

}