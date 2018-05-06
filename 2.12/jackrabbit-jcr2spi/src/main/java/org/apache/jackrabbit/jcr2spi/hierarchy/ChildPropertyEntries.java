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
package org.apache.jackrabbit.jcr2spi.hierarchy;

import org.apache.jackrabbit.spi.Name;

import java.util.Collection;

/**
 * <code>ChildPropertyEntries</code>...
 */
public interface ChildPropertyEntries {

    /**
     * Returns true if a property entry with the given name exists.
     *
     * @param propertyName
     * @return true if a property entry with the given name exists.
     */
    public boolean contains(Name propertyName);

    /**
     * Return the PropertyEntry with the given <code>Name</code> or
     * <code>null</code>.
     *
     * @param propertyName
     * @return
     */
    public PropertyEntry get(Name propertyName);

    /**
     * Returns an unmodifiable collection containing all <code>PropertyEntry</code>
     * objects present.
     *
     * @return Collection of all <code>PropertyEntry</code> objects present.
     */
    public Collection<PropertyEntry> getPropertyEntries();

    /**
     * Returns an unmodifiable collection containing all existing property names.
     *
     * @return Collection of <code>Name</code>
     */
    public Collection<Name> getPropertyNames();

    /**
     * Adds the new <code>PropertyEntry</code> to this <code>ChildPropertyEntries</code>.
     *
     * @param propertyEntry
     */
    public void add(PropertyEntry propertyEntry);

    /**
     * Adds all <code>PropertyEntry</code>s from the given collection to this
     * <code>ChildPropertyEntries</code>.
     *
     * @param propertyEntries
     */
    public void addAll(Collection<PropertyEntry> propertyEntries);

    /**
     * Remove the collection entry with the given <code>Name</code>.
     *
     * @param propertyEntry
     * @return true If this <code>ChildPropertyEntries</code> contained the
     * given entry. False otherwise.
     */
    public boolean remove(PropertyEntry propertyEntry);
}