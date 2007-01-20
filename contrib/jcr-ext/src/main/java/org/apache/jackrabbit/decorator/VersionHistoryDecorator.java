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
import org.apache.jackrabbit.decorator.DecoratingVersionIterator;
import org.apache.jackrabbit.decorator.VersionDecorator;

import javax.jcr.version.VersionHistory;
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionException;
import javax.jcr.Session;
import javax.jcr.RepositoryException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.AccessDeniedException;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 */
public class VersionHistoryDecorator extends NodeDecorator
        implements VersionHistory {

    protected final VersionHistory versionHistory;

    public VersionHistoryDecorator(DecoratorFactory factory,
                                   Session session,
                                   VersionHistory versionHistory) {
        super(factory, session, versionHistory);
        this.versionHistory = versionHistory;
    }

    /**
     * @inheritDoc
     */
    public String getVersionableUUID() throws RepositoryException {
        return versionHistory.getVersionableUUID();
    }

    /**
     * @inheritDoc
     */
    public Version getRootVersion() throws RepositoryException {
        Version version = versionHistory.getRootVersion();
        return factory.getVersionDecorator(session, version);
    }

    /**
     * @inheritDoc
     */
    public VersionIterator getAllVersions() throws RepositoryException {
        return new DecoratingVersionIterator(factory, session, versionHistory.getAllVersions());
    }

    /**
     * @inheritDoc
     */
    public Version getVersion(String versionName) throws VersionException, RepositoryException {
        Version version = versionHistory.getVersion(versionName);
        return factory.getVersionDecorator(session, version);
    }

    /**
     * @inheritDoc
     */
    public Version getVersionByLabel(String label) throws RepositoryException {
        Version version = versionHistory.getVersionByLabel(label);
        return factory.getVersionDecorator(session, version);
    }

    /**
     * @inheritDoc
     */
    public void addVersionLabel(String versionName,
                                String label,
                                boolean moveLabel) throws VersionException, RepositoryException {
        versionHistory.addVersionLabel(versionName, label, moveLabel);
    }

    /**
     * @inheritDoc
     */
    public void removeVersionLabel(String label) throws VersionException, RepositoryException {
        versionHistory.removeVersionLabel(label);
    }

    /**
     * @inheritDoc
     */
    public boolean hasVersionLabel(String label) throws RepositoryException {
        return versionHistory.hasVersionLabel(label);
    }

    /**
     * @inheritDoc
     */
    public boolean hasVersionLabel(Version version, String label)
            throws VersionException, RepositoryException {
        return versionHistory.hasVersionLabel(VersionDecorator.unwrap(version), label);
    }

    /**
     * @inheritDoc
     */
    public String[] getVersionLabels() throws RepositoryException {
        return versionHistory.getVersionLabels();
    }

    /**
     * @inheritDoc
     */
    public String[] getVersionLabels(Version version)
            throws VersionException, RepositoryException {
        return versionHistory.getVersionLabels(VersionDecorator.unwrap(version));
    }

    /**
     * @inheritDoc
     */
    public void removeVersion(String versionName)
            throws ReferentialIntegrityException, AccessDeniedException,
            UnsupportedRepositoryOperationException, VersionException,
            RepositoryException {
        versionHistory.removeVersion(versionName);
    }

}
