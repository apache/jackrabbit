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

import java.util.ArrayList;
import java.util.List;

public class DeleteManagerImpl implements DeleteManager {

    private static DeleteManager DEFAULT_MANAGER;

    private final List<DeleteHandler> deleteHandlers = new ArrayList<DeleteHandler>();

    /**
     * @see DeleteManager#delete(DeleteContext, DavResource)
     */
    public boolean delete(DeleteContext deleteContext, DavResource member) throws DavException {
        boolean success = false;
        DeleteHandler[] deleteHandlers = getDeleteHandlers();
        for (int i = 0; i < deleteHandlers.length && !success; i++) {
            DeleteHandler dh = deleteHandlers[i];
            if (dh.canDelete(deleteContext, member)) {
                success = dh.delete(deleteContext, member);
            }
        }
        return success;
    }

    /**
     * @see DeleteManager#addDeleteHandler(DeleteHandler)
     */
    public void addDeleteHandler(DeleteHandler deleteHandler) {
        if (deleteHandler == null) {
            throw new IllegalArgumentException("'null' is not a valid DeleteHandler.");
        }
        deleteHandlers.add(deleteHandler);

    }

    /**
     * @see DeleteManager#getDeleteHandlers()
     */
    public DeleteHandler[] getDeleteHandlers() {
        return deleteHandlers.toArray(new DeleteHandler[deleteHandlers.size()]);
    }

    /**
     * Returns this delete manager singleton
     */
    public static DeleteManager getDefaultManager() {
        if (DEFAULT_MANAGER == null) {
            DeleteManager manager = new DeleteManagerImpl();
            manager.addDeleteHandler(new DefaultHandler());
            DEFAULT_MANAGER = manager;
        }
        return DEFAULT_MANAGER;
    }

}
