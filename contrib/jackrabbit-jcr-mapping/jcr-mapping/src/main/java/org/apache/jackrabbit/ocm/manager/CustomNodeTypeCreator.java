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
package org.apache.jackrabbit.ocm.manager;

import org.apache.jackrabbit.ocm.exception.CustomNodeTypeCreationException;
import org.apache.jackrabbit.ocm.manager.impl.PersistenceManagerImpl;

/** Interface for custom node type creator implementations.
 *
 * @author <a href="mailto:okiessler@apache.org">Oliver Kiessler</a>
 * @version $Id: Exp $
 */
public interface CustomNodeTypeCreator {

    /** This method is supposed to create custom node types on repository
     * setup.
     * 
     * @throws org.apache.jackrabbit.ocm.exception.CustomNodeTypeCreationException 
     * @return true/false True if custom node type creation succeeded 
     */
    boolean createInitialJcrCustomNodeTypes() throws CustomNodeTypeCreationException;

    /** Method to add a jcr custom node type to an existing jcr repository.
     * 
     * @throws org.apache.jackrabbit.ocm.exception.CustomNodeTypeCreationException 
     * @return true/false True if custom node type creation succeeded
     */
    boolean addJcrCustomNodeType() throws CustomNodeTypeCreationException;
    
    /** Jcr session to be injected into implementation.
     * @param jcrSession JcrSession
     */
    void setJcrSession(PersistenceManagerImpl jcrSession);
}
