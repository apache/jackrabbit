/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.log4j.Logger;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;

/**
 * A <code>SystemTicket</code> ...
 */
class SystemSession extends SessionImpl {

    private static Logger log = Logger.getLogger(SystemSession.class);

    private static final String SYSTEM_USER_ID = "system";

    /**
     * Package private constructor.
     *
     * @param rep
     * @param wspConfig
     */
    SystemSession(RepositoryImpl rep, WorkspaceConfig wspConfig)
            throws RepositoryException {
        super(rep, SYSTEM_USER_ID, wspConfig);

        accessMgr = new SystemAccessManqager(hierMgr);
    }

    //--------------------------------------------------------< inner classes >
    private class SystemAccessManqager extends AccessManagerImpl {

        SystemAccessManqager(HierarchyManager hierMgr) {
            super(null, hierMgr);
        }

        //----------------------------------------------------< AccessManager >
        /**
         * @see AccessManager#checkPermission(ItemId, int)
         */
        public void checkPermission(ItemId id, int permissions)
                throws AccessDeniedException, ItemNotFoundException,
                RepositoryException {
            // allow everything
        }

        /**
         * @see AccessManager#isGranted(ItemId, int)
         */
        public boolean isGranted(ItemId id, int permissions)
                throws ItemNotFoundException, RepositoryException {
            // allow everything
            return true;
        }
    }
}
