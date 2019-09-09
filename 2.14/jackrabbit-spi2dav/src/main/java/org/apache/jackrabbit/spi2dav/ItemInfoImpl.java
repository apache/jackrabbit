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
import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertySet;

import javax.jcr.NamespaceException;

/**
 * <code>ItemInfoImpl</code>...
 */
abstract class ItemInfoImpl implements ItemInfo {

    private final Path path;

    ItemInfoImpl(Path path) {
        this.path = path;
    }

    ItemInfoImpl(DavPropertySet propSet, NamePathResolver resolver)
            throws NameException, NamespaceException {

        DavProperty<?> pathProp = propSet.get(JcrRemotingConstants.JCR_PATH_LN, ItemResourceConstants.NAMESPACE);
        String jcrPath = pathProp.getValue().toString();
        path = resolver.getQPath(jcrPath);
    }

    /**
     * @see ItemInfo#getPath() 
     */
    public Path getPath() {
        return path;
    }
}
