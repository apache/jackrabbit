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
package org.apache.jackrabbit.core.integration.random.operation;

import javax.jcr.Session;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.Version;
import java.util.List;
import java.util.ArrayList;

/**
 * <code>VersionOperation</code> is a base class for all version operations.
 */
public abstract class VersionOperation extends Operation {

    public VersionOperation(Session s, String path) {
        super(s, path);
    }

    /**
     * Returns a randomly chosen version for the current node or
     * <code>null</code> if the current node only has a root version.
     *
     * @param excludeReferenced exclude versions that are still referenced.
     * @return randomly chosen version or <code>null</code>.
     * @throws RepositoryException if an error occurs while reading from the
     *                             repository.
     */
    protected Version getRandomVersion(boolean excludeReferenced) throws RepositoryException {
        List allVersions = new ArrayList();
        Node n = getNode();
        for (VersionIterator it = n.getVersionHistory().getAllVersions(); it.hasNext(); ) {
            Version v = it.nextVersion();
            if (excludeReferenced) {
                // quick check if it is the base version
                if (n.getBaseVersion().isSame(v)) {
                    continue;
                }
            }
            if (v.getPredecessors().length > 0) {
                if (!excludeReferenced || !v.getReferences().hasNext()) {
                    allVersions.add(v);
                }
            }
        }
        if (allVersions.size() > 0) {
            return (Version) allVersions.get(getRandom().nextInt(allVersions.size()));
        } else {
            return null;
        }
    }
}
