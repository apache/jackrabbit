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
package org.apache.jackrabbit.commons.repository;

import javax.jcr.Credentials;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.commons.AbstractRepository;

/**
 * An empty repository with no descriptors and no workspaces. This class
 * can be used as a dummy sentinel in cases where a proper content repository
 * is not available.
 *
 * @since 1.4
 */
public class EmptyRepository extends AbstractRepository {

    /**
     * Returns <code>null</code> since this repository contains no descriptors.
     *
     * @param key descriptor key
     * @return <code>null</code>
     */
    public String getDescriptor(String key) {
        return null;
    }

    /**
     * Returns <code>null</code> since this repository contains no descriptors.
     *
     * @param key descriptor key
     * @return <code>null</code>
     */
    public Value getDescriptorValue(String key) {
        return null;
    }

    /**
     * Returns <code>null</code> since this repository contains no descriptors.
     *
     * @param key descriptor key
     * @return <code>null</code>
     */
    public Value[] getDescriptorValues(String key) {
        return null;
    }

    /**
     * Returns <code>false</code> since this repository contains no descriptors.
     *
     * @param key descriptor key
     * @return <code>false</code>
     */
    public boolean isSingleValueDescriptor(String key) {
        return false;
    }

    /**
     * Returns an empty array since this repository contains no descriptors.
     *
     * @return empty array
     */
    public String[] getDescriptorKeys() {
        return new String[0];
    }

    /**
     * Throws an exception since this repository contains no workspaces.
     *
     * @return nothing
     * @throws NoSuchWorkspaceException always thrown
     */
    public Session login(Credentials credentials, String workspace)
            throws NoSuchWorkspaceException {
        throw new NoSuchWorkspaceException("Empty repository");
    }

}
