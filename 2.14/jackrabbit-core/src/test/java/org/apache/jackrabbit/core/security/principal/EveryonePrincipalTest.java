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
package org.apache.jackrabbit.core.security.principal;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.JackrabbitPrincipal;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import java.security.Principal;

/**
 * <code>EveryonePrincipalTest</code>...
 */
public class EveryonePrincipalTest extends AbstractJCRTest {

    public void testEveryonePrincipal() {
        Principal everyone = EveryonePrincipal.getInstance();

        assertEquals(EveryonePrincipal.NAME, everyone.getName());
        assertEquals(everyone, EveryonePrincipal.getInstance());
    }
    
    public void testEveryonePrincipal2() {
        Principal everyone = EveryonePrincipal.getInstance();

        Principal someotherEveryone = new Principal() {
            public String getName() {
                return EveryonePrincipal.NAME;
            }
        };

        assertFalse(everyone.equals(someotherEveryone));
    }

    public void testEveryonePrincipal3() {
        Principal everyone = EveryonePrincipal.getInstance();

        Principal someotherEveryone = new JackrabbitPrincipal() {
            public String getName() {
                return EveryonePrincipal.NAME;
            }
            @Override
            public boolean equals(Object o) {
                if (o instanceof JackrabbitPrincipal) {
                    return getName().equals(((JackrabbitPrincipal) o).getName());
                }
                return false;
            }
            @Override
            public int hashCode() {
                return getName().hashCode();
            }
        };

        assertEquals(someotherEveryone, everyone);
        assertEquals(everyone, someotherEveryone);
    }

    public void testEveryonePrincipal4() throws NotExecutableException, RepositoryException {
        Principal everyone = EveryonePrincipal.getInstance();

        Group everyoneGroup = null;
        try {
            everyoneGroup = getUserManager(superuser).createGroup(EveryonePrincipal.NAME);
            superuser.save();

            assertEquals(everyoneGroup.getPrincipal(), everyone);
            assertEquals(everyone, everyoneGroup.getPrincipal());

        } finally {
            if (everyoneGroup != null) {
                everyoneGroup.remove();
                superuser.save();
            }
        }
    }

    private static UserManager getUserManager(Session session) throws RepositoryException, NotExecutableException {
        if (!(session instanceof JackrabbitSession)) {
            throw new NotExecutableException();
        }
        try {
            return ((JackrabbitSession) session).getUserManager();
        } catch (UnsupportedRepositoryOperationException e) {
            throw new NotExecutableException(e.getMessage());
        } catch (UnsupportedOperationException e) {
            throw new NotExecutableException(e.getMessage());
        }
    }
}