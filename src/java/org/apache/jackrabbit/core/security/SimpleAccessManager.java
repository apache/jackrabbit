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
package org.apache.jackrabbit.core.security;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemId;
import org.apache.log4j.Logger;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.security.auth.Subject;

/**
 * <code>SimpleAccessManager</code> ...
 */
public class SimpleAccessManager implements AccessManager {

    private static Logger log = Logger.getLogger(SimpleAccessManager.class);

    /**
     * Subject whose access rights this AccessManager should reflect
     */
    protected final Subject subject;

    /**
     * hierarchy manager used for ACL-based access control model
     */
    protected final HierarchyManager hierMgr;

    protected final boolean system;
    protected final boolean anonymous;

    /**
     * Constructor
     *
     * @param subject
     * @param hierMgr
     */
    public SimpleAccessManager(Subject subject, HierarchyManager hierMgr) {
        this.subject = subject;
        this.hierMgr = hierMgr;
        anonymous = !subject.getPrincipals(AnonymousPrincipal.class).isEmpty();
        system = !subject.getPrincipals(SystemPrincipal.class).isEmpty();
    }

    //--------------------------------------------------------< AccessManager >
    /**
     * @see AccessManager#checkPermission(ItemId, int)
     */
    public void checkPermission(ItemId id, int permissions)
            throws AccessDeniedException, ItemNotFoundException,
            RepositoryException {
        if (system) {
            // system has always all permissions
            return;
        } else if (anonymous) {
            // anonymous is always denied WRITE premission
            if ((permissions & WRITE) == WRITE) {
                throw new AccessDeniedException();
            }
        }
        // @todo check permission based on principals
    }

    /**
     * @see AccessManager#isGranted(ItemId, int)
     */
    public boolean isGranted(ItemId id, int permissions)
            throws ItemNotFoundException, RepositoryException {
        if (system) {
            // system has always all permissions
            return true;
        } else if (anonymous) {
            // anonymous is always denied WRITE premission
            if ((permissions & WRITE) == WRITE) {
                return false;
            }
        }

        // @todo check permission based on principals
        return true;
    }
}
