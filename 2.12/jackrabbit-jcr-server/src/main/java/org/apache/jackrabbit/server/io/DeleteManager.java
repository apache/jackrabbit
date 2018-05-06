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
 * The DeleteManager handles DELETE operation by delegating it to its handlers. It also provides a way
 * to register {@link org.apache.jackrabbit.server.io.DeleteHandler} within it. Implementers of this interface
 * must invoke the registered delete handlers appropriately when a DELETE operation is to be performed
 */
public interface DeleteManager {

    /**
     * Delegates the delete operation to the fist handler that accepts it.
     *
     * @param deleteContext The context associated with the DELETE operation
     * @param resource The resource to be deleted
     * @return {@code true} if this instance successfully executed the delete operation with the given parameters;
     *         {@code false} otherwise.
     * @throws DavException If an error occurs.
     */
    public boolean delete(DeleteContext deleteContext, DavResource resource) throws DavException;

    /**
     * Registers a delete handler
     *
     * @param deleteHandler Registers a delete handler with this delete manager
     */
    public void addDeleteHandler(DeleteHandler deleteHandler);

    /**
     * Returns the registered delete handlers
     *
     * @return An array of all the registered delete handlers.
     */
    public DeleteHandler[] getDeleteHandlers();
}
