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
package org.apache.jackrabbit.core.security.user;

import org.apache.jackrabbit.api.security.user.AbstractUserTest;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;

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
