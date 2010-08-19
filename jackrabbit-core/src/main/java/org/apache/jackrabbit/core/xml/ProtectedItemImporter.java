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
import org.apache.jackrabbit.core.util.ReferenceChangeTracker;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

import javax.jcr.RepositoryException;

/**
 * <code>ProtectedItemImporter</code>...
 */
public interface ProtectedItemImporter {

    /**
     *
     * @param session
     * @param resolver
     * @param isWorkspaceImport
     * @param uuidBehavior
     * @param referenceTracker
     * @return
     */
    boolean init(JackrabbitSession session, NamePathResolver resolver,
                 boolean isWorkspaceImport, int uuidBehavior,
                 ReferenceChangeTracker referenceTracker);
        
    /**
     * Post processing protected reference properties underneath a protected
     * or non-protected parent node. If the parent is protected it has been
     * handled by this importer already. This method is called
     * from {@link org.apache.jackrabbit.core.xml.Importer#end()}.
     *
     * @throws javax.jcr.RepositoryException If an error occurs.
     */
    void processReferences() throws RepositoryException;
}