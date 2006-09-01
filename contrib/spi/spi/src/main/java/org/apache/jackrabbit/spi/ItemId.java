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
package org.apache.jackrabbit.spi;

import org.apache.jackrabbit.name.Path;

/**
 * An <code>ItemId</code> identifies an item using a combination of UUID and
 * relative path. There are three basic forms of an ItemId. The following
 * table shows each of the allowed combinations where an <b>X</b> in
 * the column indicates that a value is set and a <b>-</b> indicates
 * that the value is <code>null</code>:
 * <table>
 * <tr><th>UUID</th><th>relative Path</th><th>Usage</th></tr>
 * <tr><td align="center"><b>X</b></td><td align="center"><b>-</b></td>
 *   <td>The item can be identified with a UUID. In most cases such an item
 *   is also mix:referenceable but there is no restriction in that respect. An
 *   SPI implementation may also use a UUID to identify non-referenceable nodes.
 *   Whether a node is referenceable is purely governed by its node type or
 *   the assigned mixin types.</td></tr>
 * <tr><td align="center"><b>-</b></td><td align="center"><b>X</b></td>
 *   <td>The item can not be identified with a UUID and none of its ancestors
 *   can be identified with a UUID. The item is identified by a relative path
 *   starting from the root node of the workspace as if calling
 *   {@link javax.jcr.Session#getRootNode() Session.getRootNode()}{@link
 *   javax.jcr.Node#getNode(String) .getNode(relPath)}.
 *   </td></tr>
 * <tr><td align="center"><b>X</b></td><td align="center"><b>X</b></td>
 *   <td>The item can not be identified with a UUID but one of its ancestors
 *   can. {@link #getUUID} returns the UUID of the nearest ancestor, which
 *   can be identified with a UUID. The relative path provides a navigation
 *   path from the above mentioned ancestor to the item identified by the
 *   <code>ItemId</code>.
 *   </td></tr>
 * </table>
 */
public interface ItemId {

    /**
     * @return <code>true</code> if this <code>ItemId</code> identifies a node;
     *         otherwise <code>false</code>.
     */
    public boolean denotesNode();

    /**
     * @return the UUID part of this item id or <code>null</code> if the
     *         identified item nor any of its ancestors can be identified with a
     *         UUID.
     */
    public String getUUID();

    /**
     * @return the relative path part of this item id. Returns <code>null</code>
     *         if this item can be identified solely with a UUID.
     */
    public Path getRelativePath();
}
