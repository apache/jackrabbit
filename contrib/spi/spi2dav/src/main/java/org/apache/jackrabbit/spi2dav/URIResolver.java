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

import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.spi.SessionInfo;

import javax.jcr.RepositoryException;

/**
 * <code>URIResolver</code> used to build HTTP compliant request URIs from
 * a given ItemId.
 * This includes:
 * <ul>
 * <li>converting qualified names consisting of {uri}localName to URI compliant jcr names</li>
 * <li>adding trailing base (repository) uri and workspace name in order to form the proper url</li>
 * </ul>
 */
// todo: namespace resolver needed for the remapping. potential consistency problems with NamespaceRegistryImpl in jcr2spi layer?
interface URIResolver extends NamespaceResolver {

    Path getQPath(String uri, SessionInfo sessionInfo) throws RepositoryException;

    NodeId getNodeId(NodeId parentId, MultiStatusResponse response) throws RepositoryException;

    NodeId getNodeId(String uri, SessionInfo sessionInfo) throws RepositoryException;

    PropertyId getPropertyId(NodeId parentId, MultiStatusResponse response) throws RepositoryException;

    PropertyId getPropertyId(String uri, SessionInfo sessionInfo) throws RepositoryException;
}