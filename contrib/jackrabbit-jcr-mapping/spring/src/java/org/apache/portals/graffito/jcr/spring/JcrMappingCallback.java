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

import org.apache.portals.graffito.jcr.exception.JcrMappingException;
import org.apache.portals.graffito.jcr.persistence.PersistenceManager;

/**
 * Callback interface for Jcr mapping code. To be used with JcrMappingTemplate's execute method, 
 * assumably often as anonymous classes within a method implementation. The typical 
 * implementation will call PersistenceManager.get/insert/remove/update to perform some operations on 
 * the repository.
 * 
 * @author Costin Leau
 *
 */
public interface JcrMappingCallback {

    /**
     * Called by {@link JcrMappingTemplate#execute} within an active PersistenceManager
     * {@link org.apache.graffito.jcr.mapper.persistence.PersistenceManager}. 
     * It is not responsible for logging out of the <code>Session</code> or handling transactions.
     *
     * Allows for returning a result object created within the
     * callback, i.e. a domain object or a collection of domain
     * objects. A thrown {@link RuntimeException} is treated as an
     * application exeception; it is propagated to the caller of the
     * template.
     */
    public Object doInJcrMapping(PersistenceManager manager) throws JcrMappingException;
}
