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
 * <code>IdFactory</code> defines methods to construct new <code>ItemId</code>s.
 * This factory is intended to build <code>ItemId</code>s from the parameters
 * passed to the various create methods and should not make an attempt to
 * apply additional logic such as e.g. roundtrips to the server or resolution of
 * <code>Path</code>s. Similarly the SPI implementation namely the
 * {@link RepositoryService} must be able to deal with the various formats of
 * an <code>ItemId</code>, since a caller may not (yet) be aware of the uniqueID
 * part of an ItemId.
 */
public interface IdFactory {

    /**
     * Creates a new <code>PropertyId</code> from the given parent id and
     * property name.
     *
     * @param parentId
     * @param propertyName
     * @return a new <code>PropertyId</code>.
     */
    public PropertyId createPropertyId(NodeId parentId, Name propertyName);

    /**
     * Creates a new <code>NodeId</code> from the given parent id and
     * the given <code>Path</code> object.
     *
     * @param parentId
     * @param path
     * @return a new <code>NodeId</code>.
     */
    public NodeId createNodeId(NodeId parentId, Path path);

    /**
     * Creates a new <code>NodeId</code> from the given unique id (which identifies
     * an ancestor <code>Node</code>) and the given <code>Path</code> object.
     *
     * @param uniqueID
     * @param path
     * @return a new <code>NodeId</code>.
     * @see ItemId ItemId for a description of the uniqueID defined by the SPI
     * item identifiers.
     */
    public NodeId createNodeId(String uniqueID, Path path);

    /**
     * Creates a new <code>NodeId</code> from the given unique id.
     *
     * @param uniqueID
     * @return a new <code>NodeId</code>.
     * @see ItemId ItemId for a description of the uniqueID defined by the SPI
     * item identifiers.
     */
    public NodeId createNodeId(String uniqueID);    

    /**
     * Returns the JCR string representation of the given <code>nodeId</code>.
     *
     * @return a JCR node identifier string.
     * @see #fromJcrIdentifier(String)
     */
    public String toJcrIdentifier(NodeId nodeId);

    /**
     * Create a new <code>NodeId</code> from the given JCR string representation.
     *
     * @param jcrIdentifier
     * @return a new <code>NodeId</code>.
     * @see #toJcrIdentifier(NodeId)
     */
    public NodeId fromJcrIdentifier(String jcrIdentifier);
}