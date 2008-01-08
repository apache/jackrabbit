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
package org.apache.jackrabbit.name;

/**
 * The name cache defines an interface that is used by the {@link NameFormat}
 * and is usually implemented by {@link NamespaceResolver}s.
 * <p/>
 * Please note, that the redundant naming of the methods is intentionally in
 * order to allow a class to implement several caches.
 *
 * @deprecated
 */
public interface NameCache {

    /**
     * Retrieves a qualified name from the cache for the given jcr name. If the
     * name is not cached <code>null</code> is returned.
     *
     * @param jcrName the jcr name
     * @return the qualified name or <code>null</code>
     */
    public QName retrieveName(String jcrName);

    /**
     * Retrieves a jcr name from the cache for the given qualified name. If the
     * name is not cached <code>null</code> is returned.
     *
     * @param name the qualified name
     * @return the jcr name or <code>null</code>
     */
    public String retrieveName(QName name);

    /**
     * Puts a name into the cache.
     *
     * @param jcrName the jcr name
     * @param name the qualified name
     */
    public void cacheName(String jcrName, QName name);

    /**
     * Evicts all names from the cache, i.e. clears it.
     */
    public void evictAllNames();

}