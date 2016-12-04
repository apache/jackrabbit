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
 * <code>PropertyId</code> identifies a property on the SPI layer.
 */
public interface PropertyId extends ItemId {

    /**
     * Returns the <code>NodeId</code> of the parent.
     *
     * @return The {@link NodeId parentId} of this <code>PropertyId</code>.
     */
    public NodeId getParentId();

    /**
     * Returns the {@link Name} of the property identified by this id.
     *
     * @return The name of the property that is identified by this <code>PropertyId</code>.
     */
    public Name getName();
}