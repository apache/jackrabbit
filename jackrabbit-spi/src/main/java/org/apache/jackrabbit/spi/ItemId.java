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

/**
 * An <code>ItemId</code> identifies an item using a combination of unique ID
 * and path. There are three basic forms of an ItemId. The following
 * table shows each of the allowed combinations where an <b>X</b> in
 * the column indicates that a value is set and a <b>-</b> indicates
 * that the value is <code>null</code>:
 * <table><caption>allowed combinations of unique ID and path</caption>
 * <tr><th>UniqueID</th><th>Path</th><th>Usage</th></tr>
 * <tr style="vertical-align:top"><td style="text-align:center"><b>X</b></td><td style="text-align:center"><b>-</b></td>
 *   <td>The item can be identified with a unique ID. In most cases such an item
 *   is also mix:referenceable but there is no restriction in that respect. An
 *   SPI implementation may also use a unique ID to identify non-referenceable nodes.
 *   Whether a node is referenceable is purely governed by its node type or
 *   the assigned mixin types. Note, that the format of the ID it is left to the
 *   implementation.</td></tr>
 * <tr style="vertical-align:top"><td style="text-align:center"><b>-</b></td><td style="text-align:center"><b>X</b></td>
 *   <td>The item can not be identified with a unique ID and none of its ancestors
 *   can be identified with a unique ID. The item is identified by an absolute path.
 *   </td></tr>
 * <tr style="vertical-align:top"><td style="text-align:center"><b>X</b></td><td style="text-align:center"><b>X</b></td>
 *   <td>The item can not be identified with a unique ID but one of its ancestors
 *   can. {@link #getUniqueID} returns the unique ID of the nearest ancestor, which
 *   can be identified with a unique ID. The relative path provides a navigation
 *   path from the above mentioned ancestor to the item identified by the
 *   <code>ItemId</code>.
 *   </td></tr>
 * </table>
 * <p>
 * Two <code>ItemId</code>s should be considered equal if both the unique part
 * and the path part are equal AND if they denote the same
 * {@link #denotesNode() type} of <code>ItemId</code>.
 */
public interface ItemId {

    /**
     * @return <code>true</code> if this <code>ItemId</code> identifies a node;
     *         otherwise <code>false</code>.
     */
    public boolean denotesNode();

    /**
     * @return the uniqueID part of this item id or <code>null</code> if the
     *         identified item nor any of its ancestors can be identified with a
     *         uniqueID.
     */
    public String getUniqueID();

    /**
     * @return the path part of this item id. Returns <code>null</code>
     *         if this item can be identified solely with a uniqueID.
     */
    public Path getPath();
}
