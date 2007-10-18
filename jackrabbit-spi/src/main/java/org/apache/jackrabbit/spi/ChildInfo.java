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
 * <code>ChildInfo</code>...
 */
public interface ChildInfo {

    /**
     * Returns the name of the child <code>Node</code>.
     *
     * @return The name of the child <code>Node</code>.
     */
    public Name getName();

    /**
     * Returns the uniqueID of the child <code>Node</code> or <code>null</code>
     * if the Node is not identified by a uniqueID.
     *
     * @return The uniqueID of the child <code>Node</code> or <code>null</code>.
     * @see ItemId ItemId for a description of the uniqueID defined by the SPI
     * item identifiers.
     */
    public String getUniqueID();

    /**
     * Returns the index of the child <code>Node</code>. Note, that the index
     * is 1-based. In other words: the <code>Node</code> represented
     * by this <code>ChildInfo</code> has same name siblings this method will
     * always return the default value (1).
     *
     * @return Returns the index of the child <code>Node</code>.
     */
    public int getIndex();
}