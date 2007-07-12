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

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;

/**
 * The <code>ItemInfo</code> is the base interface of <code>{@link NodeInfo}</code>
 * and <code>{@link PropertyInfo}</code>.
 */
public interface ItemInfo {

    /**
     * Returns the id of the parent <code>NodeInfo</code>.
     *
     * @return the id of the parent item info.
     */
    public NodeId getParentId();

    /**
     * Returns the qualified representation of the item name. For the root node
     * expected return value is {@link QName#ROOT}.
     *
     * @return the qualified representation of the item name.
     */
    public QName getQName();

    /**
     * Returns true if this <code>ItemInfo</code> denotes a node, false otherwise.
     *
     * @return true if this <code>ItemInfo</code> denotes a node, false otherwise.
     */
    public boolean denotesNode();

    /**
     * Returns the <code>Path</code> of the item represented by this
     * <code>ItemInfo</code>.
     *
     * @return the <code>Path</code> of the item represented by this
     * item info.
     */
    public Path getPath();
}