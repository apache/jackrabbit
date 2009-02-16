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
package org.apache.jackrabbit.core.util;

import javax.jcr.RepositoryException;

/**
 * Factory interface for creating {@link RepositoryLockMechanism} instances. Used
 * to decouple the repository internals from the repository configuration
 * mechanism.
 *
 * @since Jackrabbit 1.5
 * @see <a href="https://issues.apache.org/jira/browse/JCR-1438">JCR-1438</a>
 */
public interface RepositoryLockMechanismFactory {

    /**
     * Creates, configures, and returns a {@link RepositoryLockMechanism} instance
     * for use by the repository. Note that no information is passed from
     * the client, so all required configuration information must be
     * encapsulated in the factory.
     *
     * @return the configures repository lock mechanism
     * @throws RepositoryException if the repository lock mechanism can not be created
     */
    RepositoryLockMechanism getRepositoryLockMechanism() throws RepositoryException;
}
