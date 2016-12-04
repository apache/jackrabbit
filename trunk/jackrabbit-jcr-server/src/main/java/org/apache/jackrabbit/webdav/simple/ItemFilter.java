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
package org.apache.jackrabbit.webdav.simple;

import javax.jcr.Item;
import javax.jcr.Session;

/**
 * <code>ItemFilter</code>
 */
public interface ItemFilter {

    /**
     * Define the URIs that should be filtered out if present in the prefix
     * of an items name.
     *
     * @param uris
     */
    public void setFilteredURIs(String[] uris);

    /**
     * Define the namespace prefixes that should be filtered if present in
     * the prefix of an items name.
     *
     * @param prefixes
     */
    public void setFilteredPrefixes(String[] prefixes);

    /**
     * Set the nodetype names that should be used if a given item should be
     * filtered. Note that not the nodetype(s) defined for a given item
     * is relevant but rather the nodetype that defined the definition of the item.
     *
     * @param nodetypeNames
     */
    public void setFilteredNodetypes(String[] nodetypeNames);

    /**
     * Returns true if the given item should be filtered.
     *
     * @param item to be tested
     * @return true if the given item should be filtered.
     */
    public boolean isFilteredItem(Item item);

    /**
     * Returns true if the resouce with the given name should be filtered.
     *
     * @param name to be tested for a filtered namespace prefix
     * @param session used for looking up namespace mappings
     * @return true if the given resource should be filtered.
     */
    public boolean isFilteredItem(String name, Session session);
}