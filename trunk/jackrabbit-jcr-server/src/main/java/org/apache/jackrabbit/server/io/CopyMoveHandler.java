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
 * <code>CopyMoveHandler</code>...
 */
public interface CopyMoveHandler {

    /**
     * Validates if this handler is able to execute a copy with the given
     * parameters.
     *
     * @param context The context of the copy.
     * @param source The source of the copy.
     * @param destination The destination of the copy.
     * @return true if this instance can handle a copy with the given parameters;
     * false otherwise.
     */
    public boolean canCopy(CopyMoveContext context, DavResource source, DavResource destination);

    /**
     * Executes the copy with the given parameters.
     *
     * @param context The context of the copy.
     * @param source The source of the copy.
     * @param destination The destination of the copy.
     * @return true if this instance successfully executed the copy operation
     * with the given parameters; false otherwise.
     * @throws DavException If an error occurs.
     */
    public boolean copy(CopyMoveContext context, DavResource source, DavResource destination) throws DavException;

    /**
     * Validates if this handler is able to execute a move with the given
     * parameters.
     *
     * @param context The context of the move.
     * @param source The source of the move.
     * @param destination The destination of the move.
     * @return true if this instance successfully executed the move operation
     * with the given parameters; false otherwise.
     */
    public boolean canMove(CopyMoveContext context, DavResource source, DavResource destination);

    /**
     * Executes the move with the given parameters.
     *
     * @param context The context of the move.
     * @param source The source of the move.
     * @param destination The destination of the move.
     * @return true if this instance successfully executed the move operation
     * with the given parameters;
     * false otherwise.
     * @throws DavException If an error occurs.
     */
    public boolean move(CopyMoveContext context, DavResource source, DavResource destination) throws DavException;
}
