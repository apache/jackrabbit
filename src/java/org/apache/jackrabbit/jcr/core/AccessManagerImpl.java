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

import javax.jcr.Credentials;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.access.AccessManager;

/**
 * <code>AccessManagerImpl</code> ...
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.12 $, $Date: 2004/08/24 09:30:54 $
 */
public class AccessManagerImpl extends AbstractAccessManager {

    private static Logger log = Logger.getLogger(AccessManagerImpl.class);

    protected final HierarchyManager hierMgr;
    protected final NamespaceResolver nsResolver;

    /**
     * Package private constructor
     *
     * @param credentials
     * @param hierMgr
     * @param nsReg
     */
    AccessManagerImpl(Credentials credentials, HierarchyManager hierMgr, NamespaceResolver nsReg) {
	this.hierMgr = hierMgr;
	this.nsResolver = nsReg;
    }

    /**
     * @param id
     * @param permissions
     * @return
     */
    public boolean isGranted(ItemId id, long permissions) throws ItemNotFoundException, RepositoryException {
	return (getPermissions(id) & permissions) == permissions;
    }

    /**
     * @param id
     * @return
     */
    public long getPermissions(ItemId id) throws ItemNotFoundException, RepositoryException {
	// @todo implement resource-based access control

	return PermissionImpl.ALL_VALUES;
    }

    //--------------------------------------------------------< AccessManager >
    /**
     * @see AccessManager#getPermissions(String)
     */
    public long getPermissions(String absPath) throws PathNotFoundException, RepositoryException {
	try {
	    return getPermissions(hierMgr.resolvePath(Path.create(absPath, nsResolver, true)));
	} catch (MalformedPathException mpe) {
	    String msg = "failed to check permissions for " + absPath;
	    log.warn(msg, mpe);
	    throw new RepositoryException(msg, mpe);
	}
    }
}
