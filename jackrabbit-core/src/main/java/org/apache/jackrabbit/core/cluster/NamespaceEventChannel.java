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
package org.apache.jackrabbit.core.cluster;

/**
 * Event channel used to transmit namespace registry operations.
 */
public interface NamespaceEventChannel {

    /**
     * Called when a namespace has been remapped.
     *
     * @param oldPrefix old prefix. if <code>null</code> this is a fresh mapping
     * @param newPrefix new prefix. if <code>null</code> this is an unmap operation
     * @param uri uri to map prefix to
     */
    void remapped(String oldPrefix, String newPrefix, String uri);

    /**
     * Set listener that will receive information about incoming, external namespace events.
     *
     * @param listener namespace event listener
     */
    void setListener(NamespaceEventListener listener);

}
