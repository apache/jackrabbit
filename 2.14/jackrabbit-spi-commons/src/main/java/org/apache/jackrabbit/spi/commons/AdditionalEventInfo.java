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
package org.apache.jackrabbit.spi.commons;

import java.util.Set;

import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.Name;

/**
 * Provides additional information for an {@link Event}.
 */
public interface AdditionalEventInfo {

	/**
	 * @return the name of the primary node type of the node associated with the event
	 */
    public Name getPrimaryNodeTypeName() throws UnsupportedRepositoryOperationException;

	/**
	 * @return the names of the mixin node types of the node associated with the event
	 */
    public Set<Name> getMixinTypeNames() throws UnsupportedRepositoryOperationException;

    /**
     * @return the specified Session attribute
     */
    public Object getSessionAttribute(String name) throws UnsupportedRepositoryOperationException;
}
