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
package org.apache.jackrabbit.core.journal;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

/**
 * Factory interface for creating {@link Journal} instances. Used
 * to decouple the repository internals from the repository configuration
 * mechanism.
 */
public interface JournalFactory {

    /**
     * Creates, initializes, and returns a {@link Journal} instance
     * for use by the repository.
     *
     * @param resolver namespace resolver
     * @return initialized journal
     * @throws RepositoryException if the journal can not be created
     */
    Journal getJournal(NamespaceResolver resolver) throws RepositoryException;

}
