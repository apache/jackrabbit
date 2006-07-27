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
package org.apache.jackrabbit.jcr2spi.state;

import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.name.QName;

/**
 * <code>ChildNodeEntry</code> specifies the name, index (in the case of
 * same-name siblings) and the UUID of a child node entry.
 */
public interface ChildNodeEntry {

    /**
     * @return the <code>NodeId</code> of this child node entry.
     */
    public NodeId getId();

    /**
     * @return the name of this child node entry.
     */
    public QName getName();

    /**
     * @return the index of this child node entry to suppport same-name siblings.
     */
    public int getIndex();
}
