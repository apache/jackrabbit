/*
 * $Id$
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.jackrabbit.core.security.user;

import org.apache.jackrabbit.api.security.user.AbstractUserTest;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.api.security.user.Impersonation;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;

import javax.jcr.Session;
import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.AccessDeniedException;
import java.security.Principal;
import java.util.Iterator;

/**
 * <code>AdministratorTest</code>...
 */
public class AdministratorTest extends AbstractUserTest {

    public void testGetPrincipal() throws RepositoryException {
        Authorizable authr = userMgr.getAuthorizable(superuser.getUserID());
        assertNotNull(authr);
        assertFalse(authr.isGroup());
        assertTrue(authr.getPrincipal() instanceof AdminPrincipal);
    }

    public void testRemoveSelf() throws RepositoryException, NotExecutableException {
        Authorizable authr = userMgr.getAuthorizable(superuser.getUserID());
        if (authr == null) {
            throw new NotExecutableException();
        }
        try {
            authr.remove();
            fail("The Administrator should not be allowed to remove the own authorizable.");
        } catch (RepositoryException e) {
            // success
        }
    }
}
