/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.jcr.core;

import org.apache.log4j.Logger;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * A <code>SystemTicket</code> ...
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.1 $, $Date: 2004/08/25 16:44:47 $
 */
class SystemSession extends SessionImpl {

    private static Logger log = Logger.getLogger(SystemSession.class);

    private static final String SYSTEM_USER_ID = "system";

    /**
     * Package private constructor.
     *
     * @param rep
     * @param wspName
     */
    SystemSession(RepositoryImpl rep, String wspName)
	    throws RepositoryException {
	super(rep, SYSTEM_USER_ID, wspName);

	accessMgr = new SystemAccessManqager();
    }

    //--------------------------------------------------------< inner classes >
    private class SystemAccessManqager extends AccessManagerImpl {

	SystemAccessManqager() {
	    super(null, getHierarchyManager(), getNamespaceResolver());
	}

	/**
	 * @see AbstractAccessManager#getPermissions(String)
	 */
	public long getPermissions(String absPath)
		throws PathNotFoundException, RepositoryException {
	    return PermissionImpl.ALL_VALUES;
	}
    }
}
