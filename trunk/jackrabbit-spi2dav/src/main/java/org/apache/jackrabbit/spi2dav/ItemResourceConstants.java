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
package org.apache.jackrabbit.spi2dav;

import org.apache.jackrabbit.commons.webdav.JcrRemotingConstants;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.xml.Namespace;

/**
 * <code>ItemResourceConstants</code> provides constants for any resources
 * representing repository items.
 */
public interface ItemResourceConstants extends JcrRemotingConstants {

    /**
     * The namespace for all jcr specific extensions.
     */
    public static final Namespace NAMESPACE = Namespace.getNamespace(NS_PREFIX, NS_URI);

    /**
     * Extension to the WebDAV 'exclusive' lock, that allows to distinguish
     * the session-scoped and open-scoped locks on a JCR node.
     *
     * @see javax.jcr.Node#lock(boolean, boolean)
     */
    public static final Scope EXCLUSIVE_SESSION = Scope.create(XML_EXCLUSIVE_SESSION_SCOPED, NAMESPACE);
}