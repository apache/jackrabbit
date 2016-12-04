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
package org.apache.jackrabbit.core.security.authorization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * <code>AccessControlModifications</code> is an unmodifiable collection of
 * modifications made to access control content allowing the
 * {@link AccessControlListener modification listeners} to keep caches up to date.
 */
public class AccessControlModifications<K> {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(AccessControlModifications.class);

    private final Map<K, Integer> modificationMap;

    /**
     * @param modificationMap Map specifying the access control modifications.
     * The keys allows to identify the <code>Node</code> that was modified by
     * the policy modifications. The values specifies the modification type,
     * which may be any of
     * <ul>
     * <li>{@link AccessControlObserver#POLICY_ADDED}</li>
     * <li>{@link AccessControlObserver#POLICY_MODIFIED}</li>
     * <li>{@link AccessControlObserver#POLICY_REMOVED}</li>
     * </ul>
     */
    public AccessControlModifications(Map<K, Integer> modificationMap) {
        this.modificationMap = Collections.unmodifiableMap(modificationMap);
    }

    /**
     * @return Set of <code>Node</code> identifiers or paths.
     */
    public Set<K> getNodeIdentifiers() {
        return modificationMap.keySet();
    }

    /**
     * @param identifier
     * @return The modification type for the specified "identifier". Note that
     * the object type of the identifier is independent specific. 
     */
    public Integer getType(K identifier) {
        return modificationMap.get(identifier);
    }
}