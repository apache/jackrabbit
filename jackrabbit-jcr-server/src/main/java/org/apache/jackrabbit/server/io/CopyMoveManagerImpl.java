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

import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;

/**
 * <code>CopyMoveManagerImpl</code>...
 */
public class CopyMoveManagerImpl implements CopyMoveManager {

    private static CopyMoveManager DEFAULT_MANAGER;

    private final List<CopyMoveHandler> copyMoveHandlers = new ArrayList<CopyMoveHandler>();

    /**
     * Create a new <code>CopyMoveManagerImpl</code>.
     */
    public CopyMoveManagerImpl() {
    }

    //----------------------------------------------------< CopyMoveManager >---
    /**
     * @see CopyMoveManager#copy(CopyMoveContext,org.apache.jackrabbit.webdav.DavResource,org.apache.jackrabbit.webdav.DavResource)
     */
    public boolean copy(CopyMoveContext context, DavResource source, DavResource destination) throws DavException {
        boolean success = false;
        CopyMoveHandler[] copyMoveHandlers = getCopyMoveHandlers();
        for (int i = 0; i < copyMoveHandlers.length && !success; i++) {
            CopyMoveHandler cmh = copyMoveHandlers[i];
            if (cmh.canCopy(context, source, destination)) {
                success = cmh.copy(context, source, destination);
            }
        }
        return success;
    }

    /**
     * @see CopyMoveManager#move(CopyMoveContext,org.apache.jackrabbit.webdav.DavResource,org.apache.jackrabbit.webdav.DavResource)
     */
    public boolean move(CopyMoveContext context, DavResource source, DavResource destination) throws DavException {
        boolean success = false;
        CopyMoveHandler[] copyMoveHandlers = getCopyMoveHandlers();
        for (int i = 0; i < copyMoveHandlers.length && !success; i++) {
            CopyMoveHandler cmh = copyMoveHandlers[i];
            if (cmh.canMove(context, source, destination)) {
                success = cmh.move(context, source, destination);
            }
        }
        return success;
    }

    /**
     * @see org.apache.jackrabbit.server.io.CopyMoveManager#addCopyMoveHandler(CopyMoveHandler)
     */
    public void addCopyMoveHandler(CopyMoveHandler copyMoveHandler) {
        if (copyMoveHandler == null) {
            throw new IllegalArgumentException("'null' is not a valid copyMoveHandler.");
        }
        copyMoveHandlers.add(copyMoveHandler);
    }

    /**
     * @see CopyMoveManager#getCopyMoveHandlers()
     */
    public CopyMoveHandler[] getCopyMoveHandlers() {
        return copyMoveHandlers.toArray(new CopyMoveHandler[copyMoveHandlers.size()]);
    }

    //--------------------------------------------------------------------------
    /**
     * @return an instance of CopyMoveManager populated with default handlers.
     */
    public static CopyMoveManager getDefaultManager() {
        if (DEFAULT_MANAGER == null) {
            CopyMoveManager manager = new CopyMoveManagerImpl();
            manager.addCopyMoveHandler(new DefaultHandler());
            DEFAULT_MANAGER = manager;
        }
        return DEFAULT_MANAGER;
    }
}
