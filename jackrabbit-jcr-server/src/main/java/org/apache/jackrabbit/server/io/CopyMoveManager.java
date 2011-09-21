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
 *  <code>CopyMoveManager</code>...
 */
public interface CopyMoveManager {

    /**
     * Handles the copy command
     *
     * @param context The context used for this copy operation.
     * @param source The source of the copy.
     * @param destination The destination of the copy.
     * @return true if the copy succeeded.
     * @throws DavException If an error occurs.
     */
    public boolean copy(CopyMoveContext context, DavResource source, DavResource destination) throws DavException;

    /**
     * Handles the move command
     *
     * @param context The context used for this move operation.
     * @param source The source of the move.
     * @param destination The destination of the move.
     * @return true if the move succeeded.
     * @throws DavException If an error occurs.
     */
    public boolean move(CopyMoveContext context, DavResource source, DavResource destination) throws DavException;

    /**
     * Adds the specified handler to the list of handlers.
     *
     * @param copyMoveHandler handler to be added
     */
    public void addCopyMoveHandler(CopyMoveHandler copyMoveHandler);

    /**
     * Returns all handlers that have been added to this manager.
     *
     * @return Array of all handlers
     */
    public CopyMoveHandler[] getCopyMoveHandlers();
}
