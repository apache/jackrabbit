/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.core.config.WorkspaceConfig;

/**
 * <code>PersistenceManager</code> ...
 */
public interface PersistenceManager {

    /**
     * @param wspConfig
     * @throws Exception
     */
    public void init(WorkspaceConfig wspConfig) throws Exception;

    /**
     * @throws Exception
     */
    public void close() throws Exception;

    /**
     * @param state
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    public void load(PersistentNodeState state)
            throws NoSuchItemStateException, ItemStateException;

    /**
     * @param state
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    public void load(PersistentPropertyState state)
            throws NoSuchItemStateException, ItemStateException;

    /**
     * @param refs
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    public void load(NodeReferences refs)
            throws NoSuchItemStateException, ItemStateException;

    /**
     * @param state
     * @throws ItemStateException
     */
    public void store(PersistentNodeState state) throws ItemStateException;

    /**
     * @param state
     * @throws ItemStateException
     */
    public void store(PersistentPropertyState state) throws ItemStateException;

    /**
     * @param refs
     * @throws ItemStateException
     */
    public void store(NodeReferences refs) throws ItemStateException;

    /**
     * @param state
     * @throws ItemStateException
     */
    public void destroy(PersistentNodeState state) throws ItemStateException;

    /**
     * @param state
     * @throws ItemStateException
     */
    public void destroy(PersistentPropertyState state) throws ItemStateException;

    /**
     * @param refs
     * @throws ItemStateException
     */
    public void destroy(NodeReferences refs) throws ItemStateException;
}
