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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.core.QName;

/**
 * The <code>NodeTypeRegistryListener</code> interface allows an implementing
 * object to be informed about node type (un)registration.
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.2 $, $Date: 2004/08/02 16:19:45 $
 * @see NodeTypeRegistry#addListener(NodeTypeRegistryListener)
 * @see NodeTypeRegistry#removeListener(NodeTypeRegistryListener)
 */
public interface NodeTypeRegistryListener {

    /**
     * Called when a node type has been registered.
     *
     * @param ntName name of the node type that has been registered
     */
    public void nodeTypeRegistered(QName ntName);

    /**
     * Called when a node type has been deregistered.
     *
     * @param ntName name of the node type that has been unregistered
     */
    public void nodeTypeUnregistered(QName ntName);
}
