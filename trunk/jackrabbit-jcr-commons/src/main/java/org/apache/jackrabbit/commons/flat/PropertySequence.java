/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.jackrabbit.commons.flat;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * Extension of {@link Sequence Sequence&lt;Property>} which provides methods
 * for adding and removing properties by key.
 */
public interface PropertySequence extends Sequence<Property> {

    /**
     * Add a property with the given <code>key</code> and <code>value</code>.
     *
     * @param key key of the property to add
     * @param value value of the property to add
     * @return the newly added property
     * @throws RepositoryException
     */
    Property addProperty(String key, Value value) throws RepositoryException;

    /**
     * Remove the property with the given key.
     *
     * @param key The key of the property to remove
     * @throws RepositoryException If there is no property with such a key or
     *             another error occurs.
     */
    void removeProperty(String key) throws RepositoryException;
}