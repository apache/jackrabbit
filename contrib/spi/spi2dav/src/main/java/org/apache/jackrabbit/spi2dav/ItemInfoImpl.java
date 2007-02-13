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

import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.PathFormat;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * <code>ItemInfoImpl</code>...
 */
abstract class ItemInfoImpl implements ItemInfo {

    private static Logger log = LoggerFactory.getLogger(ItemInfoImpl.class);

    private final NodeId parentId;
    private final Path path;

    public ItemInfoImpl(NodeId parentId, DavPropertySet propSet, NamespaceResolver nsResolver)
        throws MalformedPathException {
        // set parentId
        this.parentId = parentId;

        DavProperty pathProp = propSet.get(ItemResourceConstants.JCR_PATH);
        String jcrPath = pathProp.getValue().toString();
        path = PathFormat.parse(jcrPath, nsResolver);

    }

    public NodeId getParentId() {
        return parentId;
    }


    public Path getPath() {
        return path;
    }
}