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
package org.apache.jackrabbit.spi.commons.namespace;

/**
 * Receives notifications when a namespace mapping changes.
 *
 * @deprecated https://issues.apache.org/jira/browse/JCR-1700
 */
public interface NamespaceListener {

    /**
     * Notifies the listeners that an existing namespace <code>uri</code> has
     * been re-mapped from <code>oldPrefix</code> to <code>newPrefix</code>.
     *
     * @param oldPrefix the old prefix.
     * @param newPrefix the new prefix.
     * @param uri       the associated namespace uri.
     */
    public void namespaceRemapped(String oldPrefix, String newPrefix, String uri);

    /**
     * Notifies the listeners that a new namespace <code>uri</code> has been
     * added and mapped to <code>prefix</code>.
     *
     * @param prefix the prefix.
     * @param uri    the namespace uri.
     */
    public void namespaceAdded(String prefix, String uri);

    /**
     * Notifies the listeners that the namespace with the given uri has been
     * unregistered.
     *
     * @param uri    the namespace uri.
     */
    public void namespaceRemoved(String uri);
}
