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

import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.SessionInfo;

import javax.jcr.RepositoryException;

/**
 * <code>URIResolver</code> used to build ItemIds from URIs.
 */
interface URIResolver {

    Path getQPath(String uri, SessionInfo sessionInfo) throws RepositoryException;

    NodeId getNodeId(String uri, SessionInfo sessionInfo) throws RepositoryException;

    NodeId getNodeIdAfterEvent(String uri, SessionInfo sessionInfo, boolean nodeIsGone) throws RepositoryException;

    PropertyId getPropertyId(String uri, SessionInfo sessionInfo) throws RepositoryException;
}