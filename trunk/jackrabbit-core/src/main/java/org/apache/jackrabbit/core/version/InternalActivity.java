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
package org.apache.jackrabbit.core.version;

import javax.jcr.RepositoryException;

/**
 * This interface defines the internal activity.
 */
public interface InternalActivity extends InternalVersionItem {

    /**
     * Returns the latest version of the given history that is referenced in this activity.
     * @param history the history
     * @return the version
     * @throws RepositoryException if an error occurs
     */
    InternalVersion getLatestVersion(InternalVersionHistory history)
            throws RepositoryException;

    /**
     * Returns the changeset of this activity.
     * This is the set of versions that are the latest members of this activity
     * in their respective version histories.
     *
     * @return the changeset
     * @throws RepositoryException if an error occurs
     */
    VersionSet getChangeSet() throws RepositoryException;

}