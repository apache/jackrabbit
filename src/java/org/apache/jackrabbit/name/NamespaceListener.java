/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.name;

/**
 * Receives notifications when a namespace mapping changes.
 */
public interface NamespaceListener {

    /**
     * Notifies the listener that the namespace <code>uri</code> has been
     * re-mapped to the new <code>prefix</code>.
     *
     * @param prefix the new prefix for <code>uri</code>.
     * @param uri    the namespace uri.
     */
    public void prefixRemapped(String prefix, String uri);
}
