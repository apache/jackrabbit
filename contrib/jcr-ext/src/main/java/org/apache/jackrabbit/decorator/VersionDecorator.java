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
package org.apache.jackrabbit.decorator;

import org.apache.jackrabbit.decorator.NodeDecorator;
import org.apache.jackrabbit.decorator.DecoratorFactory;

import javax.jcr.Session;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import java.util.Calendar;

/**
 */
public class VersionDecorator extends NodeDecorator implements Version {

    protected final Version version;

    public VersionDecorator(DecoratorFactory factory,
                            Session session,
                            Version version) {
        super(factory, session, version);
        this.version = version;
    }

    /**
     * Returns the underlying <code>Version</code> of the <code>version</code>
     * that decorates it. Unwrapping <code>null</code> returns <code>null</code>.
     *
     * @param version decorates the underlying version.
     * @return the underlying version.
     * @throws IllegalStateException if <code>version</code> is not of type
     *                               {@link VersionDecorator}.
     */
    public static Version unwrap(Version version) {
        if (version == null) {
            return null;
        }
        if (version instanceof VersionDecorator) {
            version = (Version) ((VersionDecorator) version).unwrap();
        } else {
            throw new IllegalStateException("version is not of type VersionDecorator");
        }
        return version;
    }

    /**
     * @inheritDoc
     */
    public VersionHistory getContainingHistory() throws RepositoryException {
        VersionHistory vHistory = version.getContainingHistory();
        return factory.getVersionHistoryDecorator(session, vHistory);
    }

    /**
     * @inheritDoc
     */
    public Calendar getCreated() throws RepositoryException {
        return version.getCreated();
    }

    /**
     * @inheritDoc
     */
    public Version[] getSuccessors() throws RepositoryException {
        Version[] successors = version.getSuccessors();
        for (int i = 0; i < successors.length; i++) {
            successors[i] = factory.getVersionDecorator(session, successors[i]);
        }
        return successors;
    }

    /**
     * @inheritDoc
     */
    public Version[] getPredecessors() throws RepositoryException {
        Version[] predecessors = version.getPredecessors();
        for (int i = 0; i < predecessors.length; i++) {
            predecessors[i] = factory.getVersionDecorator(session, predecessors[i]);
        }
        return predecessors;
    }
}
