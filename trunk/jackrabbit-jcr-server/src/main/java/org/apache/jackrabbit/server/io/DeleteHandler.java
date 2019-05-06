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

package org.apache.jackrabbit.server.io;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;

/**
 * The DeleteHandler is invoked when a webdav DELETE request is received. Implementers of this interface should plugin
 * their handling of DELETE request here
 */
public interface DeleteHandler {

    /**
     * Executes the delete operation with the given parameters.
     *
     * @param deleteContext The context of the delete.
     * @param resource The resource to be deleted
     * @return {@code true} if this instance successfully executed the delete operation with the given parameters;
     *         {@code false} otherwise.
     * @throws DavException If an error occurs.
     */
    public boolean delete(DeleteContext deleteContext, DavResource resource) throws DavException;


    /**
     * Validates if this handler is able to execute a delete operation with the given
     * parameters.
     *
     * @param deleteContext The context of the delete
     * @param resource The resource to be deleted
     * @return {@code true} if this instance can successfully execute the delete operation with the given parameters;
     *         {@code false} otherwise.
     */
    public boolean canDelete(DeleteContext deleteContext, DavResource resource);
}
